package com.example.gateway.service;

import com.example.gateway.config.ApiConfigProperties;
import com.example.gateway.config.JwtConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class TokenService {
  private final ApiConfigProperties api;
  private final JwtConfigProperties jwt;
  private final RestClient client;

  private volatile String token;
  private volatile Instant expiry = Instant.EPOCH;

  public TokenService(ApiConfigProperties api, JwtConfigProperties jwt, RestClient.Builder builder) {
    this.api = api;
    this.jwt = jwt;
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout(api.getTimeouts().getConnect() * 1000);
    rf.setReadTimeout(api.getTimeouts().getRead() * 1000);
    this.client = builder.requestFactory(rf).build();
  }

  public synchronized String getTokenOrNull() {
    if (token != null && Instant.now().isBefore(expiry.minusSeconds(300))) {
      return token;
    }
    return fetchToken();
  }

  private String fetchToken() {
    try {
      String url = api.getBaseUrl().replaceAll("/$", "") + jwt.getAuthEndpoint();
      MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("grant_type", "password");
      form.add("username", jwt.getUsername());
      form.add("password", jwt.getPassword());

      Map body = client.post()
              .uri(url)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(Map.class);

      if (body != null && body.containsKey("access_token")) {
        token = (String) body.get("access_token");
        expiry = Instant.now().plusSeconds(3600);
        return token;
      }
    } catch (Exception e) {
      log.error("Token fetch failed: {}", e.getMessage());
    }
    return null;
  }
}
