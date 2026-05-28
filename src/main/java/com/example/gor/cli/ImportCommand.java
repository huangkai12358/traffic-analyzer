package com.example.gor.cli;

import com.example.gor.service.ImportService;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * import 子命令。
 *
 * <p>负责接收输入文件路径、调用导入服务，并把成功数量或错误信息输出到终端。</p>
 */
@Component
@Command(name = "import", description = "Import a GoReplay .gor file")
public class ImportCommand implements Callable<Integer> {
    private final ImportService importService;

    @Parameters(index = "0", description = "Input .gor file")
    private Path input;

    /**
     * 创建导入命令。
     *
     * @param importService .gor 导入服务
     */
    public ImportCommand(ImportService importService) {
        this.importService = importService;
    }

    /**
     * 执行导入命令。
     *
     * @return 0 表示成功，1 表示失败
     */
    @Override
    public Integer call() {
        try {
            long count = importService.importFile(input);
            System.out.printf("Imported %d requests from %s%n", count, input);
            return 0;
        } catch (Exception e) {
            System.err.println("Import failed: " + e.getMessage());
            return 1;
        }
    }
}
