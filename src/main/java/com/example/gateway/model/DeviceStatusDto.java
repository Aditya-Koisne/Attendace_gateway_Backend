package com.example.gateway.model;

import java.time.Instant;

public record DeviceStatusDto(
        String serialNumber,
        boolean connected,
        Instant lastSeen,
        String lastIp
) {}
