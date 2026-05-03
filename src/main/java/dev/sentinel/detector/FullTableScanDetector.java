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
public class FullTableScanDetector implements Detector {

    private final SentinelProperties props;
    private final VSqlRepository vSql;

    public FullTableScanDetector(SentinelProperties props, VSqlRepository vSql) {
        this.props = props;
        this.vSql = vSql;
    }

    @Override
    public List<Anomaly> detect(List<SqlSample> samples) {
        long minRows = props.rules().fullScanMinRows();
        List<Anomaly> hits = new ArrayList<>();
        for (SqlSample s : samples) {
            if (s.fullTableScan() && s.rowsProcessed() >= minRows) {
                hits.add(new Anomaly(
                        AnomalyId.next(),
                        AnomalyType.FULL_TABLE_SCAN,
                        s.schema(),
                        s.sqlId(),
                        s.elapsedMs(),
                        s.rowsProcessed(),
                        Instant.now(),
                        vSql.fetchExplainPlan(s.sqlId()),
                        "Consider an index covering this query's predicates",
                        s.sqlText()
                ));
            }
        }
        return hits;
    }
}
