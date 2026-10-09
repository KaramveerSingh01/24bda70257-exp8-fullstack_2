package com.example.reliability;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EventController {
    private final EventStore eventStore;

    public EventController(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventStore.EventRecord publish(@RequestBody PublishRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }
        int failAttempts = Math.max(0, Math.min(4, request.failAttempts()));
        return eventStore.publish(request.eventId(), request.message(), failAttempts);
    }

    @GetMapping("/events")
    public List<EventStore.EventRecord> listEvents() {
        return eventStore.listEvents();
    }

    @GetMapping("/dlq")
    public List<EventStore.EventRecord> listDlq() {
        return eventStore.listDlq();
    }

    @GetMapping("/stats")
    public EventStore.Stats getStats() {
        return eventStore.getStats();
    }

    @PostMapping("/dlq/{eventId}/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventStore.EventRecord replay(@PathVariable String eventId) {
        return eventStore.replayFromDlq(eventId);
    }

    public record PublishRequest(String eventId, String message, int failAttempts) {
    }
}
