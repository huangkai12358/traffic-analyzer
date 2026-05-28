package com.example.gor.cli;

import com.example.gor.service.SplitService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * split 子命令。
 *
 * <p>对原始 .gor 文件进行物理拆分，不要求文件已导入数据库。</p>
 */
@Component
@Command(name = "split", description = "Split a GoReplay .gor file")
public class SplitCommand implements Callable<Integer> {
    private final SplitService splitService;

    private Path input;

    private Long maxCount;

    private Long maxSizeMb;

    private Path outputDir;

    /**
     * 创建拆分命令。
     *
     * @param splitService .gor 拆分服务
     */
    public SplitCommand(SplitService splitService) {
        this.splitService = splitService;
    }

    /**
     * 接收待拆分的输入文件路径。
     *
     * @param input 输入 .gor 文件
     */
    @Option(names = "--input", required = true, description = "Input .gor file")
    void setInput(Path input) {
        this.input = input;
    }

    /**
     * 接收按请求数量拆分的阈值。
     *
     * @param maxCount 每个 part 文件最多请求数
     */
    @Option(names = "--max-count", description = "Maximum requests per output file")
    void setMaxCount(Long maxCount) {
        this.maxCount = maxCount;
    }

    /**
     * 接收按文件大小拆分的阈值。
     *
     * @param maxSizeMb 每个 part 文件最大 MiB 数
     */
    @Option(names = "--max-size-mb", description = "Maximum size in MiB per output file")
    void setMaxSizeMb(Long maxSizeMb) {
        this.maxSizeMb = maxSizeMb;
    }

    /**
     * 接收拆分结果输出目录。
     *
     * @param outputDir 输出目录
     */
    @Option(names = "--output-dir", required = true, description = "Output directory")
    void setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * 执行拆分命令。
     *
     * <p>--max-count 和 --max-size-mb 必须二选一，避免同时按数量和大小轮转导致行为不清晰。</p>
     *
     * @return 0 表示成功，1 表示失败
     */
    @Override
    public Integer call() {
        try {
            if ((maxCount == null && maxSizeMb == null) || (maxCount != null && maxSizeMb != null)) {
                System.err.println("Split failed: specify exactly one of --max-count or --max-size-mb");
                return 1;
            }
            long parts = maxCount != null
                    ? splitService.splitByMaxCount(input, maxCount, outputDir)
                    : splitService.splitByMaxSizeMb(input, maxSizeMb, outputDir);
            System.out.printf("Created %d part files in %s%n", parts, outputDir);
            return 0;
        } catch (Exception e) {
            System.err.println("Split failed: " + e.getMessage());
            return 1;
        }
    }
}
