package dev.sentinel.repository;

import dev.sentinel.model.SessionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class VSessionRepository {

    private static final String QUERY = """
            SELECT sid, serial# AS serial, username, schemaname AS schema_name,
                   status, program, sql_id
              FROM v$session
             WHERE username IS NOT NULL
            """;

    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public VSessionRepository(JdbcTemplate jdbc,
                              @Value("${sentinel.oracle-views-enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    public List<SessionInfo> activeSessions() {
        if (!enabled) return Collections.emptyList();
        return jdbc.query(QUERY, (rs, i) -> new SessionInfo(
                rs.getLong("sid"),
                rs.getLong("serial"),
                rs.getString("username"),
                rs.getString("schema_name"),
                rs.getString("status"),
                rs.getString("program"),
                rs.getString("sql_id")
        ));
    }
}
