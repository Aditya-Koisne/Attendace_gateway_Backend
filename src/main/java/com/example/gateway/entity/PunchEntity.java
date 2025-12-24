package com.example.gateway.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "punches")
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

  @Column(name = "status_code")
  private String statusCode;

  @Column(name = "verify_mode")
  private String verifyMode;

  @Column(name = "work_code")
  private String workCode;

  @Column(name = "raw_line", nullable = false, length = 2000)
  private String rawLine;

  @Column(name = "inout_status", nullable = false)
  private String inoutStatus;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private String status = "pending";

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "processing_started_at")
  private Instant processingStartedAt;

  @Column(name = "retry_count", nullable = false)
  private int retryCount = 0;

  @Column(name = "last_error", length = 500)
  private String lastError;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt = Instant.now();

  public Long getId() { return id; }

  public String getDeviceSn() { return deviceSn; }
  public void setDeviceSn(String deviceSn) { this.deviceSn = deviceSn; }

  public String getEmpCode() { return empCode; }
  public void setEmpCode(String empCode) { this.empCode = empCode; }

  public LocalDateTime getTs() { return ts; }
  public void setTs(LocalDateTime ts) { this.ts = ts; }

  public String getStatusCode() { return statusCode; }
  public void setStatusCode(String statusCode) { this.statusCode = statusCode; }

  public String getVerifyMode() { return verifyMode; }
  public void setVerifyMode(String verifyMode) { this.verifyMode = verifyMode; }

  public String getWorkCode() { return workCode; }
  public void setWorkCode(String workCode) { this.workCode = workCode; }

  public String getRawLine() { return rawLine; }
  public void setRawLine(String rawLine) { this.rawLine = rawLine; }

  public String getInoutStatus() { return inoutStatus; }
  public void setInoutStatus(String inoutStatus) { this.inoutStatus = inoutStatus; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public Instant getSentAt() { return sentAt; }
  public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

  public Instant getProcessingStartedAt() { return processingStartedAt; }
  public void setProcessingStartedAt(Instant processingStartedAt) { this.processingStartedAt = processingStartedAt; }

  public int getRetryCount() { return retryCount; }
  public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

  public String getLastError() { return lastError; }
  public void setLastError(String lastError) { this.lastError = lastError; }

  public Instant getNextAttemptAt() { return nextAttemptAt; }
  public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
}
