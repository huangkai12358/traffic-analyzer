package com.example.gor.parser;

import java.util.List;
import java.util.Map;

/**
 * 表示从 GoReplay 原始请求文本中解析出来的 HTTP 请求字段。
 *
 * <p>该对象只承载便于查询、统计、分类的结构化字段；真正用于无损导出的内容仍然是数据库中的 rawGorText。</p>
 *
 * @param method      HTTP method，例如 GET、POST
 * @param host        Host header 的值
 * @param url         请求行中的原始 URL，可能包含 query string
 * @param path        URL path，不包含 query string
 * @param query       原始 query string，不做业务拆分
 * @param headers     HTTP headers，重复 header 会保留多个值
 * @param body        HTTP body，可能为空字符串
 * @param contentType Content-Type header
 * @param userAgent   User-Agent header
 */
public record ParsedHttpRequest(
        String method,
        String host,
        String url,
        String path,
        String query,
        Map<String, List<String>> headers,
        String body,
        String contentType,
        String userAgent
) {
}
