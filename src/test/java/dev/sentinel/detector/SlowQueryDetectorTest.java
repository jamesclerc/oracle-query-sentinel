package dev.sentinel.detector;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.model.AnomalyType;
import dev.sentinel.model.SqlSample;
import dev.sentinel.repository.VSqlRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SlowQueryDetectorTest {

    private final SentinelProperties props = new SentinelProperties(
            30,
            new SentinelProperties.Rules(2000, 10_000, 5, 10, 5),
            List.of("HR"),
            new SentinelProperties.Store(1000));

    @Test
    void flagsSlowSingleExecution() {
        var detector = new SlowQueryDetector(props, mock(VSqlRepository.class));
        var s = new SqlSample("a", "HR", "x", 5000, 100, 1, "p", false);
        var hits = detector.detect(List.of(s));
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).type()).isEqualTo(AnomalyType.SLOW_QUERY);
        assertThat(hits.get(0).elapsedMs()).isEqualTo(5000);
    }

    @Test
    void normalizesByExecutionCount() {
        var detector = new SlowQueryDetector(props, mock(VSqlRepository.class));
        var s = new SqlSample("a", "HR", "x", 5000, 100, 100, "p", false);
        assertThat(detector.detect(List.of(s))).isEmpty();
    }
}
