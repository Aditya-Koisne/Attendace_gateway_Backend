package com.example.gateway.model;

import java.time.Instant;

public record QueueHealthDto(
        long total,
        long pending,
        long processing,
        long sent,
        long dead,
        Instant lastSuccess,
        Instant lastRetryingError
) {}
