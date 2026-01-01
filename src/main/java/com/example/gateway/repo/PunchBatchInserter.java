package com.example.gateway.repo;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Component
public class PunchBatchInserter {

    private final JdbcTemplate jdbc;

    public PunchBatchInserter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record PunchInsertRow(
            String deviceSn,
            String empCode,
            LocalDateTime ts,
            String statusCode,
            String verify,
            String work,
            String raw,
            String inOut
    ) {}

    @Transactional
    public int batchInsertIgnore(List<PunchInsertRow> rows) {
        if (rows.isEmpty()) return 0;

        String sql = """
      INSERT INTO punches (
        device_sn,
        emp_code,
        ts,
        raw_line,
        inout_status,
        status,
        retry_count,
        created_at,
        next_attempt_at
      )
      VALUES (?, ?, ?, ?, ?, 'pending', 0, NOW(), NOW())
      """;

        int[][] result = jdbc.batchUpdate(sql, rows, 500, (ps, row) -> {
            ps.setString(1, row.deviceSn());
            ps.setString(2, row.empCode());
            ps.setObject(3, row.ts());
            ps.setString(4, row.raw());
            ps.setString(5, row.inOut());
        });

        int count = 0;
        for (int[] arr : result) {
            for (int r : arr) count += r;
        }
        return count;
    }
}
