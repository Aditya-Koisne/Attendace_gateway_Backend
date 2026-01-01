package com.example.gateway.model;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;


public record DeviceDto(
        String serialNumber,
        String name,
        String macAddress,
        String cid,
        String gatewayMacAddress,
        boolean enabled
) {}

