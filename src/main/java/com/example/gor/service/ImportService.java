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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * .gor 文件导入服务。
 *
 * <p>导入流程为：流式切分请求、解析 HTTP 字段、执行规则分类、保存结构化字段和 rawGorText。
 * 业务层只依赖 Repository，不直接写 SQL。</p>
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
        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException("Input .gor file does not exist: " + input);
        }
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
