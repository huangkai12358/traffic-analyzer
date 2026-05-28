package com.example.gor.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GorRequestReaderTest {
    @Test
    void splitsOnlyRequestRecords() throws Exception {
        Path file = Files.createTempFile("sample", ".gor");
        Files.writeString(file, """
                1 1 0
                GET /api/a HTTP/1.1
                Host: example.com

                🐵🙈🙉
                2 1 0
                HTTP/1.1 200 OK

                🐵🙈🙉
                1 2 0
                GET /api/b HTTP/1.1
                Host: example.com

                🐵🙈🙉
                """);

        List<GorRecord> records = new ArrayList<>();
        new GorRequestReader().readRequests(file, records::add);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).requestNo()).isEqualTo(1);
        assertThat(records.get(1).rawText()).contains("GET /api/b HTTP/1.1");
    }
}
