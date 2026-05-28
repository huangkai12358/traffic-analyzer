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
class ClearServiceTest {
    @Autowired
    private ClearService clearService;

    @Autowired
    private TrafficRequestMapper trafficRequestMapper;

    @Test
    void clearsAllImportedRequests() {
        trafficRequestMapper.deleteAll();
        trafficRequestMapper.insert(request("one.gor"));
        trafficRequestMapper.insert(request("two.gor"));

        int deleted = clearService.clearRequests();

        assertThat(deleted).isEqualTo(2);
        assertThat(trafficRequestMapper.count()).isZero();
    }

    private TrafficRequest request(String sourceFile) {
        TrafficRequest request = new TrafficRequest();
        request.setSourceFile(sourceFile);
        request.setRequestNo(1);
        request.setMethod("GET");
        request.setHost("localhost:8083");
        request.setUrl("/api/users/page");
        request.setPath("/api/users/page");
        request.setHeadersJson("{}");
        request.setRawGorText(firstRealRawText());
        request.setCategory("api");
        request.setTags("api");
        request.setRiskLevel(RiskLevel.LOW);
        request.setCreatedAt(Instant.now());
        return request;
    }

    private String firstRealRawText() {
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
