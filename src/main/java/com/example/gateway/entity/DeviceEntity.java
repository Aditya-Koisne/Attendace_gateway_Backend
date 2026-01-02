package com.example.gateway.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity
@Table(
        name = "devices",
        indexes = {
                @Index(name = "idx_devices_gateway_mac", columnList = "gateway_mac_address")
        }
)
@Getter @Setter
@NoArgsConstructor
public class DeviceEntity {

  @Id
  @Column(name = "serial_number", nullable = false)
  private String serialNumber;

  @Column(name = "device_id")
  private String deviceId;

  @Column(nullable = false)
  private String name;

  @Column(name = "mac_address", nullable = false)
  private String macAddress;

  @Column(nullable = false)
  private String cid;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "gateway_mac_address")
  private String gatewayMacAddress;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;


  @Column(name = "sentry_status")
  private String sentryStatus = "UNKNOWN";

  @Column(name = "sentry_error")
  private String sentryError;

  @Column(name = "sentry_last_check")
  private Instant sentryLastCheck;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
