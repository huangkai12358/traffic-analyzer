package com.example.gor.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gor.fixtures.RealGorSamples;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GorRequestReaderTest {
    @Test
    void splitsOnlyRequestRecords() throws Exception {
        Path file = Files.createTempFile("sample", ".gor");
        Files.writeString(file, RealGorSamples.AUTH_AND_USER_API);

        List<GorRecord> records = new ArrayList<>();
        new GorRequestReader().readRequests(file, records::add);

        assertThat(records).hasSize(4);
        assertThat(records.get(0).requestNo()).isEqualTo(1);
        assertThat(records.get(1).rawText()).contains("GET /api/users/page");
    }

    @Test
    void preservesRawRecordTextExactly() throws Exception {
        Path file = Files.createTempFile("raw-preserve", ".gor");
        String firstRecord = "1 1 100 0\r\n"
                + "GET /a HTTP/1.1\r\n"
                + "Host: example.com\r\n"
                + "\r\n"
                + GorRequestReader.DELIMITER
                + "\r\n";
        String secondRecord = "1 2 200 0\n"
                + "GET /b HTTP/1.1\n"
                + "Host: example.com\n"
                + "\n";
        Files.writeString(file, firstRecord + secondRecord);

        List<GorRecord> records = new ArrayList<>();
        new GorRequestReader().readRequests(file, records::add);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).rawText()).isEqualTo(firstRecord);
        assertThat(records.get(1).rawText()).isEqualTo(secondRecord);
    }
}
