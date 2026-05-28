package com.example.gor.parser;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 将单条 GoReplay 请求记录解析成结构化 HTTP 字段。
 *
 * <p>GoReplay payload 的第一行是元信息，第二行开始才是 HTTP 请求行，因此解析时会跳过第一行。
 * 该解析器尽量容错：URL 不合法时会退回到字符串切分方式提取 path/query。</p>
 */
@Component
public class GorHttpParser {
    /**
     * 解析单条 GoReplay 请求原文。
     *
     * @param rawGorText 由 {@link GorRequestReader} 切分出来的原始请求文本
     * @return 结构化 HTTP 请求对象
     * @throws IllegalArgumentException 当请求缺少 HTTP 请求行或请求行格式明显不合法时抛出
     */
    public ParsedHttpRequest parse(String rawGorText) {
        String normalized = rawGorText.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);
        if (lines.length < 2) {
            throw new IllegalArgumentException("Invalid .gor request: missing HTTP request line");
        }

        String requestLine = lines[1];
        String[] parts = requestLine.split("\\s+", 3);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid HTTP request line: " + requestLine);
        }

        String method = parts[0];
        String url = parts[1];
        Map<String, List<String>> headers = new LinkedHashMap<>();
        int i = 2;
        // Header 区到空行结束；重复 header 用 List 保留，避免覆盖 Set-Cookie 等字段。
        for (; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                i++;
                break;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            headers.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }

        StringBuilder body = new StringBuilder();
        // 空行之后直到 GoReplay 分隔符之前都视为 body，适配 JSON 和 multipart。
        for (; i < lines.length; i++) {
            String line = lines[i];
            if (line.equals(GorRequestReader.DELIMITER)) {
                break;
            }
            body.append(line);
            if (i < lines.length - 1) {
                body.append('\n');
            }
        }

        String host = firstHeader(headers, "Host");
        String path = parsePath(url);
        String query = parseQuery(url);
        return new ParsedHttpRequest(
                method,
                host,
                url,
                path,
                query,
                headers,
                trimTrailingLineBreak(body.toString()),
                firstHeader(headers, "Content-Type"),
                firstHeader(headers, "User-Agent")
        );
    }

    /**
     * 从请求 URL 中提取 path。
     *
     * @param url 请求行中的 URL，可能是相对路径，也可能包含 query
     * @return URL path；无法解析时返回问号前的原始片段
     */
    private String parsePath(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
        } catch (IllegalArgumentException e) {
            int question = url.indexOf('?');
            return question >= 0 ? url.substring(0, question) : url;
        }
    }

    /**
     * 从请求 URL 中提取原始 query string。
     *
     * @param url 请求行中的 URL
     * @return query string，不包含问号；没有 query 时返回 null
     */
    private String parseQuery(String url) {
        try {
            return URI.create(url).getRawQuery();
        } catch (IllegalArgumentException e) {
            int question = url.indexOf('?');
            return question >= 0 && question + 1 < url.length() ? url.substring(question + 1) : null;
        }
    }

    /**
     * 按大小写不敏感方式读取指定 header 的第一个值。
     *
     * @param headers 已解析的 header map
     * @param name    要查找的 header 名称
     * @return 第一个匹配值；不存在时返回 null
     */
    private String firstHeader(Map<String, List<String>> headers, String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)))
                .findFirst()
                .flatMap(entry -> entry.getValue().stream().findFirst())
                .orElse(null);
    }

    /**
     * 去掉 body 末尾由逐行拼接额外产生的单个换行符。
     *
     * @param value 原始 body 文本
     * @return 规整后的 body 文本
     */
    private String trimTrailingLineBreak(String value) {
        return value.endsWith("\n") ? value.substring(0, value.length() - 1) : value;
    }
}
