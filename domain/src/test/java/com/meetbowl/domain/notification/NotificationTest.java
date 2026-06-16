package com.meetbowl.domain.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class NotificationTest {

    private Notification sample(UUID recipient) {
        return Notification.create(
                recipient,
                NotificationType.MEETING_REMINDER,
                "회의 30분 전입니다",
                "프로젝트 회의가 곧 시작됩니다.",
                NotificationResourceType.MEETING,
                UUID.randomUUID());
    }

    // ---------- 정상 동작 ----------

    @Test
    void createStartsUnread() {
        UUID recipient = UUID.randomUUID();
        Notification notification = sample(recipient);

        assertEquals(recipient, notification.recipientUserId());
        assertEquals(NotificationType.MEETING_REMINDER, notification.type());
        assertNull(notification.readAt());
        assertFalse(notification.isRead());
        assertNull(notification.id()); // 신규는 id 미발급
    }

    @Test
    void markReadSetsReadAtAndFlag() {
        Notification notification = sample(UUID.randomUUID());
        Instant readAt = Instant.parse("2026-06-15T09:00:00Z");

        notification.markRead(readAt);

        assertTrue(notification.isRead());
        assertEquals(readAt, notification.readAt());
    }

    @Test
    void isOwnedByChecksRecipient() {
        UUID recipient = UUID.randomUUID();
        Notification notification = sample(recipient);

        assertTrue(notification.isOwnedBy(recipient));
        assertFalse(notification.isOwnedBy(UUID.randomUUID()));
    }

    // ---------- 데이터 정합성 ----------

    @Test
    void ofPreservesAllFields() {
        UUID id = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant readAt = Instant.parse("2026-06-15T10:00:00Z");
        Instant createdAt = Instant.parse("2026-06-15T09:00:00Z");

        Notification notification =
                Notification.of(
                        id,
                        recipient,
                        NotificationType.MINUTES_REVIEW_REQUEST,
                        "검토 요청",
                        "회의록 검토를 요청합니다.",
                        NotificationResourceType.MEETING_MINUTES,
                        resourceId,
                        readAt,
                        createdAt);

        assertEquals(id, notification.id());
        assertEquals(recipient, notification.recipientUserId());
        assertEquals(NotificationType.MINUTES_REVIEW_REQUEST, notification.type());
        assertEquals("검토 요청", notification.title());
        assertEquals("회의록 검토를 요청합니다.", notification.content());
        assertEquals(NotificationResourceType.MEETING_MINUTES, notification.resourceType());
        assertEquals(resourceId, notification.resourceId());
        assertEquals(readAt, notification.readAt());
        assertEquals(createdAt, notification.createdAt());
    }

    @Test
    void allowsNoResourceLink() {
        // 딥링크 없는 알림(둘 다 null)도 정상 생성된다.
        Notification notification =
                Notification.create(
                        UUID.randomUUID(),
                        NotificationType.MEETING_CANCELLED,
                        "회의 취소",
                        "회의가 취소되었습니다.",
                        null,
                        null);

        assertNull(notification.resourceType());
        assertNull(notification.resourceId());
    }

    @Test
    void markReadIsIdempotentAndPreservesFirstTime() {
        Notification notification = sample(UUID.randomUUID());
        Instant first = Instant.parse("2026-06-15T09:00:00Z");
        Instant later = Instant.parse("2026-06-15T11:00:00Z");

        notification.markRead(first);
        notification.markRead(later); // 이미 읽음 → 무시되어 첫 시각 보존

        assertEquals(first, notification.readAt());
    }

    // ---------- 예외 케이스 ----------

    @Test
    void rejectsNullRecipient() {
        assertInvalid(
                () ->
                        Notification.create(
                                null,
                                NotificationType.MEETING_REMINDER,
                                "제목",
                                "내용",
                                null,
                                null));
    }

    @Test
    void rejectsNullType() {
        assertInvalid(
                () -> Notification.create(UUID.randomUUID(), null, "제목", "내용", null, null));
    }

    @Test
    void rejectsBlankTitle() {
        assertInvalid(
                () ->
                        Notification.create(
                                UUID.randomUUID(),
                                NotificationType.MEETING_REMINDER,
                                " ",
                                "내용",
                                null,
                                null));
    }

    @Test
    void rejectsBlankContent() {
        assertInvalid(
                () ->
                        Notification.create(
                                UUID.randomUUID(),
                                NotificationType.MEETING_REMINDER,
                                "제목",
                                " ",
                                null,
                                null));
    }

    @Test
    void rejectsResourceTypeWithoutId() {
        // 종류만 있고 식별자가 없으면 어디로 딥링크할지 모호 → 거부
        assertInvalid(
                () ->
                        Notification.create(
                                UUID.randomUUID(),
                                NotificationType.MEETING_REMINDER,
                                "제목",
                                "내용",
                                NotificationResourceType.MEETING,
                                null));
    }

    @Test
    void rejectsResourceIdWithoutType() {
        assertInvalid(
                () ->
                        Notification.create(
                                UUID.randomUUID(),
                                NotificationType.MEETING_REMINDER,
                                "제목",
                                "내용",
                                null,
                                UUID.randomUUID()));
    }

    @Test
    void rejectsNullReadAtWhenMarkingUnread() {
        Notification notification = sample(UUID.randomUUID());

        assertInvalid(() -> notification.markRead(null));
    }

    @Test
    void markReadNullOnAlreadyReadIsNoOp() {
        // 이미 읽은 알림은 markRead가 즉시 반환하므로 null이어도 예외가 나지 않고 첫 시각을 유지한다.
        Notification notification = sample(UUID.randomUUID());
        Instant first = Instant.parse("2026-06-15T09:00:00Z");
        notification.markRead(first);

        notification.markRead(null);

        assertSame(first, notification.readAt());
    }

    private void assertInvalid(Executable action) {
        BusinessException exception = assertThrows(BusinessException.class, action::run);
        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @FunctionalInterface
    private interface Executable {
        void run();
    }
}
