package dev.sentinel.model;

public record SqlSample(
        String sqlId,
        String schema,
        String sqlText,
        long elapsedMs,
        long rowsProcessed,
        long executions,
        String planHash,
        boolean fullTableScan
) {}
