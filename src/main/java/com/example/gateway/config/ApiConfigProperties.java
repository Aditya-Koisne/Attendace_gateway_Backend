package com.example.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api-config")
public class ApiConfigProperties {
  private String baseUrl = "";
  private int maxRetries = 3;
  private int backoffFactor = 1;
  private Timeouts timeouts = new Timeouts();

  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

  public int getMaxRetries() { return maxRetries; }
  public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

  public int getBackoffFactor() { return backoffFactor; }
  public void setBackoffFactor(int backoffFactor) { this.backoffFactor = backoffFactor; }

  public Timeouts getTimeouts() { return timeouts; }
  public void setTimeouts(Timeouts timeouts) { this.timeouts = timeouts; }

  public static class Timeouts {
    private int connect = 10;
    private int read = 30;
    private int individualConnect = 3;
    private int individualRead = 10;

    public int getConnect() { return connect; }
    public void setConnect(int connect) { this.connect = connect; }

    public int getRead() { return read; }
    public void setRead(int read) { this.read = read; }

    public int getIndividualConnect() { return individualConnect; }
    public void setIndividualConnect(int individualConnect) { this.individualConnect = individualConnect; }

    public int getIndividualRead() { return individualRead; }
    public void setIndividualRead(int individualRead) { this.individualRead = individualRead; }
  }
}
