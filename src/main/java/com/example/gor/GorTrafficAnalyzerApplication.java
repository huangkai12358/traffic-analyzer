package com.example.gor;

import com.example.gor.cli.SplitCommand;
import com.example.gor.cli.GorCommand;
import com.example.gor.parser.GorRequestReader;
import com.example.gor.service.SplitService;
import java.util.Arrays;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import picocli.CommandLine;

/**
 * Spring Boot 启动入口。
 *
 * <p>应用启动后并不暴露 Web 服务，而是把命令行参数交给 Picocli 根命令处理。
 * 这种组织方式保留 Spring 的依赖注入、事务和配置能力，同时满足当前 CLI/MVP 的运行方式。</p>
 */
@SpringBootApplication
public class GorTrafficAnalyzerApplication implements CommandLineRunner {
    private final CommandLine.IFactory factory;
    private final GorCommand command;
    private final Environment environment;

    /**
     * 创建应用启动器。
     *
     * @param factory     Picocli 的 Spring Bean 工厂，用于创建子命令并注入依赖
     * @param command     CLI 根命令
     * @param environment Spring 环境对象，用于判断当前 profile
     */
    public GorTrafficAnalyzerApplication(CommandLine.IFactory factory, GorCommand command, Environment environment) {
        this.factory = factory;
        this.command = command;
        this.environment = environment;
    }

    /**
     * JVM 入口方法。
     *
     * @param args 用户输入的 CLI 参数
     */
    public static void main(String[] args) {
        if (isSplitCommand(args)) {
            System.exit(runSplitWithoutSpring(args));
        }
        System.exit(SpringApplication.exit(SpringApplication.run(GorTrafficAnalyzerApplication.class, args)));
    }

    /**
     * 判断当前命令是否为 split。
     *
     * <p>split 只需要读取和写入本地 .gor 文件，不需要数据库、MyBatis 或 Spring 上下文。
     * 在 main 方法里提前识别它，可以避免 MySQL 不可用时影响纯文件拆分功能。</p>
     *
     * @param args 用户输入的 CLI 参数
     * @return true 表示当前要执行 split 子命令
     */
    private static boolean isSplitCommand(String[] args) {
        return args.length > 0 && "split".equals(args[0]);
    }

    /**
     * 不启动 Spring Boot，直接执行 split 子命令。
     *
     * <p>这里手动组装 SplitCommand、SplitService 和 GorRequestReader。它们都没有数据库依赖，
     * 因此可以在输入文件不存在、MySQL 未启动、环境变量未配置等情况下仍给出干净的 CLI 错误。</p>
     *
     * @param args 用户输入的完整 CLI 参数，第一项是 split
     * @return Picocli 命令退出码，0 表示成功
     */
    private static int runSplitWithoutSpring(String[] args) {
        String[] splitArgs = Arrays.copyOfRange(args, 1, args.length);
        SplitCommand splitCommand = new SplitCommand(new SplitService(new GorRequestReader()));
        return new CommandLine(splitCommand).execute(splitArgs);
    }

    /**
     * Spring 上下文启动完成后执行 CLI 命令。
     *
     * <p>测试 profile 下跳过命令执行，避免 SpringBootTest 加载上下文时误执行 CLI。
     * 非 0 exitCode 会直接退出进程，让 shell 和脚本能正确感知失败。</p>
     *
     * @param args 用户输入的 CLI 参数
     */
    @Override
    public void run(String... args) {
        if (environment.matchesProfiles("test")) {
            return;
        }
        int exitCode = new CommandLine(command, factory).execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
