package com.example.gor.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gor.parser.GorRequestReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SplitServiceTest {
    @Test
    void splitsByMaxCount() throws Exception {
        Path input = Files.createTempFile("split", ".gor");
        Files.writeString(input, """
                1 1 0
                GET /a HTTP/1.1
                Host: example.com

                🐵🙈🙉
                1 2 0
                GET /b HTTP/1.1
                Host: example.com

                🐵🙈🙉
                1 3 0
                GET /c HTTP/1.1
                Host: example.com

                🐵🙈🙉
                """);
        Path outputDir = Files.createTempDirectory("parts");

        long parts = new SplitService(new GorRequestReader()).splitByMaxCount(input, 2, outputDir);

        assertThat(parts).isEqualTo(2);
        assertThat(outputDir.resolve("part-00001.gor")).exists();
        assertThat(outputDir.resolve("part-00002.gor")).exists();
        assertThat(Files.readString(outputDir.resolve("part-00001.gor"))).contains("GET /a", "GET /b").doesNotContain("GET /c");
    }
}
