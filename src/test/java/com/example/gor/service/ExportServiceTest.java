package com.example.gor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gor.entity.RiskLevel;
import com.example.gor.entity.TrafficRequest;
import com.example.gor.mapper.TrafficRequestMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ExportServiceTest {
    @Autowired
    private TrafficRequestMapper trafficRequestMapper;

    @Autowired
    private ExportService exportService;

    @Test
    void exportsByCategoryUsingRawGorText() throws Exception {
        trafficRequestMapper.deleteAll();
        trafficRequestMapper.insert(request("login", "1 1 0\nGET /login HTTP/1.1\nHost: example.com\n\n🐵🙈🙉\n"));
        trafficRequestMapper.insert(request("api", "1 2 0\nGET /api/users HTTP/1.1\nHost: example.com\n\n🐵🙈🙉\n"));
        Path output = Files.createTempFile("login", ".gor");

        long count = exportService.exportByCategory("login", output);

        assertThat(count).isEqualTo(1);
        assertThat(Files.readString(output)).contains("GET /login").doesNotContain("/api/users");
    }

    private TrafficRequest request(String category, String raw) {
        TrafficRequest request = new TrafficRequest();
        request.setSourceFile("test.gor");
        request.setRequestNo(1);
        request.setMethod("GET");
        request.setHost("example.com");
        request.setUrl("/login");
        request.setPath("/login");
        request.setHeadersJson("{}");
        request.setRawGorText(raw);
        request.setCategory(category);
        request.setTags(category);
        request.setRiskLevel(RiskLevel.LOW);
        request.setCreatedAt(Instant.now());
        return request;
    }
}
