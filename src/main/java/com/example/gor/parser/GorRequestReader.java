package com.example.gor.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * GoReplay .gor 文件的流式读取器。
 *
 * <p>该类只负责按 GoReplay payload 边界切分请求，不做 HTTP 字段解析，也不接触数据库。
 * 这样可以让导入和拆分功能复用同一套切分逻辑，并避免大文件一次性读入内存。</p>
 */
@Component
public class GorRequestReader {
    // 老版本 GoReplay 使用这个三猴子字符串分隔每条消息；sample.gor 保留该真实历史格式。
    public static final String DELIMITER = "🐵🙈🙉";

    /**
     * 从输入文件中逐条读取请求记录，并通过回调交给调用方处理。
     *
     * <p>只处理 payload type 为 {@code 1} 的请求记录；响应记录会被跳过，因为当前 MVP 的存储和导出维度是请求。</p>
     *
     * @param input    .gor 输入文件路径
     * @param consumer 每读取到一条请求时调用的处理函数
     * @throws IOException 当文件读取失败时抛出
     */
    public void readRequests(Path input, Consumer<GorRecord> consumer) throws IOException {
        long requestNo = 0;
        StringBuilder current = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(DELIMITER)) {
                    // 读到分隔符才提交上一条请求，保证 rawText 保留完整 GoReplay 消息。
                    requestNo = emitIfRequest(current, requestNo, consumer);
                    current.setLength(0);
                } else {
                    current.append(line).append(System.lineSeparator());
                }
            }
        }
        emitIfRequest(current, requestNo, consumer);
    }

    /**
     * 校验当前缓存是否是一条请求记录；如果是，则补回分隔符并提交给 consumer。
     *
     * @param current   当前读取到但尚未提交的一段原始文本
     * @param requestNo 已提交的请求数量
     * @param consumer  请求记录处理函数
     * @return 最新的请求数量
     */
    private long emitIfRequest(StringBuilder current, long requestNo, Consumer<GorRecord> consumer) {
        String raw = current.toString();
        if (raw.isBlank()) {
            return requestNo;
        }
        if (!raw.startsWith("1 ")) {
            return requestNo;
        }
        long nextNo = requestNo + 1;
        try {
            consumer.accept(new GorRecord(nextNo, raw + DELIMITER + System.lineSeparator()));
        } catch (UncheckedIOException e) {
            throw e;
        }
        return nextNo;
    }
}
