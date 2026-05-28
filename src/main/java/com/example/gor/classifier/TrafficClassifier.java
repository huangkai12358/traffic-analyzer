package com.example.gor.classifier;

import com.example.gor.entity.RiskLevel;
import com.example.gor.parser.ParsedHttpRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 基于规则的请求分类器。
 *
 * <p>分类器会检查 path、query、headers、body，并给请求打多个 tag；主 category 只保留一个。
 * 攻击类规则优先级高于普通业务类规则，避免疑似攻击请求被普通 api/login 分类掩盖。</p>
 */
@Component
public class TrafficClassifier {
    /**
     * 对解析后的 HTTP 请求进行分类。
     *
     * @param request 已解析的 HTTP 请求字段
     * @return 分类结果，包含主分类、所有命中标签和风险等级
     */
    public ClassificationResult classify(ParsedHttpRequest request) {
        String rawText = String.join("\n",
                safe(request.path()),
                safe(request.query()),
                request.headers().toString(),
                safe(request.body())
        ).toLowerCase(Locale.ROOT);
        // 同时检查 URL 编码和解码后的内容，否则 union%20select 这类 payload 会漏报。
        String text = rawText + "\n" + urlDecode(rawText);

        List<String> tags = new ArrayList<>();
        addIf(tags, safe(request.path()).toLowerCase(Locale.ROOT)
                .matches(".*\\.(js|css|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf|map)$"), "static_resource");
        addIf(tags, containsAny(text, "login", "auth", "sso", "token", "oauth"), "login");
        addIf(tags, containsAny(text, "upload", "import", "file"), "upload");
        addIf(tags, containsAny(text, "download", "export"), "download");
        addIf(tags, containsAny(text, "union select", "or 1=1", "sleep(", "benchmark("), "sql_injection");
        addIf(tags, containsAny(text, "<script", "onerror=", "javascript:"), "xss");
        addIf(tags, containsAny(text, "../", "..\\", "/etc/passwd"), "path_traversal");
        addIf(tags, containsAny(text, "whoami", "cmd=", "bash", "curl") || containsStandaloneId(text), "command_injection");
        if (looksLikeApi(request.path()) && tags.stream().noneMatch(this::isAttackTag)) {
            tags.add("api");
        }

        String category = choosePrimary(tags);
        RiskLevel risk = tags.stream().anyMatch(this::isAttackTag) ? RiskLevel.HIGH : RiskLevel.LOW;
        return new ClassificationResult(category, List.copyOf(tags), risk);
    }

    /**
     * 根据 tag 列表选择唯一主分类。
     *
     * @param tags 当前请求命中的所有标签
     * @return 主分类；没有命中任何规则时返回 unknown
     */
    private String choosePrimary(List<String> tags) {
        // 攻击类 tag 优先成为主分类；其他匹配仍保留在 tags 字段里。
        for (String attack : List.of("sql_injection", "xss", "path_traversal", "command_injection")) {
            if (tags.contains(attack)) {
                return attack;
            }
        }
        for (String normal : List.of("login", "upload", "download", "static_resource", "api")) {
            if (tags.contains(normal)) {
                return normal;
            }
        }
        return "unknown";
    }

    /**
     * 判断一个 path 是否像普通 API。
     *
     * @param path URL path
     * @return true 表示该 path 没有明显静态资源后缀，或以 /api/ 开头
     */
    private boolean looksLikeApi(String path) {
        String value = safe(path).toLowerCase(Locale.ROOT);
        return value.startsWith("/api/") || !value.contains(".");
    }

    /**
     * 判断 tag 是否属于攻击类标签。
     *
     * @param tag 分类标签
     * @return true 表示该标签会让请求风险升级为 HIGH
     */
    private boolean isAttackTag(String tag) {
        return List.of("sql_injection", "xss", "path_traversal", "command_injection").contains(tag);
    }

    /**
     * 检查文本是否包含任意一个给定特征。
     *
     * @param text     已规整为小写的待检查文本
     * @param patterns 小写特征词列表
     * @return 命中任意特征时返回 true
     */
    private boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文本中是否出现独立的 id 命令特征。
     *
     * <p>这里避免把 userId、order_id 这类业务字段误判为命令注入。</p>
     *
     * @param text 待检查文本
     * @return true 表示出现独立的 id token
     */
    private boolean containsStandaloneId(String text) {
        return text.matches("(?s).*(^|[^a-z0-9_])id([^a-z0-9_]|$).*");
    }

    /**
     * 在规则命中时添加 tag，并避免重复添加。
     *
     * @param tags    当前 tag 列表
     * @param matched 规则是否命中
     * @param tag     需要添加的标签
     */
    private void addIf(List<String> tags, boolean matched, String tag) {
        if (matched && !tags.contains(tag)) {
            tags.add(tag);
        }
    }

    /**
     * 将 null 安全转换为空字符串，简化分类规则中的字符串处理。
     *
     * @param value 可能为 null 的字符串
     * @return 非 null 字符串
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 尝试对 URL 编码内容进行解码。
     *
     * @param value 原始文本
     * @return 解码后的文本；遇到非法编码时返回原文
     */
    private String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
