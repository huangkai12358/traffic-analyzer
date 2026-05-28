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

    @Option(names = "--input", required = true, description = "Input .gor file")
    private Path input;

    @Option(names = "--max-count", description = "Maximum requests per output file")
    private Long maxCount;

    @Option(names = "--max-size-mb", description = "Maximum size in MiB per output file")
    private Long maxSizeMb;

    @Option(names = "--output-dir", required = true, description = "Output directory")
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
