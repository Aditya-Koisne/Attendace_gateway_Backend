package com.example.gateway.service;

import com.example.gateway.config.ApiConfigProperties;
import com.example.gateway.entity.DeviceEntity;
import com.example.gateway.entity.PunchEntity;
import com.example.gateway.repo.DeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class SentryClient {

  private static final DateTimeFormatter REAL_DATE =
          DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter REAL_TIME =
          DateTimeFormatter.ofPattern("HH:mm:ss");

  public enum Result {
    OK,     // sent & accepted
    RETRY,  // transient failure
    DEAD    // permanent failure
  }

  private final ApiConfigProperties api;
  private final DeviceRepository deviceRepo;
  private final RestClient client;

  public SentryClient(
          ApiConfigProperties api,
          DeviceRepository deviceRepo,
          RestClient.Builder builder
  ) {
    this.api = api;
    this.deviceRepo = deviceRepo;

    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout(api.getTimeouts().getIndividualConnect() * 1000);
    rf.setReadTimeout(api.getTimeouts().getIndividualRead() * 1000);

    this.client = builder.requestFactory(rf).build();
  }

  public Result send(DeviceEntity dev, PunchEntity rec, String bearerToken) {

    String url = api.getBaseUrl().replaceAll("/$", "") + "/api/mt";

    String gatewayMac =
            dev.getGatewayMacAddress() != null
                    ? dev.getGatewayMacAddress()
                    : "00-00-00-00-00-00";

    Map<String, Object> payload = Map.of(
            "DeviceMacAddress", dev.getMacAddress(),
            "SentryGatewayMacAddress", gatewayMac,
            "CID", dev.getCid(),
            "EmpId", String.valueOf(rec.getEmpCode()), // MUST be string
            "InOutStatus", rec.getInoutStatus(),
            "RealDate", rec.getTs().format(REAL_DATE),
            "RealTime", rec.getTs().format(REAL_TIME)
    );

    log.info("SENTRY SEND {}", payload);

    try {
      Map<String, Object> response =
              client.post()
                      .uri(url)
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(payload)
                      .retrieve()
                      .body(Map.class);

      String msg = response != null ? String.valueOf(response.get("Message")) : null;

      if ("Punch Recorded".equalsIgnoreCase(msg)) {
        // ✅ device is healthy
        deviceRepo.markSentryOk(dev.getSerialNumber());
        return Result.OK;
      }

      // unexpected response → retry cautiously
      log.warn("SENTRY unexpected response: {}", response);
      return Result.RETRY;

    }
    catch (HttpClientErrorException e) {
      // 🔴 4xx → PERMANENT FAILURE (most important case)

      String body = e.getResponseBodyAsString();
      log.error("SENTRY 4xx {} {}", e.getStatusCode(), body);

      // Specific hard failure you are seeing in logs
      if (e.getStatusCode().value() == 400 &&
              body != null &&
              body.contains("not registered")) {

        deviceRepo.markSentryDead(
                dev.getSerialNumber(),
                body
        );
        return Result.DEAD;
      }

      // Other 4xx → retry (token expired, bad payload, etc.)
      return Result.RETRY;
    }
    catch (HttpServerErrorException e) {
      // 🟠 5xx → RETRY (Sentry down)
      log.error("SENTRY 5xx {} {}", e.getStatusCode(), e.getResponseBodyAsString());
      return Result.RETRY;
    }
    catch (Exception e) {
      // 🟠 network, timeout, unknown → RETRY
      log.error("SENTRY ERROR {}", e.toString());
      return Result.RETRY;
    }
  }
}
