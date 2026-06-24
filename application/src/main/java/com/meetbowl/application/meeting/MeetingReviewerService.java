package com.meetbowl.application.meeting;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.meeting.MeetingAttendeeRepositoryPort;

/**
 * 회의록 도메인 연동(T4-002, FR-026/142)용 회의록 검토자 매핑 서비스다.
 *
 * <p>회의 생성·수정 시 지정된 회의록 검토자(reviewer 플래그가 붙은 참석자)를 회의 ID로 조회해, 회의록 생성 흐름이 참조할 수 있게 노출한다.
 * 회의록(Minutes)은 {@code reviewerUserId}가 필수이므로, 회의록 생성 측은 이 서비스로 검토자를 얻어 {@code
 * Minutes.createDraft(...)}에 전달한다.
 *
 * <p>경계: 검토자 지정/저장은 회의 생성·수정 UseCase가 담당하고(reviewer 플래그로 저장), 본 서비스는 그 검토자를 읽어 다른 도메인(회의록)에 넘기는 읽기
 * 전용 연동 지점이다. 검토자 사용자 자체의 유효성은 유저(조직) 도메인 책임이다.
 */
@Service
public class MeetingReviewerService {

    private final MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort;

    public MeetingReviewerService(MeetingAttendeeRepositoryPort meetingAttendeeRepositoryPort) {
        this.meetingAttendeeRepositoryPort = meetingAttendeeRepositoryPort;
    }

    /**
     * 회의의 회의록 검토자 userId를 반환한다. 검토자를 지정하지 않은 회의는 빈 값을 반환하며, 이때 회의록 생성 가능 여부는 회의록 도메인이 판단한다.
     *
     * <p>회의록 생성 연동(인수인계, T4-002): 회의 검토자는 {@code getReviewerUserId(meetingId)}로 가져가서 {@code
     * Minutes.createDraft(...)}의 {@code reviewerUserId}에 넣으면 된다. 회의 생성 때 지정한 검토자(참석자 중 1명)가 그 값으로
     * 나온다.
     */
    @Transactional(readOnly = true)
    public Optional<UUID> getReviewerUserId(UUID meetingId) {
        return meetingAttendeeRepositoryPort.findReviewerUserId(meetingId);
    }
}
