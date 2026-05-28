package com.example.gor.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gor.fixtures.RealGorSamples;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GorHttpParserTest {
    @Test
    void parsesMethodPathHeadersAndBody() {
        String raw = firstRecord();

        ParsedHttpRequest request = new GorHttpParser().parse(raw);

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.host()).isEqualTo("localhost:8083");
        assertThat(request.path()).isEqualTo("/api/auth/login");
        assertThat(request.query()).isNull();
        assertThat(request.contentType()).isEqualTo("application/json");
        assertThat(request.userAgent()).isEqualTo("Apifox/1.0.0 (https://apifox.com)");
        assertThat(request.body()).contains("\"username\": \"admin\"", "\"password\": \"123456\"");
    }

    private String firstRecord() {
        try {
            Path input = Files.createTempFile("real-sample", ".gor");
            Files.writeString(input, RealGorSamples.AUTH_AND_USER_API);
            List<GorRecord> records = new ArrayList<>();
            new GorRequestReader().readRequests(input, records::add);
            return records.getFirst().rawText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create real .gor sample", e);
        }
    }
}
