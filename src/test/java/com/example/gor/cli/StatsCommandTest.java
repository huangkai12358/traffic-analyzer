package com.example.gor.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gor.mapper.TrafficRequestMapper.CountRow;
import com.example.gor.service.StatsService;
import com.example.gor.service.StatsService.StatsSnapshot;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatsCommandTest {
    @Test
    void printsReadableGroupedStatistics() throws Exception {
        StatsCommand command = new StatsCommand(new StubStatsService());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            int exitCode = command.call();

            String text = output.toString(StandardCharsets.UTF_8);
            assertThat(exitCode).isZero();
            assertThat(text).contains("GoReplay Traffic Statistics");
            assertThat(text).contains("Total Requests");
            assertThat(text).contains("High Risk Requests");
            assertThat(text).contains("Category Distribution");
            assertThat(text).contains("sql_injection");
            assertThat(text).contains("20.0%");
            assertThat(text).contains("Top Hosts");
            assertThat(text).contains("demo.example.com");
            assertThat(text).contains("Top Paths");
            assertThat(text).contains("/api/auth/login");
        } finally {
            System.setOut(originalOut);
        }
    }

    private static class StubStatsService extends StatsService {
        StubStatsService() {
            super(null);
        }

        @Override
        public StatsSnapshot snapshot() {
            return new StatsSnapshot(
                    10,
                    List.of(new CountRow("api", 5), new CountRow("sql_injection", 2)),
                    2,
                    List.of(new CountRow("demo.example.com", 8)),
                    List.of(new CountRow("/api/auth/login", 3))
            );
        }
    }
}
