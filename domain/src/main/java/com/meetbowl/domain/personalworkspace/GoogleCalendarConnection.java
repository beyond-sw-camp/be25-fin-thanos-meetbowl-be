package com.meetbowl.domain.personalworkspace;

import java.time.Instant;
import java.util.UUID;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public class GoogleCalendarConnection {

    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_CALENDAR_ID_LENGTH = 255;
    private static final int MAX_CREDENTIAL_REF_LENGTH = 500;

    private final UUID id;
    private final UUID ownerUserId;
    private final String googleAccountEmail;
    private final String externalCalendarId;
    private final String credentialRef;
    private final Instant connectedAt;
    private final Instant disconnectedAt;

    private GoogleCalendarConnection(
            UUID id,
            UUID ownerUserId,
            String googleAccountEmail,
            String externalCalendarId,
            String credentialRef,
            Instant connectedAt,
            Instant disconnectedAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.googleAccountEmail = googleAccountEmail;
        this.externalCalendarId = externalCalendarId;
        this.credentialRef = credentialRef;
        this.connectedAt = connectedAt;
        this.disconnectedAt = disconnectedAt;
    }

    public static GoogleCalendarConnection connect(
            UUID ownerUserId,
            String googleAccountEmail,
            String externalCalendarId,
            String credentialRef,
            Instant connectedAt) {
        return of(
                null,
                ownerUserId,
                googleAccountEmail,
                externalCalendarId,
                credentialRef,
                connectedAt,
                null);
    }

    public static GoogleCalendarConnection of(
            UUID id,
            UUID ownerUserId,
            String googleAccountEmail,
            String externalCalendarId,
            String credentialRef,
            Instant connectedAt,
            Instant disconnectedAt) {
        PersonalWorkspaceCalendarEvent.requireId(ownerUserId, "구글 캘린더 연결 소유자 ID는 필수입니다.");
        PersonalWorkspaceCalendarEvent.requireText(
                googleAccountEmail,
                MAX_EMAIL_LENGTH,
                "구글 계정 이메일은 필수입니다.",
                "구글 계정 이메일은 255자 이하여야 합니다.");
        PersonalWorkspaceCalendarEvent.requireText(
                externalCalendarId,
                MAX_CALENDAR_ID_LENGTH,
                "외부 캘린더 ID는 필수입니다.",
                "외부 캘린더 ID는 255자 이하여야 합니다.");
        PersonalWorkspaceCalendarEvent.requireText(
                credentialRef,
                MAX_CREDENTIAL_REF_LENGTH,
                "외부 자격 증명 참조는 필수입니다.",
                "외부 자격 증명 참조는 500자 이하여야 합니다.");
        if (connectedAt == null) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "구글 캘린더 연결 시각은 필수입니다.");
        }
        if (disconnectedAt != null && disconnectedAt.isBefore(connectedAt)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "구글 캘린더 해제 시각은 연결 시각보다 이전일 수 없습니다.");
        }

        return new GoogleCalendarConnection(
                id,
                ownerUserId,
                googleAccountEmail.trim(),
                externalCalendarId.trim(),
                credentialRef.trim(),
                connectedAt,
                disconnectedAt);
    }

    public GoogleCalendarConnection disconnect(Instant disconnectedAt) {
        return of(
                id,
                ownerUserId,
                googleAccountEmail,
                externalCalendarId,
                credentialRef,
                connectedAt,
                disconnectedAt);
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerUserId.equals(userId);
    }

    public boolean isConnected() {
        return disconnectedAt == null;
    }

    public UUID id() {
        return id;
    }

    public UUID ownerUserId() {
        return ownerUserId;
    }

    public String googleAccountEmail() {
        return googleAccountEmail;
    }

    public String externalCalendarId() {
        return externalCalendarId;
    }

    public String credentialRef() {
        return credentialRef;
    }

    public Instant connectedAt() {
        return connectedAt;
    }

    public Instant disconnectedAt() {
        return disconnectedAt;
    }
}
