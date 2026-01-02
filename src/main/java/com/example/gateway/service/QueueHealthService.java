// src/main/java/com/example/gateway/service/QueueHealthService.java
package com.example.gateway.service;

import com.example.gateway.config.AppProperties;
import com.example.gateway.repo.PunchRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class QueueHealthService {
  private static final Logger log = LoggerFactory.getLogger(QueueHealthService.class);

  private final AppProperties props;
  private final PunchRepository repo;
  private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

  // Throttle repeated WARNs
  private volatile Instant lastWarnPending = Instant.EPOCH;
  private volatile Instant lastWarnDead = Instant.EPOCH;
  private volatile Instant lastWarnLastOk = Instant.EPOCH;

  // ✅ bigger interval + warn cooldown
  private static final long CHECK_INTERVAL_SECONDS = 300;   // 5 minutes
  private static final long WARN_COOLDOWN_SECONDS  = 900;   // 15 minutes

  public QueueHealthService(AppProperties props, PunchRepository repo) {
    this.props = props;
    this.repo = repo;
  }

  @PostConstruct
  public void start() {
    exec.scheduleWithFixedDelay(this::check, 30, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  private void warnThrottled(String type, String msg) {
    Instant now = Instant.now();
    Instant last =
            switch (type) {
              case "pending" -> lastWarnPending;
              case "dead" -> lastWarnDead;
              case "lastok" -> lastWarnLastOk;
              default -> Instant.EPOCH;
            };

    if (Duration.between(last, now).getSeconds() < WARN_COOLDOWN_SECONDS) return;

    log.warn(msg);

    switch (type) {
      case "pending" -> lastWarnPending = now;
      case "dead" -> lastWarnDead = now;
      case "lastok" -> lastWarnLastOk = now;
    }
  }

  private void check() {
    try {
      long pending = repo.countByStatus("pending");
      long dead = repo.countByStatus("dead");
      Instant lastOk = repo.lastSuccess();

      if (pending > props.getAlerts().getPendingThreshold()) {
        warnThrottled("pending", "ALERT: pending backlog high: " + pending);
      }
      if (dead > props.getAlerts().getDeadThreshold()) {
        warnThrottled("dead", "ALERT: dead backlog high: " + dead);
      }
      if (lastOk != null) {
        long ageMin = Duration.between(lastOk, Instant.now()).toMinutes();
        if (ageMin > props.getAlerts().getLastSuccessMaxAgeMinutes()) {
          warnThrottled("lastok", "ALERT: last success " + ageMin + " minutes ago");
        }
      }
    } catch (Exception e) {
      // keep this rare (also throttled behavior not needed here)
      log.warn("ALERT: check error: {}", e.toString());
    }
  }
}
