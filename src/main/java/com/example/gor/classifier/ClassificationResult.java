package com.example.gor.classifier;

import com.example.gor.entity.RiskLevel;
import java.util.List;

/**
 * 表示一次请求分类后的结果。
 *
 * @param category  主分类，只能有一个，用于统计和按分类导出
 * @param tags      命中的所有标签，一个请求可以同时命中 login、sql_injection 等多个标签
 * @param riskLevel 风险等级，当前攻击类规则统一标记为 HIGH
 */
public record ClassificationResult(String category, List<String> tags, RiskLevel riskLevel) {
}
