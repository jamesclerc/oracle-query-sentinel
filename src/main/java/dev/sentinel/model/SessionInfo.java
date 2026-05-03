package dev.sentinel.model;

public record SessionInfo(
        long sid,
        long serial,
        String username,
        String schema,
        String status,
        String program,
        String currentSqlId
) {}
