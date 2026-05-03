package dev.sentinel.model;

public enum AnomalyType {
    FULL_TABLE_SCAN,
    SLOW_QUERY,
    LOCK_CONTENTION,
    DEADLOCK,
    N_PLUS_ONE
}
