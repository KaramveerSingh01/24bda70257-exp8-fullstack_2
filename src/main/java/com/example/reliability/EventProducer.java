package com.example.reliability;

import org.springframework.stereotype.Component;

@Component
public class EventProducer {
    private final EventStore eventStore;

    public EventProducer(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public EventStore.EventRecord send(String message, int failAttempts) {
        return eventStore.publish(message, failAttempts);
    }
}
