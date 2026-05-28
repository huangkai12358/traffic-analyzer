package com.example.gor.classifier;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.gor.entity.RiskLevel;
import com.example.gor.parser.ParsedHttpRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TrafficClassifierTest {
    private final TrafficClassifier classifier = new TrafficClassifier();

    @Test
    void classifiesLoginAndAttackTags() {
        ParsedHttpRequest request = new ParsedHttpRequest(
                "GET",
                "example.com",
                "/api/login?q=1 union select password",
                "/api/login",
                "q=1 union select password",
                Map.of(),
                "",
                null,
                null
        );

        ClassificationResult result = classifier.classify(request);

        assertThat(result.category()).isEqualTo("sql_injection");
        assertThat(result.tags()).contains("login", "sql_injection");
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void classifiesUrlEncodedSqlInjection() {
        ParsedHttpRequest request = new ParsedHttpRequest(
                "GET",
                "example.com",
                "/api/search?q=1%20union%20select%20password",
                "/api/search",
                "q=1%20union%20select%20password",
                Map.of(),
                "",
                null,
                null
        );

        ClassificationResult result = classifier.classify(request);

        assertThat(result.category()).isEqualTo("sql_injection");
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void classifiesStaticResource() {
        ClassificationResult result = classifier.classify(new ParsedHttpRequest(
                "GET", "static.example.com", "/assets/app.js", "/assets/app.js", null, Map.of(), "", null, null
        ));

        assertThat(result.category()).isEqualTo("static_resource");
        assertThat(result.tags()).isEqualTo(List.of("static_resource"));
    }
}
