package com.example.gor.service;

import com.example.gor.mapper.TrafficRequestMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据清理服务。
 *
 * <p>当前 CLI 使用模式偏向“本次处理、本次导出”，因此提供清空历史请求的能力，
 * 避免导出时混入之前导入的 .gor 文件内容。</p>
 */
@Service
public class ClearService {
    private final TrafficRequestMapper trafficRequestMapper;

    /**
     * 创建清理服务。
     *
     * @param trafficRequestMapper 请求数据访问 Mapper
     */
    public ClearService(TrafficRequestMapper trafficRequestMapper) {
        this.trafficRequestMapper = trafficRequestMapper;
    }

    /**
     * 清空 traffic_request 表。
     *
     * @return 被删除的请求数量
     */
    @Transactional
    public int clearRequests() {
        return trafficRequestMapper.deleteAll();
    }
}
