// src/main/java/com/example/gateway/service/SentryClient.java
package com.example.gateway.service;

import com.example.gateway.config.ApiConfigProperties;
import com.example.gateway.entity.DeviceEntity;
import com.example.gateway.entity.PunchEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
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

  public SentryClient(ApiConfigProperties api, RestClientFactory factory) {
    this.api = api;
    this.client = factory.build(
            api.getTimeouts().getIndividualConnect(),
            api.getTimeouts().getIndividualRead()
    );
  }

  public Result send(DeviceEntity dev, PunchEntity rec, String bearerToken) {

    String baseUrl = api.getBaseUrl().replaceAll("/$", "");
    String url = baseUrl + "/api/mt";

    // ✅ PER-DEVICE Gateway MAC (required)
    String perDeviceGatewayMac = dev.getGatewayMacAddress();
    if (perDeviceGatewayMac == null || perDeviceGatewayMac.isBlank()) {
      perDeviceGatewayMac = "00-00-00-00-00-00";
    }

    Map<String, Object> payload = Map.of(
            "DeviceMacAddress", dev.getMacAddress(),
            "SentryGatewayMacAddress", perDeviceGatewayMac,
            "CID", dev.getCid(),
            "EmpId", rec.getEmpCode(),
            "InOutStatus", rec.getInoutStatus(),
            "RealDate", rec.getTs().format(REAL_DATE),
            "RealTime", rec.getTs().format(REAL_TIME)
    );

    try {
      ResponseEntity<Map> response = client.post()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
              .contentType(MediaType.APPLICATION_JSON)
              .body(payload)
              .retrieve()
              .toEntity(Map.class);

      Map<?, ?> responseBody = response.getBody();
      Object message = responseBody == null ? null : responseBody.get("Message");

      if ("Punch Recorded".equals(message)) return Result.OK;
      return Result.RETRY;

    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.BAD_REQUEST ||
              e.getStatusCode() == HttpStatus.NOT_FOUND ||
              e.getStatusCode().value() == 422) {
        return Result.DEAD;
      }
      return Result.RETRY;

    } catch (HttpServerErrorException e) {
      return Result.RETRY;

    } catch (Exception e) {
      return Result.RETRY;
    }
  }

  public enum Result { OK, RETRY, DEAD }
}
