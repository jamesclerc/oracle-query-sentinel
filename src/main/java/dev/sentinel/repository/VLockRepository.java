package dev.sentinel.repository;

import dev.sentinel.model.LockInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class VLockRepository {

    private static final String QUERY = """
            SELECT s.sid,
                   s.serial# AS serial,
                   s.username,
                   s.schemaname AS schema_name,
                   l.type AS lock_type,
                   DECODE(l.lmode, 0,'NONE',1,'NULL',2,'ROW-S',3,'ROW-X',
                                   4,'SHARE',5,'S/ROW-X',6,'EXCLUSIVE','UNKNOWN') AS lmode,
                   s.seconds_in_wait AS wait_seconds,
                   NVL(o.object_name, ' ') AS object_name,
                   NVL(s.blocking_session, 0) AS blocking_session
              FROM v$session s
              JOIN v$lock l ON l.sid = s.sid
              LEFT JOIN dba_objects o ON o.object_id = l.id1
             WHERE s.seconds_in_wait >= ?
            """;

    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public VLockRepository(JdbcTemplate jdbc,
                           @Value("${sentinel.oracle-views-enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    public List<LockInfo> contendedLocks(long minWaitSeconds) {
        if (!enabled) return Collections.emptyList();
        return jdbc.query(QUERY, new Object[]{minWaitSeconds}, (rs, i) -> new LockInfo(
                rs.getLong("sid"),
                rs.getLong("blocking_session"),
                rs.getString("username"),
                rs.getString("schema_name"),
                rs.getString("lock_type"),
                rs.getString("lmode"),
                rs.getLong("wait_seconds"),
                rs.getString("object_name")
        ));
    }
}
