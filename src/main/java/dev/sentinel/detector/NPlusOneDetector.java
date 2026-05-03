package dev.sentinel.detector;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.model.Anomaly;
import dev.sentinel.model.AnomalyType;
import dev.sentinel.model.SqlSample;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NPlusOneDetector implements Detector {

    private final SentinelProperties props;

    public NPlusOneDetector(SentinelProperties props) {
        this.props = props;
    }

    @Override
    public List<Anomaly> detect(List<SqlSample> samples) {
        Map<String, SqlSample> byPlan = new HashMap<>();
        Map<String, Long> count = new HashMap<>();

        for (SqlSample s : samples) {
            String key = s.planHash() != null ? s.planHash() : s.sqlId();
            byPlan.putIfAbsent(key, s);
            count.merge(key, s.executions() == 0 ? 1L : s.executions(), Long::sum);
        }

        int min = props.rules().nPlusOneMinOccurrences();
        List<Anomaly> hits = new ArrayList<>();
        for (var entry : count.entrySet()) {
            if (entry.getValue() >= min) {
                SqlSample s = byPlan.get(entry.getKey());
                hits.add(new Anomaly(
                        AnomalyId.next(),
                        AnomalyType.N_PLUS_ONE,
                        s.schema(),
                        s.sqlId(),
                        s.elapsedMs(),
                        s.rowsProcessed(),
                        Instant.now(),
                        "",
                        "Repeated %d executions of identical plan — likely N+1".formatted(entry.getValue()),
                        s.sqlText()
                ));
            }
        }
        return hits;
    }
}
