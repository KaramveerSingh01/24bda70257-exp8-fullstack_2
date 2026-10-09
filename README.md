# Event Reliability Lab

This is a simple full-stack Java Spring Boot project for learning:
- retry with backoff
- dead-letter queue (DLQ)
- idempotent event handling

## Features
- HTML + CSS + JavaScript frontend
- Java backend REST API
- In-memory event tracking
- Simple retry simulation
- DLQ replay

## Run
```bash
mvn spring-boot:run
```

Open: http://localhost:8080

## API
- POST `/api/events` with JSON like:
  ```json
  {
    "eventId": "abc-123",
    "message": "Order created",
    "failAttempts": 2
  }
  ```
- GET `/api/events`
- GET `/api/dlq`
- GET `/api/stats`
- POST `/api/dlq/{eventId}/replay`

## Theory
- Retry helps recover from temporary failures.
- DLQ holds messages that fail repeatedly.
- Idempotency prevents duplicate processing using the same event ID.
