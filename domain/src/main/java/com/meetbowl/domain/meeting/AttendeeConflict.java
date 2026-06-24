package com.meetbowl.domain.meeting;

import java.time.Instant;
import java.util.UUID;

/**
 * 참석자 시간 겹침 조회 결과 한 건이다. 한 사용자가 [from, to)와 겹치는 활성 회의 하나당 한 건씩 나오며, 같은 사용자가 여러 회의와 겹치면 여러 건이 된다.
 *
 * <p>회의실 겹침이 회의실 행 단위인 것과 달리 참석자 겹침은 (사용자, 회의) 쌍 단위로 표현해, "○○님은 '△△ 회의'에 참석 중" 같은 사용자별 경고를 만들 수 있게
 * 한다. 회의 제목/시각을 함께 담아 프론트가 어떤 회의와 겹쳤는지 보여줄 수 있다.
 */
public record AttendeeConflict(
        UUID userId,
        UUID meetingId,
        String meetingTitle,
        Instant scheduledAt,
        Instant scheduledEndAt) {}
