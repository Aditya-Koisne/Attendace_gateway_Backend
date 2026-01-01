package com.example.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt-config")
public class JwtConfigProperties {
  private String authEndpoint;
  private String username;
  private String password;
}
