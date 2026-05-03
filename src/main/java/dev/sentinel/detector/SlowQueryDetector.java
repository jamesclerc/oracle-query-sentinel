package dev.sentinel.detector;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.model.Anomaly;
import dev.sentinel.model.AnomalyType;
import dev.sentinel.model.SqlSample;
import dev.sentinel.repository.VSqlRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class SlowQueryDetector implements Detector {

    private final SentinelProperties props;
    private final VSqlRepository vSql;

    public SlowQueryDetector(SentinelProperties props, VSqlRepository vSql) {
        this.props = props;
        this.vSql = vSql;
    }

    @Override
    public List<Anomaly> detect(List<SqlSample> samples) {
        long thresholdMs = props.rules().slowQueryThresholdMs();
        List<Anomaly> hits = new ArrayList<>();
        for (SqlSample s : samples) {
            long perExec = s.executions() > 0 ? s.elapsedMs() / s.executions() : s.elapsedMs();
            if (perExec >= thresholdMs) {
                hits.add(new Anomaly(
                        AnomalyId.next(),
                        AnomalyType.SLOW_QUERY,
                        s.schema(),
                        s.sqlId(),
                        perExec,
                        s.rowsProcessed(),
                        Instant.now(),
                        vSql.fetchExplainPlan(s.sqlId()),
                        "Query exceeds %d ms threshold (per execution)".formatted(thresholdMs),
                        s.sqlText()
                ));
            }
        }
        return hits;
    }
}
