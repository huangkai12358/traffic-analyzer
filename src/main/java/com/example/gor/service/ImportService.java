package com.example.gor.service;

import com.example.gor.classifier.ClassificationResult;
import com.example.gor.classifier.TrafficClassifier;
import com.example.gor.entity.TrafficRequest;
import com.example.gor.mapper.TrafficRequestMapper;
import com.example.gor.parser.GorHttpParser;
import com.example.gor.parser.GorRequestReader;
import com.example.gor.parser.ParsedHttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * .gor 文件导入服务。
 *
 * <p>导入流程为：可选清空旧数据、流式切分请求、解析 HTTP 字段、执行规则分类、保存结构化字段和 rawGorText。
 * 业务层只依赖 Mapper，不直接写 SQL。</p>
 */
@Service
public class ImportService {
    private final GorRequestReader reader;
    private final GorHttpParser parser;
    private final TrafficClassifier classifier;
    private final TrafficRequestMapper trafficRequestMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建导入服务。
     *
     * @param reader       GoReplay 请求切分器
     * @param parser       HTTP 字段解析器
     * @param classifier   请求分类器
     * @param trafficRequestMapper MyBatis 请求 Mapper
     * @param objectMapper 用于把 headers 序列化成 JSON
     */
    public ImportService(
            GorRequestReader reader,
            GorHttpParser parser,
            TrafficClassifier classifier,
            TrafficRequestMapper trafficRequestMapper,
            ObjectMapper objectMapper
    ) {
        this.reader = reader;
        this.parser = parser;
        this.classifier = classifier;
        this.trafficRequestMapper = trafficRequestMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 导入指定 .gor 文件。
     *
     * @param input 输入文件路径
     * @return 成功导入的请求数量
     * @throws IOException              文件读取失败时抛出
     * @throws IllegalArgumentException 输入文件不存在或不是普通文件时抛出
     */
    @Transactional
    public long importFile(Path input) throws IOException {
        return importFile(input, false);
    }

    /**
     * 导入指定 .gor 文件，并可在导入前清空旧数据。
     *
     * @param input             输入文件路径
     * @param clearBeforeImport true 表示导入前先清空 traffic_request
     * @return 成功导入的请求数量
     * @throws IOException              文件读取失败时抛出
     * @throws IllegalArgumentException 输入文件不存在或不是普通文件时抛出
     */
    @Transactional
    public long importFile(Path input, boolean clearBeforeImport) throws IOException {
        return importFiles(List.of(input), clearBeforeImport);
    }

    /**
     * 批量导入多个 .gor 文件，并可在整批导入前清空旧数据。
     *
     * <p>clearBeforeImport 只会在批量导入开始前执行一次，不会在每个文件前重复清空。</p>
     *
     * @param inputs            输入文件列表
     * @param clearBeforeImport true 表示导入前先清空 traffic_request
     * @return 成功导入的请求总数量
     * @throws IOException              文件读取失败时抛出
     * @throws IllegalArgumentException 文件列表为空或任一输入不是普通文件时抛出
     */
    @Transactional
    public long importFiles(List<Path> inputs, boolean clearBeforeImport) throws IOException {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("At least one input .gor file is required");
        }
        for (Path input : inputs) {
            validateInputFile(input);
        }
        if (clearBeforeImport) {
            trafficRequestMapper.deleteAll();
        }
        long total = 0;
        for (Path input : inputs) {
            total += importSingleFile(input);
        }
        return total;
    }

    /**
     * 导入目录下的所有 .gor 文件，并可在导入前清空旧数据。
     *
     * @param inputDir          输入目录
     * @param clearBeforeImport true 表示导入前先清空 traffic_request
     * @return 成功导入的请求总数量
     * @throws IOException              目录扫描或文件读取失败时抛出
     * @throws IllegalArgumentException 输入路径不是目录，或目录下没有 .gor 文件时抛出
     */
    @Transactional
    public long importDirectory(Path inputDir, boolean clearBeforeImport) throws IOException {
        if (!Files.isDirectory(inputDir)) {
            throw new IllegalArgumentException("Input directory does not exist: " + inputDir);
        }
        List<Path> inputs;
        try (var paths = Files.list(inputDir)) {
            inputs = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".gor"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("No .gor files found in directory: " + inputDir);
        }
        return importFiles(inputs, clearBeforeImport);
    }

    /**
     * 导入单个已校验的 .gor 文件。
     *
     * @param input 输入文件路径
     * @return 成功导入的请求数量
     * @throws IOException 文件读取失败时抛出
     */
    private long importSingleFile(Path input) throws IOException {
        validateInputFile(input);
        final long[] count = {0};
        try {
            // 这里由 GorRequestReader 逐条回调，避免大 .gor 文件一次性进入内存。
            reader.readRequests(input, record -> {
                ParsedHttpRequest request = parser.parse(record.rawText());
                ClassificationResult classification = classifier.classify(request);
                trafficRequestMapper.insert(toEntity(input, record.requestNo(), record.rawText(), request, classification));
                count[0]++;
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return count[0];
    }

    /**
     * 校验输入路径是否是普通文件。
     *
     * @param input 输入文件路径
     */
    private void validateInputFile(Path input) {
        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException("Input .gor file does not exist: " + input);
        }
    }

    /**
     * 将解析结果和分类结果合并为数据库实体。
     *
     * @param input          来源文件路径
     * @param requestNo      文件内请求序号
     * @param rawText        原始 GoReplay 文本
     * @param request        HTTP 解析结果
     * @param classification 分类结果
     * @return 待写入数据库的 TrafficRequest
     */
    private TrafficRequest toEntity(
            Path input,
            long requestNo,
            String rawText,
            ParsedHttpRequest request,
            ClassificationResult classification
    ) {
        TrafficRequest entity = new TrafficRequest();
        entity.setSourceFile(input.toAbsolutePath().normalize().toString());
        entity.setRequestNo(requestNo);
        entity.setMethod(request.method());
        entity.setHost(request.host());
        entity.setUrl(request.url());
        entity.setPath(request.path());
        entity.setQueryString(request.query());
        entity.setHeadersJson(writeJson(request));
        entity.setBody(request.body());
        // 导出和回放依赖原始文本，解析后的字段只用于查询、统计和分类。
        entity.setRawGorText(rawText);
        entity.setCategory(classification.category());
        entity.setTags(String.join(",", classification.tags()));
        entity.setRiskLevel(classification.riskLevel());
        entity.setCreatedAt(Instant.now());
        entity.setContentType(request.contentType());
        entity.setUserAgent(request.userAgent());
        return entity;
    }

    /**
     * 将 header map 序列化为 JSON 字符串。
     *
     * @param request 已解析请求
     * @return headers_json 字段值
     */
    private String writeJson(ParsedHttpRequest request) {
        try {
            return objectMapper.writeValueAsString(request.headers());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize request headers", e);
        }
    }
}
