package com.example.gateway.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "punches")
@Getter @Setter
@NoArgsConstructor
public class PunchEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "device_sn", nullable = false)
  private String deviceSn;

  @Column(name = "emp_code", nullable = false)
  private String empCode;

  @Column(nullable = false)
  private LocalDateTime ts;

  @Column(name = "raw_line", nullable = false)
  private String rawLine;

  @Column(name = "inout_status", nullable = false)
  private String inoutStatus;

  @Column(nullable = false)
  private String status = "pending";

  @Column(name = "retry_count", nullable = false)
  private int retryCount = 0;

  @Column(name = "last_error")
  private String lastError;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "processing_started_at")
  private Instant processingStartedAt;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    nextAttemptAt = now;
  }
}
