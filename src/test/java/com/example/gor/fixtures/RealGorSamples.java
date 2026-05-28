package com.example.gor.fixtures;

/**
 * 测试用真实风格 GoReplay .gor 样例。
 */
public final class RealGorSamples {
    public static final String AUTH_AND_USER_API = """
            1 f9ad1f9300000001d8ac16a1 1779950763211073000 0
            POST /api/auth/login HTTP/1.1
            User-Agent: Apifox/1.0.0 (https://apifox.com)
            Content-Type: application/json
            Accept: */*
            Host: localhost:8083
            Accept-Encoding: gzip, deflate, br
            Connection: keep-alive
            Content-Length: 53

            {
                "username": "admin",
                "password": "123456"
            }
            🐵🙈🙉
            1 f9ad1f9300000001d8ac18a2 1779950773081008000 0
            GET /api/users/page?username=test&pageNum=1&pageSize=2&sortField=createTime&sortOrder=desc HTTP/1.1
            User-Agent: Apifox/1.0.0 (https://apifox.com)
            Authorization: Bearer token
            Accept: */*
            Host: localhost:8083
            Accept-Encoding: gzip, deflate, br
            Connection: keep-alive


            🐵🙈🙉
            1 f9ad1f9300000001d8ac1ba0 1779950779985618000 0
            PUT /api/users/52/status HTTP/1.1
            User-Agent: Apifox/1.0.0 (https://apifox.com)
            Content-Type: application/json
            Authorization: Bearer token
            Accept: */*
            Host: localhost:8083
            Accept-Encoding: gzip, deflate, br
            Connection: keep-alive
            Content-Length: 19

            {
                "status": 0
            }
            🐵🙈🙉
            1 f9ad1f9300000001d8ac1cd0 1779950785608067000 0
            POST /api/users/52/password/reset HTTP/1.1
            User-Agent: Apifox/1.0.0 (https://apifox.com)
            Authorization: Bearer token
            Accept: */*
            Host: localhost:8083
            Accept-Encoding: gzip, deflate, br
            Connection: keep-alive
            Content-Length: 0


            🐵🙈🙉
            """;

    private RealGorSamples() {
    }
}
