package com.example.gateway.service;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientFactory {

  public RestClient build(int connectSeconds, int readSeconds) {
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout(connectSeconds * 1000);
    rf.setReadTimeout(readSeconds * 1000);
    return RestClient.builder().requestFactory(rf).build();
  }
}
