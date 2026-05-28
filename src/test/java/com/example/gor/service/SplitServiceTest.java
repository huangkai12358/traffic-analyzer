package com.example.gor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.gor.fixtures.RealGorSamples;
import com.example.gor.parser.GorRequestReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SplitServiceTest {
    @Test
    void splitsByMaxCount() throws Exception {
        Path input = Files.createTempFile("split", ".gor");
        Files.writeString(input, RealGorSamples.AUTH_AND_USER_API);
        Path outputDir = Files.createTempDirectory("parts");

        long parts = new SplitService(new GorRequestReader()).splitByMaxCount(input, 2, outputDir);

        assertThat(parts).isEqualTo(2);
        assertThat(outputDir.resolve("part-00001.gor")).exists();
        assertThat(outputDir.resolve("part-00002.gor")).exists();
        assertThat(Files.readString(outputDir.resolve("part-00001.gor"))).contains("/api/auth/login", "/api/users/page").doesNotContain("/api/users/52/status");
    }

    @Test
    void rejectsMissingInputFileWithReadableMessage() throws Exception {
        Path missingInput = Files.createTempDirectory("missing-input").resolve("input.gor");
        Path outputDir = Files.createTempDirectory("missing-input-parts");

        assertThatThrownBy(() -> new SplitService(new GorRequestReader()).splitByMaxCount(missingInput, 1, outputDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("input file does not exist");
    }
}
