package dev.sentinel.detector;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.model.Anomaly;
import dev.sentinel.model.AnomalyType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyStoreTest {

    @Test
    void evictsOldestWhenFull() {
        var props = new SentinelProperties(30,
                new SentinelProperties.Rules(2000, 10_000, 5, 10, 5),
                List.of(),
                new SentinelProperties.Store(100));
        var store = new AnomalyStore(props);

        var anomalies = IntStream.range(0, 150)
                .mapToObj(i -> new Anomaly("id-" + i, AnomalyType.SLOW_QUERY,
                        "HR", "sql", 100, 1, Instant.now(), "", "", null))
                .toList();
        store.addAll(anomalies);

        assertThat(store.size()).isEqualTo(100);
        assertThat(store.findById("id-0")).isEmpty();
        assertThat(store.findById("id-149")).isPresent();
    }

    @Test
    void filtersByType() {
        var props = new SentinelProperties(30,
                new SentinelProperties.Rules(2000, 10_000, 5, 10, 5),
                List.of(),
                new SentinelProperties.Store(100));
        var store = new AnomalyStore(props);
        store.addAll(List.of(
                new Anomaly("a", AnomalyType.SLOW_QUERY, "HR", "x", 1, 1, Instant.now(), "", "", null),
                new Anomaly("b", AnomalyType.FULL_TABLE_SCAN, "HR", "x", 1, 1, Instant.now(), "", "", null)
        ));
        assertThat(store.page(0, 10, AnomalyType.FULL_TABLE_SCAN)).hasSize(1);
    }
}
