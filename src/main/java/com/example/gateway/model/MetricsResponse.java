package com.example.gateway.model;

import java.time.Instant;
import java.util.List;

public record MetricsResponse(
        long total,
        long pending,
        long processing,
        long sent,
        long dead,
        Instant lastSuccess,
        Instant lastRetryingError,
        List<PunchDto> recent
) {}
