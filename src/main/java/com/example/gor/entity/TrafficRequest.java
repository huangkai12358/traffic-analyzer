package com.example.gor.entity;

import java.time.Instant;

/**
 * traffic_request 表对应的数据对象。
 *
 * <p>该类不包含业务逻辑，只承载 MyBatis 写入/读取数据库所需字段。
 * 其中 rawGorText 是无损导出的核心字段，其他结构化字段用于查询、统计和分类。</p>
 */
public class TrafficRequest {
    /** 数据库自增主键。 */
    private Long id;

    /** 来源 .gor 文件的绝对路径。 */
    private String sourceFile;

    /** 来源文件内的请求序号，从 1 开始。 */
    private long requestNo;

    /** HTTP method，例如 GET、POST。 */
    private String method;

    /** Host header。 */
    private String host;

    /** 请求行中的原始 URL。 */
    private String url;

    /** URL path，不包含 query string。 */
    private String path;

    /** 原始 query string。 */
    private String queryString;

    /** headers 的 JSON 序列化结果。 */
    private String headersJson;

    /** HTTP body。 */
    private String body;

    /** GoReplay 原始请求文本，用于无损导出和回放。 */
    private String rawGorText;

    /** 主分类，只允许一个。 */
    private String category;

    /** 所有命中的标签，逗号分隔。 */
    private String tags;

    /** 风险等级。 */
    private RiskLevel riskLevel;

    /** 入库时间。 */
    private Instant createdAt;

    /** Content-Type header。 */
    private String contentType;

    /** User-Agent header。 */
    private String userAgent;

    /**
     * 获取数据库自增主键。
     *
     * @return 数据库 id
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置数据库自增主键。
     *
     * @param id 数据库 id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取来源文件路径。
     *
     * @return 来源 .gor 文件路径
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * 设置来源文件路径。
     *
     * @param sourceFile 来源 .gor 文件路径
     */
    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * 获取文件内请求序号。
     *
     * @return 请求序号
     */
    public long getRequestNo() {
        return requestNo;
    }

    /**
     * 设置文件内请求序号。
     *
     * @param requestNo 请求序号
     */
    public void setRequestNo(long requestNo) {
        this.requestNo = requestNo;
    }

    /**
     * 获取 HTTP method。
     *
     * @return HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * 设置 HTTP method。
     *
     * @param method HTTP method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * 获取 Host header。
     *
     * @return Host header
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置 Host header。
     *
     * @param host Host header
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取请求 URL。
     *
     * @return 请求 URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * 设置请求 URL。
     *
     * @param url 请求 URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 获取 URL path。
     *
     * @return URL path
     */
    public String getPath() {
        return path;
    }

    /**
     * 设置 URL path。
     *
     * @param path URL path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 获取 query string。
     *
     * @return query string
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * 设置 query string。
     *
     * @param queryString query string
     */
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    /**
     * 获取 headers JSON。
     *
     * @return headers JSON 字符串
     */
    public String getHeadersJson() {
        return headersJson;
    }

    /**
     * 设置 headers JSON。
     *
     * @param headersJson headers JSON 字符串
     */
    public void setHeadersJson(String headersJson) {
        this.headersJson = headersJson;
    }

    /**
     * 获取 HTTP body。
     *
     * @return HTTP body
     */
    public String getBody() {
        return body;
    }

    /**
     * 设置 HTTP body。
     *
     * @param body HTTP body
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * 获取 GoReplay 原始文本。
     *
     * @return rawGorText
     */
    public String getRawGorText() {
        return rawGorText;
    }

    /**
     * 设置 GoReplay 原始文本。
     *
     * @param rawGorText 用于无损导出的原始文本
     */
    public void setRawGorText(String rawGorText) {
        this.rawGorText = rawGorText;
    }

    /**
     * 获取主分类。
     *
     * @return 主分类
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置主分类。
     *
     * @param category 主分类
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 获取标签列表字符串。
     *
     * @return 逗号分隔标签
     */
    public String getTags() {
        return tags;
    }

    /**
     * 设置标签列表字符串。
     *
     * @param tags 逗号分隔标签
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * 获取风险等级。
     *
     * @return 风险等级
     */
    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    /**
     * 设置风险等级。
     *
     * @param riskLevel 风险等级
     */
    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    /**
     * 获取入库时间。
     *
     * @return 入库时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置入库时间。
     *
     * @param createdAt 入库时间
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取 Content-Type。
     *
     * @return Content-Type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 设置 Content-Type。
     *
     * @param contentType Content-Type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 获取 User-Agent。
     *
     * @return User-Agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * 设置 User-Agent。
     *
     * @param userAgent User-Agent
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
