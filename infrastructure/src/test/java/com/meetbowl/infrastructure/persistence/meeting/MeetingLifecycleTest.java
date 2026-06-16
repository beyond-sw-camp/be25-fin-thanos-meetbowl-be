package com.meetbowl.infrastructure.persistence.meeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.application.meeting.CancelMeetingUseCase;
import com.meetbowl.application.meeting.CreateMeetingCommand;
import com.meetbowl.application.meeting.CreateMeetingUseCase;
import com.meetbowl.application.meeting.GetMeetingUseCase;
import com.meetbowl.application.meeting.MeetingListFilter;
import com.meetbowl.application.meeting.MeetingResult;
import com.meetbowl.application.meeting.MeetingRoomReservationGuard;
import com.meetbowl.application.meeting.UpdateMeetingCommand;
import com.meetbowl.application.meeting.UpdateMeetingUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;
import com.meetbowl.infrastructure.config.InfrastructureConfig;
import com.meetbowl.infrastructure.persistence.meetingroom.JpaMeetingRoomRepositoryAdapter;
import com.meetbowl.infrastructure.persistence.meetingroom.MeetingRoomJpaConfig;
import com.meetbowl.infrastructure.persistence.meetingroom.SpringDataMeetingRoomRepository;

/** 회의 조회/수정/취소 UseCase의 동작과 권한·겹침 규칙을 실제 DB(H2)로 검증한다. */
@SpringBootTest(classes = MeetingLifecycleTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:meeting-lifecycle-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class MeetingLifecycleTest {

    private static final Instant START = Instant.parse("2099-04-01T01:00:00Z");
    private static final Instant END = Instant.parse("2099-04-01T02:00:00Z");

    @Autowired private CreateMeetingUseCase createMeetingUseCase;
    @Autowired private GetMeetingUseCase getMeetingUseCase;
    @Autowired private UpdateMeetingUseCase updateMeetingUseCase;
    @Autowired private CancelMeetingUseCase cancelMeetingUseCase;
    @Autowired private MeetingRoomRepositoryPort meetingRoomRepositoryPort;
    @Autowired private SpringDataMeetingRepository springDataMeetingRepository;
    @Autowired private SpringDataMeetingRoomRepository springDataMeetingRoomRepository;

    @BeforeEach
    void cleanUp() {
        springDataMeetingRepository.deleteAll();
        springDataMeetingRoomRepository.deleteAll();
    }

    private UUID givenRoom() {
        return meetingRoomRepositoryPort
                .save(MeetingRoom.of(null, UUID.randomUUID(), "회의실 A", 3, "본관", 8, true))
                .id();
    }

    private MeetingResult createRoomMeeting(UUID roomId, UUID hostId, Instant start, Instant end) {
        return createMeetingUseCase.execute(
                new CreateMeetingCommand(
                        "회의", start, end, hostId, roomId, null, null, null, null, null));
    }

    @Test
    void getByIdReturnsCreatedMeeting() {
        UUID host = UUID.randomUUID();
        MeetingResult created = createRoomMeeting(givenRoom(), host, START, END);

        MeetingResult found = getMeetingUseCase.getById(created.meetingId(), host, false);

        assertThat(found.meetingId()).isEqualTo(created.meetingId());
        assertThat(found.hostUserId()).isEqualTo(host);
        // 주최자는 HOST 참석자로 자동 포함된다
        assertThat(found.attendees()).extracting(a -> a.role()).containsExactly("HOST");
    }

    @Test
    void descriptionIsPersistedAndReturnedOnDetail() {
        UUID host = UUID.randomUUID();
        UUID roomId = givenRoom();
        // 회의 내용(description)을 담아 생성 → DB 저장 → 상세 조회로 다시 읽었을 때 그대로 돌아오는지(라운드트립) 검증
        MeetingResult created =
                createMeetingUseCase.execute(
                        new CreateMeetingCommand(
                                "회의",
                                START,
                                END,
                                host,
                                roomId,
                                null,
                                null,
                                null,
                                null,
                                "회의 안건 메모"));

        MeetingResult found = getMeetingUseCase.getById(created.meetingId(), host, false);

        assertThat(found.description()).isEqualTo("회의 안건 메모");
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                getMeetingUseCase.getById(
                                        UUID.randomUUID(), UUID.randomUUID(), false));
        assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEETING_NOT_FOUND);
    }

    @Test
    void getByIdRejectsNonParticipant() {
        UUID host = UUID.randomUUID();
        MeetingResult created = createRoomMeeting(givenRoom(), host, START, END);

        UUID stranger = UUID.randomUUID();
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> getMeetingUseCase.getById(created.meetingId(), stranger, false));
        assertThat(exception.errorCode()).isEqualTo(ErrorCode.COMMON_FORBIDDEN);
    }

    @Test
    void getMyHostMeetingsReturnsOnlyHostMeetings() {
        UUID host = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        createRoomMeeting(givenRoom(), host, START, END);
        createRoomMeeting(givenRoom(), other, START, END);

        assertThat(getMeetingUseCase.getMyMeetings(host, MeetingListFilter.HOST, null, null))
                .hasSize(1);
    }

    @Test
    void invitedFilterReturnsMeetingsWhereUserIsParticipant() {
        UUID host = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        createMeetingUseCase.execute(
                new CreateMeetingCommand(
                        "초대 회의", START, END, host, null, null, null, List.of(invitee), null, null));

        // 초대된 사람: 초대 탭 1건, 주최 탭 0건
        assertThat(getMeetingUseCase.getMyMeetings(invitee, MeetingListFilter.INVITED, null, null))
                .hasSize(1);
        assertThat(getMeetingUseCase.getMyMeetings(invitee, MeetingListFilter.HOST, null, null))
                .isEmpty();
        // 주최자: 주최 탭 1건, 초대 탭 0건(HOST 역할은 초대에서 제외)
        assertThat(getMeetingUseCase.getMyMeetings(host, MeetingListFilter.HOST, null, null))
                .hasSize(1);
        assertThat(getMeetingUseCase.getMyMeetings(host, MeetingListFilter.INVITED, null, null))
                .isEmpty();
        // 전체 탭: 둘 다 각각 1건
        assertThat(getMeetingUseCase.getMyMeetings(invitee, MeetingListFilter.ALL, null, null))
                .hasSize(1);
    }

    @Test
    void hostCanUpdateTitleAndTime() {
        UUID host = UUID.randomUUID();
        UUID roomId = givenRoom();
        MeetingResult created = createRoomMeeting(roomId, host, START, END);

        Instant newStart = Instant.parse("2099-04-01T05:00:00Z");
        Instant newEnd = Instant.parse("2099-04-01T06:00:00Z");
        MeetingResult updated =
                updateMeetingUseCase.execute(
                        new UpdateMeetingCommand(
                                created.meetingId(),
                                host,
                                "수정된 회의",
                                newStart,
                                newEnd,
                                roomId,
                                null));

        assertThat(updated.title()).isEqualTo("수정된 회의");
        assertThat(updated.scheduledAt()).isEqualTo(newStart);
        assertThat(updated.scheduledEndAt()).isEqualTo(newEnd);
    }

    @Test
    void updateKeepingSameSlotDoesNotConflictWithItself() {
        UUID host = UUID.randomUUID();
        UUID roomId = givenRoom();
        MeetingResult created = createRoomMeeting(roomId, host, START, END);

        // 시간/회의실은 그대로 두고 제목만 변경 → 자기 자신과의 겹침을 충돌로 보면 안 된다
        MeetingResult updated =
                updateMeetingUseCase.execute(
                        new UpdateMeetingCommand(
                                created.meetingId(), host, "제목만 변경", START, END, roomId, null));

        assertThat(updated.title()).isEqualTo("제목만 변경");
    }

    @Test
    void updateRejectedWhenMovingOntoAnotherMeetingSlot() {
        UUID host = UUID.randomUUID();
        UUID roomId = givenRoom();
        createRoomMeeting(roomId, host, START, END); // 1~2시 점유
        Instant laterStart = Instant.parse("2099-04-01T03:00:00Z");
        Instant laterEnd = Instant.parse("2099-04-01T04:00:00Z");
        MeetingResult movable = createRoomMeeting(roomId, host, laterStart, laterEnd); // 3~4시

        // 3~4시 회의를 1~2시로 이동 → 기존 1~2시 회의와 겹침 → 거부
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                updateMeetingUseCase.execute(
                                        new UpdateMeetingCommand(
                                                movable.meetingId(),
                                                host,
                                                "이동",
                                                START,
                                                END,
                                                roomId,
                                                null)));

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEETING_ROOM_ALREADY_RESERVED);
    }

    @Test
    void updateRejectedWhenRequesterIsNotHost() {
        UUID host = UUID.randomUUID();
        UUID roomId = givenRoom();
        MeetingResult created = createRoomMeeting(roomId, host, START, END);

        UUID stranger = UUID.randomUUID();
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                updateMeetingUseCase.execute(
                                        new UpdateMeetingCommand(
                                                created.meetingId(),
                                                stranger,
                                                "남이 수정",
                                                START,
                                                END,
                                                roomId,
                                                null)));

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.COMMON_FORBIDDEN);
    }

    @Test
    void hostCanCancelMeeting() {
        UUID host = UUID.randomUUID();
        MeetingResult created = createRoomMeeting(givenRoom(), host, START, END);

        MeetingResult cancelled = cancelMeetingUseCase.execute(created.meetingId(), host);

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelRejectedWhenRequesterIsNotHost() {
        UUID host = UUID.randomUUID();
        MeetingResult created = createRoomMeeting(givenRoom(), host, START, END);

        UUID stranger = UUID.randomUUID();
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> cancelMeetingUseCase.execute(created.meetingId(), stranger));

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.COMMON_FORBIDDEN);
    }

    @Test
    void cancellingFreesTheSlotForAnotherReservation() {
        UUID host = UUID.randomUUID();
        UUID roomId = givenRoom();
        MeetingResult first = createRoomMeeting(roomId, host, START, END);

        cancelMeetingUseCase.execute(first.meetingId(), host);

        // 취소된 회의는 겹침 검사에서 제외되므로 같은 시간대를 다시 예약할 수 있다
        MeetingResult second = createRoomMeeting(roomId, host, START, END);
        assertThat(second.meetingId()).isNotNull();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        MeetingJpaConfig.class,
        MeetingRoomJpaConfig.class,
        JpaMeetingRepositoryAdapter.class,
        JpaMeetingAttendeeRepositoryAdapter.class,
        JpaMeetingRoomRepositoryAdapter.class,
        MeetingRoomReservationGuard.class,
        CreateMeetingUseCase.class,
        GetMeetingUseCase.class,
        UpdateMeetingUseCase.class,
        CancelMeetingUseCase.class
    })
    static class TestApplication {}
}
