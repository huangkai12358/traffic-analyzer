package com.example.gor.cli;

import com.example.gor.service.ImportService;
import java.util.List;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * import 子命令。
 *
 * <p>负责接收输入文件路径或输入目录、调用导入服务，并把成功数量或错误信息输出到终端。</p>
 */
@Component
@Command(name = "import", description = "Import a GoReplay .gor file")
public class ImportCommand implements Callable<Integer> {
    private final ImportService importService;

    private List<Path> inputs;

    private Path inputDir;

    private boolean clear;

    /**
     * 创建导入命令。
     *
     * @param importService .gor 导入服务
     */
    public ImportCommand(ImportService importService) {
        this.importService = importService;
    }

    /**
     * 接收 import 命令的位置参数。
     *
     * <p>Picocli 会在命令执行前调用该方法注入用户输入的一个或多个文件路径。</p>
     *
     * @param inputs 输入 .gor 文件路径列表
     */
    @Parameters(index = "0..*", description = "Input .gor file(s)")
    void setInputs(List<Path> inputs) {
        this.inputs = inputs;
    }

    /**
     * 接收待批量导入的目录。
     *
     * @param inputDir 包含 .gor 文件的目录
     */
    @Option(names = "--dir", description = "Directory containing .gor files to import")
    void setInputDir(Path inputDir) {
        this.inputDir = inputDir;
    }

    /**
     * 接收是否在导入前清空旧数据的选项。
     *
     * @param clear true 表示导入前先清空 traffic_request
     */
    @Option(names = "--clear", description = "Clear existing imported requests before importing")
    void setClear(boolean clear) {
        this.clear = clear;
    }

    /**
     * 执行导入命令。
     *
     * @return 0 表示成功，1 表示失败
     */
    @Override
    public Integer call() {
        try {
            if (inputDir != null && inputs != null && !inputs.isEmpty()) {
                System.err.println("Import failed: specify either input files or --dir, not both");
                return 1;
            }
            long count;
            String source;
            if (inputDir != null) {
                count = importService.importDirectory(inputDir, clear);
                source = inputDir.toString();
            } else {
                count = importService.importFiles(inputs, clear);
                source = String.valueOf(inputs);
            }
            if (clear) {
                System.out.println("Cleared existing imported requests before import.");
            }
            System.out.printf("Imported %d requests from %s%n", count, source);
            return 0;
        } catch (Exception e) {
            System.err.println("Import failed: " + e.getMessage());
            return 1;
        }
    }
}
