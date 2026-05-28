package com.example.gor.cli;

import com.example.gor.service.ExportService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * export 子命令。
 *
 * <p>支持按主分类或风险等级从数据库中导出请求，输出文件内容来自 rawGorText。</p>
 */
@Component
@Command(name = "export", description = "Export imported requests by category or risk")
public class ExportCommand implements Callable<Integer> {
    private final ExportService exportService;

    private String category;

    private String risk;

    private Path output;

    /**
     * 创建导出命令。
     *
     * @param exportService .gor 导出服务
     */
    public ExportCommand(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * 接收按分类导出的过滤条件。
     *
     * @param category 要导出的主分类
     */
    @Option(names = "--category", description = "Category to export")
    void setCategory(String category) {
        this.category = category;
    }

    /**
     * 接收按风险导出的过滤条件。
     *
     * @param risk 风险等级，例如 low、medium、high
     */
    @Option(names = "--risk", description = "Risk level to export: low, medium, high")
    void setRisk(String risk) {
        this.risk = risk;
    }

    /**
     * 接收导出文件路径。
     *
     * @param output 输出 .gor 文件
     */
    @Option(names = "--output", required = true, description = "Output .gor file")
    void setOutput(Path output) {
        this.output = output;
    }

    /**
     * 执行导出命令。
     *
     * <p>--category 和 --risk 必须二选一，避免一个命令出现歧义过滤条件。</p>
     *
     * @return 0 表示成功，1 表示失败
     */
    @Override
    public Integer call() {
        try {
            if ((category == null && risk == null) || (category != null && risk != null)) {
                System.err.println("Export failed: specify exactly one of --category or --risk");
                return 1;
            }
            long count = category != null
                    ? exportService.exportByCategory(category, output)
                    : exportService.exportByRisk(risk, output);
            System.out.printf("Exported %d requests to %s%n", count, output);
            return 0;
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
            return 1;
        }
    }
}
