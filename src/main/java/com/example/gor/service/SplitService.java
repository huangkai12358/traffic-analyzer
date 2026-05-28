package com.example.gor.service;

import com.example.gor.parser.GorRecord;
import com.example.gor.parser.GorRequestReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

/**
 * GoReplay .gor 文件拆分服务。
 *
 * <p>拆分直接基于输入文件的 raw record 工作，不依赖数据库；这样可以对尚未导入的大文件先做切片处理。</p>
 */
@Service
public class SplitService {
    private final GorRequestReader reader;

    /**
     * 创建拆分服务。
     *
     * @param reader GoReplay 请求切分器
     */
    public SplitService(GorRequestReader reader) {
        this.reader = reader;
    }

    /**
     * 按每个 part 文件最多包含的请求数拆分 .gor 文件。
     *
     * @param input     输入 .gor 文件
     * @param maxCount  每个输出文件最多包含的请求数量
     * @param outputDir 输出目录
     * @return 生成的 part 文件数量
     * @throws IOException              读写文件失败时抛出
     * @throws IllegalArgumentException maxCount 小于等于 0 时抛出
     */
    public long splitByMaxCount(Path input, long maxCount, Path outputDir) throws IOException {
        if (maxCount <= 0) {
            throw new IllegalArgumentException("--max-count must be greater than 0");
        }
        validateInputFile(input);
        Files.createDirectories(outputDir);
        SplitWriter writer = new SplitWriter(outputDir);
        try {
            // split 直接从输入文件流式读取，不依赖数据库，适合超大 .gor 文件快速拆分。
            reader.readRequests(input, record -> {
                try {
                    writer.writeByCount(record, maxCount);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return writer.closeAndPartCount();
        } catch (RuntimeException e) {
            writer.close();
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }

    /**
     * 按每个 part 文件最大大小拆分 .gor 文件。
     *
     * @param input     输入 .gor 文件
     * @param maxSizeMb 每个输出文件最大 MiB 数
     * @param outputDir 输出目录
     * @return 生成的 part 文件数量
     * @throws IOException              读写文件失败时抛出
     * @throws IllegalArgumentException maxSizeMb 小于等于 0 时抛出
     */
    public long splitByMaxSizeMb(Path input, long maxSizeMb, Path outputDir) throws IOException {
        if (maxSizeMb <= 0) {
            throw new IllegalArgumentException("--max-size-mb must be greater than 0");
        }
        validateInputFile(input);
        Files.createDirectories(outputDir);
        long maxBytes = maxSizeMb * 1024 * 1024;
        SplitWriter writer = new SplitWriter(outputDir);
        try {
            reader.readRequests(input, record -> {
                try {
                    writer.writeBySize(record, maxBytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return writer.closeAndPartCount();
        } catch (RuntimeException e) {
            writer.close();
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }

    /**
     * 校验拆分输入路径必须是一个已存在的普通文件。
     *
     * <p>这里提前抛出业务可读的异常，避免底层 Files.newBufferedReader 抛出的
     * NoSuchFileException 只显示文件名，导致 CLI 用户无法判断失败原因。</p>
     *
     * @param input 用户通过 --input 传入的 .gor 文件路径
     */
    private void validateInputFile(Path input) {
        if (!Files.exists(input)) {
            throw new IllegalArgumentException("input file does not exist: " + input);
        }
        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException("input path is not a file: " + input);
        }
    }

    /**
     * 管理拆分输出文件的轮转和计数。
     *
     * <p>该类有状态，只在单次 split 调用内部使用，不暴露给外部并发调用。</p>
     */
    private static class SplitWriter {
        private final Path outputDir;
        private BufferedWriter writer;
        private long partNo;
        private long currentCount;
        private long currentBytes;

        /**
         * 创建 part 文件写入器。
         *
         * @param outputDir 输出目录
         */
        SplitWriter(Path outputDir) {
            this.outputDir = outputDir;
        }

        /**
         * 按请求数量限制写入记录，超过阈值时轮转到下一个 part 文件。
         *
         * @param record   当前请求原始记录
         * @param maxCount 每个 part 最大请求数
         * @throws IOException 写文件失败时抛出
         */
        void writeByCount(GorRecord record, long maxCount) throws IOException {
            if (writer == null || currentCount >= maxCount) {
                rotate();
            }
            write(record);
        }

        /**
         * 按文件大小限制写入记录，超过阈值时轮转到下一个 part 文件。
         *
         * @param record   当前请求原始记录
         * @param maxBytes 每个 part 最大字节数
         * @throws IOException 写文件失败时抛出
         */
        void writeBySize(GorRecord record, long maxBytes) throws IOException {
            long bytes = record.rawText().getBytes(StandardCharsets.UTF_8).length;
            // 单条请求超过 maxBytes 时仍单独写入一个 part，避免截断 rawGorText。
            if (writer == null || (currentBytes > 0 && currentBytes + bytes > maxBytes)) {
                rotate();
            }
            write(record);
        }

        /**
         * 关闭当前 writer 并返回已创建的 part 数量。
         *
         * @return part 文件数量
         * @throws IOException 关闭文件失败时抛出
         */
        long closeAndPartCount() throws IOException {
            close();
            return partNo;
        }

        /**
         * 关闭当前打开的 part 文件。
         *
         * @throws IOException 关闭文件失败时抛出
         */
        void close() throws IOException {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        }

        /**
         * 轮转到新的 part 文件，并重置当前 part 的计数和字节数。
         *
         * @throws IOException 创建新文件失败时抛出
         */
        private void rotate() throws IOException {
            close();
            partNo++;
            currentCount = 0;
            currentBytes = 0;
            writer = Files.newBufferedWriter(outputDir.resolve("part-" + String.format("%05d", partNo) + ".gor"), StandardCharsets.UTF_8);
        }

        /**
         * 向当前 part 文件写入一条完整 raw record。
         *
         * @param record 请求原始记录
         * @throws IOException 写文件失败时抛出
         */
        private void write(GorRecord record) throws IOException {
            writer.write(record.rawText());
            currentCount++;
            currentBytes += record.rawText().getBytes(StandardCharsets.UTF_8).length;
        }
    }
}
