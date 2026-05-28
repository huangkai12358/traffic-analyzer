package com.example.gor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gor.entity.RiskLevel;
import com.example.gor.entity.TrafficRequest;
import com.example.gor.fixtures.RealGorSamples;
import com.example.gor.mapper.TrafficRequestMapper;
import com.example.gor.parser.GorRecord;
import com.example.gor.parser.GorRequestReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
        List<GorRecord> records = realRecords();
        trafficRequestMapper.insert(request("login", records.get(0).rawText()));
        trafficRequestMapper.insert(request("api", records.get(1).rawText()));
        Path output = Files.createTempFile("login", ".gor");

        long count = exportService.exportByCategory("login", output);

        assertThat(count).isEqualTo(1);
        assertThat(Files.readString(output)).contains("/api/auth/login").doesNotContain("/api/users/page");
    }

    private List<GorRecord> realRecords() throws Exception {
        Path input = Files.createTempFile("real-sample", ".gor");
        Files.writeString(input, RealGorSamples.AUTH_AND_USER_API);
        List<GorRecord> records = new ArrayList<>();
        new GorRequestReader().readRequests(input, records::add);
        return records;
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
