package com.example.gor.parser;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
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
    // GoReplay .gor 文件使用该 payload 分隔符分隔每条消息。
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
        byte[] delimiter = DELIMITER.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream current = new ByteArrayOutputStream();
        try (PushbackInputStream inputStream = new PushbackInputStream(new BufferedInputStream(Files.newInputStream(input)), 1)) {
            int matched = 0;
            int nextByte;
            while ((nextByte = inputStream.read()) != -1) {
                current.write(nextByte);
                if (nextByte == Byte.toUnsignedInt(delimiter[matched])) {
                    matched++;
                    if (matched == delimiter.length) {
                        appendTrailingLineBreakIfPresent(inputStream, current);
                        requestNo = emitIfRequest(current, requestNo, consumer);
                        current.reset();
                        matched = 0;
                    }
                } else {
                    matched = nextByte == Byte.toUnsignedInt(delimiter[0]) ? 1 : 0;
                }
            }
        }
        emitIfRequest(current, requestNo, consumer);
    }

    /**
     * 如果分隔符后面紧跟换行符，则把换行符保留在当前 raw record 中。
     *
     * <p>GoReplay 文本通常是一行三猴子分隔符后换行再进入下一条记录。这里不使用 readLine，
     * 而是按字节判断并保留原始的 LF 或 CRLF，避免导出时改变文件换行格式。</p>
     *
     * @param inputStream 支持回退 1 字节的输入流
     * @param current     当前请求原始字节缓存
     * @throws IOException 读取文件失败时抛出
     */
    private void appendTrailingLineBreakIfPresent(PushbackInputStream inputStream, ByteArrayOutputStream current) throws IOException {
        int nextByte = inputStream.read();
        if (nextByte == -1) {
            return;
        }
        if (nextByte == '\n') {
            current.write(nextByte);
            return;
        }
        if (nextByte == '\r') {
            current.write(nextByte);
            int maybeLineFeed = inputStream.read();
            if (maybeLineFeed == '\n') {
                current.write(maybeLineFeed);
            } else if (maybeLineFeed != -1) {
                inputStream.unread(maybeLineFeed);
            }
            return;
        }
        inputStream.unread(nextByte);
    }

    /**
     * 校验当前缓存是否是一条请求记录；如果是，则按原始文本提交给 consumer。
     *
     * @param current   当前读取到但尚未提交的一段原始文本
     * @param requestNo 已提交的请求数量
     * @param consumer  请求记录处理函数
     * @return 最新的请求数量
     */
    private long emitIfRequest(ByteArrayOutputStream current, long requestNo, Consumer<GorRecord> consumer) {
        String raw = current.toString(StandardCharsets.UTF_8);
        if (raw.isBlank()) {
            return requestNo;
        }
        if (!raw.startsWith("1 ")) {
            return requestNo;
        }
        long nextNo = requestNo + 1;
        try {
            consumer.accept(new GorRecord(nextNo, raw));
        } catch (UncheckedIOException e) {
            throw e;
        }
        return nextNo;
    }
}
