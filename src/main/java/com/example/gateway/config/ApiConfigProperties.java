package com.example.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "api-config")
public class ApiConfigProperties {
  private String baseUrl;
  private int maxRetries = 3;
  private int backoffFactor = 1;
  private Timeouts timeouts = new Timeouts();

  @Data
  public static class Timeouts {
    private int connect = 10;
    private int read = 30;
    private int individualConnect = 3;
    private int individualRead = 10;
  }
}
