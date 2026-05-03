package dev.sentinel.repository;

import dev.sentinel.model.SqlSample;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class AwrRepository {

    private static final String QUERY = """
            SELECT sql_id,
                   parsing_schema_name AS schema_name,
                   SUBSTR(sql_text, 1, 4000) AS sql_text,
                   elapsed_time_total / 1000 AS elapsed_ms,
                   rows_processed_total AS rows_processed,
                   executions_total AS executions,
                   TO_CHAR(plan_hash_value) AS plan_hash
              FROM dba_hist_sqlstat sst
              JOIN dba_hist_sqltext  stx USING (sql_id)
             WHERE sst.snap_id = (SELECT MAX(snap_id) FROM dba_hist_snapshot)
            """;

    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public AwrRepository(JdbcTemplate jdbc,
                         @Value("${sentinel.awr-enabled:false}") boolean enabled) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    public List<SqlSample> latestSnapshot() {
        if (!enabled) return Collections.emptyList();
        return jdbc.query(QUERY, (rs, i) -> new SqlSample(
                rs.getString("sql_id"),
                rs.getString("schema_name"),
                rs.getString("sql_text"),
                rs.getLong("elapsed_ms"),
                rs.getLong("rows_processed"),
                rs.getLong("executions"),
                rs.getString("plan_hash"),
                false
        ));
    }
}
