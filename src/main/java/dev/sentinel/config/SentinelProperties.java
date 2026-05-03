package dev.sentinel.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "sentinel")
public record SentinelProperties(
        @Min(1) int pollingIntervalSeconds,
        @NotNull Rules rules,
        List<String> schemas,
        Store store
) {
    public SentinelProperties {
        if (schemas == null) schemas = List.of();
        if (store == null) store = new Store(10_000);
    }

    public record Rules(
            @Min(0) long slowQueryThresholdMs,
            @Min(0) long fullScanMinRows,
            @Min(0) long lockWaitThresholdSeconds,
            @Min(1) int nPlusOneWindowSeconds,
            @Min(2) int nPlusOneMinOccurrences
    ) {}

    public record Store(@Min(100) int maxAnomalies) {}
}
