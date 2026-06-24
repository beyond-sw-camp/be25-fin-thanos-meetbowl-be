package com.meetbowl.application.mail;

/** 메일 보관 정책 자동 적용 결과다. 스케줄러 로그와 테스트에서 실제 처리 건수를 확인하기 위해 반환한다. */
public record MailRetentionApplyResult(
        boolean enabled,
        int inboxMovedToTrashCount,
        int sentMovedToTrashCount,
        int trashPermanentlyDeletedCount) {

    public static MailRetentionApplyResult disabled() {
        return new MailRetentionApplyResult(false, 0, 0, 0);
    }
}
