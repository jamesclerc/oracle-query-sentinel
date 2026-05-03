package dev.sentinel.repository;

import dev.sentinel.model.SqlSample;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class VSqlRepository {

    private static final String QUERY = """
            SELECT s.sql_id,
                   s.parsing_schema_name AS schema_name,
                   SUBSTR(s.sql_fulltext, 1, 4000) AS sql_text,
                   s.elapsed_time / 1000 AS elapsed_ms,
                   s.rows_processed,
                   s.executions,
                   TO_CHAR(s.plan_hash_value) AS plan_hash,
                   CASE WHEN EXISTS (
                       SELECT 1 FROM v$sql_plan p
                       WHERE p.sql_id = s.sql_id
                         AND p.operation = 'TABLE ACCESS'
                         AND p.options = 'FULL'
                   ) THEN 1 ELSE 0 END AS full_scan
              FROM v$sql s
             WHERE s.parsing_schema_name IN (%s)
               AND s.last_active_time > SYSDATE - INTERVAL '5' MINUTE
            """;

    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public VSqlRepository(JdbcTemplate jdbc,
                          @Value("${sentinel.oracle-views-enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    public List<SqlSample> recentSamples(List<String> schemas) {
        if (!enabled || schemas == null || schemas.isEmpty()) {
            return Collections.emptyList();
        }
        String placeholders = String.join(",", schemas.stream().map(s -> "?").toList());
        String sql = String.format(QUERY, placeholders);
        return jdbc.query(sql, schemas.toArray(), (rs, i) -> new SqlSample(
                rs.getString("sql_id"),
                rs.getString("schema_name"),
                rs.getString("sql_text"),
                rs.getLong("elapsed_ms"),
                rs.getLong("rows_processed"),
                rs.getLong("executions"),
                rs.getString("plan_hash"),
                rs.getInt("full_scan") == 1
        ));
    }

    public String fetchExplainPlan(String sqlId) {
        if (!enabled) return "";
        String sql = """
                SELECT LISTAGG(operation || ' ' || NVL(options, '') || ' | ' ||
                               NVL(object_name, '') || ' | cost=' || NVL(TO_CHAR(cost), '0'),
                               CHR(10)) WITHIN GROUP (ORDER BY id) AS plan
                  FROM v$sql_plan
                 WHERE sql_id = ?
                """;
        try {
            return jdbc.queryForObject(sql, String.class, sqlId);
        } catch (Exception e) {
            return "";
        }
    }
}
