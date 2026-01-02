package com.example.gateway.service;

import com.example.gateway.repo.PunchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class DeadPunchReplayService {

    private static final Logger log = LoggerFactory.getLogger(DeadPunchReplayService.class);

    private final PunchRepository repo;

    // consider sentry healthy if a punch succeeded in last 10 minutes
    private static final Duration HEALTH_WINDOW = Duration.ofMinutes(10);

    public DeadPunchReplayService(PunchRepository repo) {
        this.repo = repo;
    }

    @Scheduled(cron = "0 0 2 ? * SUN")
    public void replayDeadIfHealthy() {
        Instant lastOk = repo.lastSuccess();
        if (lastOk == null ||
                Duration.between(lastOk, Instant.now()).compareTo(HEALTH_WINDOW) > 0) {

            log.warn("REPLAY SKIPPED: Sentry not healthy (last success = {})", lastOk);
            return;
        }

        int count = repo.requeueDeadApiErrors();
        log.warn("REPLAY DONE: requeued {} dead punches", count);
    }
}
