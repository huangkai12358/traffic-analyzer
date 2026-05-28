package com.example.gor.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

/**
 * CLI 根命令。
 *
 * <p>根命令只负责注册子命令和展示帮助入口；实际业务由 import、clear、stats、export、split 子命令完成。</p>
 */
@Component
@Command(
        name = "gor-tool",
        mixinStandardHelpOptions = true,
        description = "GoReplay .gor traffic processing CLI",
        subcommands = {
                ImportCommand.class,
                ClearCommand.class,
                StatsCommand.class,
                ExportCommand.class,
                SplitCommand.class
        }
)
public class GorCommand implements Runnable {
    /**
     * 未指定子命令时执行。
     *
     * <p>输出简短提示而不是直接执行默认业务，避免用户误操作。</p>
     */
    @Override
    public void run() {
        System.out.println("Use --help to see available commands.");
    }
}
