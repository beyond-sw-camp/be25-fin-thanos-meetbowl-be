package com.meetbowl.infrastructure.persistence.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.meetbowl.domain.personalworkspace.GoogleCalendarConnection;
import com.meetbowl.infrastructure.persistence.common.BaseEntity;

@Entity
@Table(
        name = "google_calendar_connections",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_google_calendar_active_connection",
                        columnNames = {"owner_user_id", "active_slot"}))
public class GoogleCalendarConnectionEntity extends BaseEntity {

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerUserId;

    @Column(nullable = false, length = 255)
    private String googleAccountEmail;

    @Column(nullable = false, length = 255)
    private String externalCalendarId;

    @Column(nullable = false, length = 500)
    private String credentialRef;

    @Column(nullable = false)
    private Instant connectedAt;

    @Column private Instant disconnectedAt;

    @Column(name = "active_slot")
    private Integer activeSlot;

    protected GoogleCalendarConnectionEntity() {}

    private GoogleCalendarConnectionEntity(
            UUID ownerUserId,
            String googleAccountEmail,
            String externalCalendarId,
            String credentialRef,
            Instant connectedAt,
            Instant disconnectedAt) {
        this.ownerUserId = ownerUserId;
        this.googleAccountEmail = googleAccountEmail;
        this.externalCalendarId = externalCalendarId;
        this.credentialRef = credentialRef;
        this.connectedAt = connectedAt;
        this.disconnectedAt = disconnectedAt;
        this.activeSlot = disconnectedAt == null ? 1 : null;
    }

    static GoogleCalendarConnectionEntity from(GoogleCalendarConnection connection) {
        GoogleCalendarConnectionEntity entity =
                new GoogleCalendarConnectionEntity(
                        connection.ownerUserId(),
                        connection.googleAccountEmail(),
                        connection.externalCalendarId(),
                        connection.credentialRef(),
                        connection.connectedAt(),
                        connection.disconnectedAt());
        entity.setId(connection.id());
        return entity;
    }

    GoogleCalendarConnection toDomain() {
        return GoogleCalendarConnection.of(
                getId(),
                ownerUserId,
                googleAccountEmail,
                externalCalendarId,
                credentialRef,
                connectedAt,
                disconnectedAt);
    }
}
