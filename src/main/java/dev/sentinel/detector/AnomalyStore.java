package dev.sentinel.detector;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.model.Anomaly;
import dev.sentinel.model.AnomalyType;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

@Component
public class AnomalyStore {

    private final int max;
    private final Deque<Anomaly> ring = new ArrayDeque<>();

    public AnomalyStore(SentinelProperties props) {
        this.max = props.store().maxAnomalies();
    }

    public synchronized void addAll(List<Anomaly> anomalies) {
        for (Anomaly a : anomalies) {
            if (ring.size() >= max) ring.removeFirst();
            ring.addLast(a);
        }
    }

    public synchronized List<Anomaly> all() {
        return new ArrayList<>(ring);
    }

    public synchronized List<Anomaly> page(int offset, int limit, AnomalyType filter) {
        return ring.stream()
                .filter(a -> filter == null || a.type() == filter)
                .sorted(Comparator.comparing(Anomaly::detectedAt).reversed())
                .skip(Math.max(0, offset))
                .limit(Math.max(0, limit))
                .toList();
    }

    public synchronized Optional<Anomaly> findById(String id) {
        return ring.stream().filter(a -> a.id().equals(id)).findFirst();
    }

    public synchronized int size() {
        return ring.size();
    }
}
