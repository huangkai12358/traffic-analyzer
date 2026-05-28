package com.example.gor.service;

import com.example.gor.entity.RiskLevel;
import com.example.gor.mapper.TrafficRequestMapper;
import com.example.gor.mapper.TrafficRequestMapper.CountRow;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 统计服务。
 *
 * <p>从 MyBatis Repository 聚合数据库中的请求数量、分类分布、攻击数量和 Top 维度，
 * CLI 只负责格式化输出，不直接关心 SQL 查询细节。</p>
 */
@Service
public class StatsService {
    private final TrafficRequestMapper trafficRequestMapper;

    /**
     * 创建统计服务。
     *
     * @param trafficRequestMapper 请求数据访问 Mapper
     */
    public StatsService(TrafficRequestMapper trafficRequestMapper) {
        this.trafficRequestMapper = trafficRequestMapper;
    }

    /**
     * 获取当前数据库中的流量统计快照。
     *
     * @return 统计快照，包含总量、分类分布、攻击数量、Top Host 和 Top Path
     */
    public StatsSnapshot snapshot() {
        return new StatsSnapshot(
                trafficRequestMapper.count(),
                trafficRequestMapper.countGroupedByCategory(),
                trafficRequestMapper.countByRiskLevel(RiskLevel.HIGH),
                trafficRequestMapper.topHosts(10),
                trafficRequestMapper.topPaths(10)
        );
    }

    /**
     * CLI 统计命令使用的聚合结果对象。
     *
     * @param total                 请求总数
     * @param categoryCounts        按 category 聚合的数量
     * @param suspiciousAttackCount HIGH 风险请求数量
     * @param topHosts              请求最多的 Host
     * @param topPaths              请求最多的 Path
     */
    public record StatsSnapshot(
            long total,
            List<CountRow> categoryCounts,
            long suspiciousAttackCount,
            List<CountRow> topHosts,
            List<CountRow> topPaths
    ) {
    }
}
