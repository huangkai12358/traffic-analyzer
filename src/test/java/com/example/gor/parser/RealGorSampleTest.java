package com.example.gor.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gor.classifier.TrafficClassifier;
import com.example.gor.fixtures.RealGorSamples;
import com.example.gor.service.SplitService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RealGorSampleTest {
    @Test
    void readsAndParsesRealGorSample() throws Exception {
        Path input = Files.createTempFile("real-sample", ".gor");
        Files.writeString(input, RealGorSamples.AUTH_AND_USER_API);

        List<GorRecord> records = new ArrayList<>();
        new GorRequestReader().readRequests(input, records::add);

        assertThat(records).hasSize(4);

        GorHttpParser parser = new GorHttpParser();
        ParsedHttpRequest login = parser.parse(records.get(0).rawText());
        ParsedHttpRequest page = parser.parse(records.get(1).rawText());
        ParsedHttpRequest status = parser.parse(records.get(2).rawText());
        ParsedHttpRequest reset = parser.parse(records.get(3).rawText());

        assertThat(login.method()).isEqualTo("POST");
        assertThat(login.path()).isEqualTo("/api/auth/login");
        assertThat(login.host()).isEqualTo("localhost:8083");
        assertThat(login.contentType()).isEqualTo("application/json");
        assertThat(login.body()).contains("\"username\": \"admin\"", "\"password\": \"123456\"");

        assertThat(page.method()).isEqualTo("GET");
        assertThat(page.path()).isEqualTo("/api/users/page");
        assertThat(page.query()).isEqualTo("username=test&pageNum=1&pageSize=2&sortField=createTime&sortOrder=desc");
        assertThat(page.body()).isEmpty();

        assertThat(status.method()).isEqualTo("PUT");
        assertThat(status.path()).isEqualTo("/api/users/52/status");
        assertThat(status.body()).contains("\"status\": 0");

        assertThat(reset.method()).isEqualTo("POST");
        assertThat(reset.path()).isEqualTo("/api/users/52/password/reset");
        assertThat(reset.body()).isEmpty();
    }

    @Test
    void classifiesAuthorizedApiRequestsAsApiNotLogin() throws Exception {
        Path input = Files.createTempFile("real-sample", ".gor");
        Files.writeString(input, RealGorSamples.AUTH_AND_USER_API);

        List<GorRecord> records = new ArrayList<>();
        new GorRequestReader().readRequests(input, records::add);

        GorHttpParser parser = new GorHttpParser();
        TrafficClassifier classifier = new TrafficClassifier();

        assertThat(classifier.classify(parser.parse(records.get(0).rawText())).category()).isEqualTo("login");
        assertThat(classifier.classify(parser.parse(records.get(1).rawText())).category()).isEqualTo("api");
        assertThat(classifier.classify(parser.parse(records.get(2).rawText())).category()).isEqualTo("api");
        assertThat(classifier.classify(parser.parse(records.get(3).rawText())).category()).isEqualTo("api");
    }

    @Test
    void splitsRealGorSampleByCount() throws Exception {
        Path input = Files.createTempFile("real-sample", ".gor");
        Path outputDir = Files.createTempDirectory("real-sample-parts");
        Files.writeString(input, RealGorSamples.AUTH_AND_USER_API);

        long parts = new SplitService(new GorRequestReader()).splitByMaxCount(input, 2, outputDir);

        assertThat(parts).isEqualTo(2);
        assertThat(Files.readString(outputDir.resolve("part-00001.gor"))).contains("/api/auth/login", "/api/users/page");
        assertThat(Files.readString(outputDir.resolve("part-00002.gor"))).contains("/api/users/52/status", "/api/users/52/password/reset");
    }
}
