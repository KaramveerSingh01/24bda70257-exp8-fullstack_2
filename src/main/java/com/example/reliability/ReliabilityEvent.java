package com.example.reliability;

import java.time.Instant;

public record ReliabilityEvent(
        String eventId,
        String message,
        int failAttempts,
        Instant createdAt
) {
}
