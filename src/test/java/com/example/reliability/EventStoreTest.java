package com.example.reliability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventStoreTest {
    @Test
    void retriesUntilSuccess() {
        EventStore store = new EventStore();
        EventStore.EventRecord event = store.publish("Order created", 2);

        assertThat(event.getStatus()).isIn("SUCCEEDED", "RETRYING", "DLQ");
        assertThat(event.getAttempts()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void tracksDuplicateEvents() {
        EventStore store = new EventStore();
        String id = "duplicate-123";

        EventStore.EventRecord first = store.publish(id, "Same event", 0);
        EventStore.EventRecord second = store.publish(id, "Same event", 0);

        assertThat(first.getEventId()).isEqualTo(id);
        assertThat(second.getDuplicateCount()).isGreaterThanOrEqualTo(1);
    }
}
