package dev.sentinel.batch;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.detector.AnomalyStore;
import dev.sentinel.detector.Detector;
import dev.sentinel.detector.LockContentionDetector;
import dev.sentinel.model.Anomaly;
import dev.sentinel.model.SqlSample;
import dev.sentinel.repository.AwrRepository;
import dev.sentinel.repository.VSqlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(PollingScheduler.class);

    private final SentinelProperties props;
    private final VSqlRepository vSql;
    private final AwrRepository awr;
    private final List<Detector> detectors;
    private final LockContentionDetector lockDetector;
    private final AnomalyStore store;

    public PollingScheduler(SentinelProperties props,
                            VSqlRepository vSql,
                            AwrRepository awr,
                            List<Detector> detectors,
                            LockContentionDetector lockDetector,
                            AnomalyStore store) {
        this.props = props;
        this.vSql = vSql;
        this.awr = awr;
        this.detectors = detectors;
        this.lockDetector = lockDetector;
        this.store = store;
    }

    @Scheduled(fixedDelayString = "#{${sentinel.polling-interval-seconds} * 1000}")
    public void poll() {
        try {
            List<SqlSample> samples = new ArrayList<>(vSql.recentSamples(props.schemas()));
            samples.addAll(awr.latestSnapshot());

            List<Anomaly> found = new ArrayList<>();
            for (Detector d : detectors) {
                found.addAll(d.detect(samples));
            }
            found.addAll(lockDetector.detect());

            if (!found.isEmpty()) {
                store.addAll(found);
                for (Anomaly a : found) {
                    log.info("anomaly type={} schema={} sql_id={} elapsed_ms={}",
                            a.type(), a.schema(), a.sqlId(), a.elapsedMs());
                }
            }
        } catch (Exception e) {
            log.warn("polling cycle failed: {}", e.getMessage());
        }
    }
}
