package com.example.gateway.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PunchBatchInserter {

  @PersistenceContext
  private EntityManager em;

  public record PunchInsertRow(
      String deviceSn,
      String empCode,
      LocalDateTime ts,
      String statusCode,
      String verifyMode,
      String workCode,
      String rawLine,
      String inoutStatus
  ) {}

  @Transactional
  public int batchInsertIgnore(List<PunchInsertRow> rows) {
    if (rows == null || rows.isEmpty()) return 0;

    org.hibernate.Session session = em.unwrap(org.hibernate.Session.class);

    final int[] inserted = {0};

    session.doWork(conn -> {
      String sql = """
        INSERT INTO punches
          (device_sn, emp_code, ts, status_code, verify_mode, work_code, raw_line, inout_status, status, next_attempt_at)
        VALUES
          (?, ?, ?, ?, ?, ?, ?, ?, 'pending', NOW())
        ON CONFLICT ON CONSTRAINT uq_punch_dedupe DO NOTHING
        """;

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        int batch = 0;
        for (PunchInsertRow r : rows) {
          ps.setString(1, r.deviceSn());
          ps.setString(2, r.empCode());
          ps.setTimestamp(3, Timestamp.valueOf(r.ts()));
          ps.setString(4, r.statusCode());
          ps.setString(5, r.verifyMode());
          ps.setString(6, r.workCode());
          ps.setString(7, r.rawLine());
          ps.setString(8, r.inoutStatus());
          ps.addBatch();
          batch++;

          if (batch >= 200) {
            int[] res = ps.executeBatch();
            for (int x : res) if (x > 0) inserted[0] += x;
            batch = 0;
          }
        }
        if (batch > 0) {
          int[] res = ps.executeBatch();
          for (int x : res) if (x > 0) inserted[0] += x;
        }
      }
    });

    return inserted[0];
  }
}
