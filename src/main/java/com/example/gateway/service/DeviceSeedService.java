// src/main/java/com/example/gateway/service/DeviceSeedService.java
package com.example.gateway.service;

import com.example.gateway.entity.DeviceEntity;
import com.example.gateway.repo.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceSeedService {
  private static final Logger log = LoggerFactory.getLogger(DeviceSeedService.class);

  private final Environment env;
  private final DeviceRepository repo;

  public DeviceSeedService(Environment env, DeviceRepository repo) {
    this.env = env;
    this.repo = repo;
  }

  public record DeviceSeed(
          String id,                   // optional; ignored if you removed deviceId
          String name,
          String serialNumber,
          String macAddress,
          String cid,
          String gatewayMacAddress,
          boolean enabled
  ) {}

  @EventListener(ApplicationReadyEvent.class)
  public void seed() {
    List<DeviceSeed> devices = Binder.get(env)
            .bind("devices", Bindable.listOf(DeviceSeed.class))
            .orElse(List.of());

    if (devices.isEmpty()) {
      log.info("DEVICE SEED: no devices in config");
      return;
    }

    int upserted = 0;
    for (DeviceSeed d : devices) {
      if (d.serialNumber() == null || d.serialNumber().isBlank()) continue;

      DeviceEntity e = repo.findById(d.serialNumber()).orElseGet(DeviceEntity::new);

      e.setSerialNumber(d.serialNumber());
      e.setName(d.name() == null || d.name().isBlank() ? d.serialNumber() : d.name());
      e.setMacAddress(d.macAddress() == null || d.macAddress().isBlank() ? "00-00-00-00-00-00" : d.macAddress());
      e.setCid(d.cid() == null || d.cid().isBlank() ? "000" : d.cid());
      e.setGatewayMacAddress(d.gatewayMacAddress() == null || d.gatewayMacAddress().isBlank()
              ? "00-00-00-00-00-00"
              : d.gatewayMacAddress());
      e.setEnabled(d.enabled());
      e.setDeviceId(d.id);
      // If you removed deviceId from entity, ignore d.id()

      repo.save(e);
      upserted++;
    }

    log.info("DEVICE SEED: seeded/updated {} devices", upserted);
  }
}
