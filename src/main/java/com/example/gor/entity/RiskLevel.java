package com.example.gor.entity;

/**
 * 请求风险等级。
 *
 * <p>MVP 中普通流量为 LOW，命中攻击特征为 HIGH，MEDIUM 预留给后续更细粒度规则。</p>
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
