package com.example.gateway.service;

import com.example.gateway.config.ApiConfigProperties;
import com.example.gateway.entity.DeviceEntity;
import com.example.gateway.entity.PunchEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class SentryClient {
  private static final DateTimeFormatter REAL_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter REAL_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final ApiConfigProperties api;
  private final RestClient client;

  public SentryClient(ApiConfigProperties api, RestClient.Builder builder) {
    this.api = api;
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout(api.getTimeouts().getIndividualConnect() * 1000);
    rf.setReadTimeout(api.getTimeouts().getIndividualRead() * 1000);
    this.client = builder.requestFactory(rf).build();
  }

  public enum Result { OK, RETRY, DEAD }

  public Result send(DeviceEntity dev, PunchEntity rec, String bearerToken) {
    String url = api.getBaseUrl().replaceAll("/$", "") + "/api/mt";

    // ✅ CHANGED: Use device's configured gateway mac address
    String gwMac = dev.getGatewayMacAddress() != null ? dev.getGatewayMacAddress() : "00-00-00-00-00-00";

    Map<String, Object> payload = Map.of(
            "DeviceMacAddress", dev.getMacAddress(),
            "SentryGatewayMacAddress", gwMac,
            "CID", dev.getCid(),
            "EmpId", rec.getEmpCode(),
            "InOutStatus", rec.getInoutStatus(),
            "RealDate", rec.getTs().format(REAL_DATE),
            "RealTime", rec.getTs().format(REAL_TIME)
    );

    try {
      log.debug("Sending punch: {}", payload);
      var response = client.post()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .body(payload)
              .retrieve()
              .toEntity(Map.class);

      Map<?, ?> body = response.getBody();
      if (body != null && "Punch Recorded".equals(body.get("Message"))) {
        return Result.OK;
      }
      return Result.RETRY;
    } catch (Exception e) {
      log.error("Sentry API Error: {}", e.getMessage());
      // Add specific logic for 400/422 as DEAD if needed
      return Result.RETRY;
    }
  }
}
