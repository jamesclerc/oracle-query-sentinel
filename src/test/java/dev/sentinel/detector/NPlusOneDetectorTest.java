package dev.sentinel.detector;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.model.AnomalyType;
import dev.sentinel.model.SqlSample;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NPlusOneDetectorTest {

    private final SentinelProperties props = new SentinelProperties(
            30,
            new SentinelProperties.Rules(2000, 10_000, 5, 10, 5),
            List.of("HR"),
            new SentinelProperties.Store(1000));

    @Test
    void flagsRepeatedPlanHashAboveThreshold() {
        var detector = new NPlusOneDetector(props);
        var s = new SqlSample("a", "HR", "select * from t where id=?", 10, 1, 7, "PLAN1", false);
        var hits = detector.detect(List.of(s));
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).type()).isEqualTo(AnomalyType.N_PLUS_ONE);
    }

    @Test
    void ignoresBelowThreshold() {
        var detector = new NPlusOneDetector(props);
        var s = new SqlSample("a", "HR", "x", 10, 1, 2, "PLAN1", false);
        assertThat(detector.detect(List.of(s))).isEmpty();
    }
}
