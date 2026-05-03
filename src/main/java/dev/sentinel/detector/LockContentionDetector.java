package dev.sentinel.detector;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.model.Anomaly;
import dev.sentinel.model.AnomalyType;
import dev.sentinel.model.LockInfo;
import dev.sentinel.repository.VLockRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class LockContentionDetector {

    private final SentinelProperties props;
    private final VLockRepository vLock;

    public LockContentionDetector(SentinelProperties props, VLockRepository vLock) {
        this.props = props;
        this.vLock = vLock;
    }

    public List<Anomaly> detect() {
        long threshold = props.rules().lockWaitThresholdSeconds();
        List<LockInfo> locks = vLock.contendedLocks(threshold);
        List<Anomaly> hits = new ArrayList<>();
        for (LockInfo l : locks) {
            AnomalyType type = l.blockingSessionId() > 0 ? AnomalyType.DEADLOCK : AnomalyType.LOCK_CONTENTION;
            hits.add(new Anomaly(
                    AnomalyId.next(),
                    type,
                    l.schema(),
                    null,
                    l.waitSeconds() * 1000,
                    0,
                    Instant.now(),
                    "",
                    "Session %d waiting %ds on %s (%s) — blocker=%d".formatted(
                            l.sessionId(), l.waitSeconds(), l.objectName(), l.lockType(), l.blockingSessionId()),
                    null
            ));
        }
        return hits;
    }
}
