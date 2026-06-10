package com.meetbowl.domain.personalworkspace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class GoogleCalendarConnectionTest {

    @Test
    void connectAndDisconnect() {
        Instant connectedAt = Instant.parse("2099-01-01T01:00:00Z");
        GoogleCalendarConnection connection =
                GoogleCalendarConnection.connect(
                        UUID.randomUUID(),
                        "user@example.com",
                        "primary",
                        "secret/google-calendar/user",
                        connectedAt);

        assertTrue(connection.isConnected());

        GoogleCalendarConnection disconnected =
                connection.disconnect(Instant.parse("2099-01-02T01:00:00Z"));

        assertFalse(disconnected.isConnected());
    }
}
