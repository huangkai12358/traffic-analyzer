package com.example.gor.cli;

import com.example.gor.service.StatsService;
import com.example.gor.service.StatsService.StatsSnapshot;
import com.example.gor.mapper.TrafficRequestMapper.CountRow;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

/**
 * stats 子命令。
 *
 * <p>从数据库读取聚合统计信息，并以面向终端的文本格式输出。</p>
 */
@Component
@Command(name = "stats", description = "Show imported request statistics")
public class StatsCommand implements Callable<Integer> {
    private final StatsService statsService;

    /**
     * 创建统计命令。
     *
     * @param statsService 统计服务
     */
    public StatsCommand(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * 执行统计命令。
     *
     * @return 0 表示成功
     */
    @Override
    public Integer call() {
        StatsSnapshot snapshot = statsService.snapshot();
        System.out.println("Total requests: " + snapshot.total());
        System.out.println("By category:");
        snapshot.categoryCounts().forEach(row -> System.out.printf("  %s: %s%n", row.name(), row.countValue()));
        System.out.println("Suspicious attacks: " + snapshot.suspiciousAttackCount());
        printTop("Top Host:", snapshot.topHosts());
        printTop("Top Path:", snapshot.topPaths());
        return 0;
    }

    /**
     * 输出 Top N 聚合列表。
     *
     * @param title 列表标题，例如 Top Host
     * @param rows  聚合结果行
     */
    private void printTop(String title, Iterable<CountRow> rows) {
        System.out.println(title);
        for (CountRow row : rows) {
            System.out.printf("  %s: %s%n", row.name(), row.countValue());
        }
    }
}
