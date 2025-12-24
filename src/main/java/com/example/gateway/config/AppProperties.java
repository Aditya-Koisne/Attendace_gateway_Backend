package com.example.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private final Alerts alerts = new Alerts();
  private final Retention retention = new Retention();
  private final Sender sender = new Sender();

  public Alerts getAlerts() {
    return alerts;
  }

  public Retention getRetention() {
    return retention;
  }

  public Sender getSender() {
    return sender;
  }

  // ================= ALERTS =================
  public static class Alerts {
    private long pendingThreshold = 1000;
    private long deadThreshold = 50;
    private long lastSuccessMaxAgeMinutes = 60;

    public long getPendingThreshold() {
      return pendingThreshold;
    }

    public void setPendingThreshold(long pendingThreshold) {
      this.pendingThreshold = pendingThreshold;
    }

    public long getDeadThreshold() {
      return deadThreshold;
    }

    public void setDeadThreshold(long deadThreshold) {
      this.deadThreshold = deadThreshold;
    }

    public long getLastSuccessMaxAgeMinutes() {
      return lastSuccessMaxAgeMinutes;
    }

    public void setLastSuccessMaxAgeMinutes(long lastSuccessMaxAgeMinutes) {
      this.lastSuccessMaxAgeMinutes = lastSuccessMaxAgeMinutes;
    }
  }

  // ================= RETENTION =================
  public static class Retention {

    /**
     * default false → cleanup disabled
     */
    private boolean enabled = false;

    private int sentDays = 7;
    private int deadDays = 30;
    private int pendingDays = 30;
    private int intervalMinutes = 60;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getSentDays() {
      return sentDays;
    }

    public void setSentDays(int sentDays) {
      this.sentDays = sentDays;
    }

    public int getDeadDays() {
      return deadDays;
    }

    public void setDeadDays(int deadDays) {
      this.deadDays = deadDays;
    }

    public int getPendingDays() {
      return pendingDays;
    }

    public void setPendingDays(int pendingDays) {
      this.pendingDays = pendingDays;
    }

    public int getIntervalMinutes() {
      return intervalMinutes;
    }

    public void setIntervalMinutes(int intervalMinutes) {
      this.intervalMinutes = intervalMinutes;
    }
  }

  // ================= SENDER =================
  public static class Sender {

    private boolean enabled = true;
    private int workers = 10;
    private int maxRetries = 5;
    private long baseBackoffMs = 5_000;
    private long maxBackoffMs = 300_000;
    private int stuckProcessingMinutes = 10;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getWorkers() {
      return workers;
    }

    public void setWorkers(int workers) {
      this.workers = workers;
    }

    public int getMaxRetries() {
      return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
    }

    public long getBaseBackoffMs() {
      return baseBackoffMs;
    }

    public void setBaseBackoffMs(long baseBackoffMs) {
      this.baseBackoffMs = baseBackoffMs;
    }

    public long getMaxBackoffMs() {
      return maxBackoffMs;
    }

    public void setMaxBackoffMs(long maxBackoffMs) {
      this.maxBackoffMs = maxBackoffMs;
    }

    public int getStuckProcessingMinutes() {
      return stuckProcessingMinutes;
    }

    public void setStuckProcessingMinutes(int stuckProcessingMinutes) {
      this.stuckProcessingMinutes = stuckProcessingMinutes;
    }
  }
}
