package dev.sentinel.api;

import dev.sentinel.detector.AnomalyStore;
import dev.sentinel.model.Anomaly;
import dev.sentinel.model.AnomalyType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/anomalies")
@Tag(name = "anomalies")
public class AnomalyController {

    private final AnomalyStore store;

    public AnomalyController(AnomalyStore store) {
        this.store = store;
    }

    @GetMapping
    @Operation(summary = "List detected anomalies (paginated)")
    public List<Anomaly> list(@RequestParam(defaultValue = "0") int offset,
                              @RequestParam(defaultValue = "50") int limit,
                              @RequestParam(required = false) AnomalyType type) {
        return store.page(offset, limit, type);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Anomaly detail with explain plan")
    public ResponseEntity<Anomaly> byId(@PathVariable String id) {
        return store.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "CSV export of all anomalies")
    public ResponseEntity<StreamingResponseBody> export() {
        StreamingResponseBody body = out -> {
            try (var w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                 var p = new CSVPrinter(w, CSVFormat.DEFAULT.builder()
                         .setHeader("id", "type", "schema", "sql_id", "elapsed_ms",
                                 "rows_examined", "detected_at", "recommendation")
                         .build())) {
                for (Anomaly a : store.all()) {
                    p.printRecord(a.id(), a.type(), a.schema(), a.sqlId(),
                            a.elapsedMs(), a.rowsExamined(),
                            a.detectedAt(), a.recommendation());
                }
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=anomalies.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }
}
