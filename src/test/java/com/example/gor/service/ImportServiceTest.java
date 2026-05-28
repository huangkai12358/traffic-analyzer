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
class ImportServiceTest {
    @Autowired
    private ImportService importService;

    @Autowired
    private TrafficRequestMapper trafficRequestMapper;

    @Test
    void clearsExistingRequestsBeforeImportWhenRequested() throws Exception {
        trafficRequestMapper.deleteAll();
        trafficRequestMapper.insert(existingRequest());
        Path input = Files.createTempFile("real-sample", ".gor");
        Files.writeString(input, RealGorSamples.AUTH_AND_USER_API);

        long imported = importService.importFile(input, true);

        assertThat(imported).isEqualTo(4);
        assertThat(trafficRequestMapper.count()).isEqualTo(4);
        assertThat(trafficRequestMapper.countByCategory("login")).isEqualTo(1);
    }

    @Test
    void importsMultipleFilesAndClearsOnlyOnceBeforeBatch() throws Exception {
        trafficRequestMapper.deleteAll();
        trafficRequestMapper.insert(existingRequest());
        Path first = Files.createTempFile("first-real-sample", ".gor");
        Path second = Files.createTempFile("second-real-sample", ".gor");
        Files.writeString(first, RealGorSamples.AUTH_AND_USER_API);
        Files.writeString(second, RealGorSamples.AUTH_AND_USER_API);

        long imported = importService.importFiles(List.of(first, second), true);

        assertThat(imported).isEqualTo(8);
        assertThat(trafficRequestMapper.count()).isEqualTo(8);
        assertThat(trafficRequestMapper.countByCategory("login")).isEqualTo(2);
    }

    @Test
    void importsAllGorFilesFromDirectory() throws Exception {
        trafficRequestMapper.deleteAll();
        Path dir = Files.createTempDirectory("gor-import-dir");
        Files.writeString(dir.resolve("b.gor"), RealGorSamples.AUTH_AND_USER_API);
        Files.writeString(dir.resolve("a.gor"), RealGorSamples.AUTH_AND_USER_API);
        Files.writeString(dir.resolve("ignored.txt"), RealGorSamples.AUTH_AND_USER_API);

        long imported = importService.importDirectory(dir, true);

        assertThat(imported).isEqualTo(8);
        assertThat(trafficRequestMapper.count()).isEqualTo(8);
    }

    private TrafficRequest existingRequest() {
        TrafficRequest request = new TrafficRequest();
        request.setSourceFile("old.gor");
        request.setRequestNo(1);
        request.setMethod("GET");
        request.setHost("old.example");
        request.setUrl("/old");
        request.setPath("/old");
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
