package com.example.gateway.service;

import com.example.gateway.config.AppProperties;
import com.example.gateway.entity.DeviceEntity;
import com.example.gateway.entity.PunchEntity;
import com.example.gateway.repo.DeviceRepository;
import com.example.gateway.repo.PunchRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SenderService {
  private static final Logger log = LoggerFactory.getLogger(SenderService.class);

  private final AppProperties props;
  private final PunchRepository punchRepo;
  private final DeviceRepository deviceRepo;
  private final TokenService tokenService;
  private final SentryClient sentryClient;

  private final ScheduledExecutorService exec;
  private final Random rnd = new Random();
  private final TransactionTemplate tx;

  public SenderService(AppProperties props,
                       PunchRepository punchRepo,
                       DeviceRepository deviceRepo,
                       TokenService tokenService,
                       SentryClient sentryClient,
                       PlatformTransactionManager txManager) {
    this.props = props;
    this.punchRepo = punchRepo;
    this.deviceRepo = deviceRepo;
    this.tokenService = tokenService;
    this.sentryClient = sentryClient;

    this.exec = Executors.newScheduledThreadPool(Math.max(2, props.getSender().getWorkers() + 2));

    this.tx = new TransactionTemplate(txManager);
    // keep each worker run short; default settings usually OK
  }

  @PostConstruct
  public void start() {
    if (!props.getSender().isEnabled()) {
      log.info("SENDER: disabled by config");
      return;
    }

    int workers = Math.max(1, props.getSender().getWorkers());
    log.info("SENDER: starting {} workers", workers);

    for (int i = 0; i < workers; i++) {
      exec.scheduleWithFixedDelay(this::workerLoopSafe, 500, 200, TimeUnit.MILLISECONDS);
    }

    exec.scheduleWithFixedDelay(this::releaseStuckSafe, 60, 60, TimeUnit.SECONDS);
  }

  private void workerLoopSafe() {
    try {
      tx.executeWithoutResult(status -> workerLoopTx());
    } catch (Exception e) {
      log.warn("SENDER worker exception: {}", e.toString());
    }
  }

  /**
   * Runs INSIDE a transaction (via TransactionTemplate),
   * so FOR UPDATE SKIP LOCKED works correctly.
   */
  private void workerLoopTx() {
    Long id = punchRepo.findNextPendingIdForUpdate();
    if (id == null) return;

    int updated = punchRepo.markProcessing(id);
    if (updated == 0) return;

    PunchEntity rec = punchRepo.findById(id).orElse(null);
    if (rec == null) return;

    Optional<DeviceEntity> devOpt = deviceRepo.findById(rec.getDeviceSn());
    if (devOpt.isEmpty() || !devOpt.get().isEnabled()) {
      punchRepo.markDead(rec.getId(), "disabled/unknown device");
      return;
    }

    String token = tokenService.getTokenOrNull();
    if (token == null) {
      retry(rec, "no_token");
      return;
    }

    SentryClient.Result r = sentryClient.send(devOpt.get(), rec, token);
    if (r == SentryClient.Result.OK) {
      punchRepo.markSent(rec.getId());
      return;
    }
    if (r == SentryClient.Result.DEAD) {
      punchRepo.markDead(rec.getId(), "non_retryable");
      return;
    }

    retry(rec, "send_failed");
  }

  private void retry(PunchEntity rec, String err) {
    int nextRetryCount = rec.getRetryCount() + 1;
    boolean deadNow = nextRetryCount >= props.getSender().getMaxRetries();

    long delayMs = computeBackoffWithJitterMs(nextRetryCount);
    Instant next = Instant.now().plusMillis(delayMs);

    String trimmed = trim(err);
    if (deadNow) {
      punchRepo.markRetryDead(rec.getId(), trimmed);
    } else {
      punchRepo.markRetryPending(rec.getId(), trimmed, next);
    }
  }

  private long computeBackoffWithJitterMs(int retryCount) {
    long base = props.getSender().getBaseBackoffMs();
    long max = props.getSender().getMaxBackoffMs();

    long exp = base * (1L << Math.max(0, retryCount - 1));
    if (exp < 0) exp = max;
    long capped = Math.min(exp, max);

    long jitter = (long) (rnd.nextDouble() * base);
    return Math.min(capped + jitter, max);
  }

  private void releaseStuckSafe() {
    try {
      tx.executeWithoutResult(status -> {
        Duration olderThan = Duration.ofMinutes(props.getSender().getStuckProcessingMinutes());
        int released = punchRepo.releaseStuckProcessing(olderThan.getSeconds());
        if (released > 0) log.warn("SENDER: released {} stuck processing rows", released);
      });
    } catch (Exception e) {
      log.warn("SENDER: stuck reaper error: {}", e.toString());
    }
  }

  private String trim(String s) {
    if (s == null) return null;
    return s.length() > 500 ? s.substring(0, 500) : s;
  }
}
