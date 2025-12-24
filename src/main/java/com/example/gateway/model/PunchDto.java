package com.example.gateway.model;

import java.time.Instant;
import java.time.LocalDateTime;

public record PunchDto(
        Long id,
        String deviceSn,
        String empCode,
        LocalDateTime ts,
        String inoutStatus,
        String status,
        int retryCount,
        String lastError,
        Instant createdAt,
        Instant sentAt,
        Instant nextAttemptAt
) {}
