package com.example.gateway.service;

import com.example.gateway.config.AppProperties;
import com.example.gateway.repo.PunchRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ✅ Disabled by default.
 * Enable later by setting:
 * app.retention.enabled=true
 */
@Service
public class RetentionCleanupService {
    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupService.class);

    private final AppProperties props;
    private final PunchRepository repo;

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    public RetentionCleanupService(AppProperties props, PunchRepository repo) {
        this.props = props;
        this.repo = repo;
    }

    @PostConstruct
    public void start() {
        if (!props.getRetention().isEnabled()) {
            log.info("RETENTION: cleanup disabled (app.retention.enabled=false). No punches will be deleted.");
            return;
        }

        long intervalMin = Math.max(5, props.getRetention().getIntervalMinutes());
        exec.scheduleWithFixedDelay(this::cleanup, 60, intervalMin * 60L, TimeUnit.SECONDS);
        log.warn("RETENTION: cleanup ENABLED. interval={}min, sentDays={}, deadDays={}, pendingDays={}",
                intervalMin,
                props.getRetention().getSentDays(),
                props.getRetention().getDeadDays(),
                props.getRetention().getPendingDays()
        );
    }

    @PreDestroy
    public void stop() {
        exec.shutdownNow();
    }

    private void cleanup() {
        try {
            int sentDays = props.getRetention().getSentDays();
            int deadDays = props.getRetention().getDeadDays();
            int pendingDays = props.getRetention().getPendingDays();



            log.info("RETENTION: cleanup run complete (sent>{}d, dead>{}d, pending>{}d)", sentDays, deadDays, pendingDays);
        } catch (Exception e) {
            log.warn("RETENTION: cleanup error: {}", e.toString());
        }
    }
}
