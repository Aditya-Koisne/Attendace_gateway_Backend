package com.example.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private final Alerts alerts = new Alerts();
  private final Retention retention = new Retention();
  private final Sender sender = new Sender();

  @Data
  public static class Alerts {
    private long pendingThreshold = 1000;
    private long deadThreshold = 50;
    private long lastSuccessMaxAgeMinutes = 60;
  }

  @Data
  public static class Retention {
    private boolean enabled = false;
    private int sentDays = 7;
    private int deadDays = 30;
    private int pendingDays = 30;
    private int intervalMinutes = 60;
  }

  @Data
  public static class Sender {
    private boolean enabled = true;
    private int workers = 10;
    private int maxRetries = 5;
    private long baseBackoffMs = 5_000;
    private long maxBackoffMs = 300_000;
    private int stuckProcessingMinutes = 10;
  }
}
