package com.example.gateway.service;

import com.example.gateway.config.ApiConfigProperties;
import com.example.gateway.config.JwtConfigProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Service
public class TokenService {

  private final ApiConfigProperties api;
  private final JwtConfigProperties jwt;
  private final RestClient client;

  private volatile String token;
  private volatile Instant expiry = Instant.EPOCH;

  public TokenService(ApiConfigProperties api, JwtConfigProperties jwt, RestClientFactory factory) {
    this.api = api;
    this.jwt = jwt;
    this.client = factory.build(api.getTimeouts().getConnect(), api.getTimeouts().getRead());
  }

  public synchronized String getTokenOrNull() {
    if (token != null && Instant.now().isBefore(expiry.minusSeconds(300))) {
      return token;
    }
    try {
      String base = api.getBaseUrl().replaceAll("/$", "");
      String tokenUrl = base + jwt.getAuthEndpoint();

      MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("grant_type", "password");
      form.add("username", jwt.getUsername());
      form.add("password", jwt.getPassword());

      Map<?, ?> resp = client.post()
          .uri(tokenUrl)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .body(Map.class);

      if (resp == null) return null;
      String access = (String) resp.get("access_token");
      if (access == null || access.isBlank()) return null;

      token = access;
      expiry = Instant.now().plusSeconds(3600);
      return token;
    } catch (Exception e) {
      return null;
    }
  }
}
