package com.example.gor.cli;

import com.example.gor.service.StatsService;
import com.example.gor.service.StatsService.StatsSnapshot;
import com.example.gor.mapper.TrafficRequestMapper.CountRow;
import java.util.List;
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
        printSummary(snapshot);
        printRows("Category Distribution", "Category", snapshot.categoryCounts(), snapshot.total());
        printRows("Top Hosts", "Host", snapshot.topHosts(), snapshot.total());
        printRows("Top Paths", "Path", snapshot.topPaths(), snapshot.total());
        return 0;
    }

    /**
     * 输出统计概览。
     *
     * <p>概览部分放最重要的总量和风险量，方便用户先判断当前导入的数据规模和安全风险。</p>
     *
     * @param snapshot 当前统计快照
     */
    private void printSummary(StatsSnapshot snapshot) {
        System.out.println();
        System.out.println("GoReplay Traffic Statistics");
        System.out.println("========================================");
        System.out.printf("%-24s %10d%n", "Total Requests", snapshot.total());
        System.out.printf("%-24s %10d", "High Risk Requests", snapshot.suspiciousAttackCount());
        System.out.printf("  (%s)%n", percent(snapshot.suspiciousAttackCount(), snapshot.total()));
        System.out.println();
    }

    /**
     * 输出带百分比的聚合表格。
     *
     * <p>所有聚合结果都使用同一种格式，降低用户阅读成本；如果列表为空，则明确提示当前没有数据。</p>
     *
     * @param title       表格标题
     * @param nameHeader  第一列标题，例如 Category、Host 或 Path
     * @param rows        聚合结果行
     * @param total       请求总数，用于计算占比
     */
    private void printRows(String title, String nameHeader, List<CountRow> rows, long total) {
        System.out.println(title);
        System.out.println("----------------------------------------");
        if (rows.isEmpty()) {
            System.out.println("No data.");
            System.out.println();
            return;
        }
        System.out.printf("%-28s %8s %8s%n", nameHeader, "Count", "Percent");
        for (CountRow row : rows) {
            System.out.printf("%-28s %8d %8s%n", displayName(row.name()), row.countValue(), percent(row.countValue(), total));
        }
        System.out.println();
    }

    /**
     * 计算某个聚合数量在总请求数中的占比。
     *
     * @param count 当前维度数量
     * @param total 请求总数
     * @return 百分比文本；总数为 0 时返回 0.0%
     */
    private String percent(long count, long total) {
        if (total <= 0) {
            return "0.0%";
        }
        return String.format("%.1f%%", count * 100.0 / total);
    }

    /**
     * 处理聚合维度为空的展示值。
     *
     * @param name 数据库返回的维度名称
     * @return 终端展示名称
     */
    private String displayName(String name) {
        return name == null || name.isBlank() ? "(empty)" : name;
    }
}
