package com.example.gateway.repo;

import com.example.gateway.entity.PunchEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface PunchRepository extends JpaRepository<PunchEntity, Long> {

  @Query(value = """
    SELECT inout_status FROM punches
    WHERE device_sn = :sn AND emp_code = :emp AND ts::date = :day
    ORDER BY ts DESC LIMIT 1
    """, nativeQuery = true)
  String lastInOutForDay(
          @Param("sn") String sn,
          @Param("emp") String emp,
          @Param("day") LocalDate day
  );

  @Query(value = """
    SELECT id FROM punches
    WHERE status IN ('pending', 'retry_pending')
      AND next_attempt_at <= :now
    ORDER BY ts
    LIMIT 1
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
  Long findNextPendingIdForUpdate(@Param("now") Instant now);

  @Modifying
  @Query("""
    UPDATE PunchEntity p
    SET p.status = 'processing',
        p.processingStartedAt = CURRENT_TIMESTAMP
    WHERE p.id = :id
    """)
  void markProcessing(@Param("id") Long id);

  @Modifying
  @Query("""
  UPDATE PunchEntity p
  SET p.status = 'pending',
      p.retryCount = 0,
      p.nextAttemptAt = CURRENT_TIMESTAMP,
      p.lastError = null
  WHERE p.id = :id
""")
  void requeueById(@Param("id") Long id);

  @Modifying
  @Query("""
    UPDATE PunchEntity p
    SET p.status = 'sent',
        p.sentAt = CURRENT_TIMESTAMP
    WHERE p.id = :id
    """)
  void markSent(@Param("id") Long id);

  @Modifying
  @Query("""
    UPDATE PunchEntity p
    SET p.status = 'dead',
        p.lastError = :error
    WHERE p.id = :id
    """)
  void markDead(@Param("id") Long id, @Param("error") String error);

  @Modifying
  @Query("""
    UPDATE PunchEntity p
    SET p.status = 'retry_pending',
        p.retryCount = p.retryCount + 1,
        p.lastError = :error,
        p.nextAttemptAt = :next
    WHERE p.id = :id
    """)
  void markRetryPending(
          @Param("id") Long id,
          @Param("error") String error,
          @Param("next") Instant next
  );

  // ✅ THIS WAS MISSING
  @Modifying
  @Query("""
    UPDATE PunchEntity p
    SET p.status = 'dead',
        p.retryCount = p.retryCount + 1,
        p.lastError = :error
    WHERE p.id = :id
    """)
  void markRetryDead(@Param("id") Long id, @Param("error") String error);

  @Modifying
  @Query("""
    UPDATE PunchEntity p
    SET p.status = 'pending',
        p.processingStartedAt = null
    WHERE p.status = 'processing'
      AND p.processingStartedAt < :cutoff
    """)
  int releaseStuckProcessing(@Param("cutoff") Instant cutoff);

  @Query("""
    SELECT p FROM PunchEntity p
    WHERE (:sn IS NULL OR p.deviceSn = :sn)
      AND (:status IS NULL OR p.status = :status)
    ORDER BY p.ts DESC
    """)
  List<PunchEntity> recent(
          @Param("sn") String sn,
          @Param("status") String status,
          org.springframework.data.domain.Pageable pageable
  );
}
