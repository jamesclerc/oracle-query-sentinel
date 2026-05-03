package dev.sentinel.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record Anomaly(
        String id,
        AnomalyType type,
        String schema,
        @JsonProperty("sql_id") String sqlId,
        @JsonProperty("elapsed_ms") long elapsedMs,
        @JsonProperty("rows_examined") long rowsExamined,
        @JsonProperty("detected_at") Instant detectedAt,
        @JsonProperty("explain_plan") String explainPlan,
        String recommendation,
        String sqlText
) {}
