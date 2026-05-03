package dev.sentinel.model;

public record LockInfo(
        long sessionId,
        long blockingSessionId,
        String username,
        String schema,
        String lockType,
        String mode,
        long waitSeconds,
        String objectName
) {}
