package com.example.gor.service;

import com.example.gor.entity.RiskLevel;
import com.example.gor.entity.TrafficRequest;
import com.example.gor.mapper.TrafficRequestMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.apache.ibatis.cursor.Cursor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GoReplay .gor 导出服务。
 *
 * <p>导出时只写数据库中的 rawGorText，绝不根据解析字段重新拼接 HTTP 请求，
 * 这样才能尽量保证导出的文件仍可被 GoReplay 回放。</p>
 */
@Service
public class ExportService {
    private final TrafficRequestMapper trafficRequestMapper;

    /**
     * 创建导出服务。
     *
     * @param trafficRequestMapper 请求数据访问 Mapper
     */
    public ExportService(TrafficRequestMapper trafficRequestMapper) {
        this.trafficRequestMapper = trafficRequestMapper;
    }

    /**
     * 按主分类导出请求。
     *
     * @param category 要导出的主分类，例如 login、api、sql_injection
     * @param output   输出 .gor 文件路径
     * @return 导出的请求数量
     * @throws IOException 写文件失败时抛出
     */
    @Transactional
    public long exportByCategory(String category, Path output) throws IOException {
        try (Cursor<TrafficRequest> cursor = trafficRequestMapper.findByCategoryOrderByIdAsc(category)) {
            return export(cursor, output);
        }
    }

    /**
     * 按风险等级导出请求。
     *
     * @param risk   风险等级字符串，例如 low、medium、high
     * @param output 输出 .gor 文件路径
     * @return 导出的请求数量
     * @throws IOException              写文件失败时抛出
     * @throws IllegalArgumentException risk 无法映射到 RiskLevel 时抛出
     */
    @Transactional
    public long exportByRisk(String risk, Path output) throws IOException {
        RiskLevel riskLevel = RiskLevel.valueOf(risk.toUpperCase(Locale.ROOT));
        try (Cursor<TrafficRequest> cursor = trafficRequestMapper.findByRiskLevelOrderByIdAsc(riskLevel)) {
            return export(cursor, output);
        }
    }

    /**
     * 将一批请求写入目标 .gor 文件。
     *
     * @param requests 请求迭代器，通常来自 MyBatis Cursor 以支持流式导出
     * @param output   输出文件路径
     * @return 写入的请求数量
     * @throws IOException 创建目录或写文件失败时抛出
     */
    private long export(Iterable<TrafficRequest> requests, Path output) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        long count = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            // 必须写 rawGorText，不能用解析字段重组，否则会破坏 GoReplay 回放格式。
            for (TrafficRequest request : requests) {
                writer.write(request.getRawGorText());
                if (!request.getRawGorText().endsWith(System.lineSeparator())) {
                    writer.newLine();
                }
                count++;
            }
        }
        return count;
    }
}
