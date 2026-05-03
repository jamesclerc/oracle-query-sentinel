package dev.sentinel.api;

import dev.sentinel.detector.AnomalyStore;
import dev.sentinel.model.Anomaly;
import dev.sentinel.model.AnomalyType;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "metrics")
public class MetricsController {

    private final AnomalyStore store;

    public MetricsController(AnomalyStore store) {
        this.store = store;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<AnomalyType, Long> byType = new EnumMap<>(AnomalyType.class);
        Map<String, Long> bySchema = new HashMap<>();
        for (Anomaly a : store.all()) {
            byType.merge(a.type(), 1L, Long::sum);
            if (a.schema() != null) bySchema.merge(a.schema(), 1L, Long::sum);
        }
        return Map.of(
                "total", store.size(),
                "by_type", byType,
                "by_schema", bySchema
        );
    }
}
