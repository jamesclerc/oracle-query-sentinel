package dev.sentinel.detector;

import dev.sentinel.model.Anomaly;
import dev.sentinel.model.SqlSample;

import java.util.List;

public interface Detector {
    List<Anomaly> detect(List<SqlSample> samples);
}
