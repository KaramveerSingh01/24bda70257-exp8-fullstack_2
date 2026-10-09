package com.example.reliability;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventStore {
    private final Map<String, EventRecord> events = new LinkedHashMap<>();
    private final Map<String, EventRecord> dlq = new LinkedHashMap<>();

    public EventRecord publish(String message, int failAttempts) {
        return publish(UUID.randomUUID().toString(), message, failAttempts);
    }

    public EventRecord publish(String eventId, String message, int failAttempts) {
        EventRecord existing = events.get(eventId);
        if (existing != null) {
            existing.duplicateCount++;
            return existing;
        }

        EventRecord record = new EventRecord(eventId, message, failAttempts, "QUEUED");
        events.put(eventId, record);
        process(record);
        return record;
    }

    public EventRecord replayFromDlq(String eventId) {
        EventRecord record = dlq.remove(eventId);
        if (record == null) {
            throw new IllegalArgumentException("Event not found in DLQ");
        }
        record.attempts = 0;
        record.failAttempts = 0;
        record.status = "REPLAYING";
        record.lastError = "Replayed from DLQ";
        record.updatedAt = Instant.now();
        events.put(eventId, record);
        process(record);
        return record;
    }

    public List<EventRecord> listEvents() {
        return new ArrayList<>(events.values());
    }

    public List<EventRecord> listDlq() {
        return new ArrayList<>(dlq.values());
    }

    public Stats getStats() {
        long processed = events.values().stream().filter(e -> "SUCCEEDED".equals(e.status)).count();
        long retries = events.values().stream().mapToLong(e -> e.retryCount).sum();
        long duplicates = events.values().stream().mapToLong(e -> e.duplicateCount).sum();
        long deadLetters = dlq.size();
        long published = events.size() + dlq.size();
        return new Stats(published, processed, retries, duplicates, deadLetters);
    }

    private void process(EventRecord record) {
        int maxAttempts = 4;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            record.attempts = attempt;
            record.updatedAt = Instant.now();

            if (attempt <= record.failAttempts) {
                record.status = "RETRYING";
                record.lastError = "Simulated failure on attempt " + attempt;
                record.retryCount++;
                sleepWithBackoff(attempt);
                continue;
            }

            record.status = "SUCCEEDED";
            record.lastError = "";
            record.updatedAt = Instant.now();
            return;
        }

        record.status = "DLQ";
        record.lastError = "Message exhausted all retries";
        record.updatedAt = Instant.now();
        dlq.put(record.eventId, record);
    }

    private void sleepWithBackoff(int attempt) {
        long delay = (long) Math.pow(2, attempt - 1) * 200L;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class EventRecord {
        private final String eventId;
        private final String message;
        private int failAttempts;
        private String status;
        private int attempts;
        private int retryCount;
        private int duplicateCount;
        private String lastError;
        private Instant updatedAt;

        public EventRecord(String eventId, String message, int failAttempts, String status) {
            this.eventId = eventId;
            this.message = message;
            this.failAttempts = failAttempts;
            this.status = status;
            this.attempts = 0;
            this.retryCount = 0;
            this.duplicateCount = 0;
            this.lastError = "";
            this.updatedAt = Instant.now();
        }

        public String getEventId() { return eventId; }
        public String getMessage() { return message; }
        public int getFailAttempts() { return failAttempts; }
        public String getStatus() { return status; }
        public int getAttempts() { return attempts; }
        public int getRetryCount() { return retryCount; }
        public int getDuplicateCount() { return duplicateCount; }
        public String getLastError() { return lastError; }
        public Instant getUpdatedAt() { return updatedAt; }
    }

    public record Stats(long published, long processed, long retries, long duplicates, long deadLetters) {
    }
}
