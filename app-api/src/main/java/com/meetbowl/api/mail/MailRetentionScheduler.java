package com.meetbowl.api.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.meetbowl.api.config.ConditionalOnMeetbowlAppRole;
import com.meetbowl.api.config.MeetbowlAppRole;
import com.meetbowl.application.mail.ApplyMailRetentionPolicyUseCase;
import com.meetbowl.application.mail.MailRetentionApplyResult;

/**
 * 메일 보관 정책 자동 삭제의 주기 트리거다.
 *
 * <p>메일 정리 기준과 상태 변경은 application UseCase가 담당하고, API 모듈의 스케줄러는 실행 시점만 제공한다. 업무 운영 시각은 KST 기준이므로 기본
 * cron도 Asia/Seoul 시간대로 해석한다.
 */
@Component
@ConditionalOnMeetbowlAppRole({MeetbowlAppRole.ALL, MeetbowlAppRole.WORKER})
public class MailRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(MailRetentionScheduler.class);

    private final ApplyMailRetentionPolicyUseCase applyMailRetentionPolicyUseCase;

    public MailRetentionScheduler(ApplyMailRetentionPolicyUseCase applyMailRetentionPolicyUseCase) {
        this.applyMailRetentionPolicyUseCase = applyMailRetentionPolicyUseCase;
    }

    @Scheduled(cron = "${meetbowl.mail.retention.cron:0 0 3 * * *}", zone = "Asia/Seoul")
    public void applyRetentionPolicy() {
        MailRetentionApplyResult result = applyMailRetentionPolicyUseCase.execute();
        if (!result.enabled()) {
            log.debug("메일 자동 삭제 정책이 비활성화되어 정리 작업을 건너뜁니다.");
            return;
        }
        log.info(
                "메일 보관 정책 자동 적용 완료: inboxMoved={}, sentMoved={}, trashDeleted={}",
                result.inboxMovedToTrashCount(),
                result.sentMovedToTrashCount(),
                result.trashPermanentlyDeletedCount());
    }
}
