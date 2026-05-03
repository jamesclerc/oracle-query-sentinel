package dev.sentinel.detector;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.model.Anomaly;
import dev.sentinel.model.AnomalyType;
import dev.sentinel.model.SqlSample;
import dev.sentinel.repository.VSqlRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FullTableScanDetectorTest {

    private final SentinelProperties props = new SentinelProperties(
            30,
            new SentinelProperties.Rules(2000, 10_000, 5, 10, 5),
            List.of("HR"),
            new SentinelProperties.Store(1000));

    @Test
    void flagsFullScanAboveRowThreshold() {
        VSqlRepository vSql = mock(VSqlRepository.class);
        when(vSql.fetchExplainPlan("abc")).thenReturn("TABLE ACCESS FULL");
        var detector = new FullTableScanDetector(props, vSql);

        var sample = new SqlSample("abc", "HR", "select * from employees",
                500, 50_000, 1, "p1", true);

        List<Anomaly> hits = detector.detect(List.of(sample));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).type()).isEqualTo(AnomalyType.FULL_TABLE_SCAN);
        assertThat(hits.get(0).explainPlan()).contains("FULL");
    }

    @Test
    void ignoresFullScanBelowRowThreshold() {
        var detector = new FullTableScanDetector(props, mock(VSqlRepository.class));
        var sample = new SqlSample("abc", "HR", "x", 100, 10, 1, "p1", true);
        assertThat(detector.detect(List.of(sample))).isEmpty();
    }

    @Test
    void ignoresIndexedQueries() {
        var detector = new FullTableScanDetector(props, mock(VSqlRepository.class));
        var sample = new SqlSample("abc", "HR", "x", 100, 1_000_000, 1, "p1", false);
        assertThat(detector.detect(List.of(sample))).isEmpty();
    }
}
