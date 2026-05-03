package dev.sentinel.detector;

import java.util.concurrent.atomic.AtomicLong;

final class AnomalyId {
    private static final AtomicLong COUNTER = new AtomicLong();

    private AnomalyId() {}

    static String next() {
        return String.format("anomaly-%04d", COUNTER.incrementAndGet());
    }
}
