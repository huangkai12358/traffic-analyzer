package com.example.gor.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GorHttpParserTest {
    @Test
    void parsesMethodPathHeadersAndBody() {
        String raw = """
                1 1 0
                POST /api/login?next=/home HTTP/1.1
                Host: example.com
                User-Agent: test-agent
                Content-Type: application/json

                {"name":"demo"}
                🐵🙈🙉
                """;

        ParsedHttpRequest request = new GorHttpParser().parse(raw);

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.host()).isEqualTo("example.com");
        assertThat(request.path()).isEqualTo("/api/login");
        assertThat(request.query()).isEqualTo("next=/home");
        assertThat(request.contentType()).isEqualTo("application/json");
        assertThat(request.userAgent()).isEqualTo("test-agent");
        assertThat(request.body()).isEqualTo("{\"name\":\"demo\"}");
    }
}
