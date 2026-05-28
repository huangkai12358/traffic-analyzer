package com.example.gor.cli;

import com.example.gor.service.ClearService;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

/**
 * clear 子命令。
 *
 * <p>用于在一次新的流量处理前清空旧数据，避免 stats/export 混入历史导入记录。</p>
 */
@Component
@Command(name = "clear", description = "Clear all imported requests")
public class ClearCommand implements Callable<Integer> {
    private final ClearService clearService;

    /**
     * 创建清理命令。
     *
     * @param clearService 数据清理服务
     */
    public ClearCommand(ClearService clearService) {
        this.clearService = clearService;
    }

    /**
     * 执行清理命令。
     *
     * @return 0 表示成功，1 表示失败
     */
    @Override
    public Integer call() {
        try {
            int deleted = clearService.clearRequests();
            System.out.printf("Cleared %d imported requests.%n", deleted);
            return 0;
        } catch (Exception e) {
            System.err.println("Clear failed: " + e.getMessage());
            return 1;
        }
    }
}
