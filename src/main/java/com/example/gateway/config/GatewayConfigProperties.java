// src/main/java/com/example/gateway/config/GatewayConfigProperties.java
package com.example.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kept only for host/port binding if you still want gateway.host/gateway.port in YAML.
 * macAddress/retentionDays/maxRetries removed because per-device gateway MAC is stored on DeviceEntity.
 */
@ConfigurationProperties(prefix = "gateway")
public class GatewayConfigProperties {

  private String host = "0.0.0.0";
  private int port = 7890;

  public String getHost() { return host; }
  public void setHost(String host) { this.host = host; }

  public int getPort() { return port; }
  public void setPort(int port) { this.port = port; }
}
