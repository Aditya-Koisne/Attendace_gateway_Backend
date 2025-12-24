// src/main/java/com/example/gateway/entity/DeviceEntity.java
package com.example.gateway.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class DeviceEntity {

  @Id
  @Column(name = "serial_number", nullable = false)
  private String serialNumber;

  // If you truly want to remove deviceId, keep it commented (and also remove from DB later if you want)
   @Column(name = "device_id")
   private String deviceId;

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  @Column(nullable = false)
  private String name;

  @Column(name = "mac_address", nullable = false)
  private String macAddress;

  @Column(nullable = false)
  private String cid;

  @Column(name = "gateway_mac_address", nullable = false)
  private String gatewayMacAddress;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }

  public String getSerialNumber() { return serialNumber; }
  public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getMacAddress() { return macAddress; }
  public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

  public String getCid() { return cid; }
  public void setCid(String cid) { this.cid = cid; }

  public String getGatewayMacAddress() { return gatewayMacAddress; }
  public void setGatewayMacAddress(String gatewayMacAddress) { this.gatewayMacAddress = gatewayMacAddress; }

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
