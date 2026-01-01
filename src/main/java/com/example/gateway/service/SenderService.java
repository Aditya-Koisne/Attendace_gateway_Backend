package com.example.gateway.service;

import com.example.gateway.config.AppProperties;
import com.example.gateway.entity.PunchEntity;
import com.example.gateway.repo.DeviceRepository;
import com.example.gateway.repo.PunchRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SenderService {
  private final AppProperties props;
  private final PunchRepository punchRepo;
  private final DeviceRepository deviceRepo;
  private final TokenService tokenService;
  private final SentryClient sentryClient;
  private final TransactionTemplate tx;
  private final ScheduledExecutorService exec;
  private final Random rnd = new Random();

  public SenderService(AppProperties props, PunchRepository punchRepo, DeviceRepository deviceRepo,
                       TokenService tokenService, SentryClient sentryClient, PlatformTransactionManager txManager) {
    this.props = props;
    this.punchRepo = punchRepo;
    this.deviceRepo = deviceRepo;
    this.tokenService = tokenService;
    this.sentryClient = sentryClient;
    this.tx = new TransactionTemplate(txManager);
    this.exec = Executors.newScheduledThreadPool(Math.max(2, props.getSender().getWorkers() + 2));
  }

  @PostConstruct
  public void start() {
    if (!props.getSender().isEnabled()) return;
    int workers = Math.max(1, props.getSender().getWorkers());
    for (int i = 0; i < workers; i++) {
      exec.scheduleWithFixedDelay(this::processLoop, 500, 200, TimeUnit.MILLISECONDS);
    }
    exec.scheduleWithFixedDelay(this::unstuckLoop, 60, 60, TimeUnit.SECONDS);
  }

  private void processLoop() {
    try {
      tx.executeWithoutResult(status -> processOne());
    } catch (Exception e) {
      log.warn("Sender loop error: {}", e.getMessage());
    }
  }

  private void processOne() {
    Long id = punchRepo.findNextPendingIdForUpdate(Instant.now());
    if (id == null) return;
    punchRepo.markProcessing(id);

    PunchEntity rec = punchRepo.findById(id).orElse(null);
    if (rec == null) return;

    var dev = deviceRepo.findById(rec.getDeviceSn());
    if (dev.isEmpty() || !dev.get().isEnabled()) {
      punchRepo.markDead(id, "Device disabled or unknown");
      return;
    }

    String token = tokenService.getTokenOrNull();
    if (token == null) {
      scheduleRetry(rec, "No Token");
      return;
    }

    SentryClient.Result res = sentryClient.send(dev.get(), rec, token);
    switch (res) {
      case OK -> punchRepo.markSent(id);
      case DEAD -> punchRepo.markDead(id, "Sentry Rejected");
      case RETRY -> scheduleRetry(rec, "API Error");
    }
  }

  private void scheduleRetry(PunchEntity rec, String error) {
    if (rec.getRetryCount() >= props.getSender().getMaxRetries()) {
      punchRepo.markRetryDead(rec.getId(), error);
    } else {
      long delay = computeBackoff(rec.getRetryCount());
      punchRepo.markRetryPending(rec.getId(), error, Instant.now().plusMillis(delay));
    }
  }

  private long computeBackoff(int retries) {
    long base = props.getSender().getBaseBackoffMs();
    long max = props.getSender().getMaxBackoffMs();
    long exp = base * (1L << retries);
    return Math.min(exp + rnd.nextInt((int)base), max);
  }

  private void unstuckLoop() {
    Instant cutoff = Instant.now().minus(props.getSender().getStuckProcessingMinutes(), java.time.temporal.ChronoUnit.MINUTES);
    punchRepo.releaseStuckProcessing(cutoff);
  }
}
