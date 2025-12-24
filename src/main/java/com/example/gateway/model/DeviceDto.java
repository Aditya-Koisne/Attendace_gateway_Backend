// src/main/java/com/example/gateway/model/DeviceDto.java
package com.example.gateway.model;

import jakarta.validation.constraints.NotBlank;

/**
 * NOTE:
 * - serialNumber is NOT validated from body because SN comes from URL path (/api/admin/devices/{sn})
 * - gatewayMacAddress is required per-device for Sentry payload ("SentryGatewayMacAddress")
 */
public record DeviceDto(
        String id,                 // optional (kept for backward compatibility; not persisted if you removed deviceId)
        @NotBlank String name,
        String serialNumber,        // optional in request body; always present in responses
        @NotBlank String macAddress,
        @NotBlank String cid,
        @NotBlank String gatewayMacAddress,
        boolean enabled
) {}
