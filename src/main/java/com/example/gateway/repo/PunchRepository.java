package com.example.gateway.repo;

import com.example.gateway.entity.PunchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface PunchRepository extends JpaRepository<PunchEntity, Long> {

  /**
   * Step 1 (claim): lock one pending row using SKIP LOCKED so multiple workers can run safely.
   * MUST run inside a transaction.
   */
  @Query(value = """
    SELECT id
    FROM punches
    WHERE status = 'pending'
      AND next_attempt_at <= NOW()
    ORDER BY ts ASC
    FOR UPDATE SKIP LOCKED
    LIMIT 1
    """, nativeQuery = true)
  Long findNextPendingIdForUpdate(); // may return null

  /**
   * Step 2 (claim): mark the locked row as processing.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    UPDATE punches
    SET status = 'processing',
        processing_started_at = NOW()
    WHERE id = :id
      AND status = 'pending'
    """, nativeQuery = true)
  int markProcessing(@Param("id") long id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    UPDATE punches
    SET status='sent', sent_at=NOW(), last_error=NULL
    WHERE id=:id
    """, nativeQuery = true)
  int markSent(@Param("id") long id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    UPDATE punches
    SET status='dead', last_error=:err, next_attempt_at=NOW()
    WHERE id=:id
    """, nativeQuery = true)
  int markDead(@Param("id") long id, @Param("err") String err);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    UPDATE punches
    SET retry_count = retry_count + 1,
        status='pending',
        last_error=:err,
        next_attempt_at=:nextAt,
        processing_started_at=NULL
    WHERE id=:id
    """, nativeQuery = true)
  int markRetryPending(@Param("id") long id, @Param("err") String err, @Param("nextAt") Instant nextAt);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    UPDATE punches
    SET retry_count = retry_count + 1,
        status='dead',
        last_error=:err,
        next_attempt_at=NOW(),
        processing_started_at=NULL
    WHERE id=:id
    """, nativeQuery = true)
  int markRetryDead(@Param("id") long id, @Param("err") String err);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    UPDATE punches
    SET status='pending',
        processing_started_at=NULL,
        next_attempt_at=NOW()
    WHERE status='processing'
      AND processing_started_at IS NOT NULL
      AND processing_started_at < NOW() - (:seconds || ' seconds')::interval
    """, nativeQuery = true)
  int releaseStuckProcessing(@Param("seconds") long seconds);

  @Query(value = "SELECT COUNT(*) FROM punches WHERE status=:status", nativeQuery = true)
  long countByStatus(@Param("status") String status);

  @Query(value = "SELECT COUNT(*) FROM punches", nativeQuery = true)
  long countAll();

  @Query(value = "SELECT MAX(sent_at) FROM punches WHERE status='sent'", nativeQuery = true)
  Instant lastSuccess();

  @Query(value = "SELECT MAX(created_at) FROM punches WHERE status='pending' AND retry_count > 0", nativeQuery = true)
  Instant lastRetryingError();

  @Query(value = """
    SELECT inout_status
    FROM punches
    WHERE device_sn=:deviceSn AND emp_code=:empCode AND ts::date = :day
    ORDER BY ts DESC
    LIMIT 1
    """, nativeQuery = true)
  String lastInOutForDay(@Param("deviceSn") String deviceSn, @Param("empCode") String empCode, @Param("day") LocalDate day);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    UPDATE punches
    SET status='pending', retry_count=0, last_error=NULL, next_attempt_at=NOW(), processing_started_at=NULL
    WHERE id=:id
    """, nativeQuery = true)
  int requeueById(@Param("id") long id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    UPDATE punches
    SET status='pending', retry_count=0, last_error=NULL, next_attempt_at=NOW(), processing_started_at=NULL
    WHERE status='dead' AND (:deviceSn IS NULL OR :deviceSn = '' OR device_sn=:deviceSn)
    """, nativeQuery = true)
  int requeueDead(@Param("deviceSn") String deviceSn);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    DELETE FROM punches
    WHERE status='sent' AND sent_at IS NOT NULL
      AND sent_at < NOW() - (:days || ' days')::interval
    """, nativeQuery = true)
  int deleteSentOlderThan(@Param("days") int days);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    DELETE FROM punches
    WHERE status='dead'
      AND created_at < NOW() - (:days || ' days')::interval
    """, nativeQuery = true)
  int deleteDeadOlderThan(@Param("days") int days);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = """
    DELETE FROM punches
    WHERE status='pending'
      AND created_at < NOW() - (:days || ' days')::interval
    """, nativeQuery = true)
  int deletePendingOlderThan(@Param("days") int days);

  @Query(value = """
    SELECT * FROM punches
    WHERE (:deviceSn IS NULL OR :deviceSn = '' OR device_sn = :deviceSn)
      AND (:status IS NULL OR :status = '' OR status = :status)
    ORDER BY ts DESC
    LIMIT :limit
    """, nativeQuery = true)
  List<PunchEntity> recent(@Param("deviceSn") String deviceSn, @Param("status") String status, @Param("limit") int limit);

  // PunchRepository.java
}
