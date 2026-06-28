package com.meetbowl.infrastructure.persistence.meetingroom;

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

import com.meetbowl.application.meeting.CreateMeetingCommand;
import com.meetbowl.application.meeting.CreateMeetingUseCase;
import com.meetbowl.application.meeting.MeetingAttendeeOverlapGuard;
import com.meetbowl.application.meeting.MeetingListFilter;
import com.meetbowl.application.meeting.MeetingAttendeeWriter;
import com.meetbowl.application.meeting.MeetingRoomReservationGuard;
import com.meetbowl.application.meetingroom.GetRoomReservationsUseCase;
import com.meetbowl.application.meetingroom.ReservationItemResult;
import com.meetbowl.application.meetingroom.RoomReservationsResult;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meetingroom.Building;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;
import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;
import com.meetbowl.infrastructure.config.InfrastructureConfig;
import com.meetbowl.infrastructure.persistence.meeting.JpaMeetingAttendeeRepositoryAdapter;
import com.meetbowl.infrastructure.persistence.meeting.JpaMeetingRepositoryAdapter;
import com.meetbowl.infrastructure.persistence.meeting.MeetingJpaConfig;

/** 회의실 예약 현황 조회(F4)를 실제 DB(H2)로 검증한다. */
@SpringBootTest(classes = MeetingRoomReservationsTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:room-reservations-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class MeetingRoomReservationsTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final Instant DAY_START = Instant.parse("2099-06-01T00:00:00Z");
    private static final Instant DAY_END = Instant.parse("2099-06-02T00:00:00Z");
    private static final Instant SLOT_START = Instant.parse("2099-06-01T01:00:00Z");
    private static final Instant SLOT_END = Instant.parse("2099-06-01T02:00:00Z");

    @Autowired private GetRoomReservationsUseCase getRoomReservationsUseCase;
    @Autowired private CreateMeetingUseCase createMeetingUseCase;
    @Autowired private MeetingRoomRepositoryPort meetingRoomRepositoryPort;
    @Autowired private BuildingRepositoryPort buildingRepositoryPort;
    @Autowired private SiteRepositoryPort siteRepositoryPort;

    private UUID buildingId;

    @BeforeEach
    void setUp() {
        meetingRoomRepositoryPort
                .findAll()
                .forEach(room -> meetingRoomRepositoryPort.deleteById(room.id()));
        UUID siteId =
                siteRepositoryPort
                        .save(Site.of(null, UUID.randomUUID(), "테헤란로", "서울"))
                        .id();
        buildingId = buildingRepositoryPort.save(Building.of(null, siteId, "본관")).id();
    }

    private UUID givenRoom(String name) {
        return meetingRoomRepositoryPort
                .save(MeetingRoom.of(null, buildingId, name, 3, "본관", 8, true))
                .id();
    }

    private void reserve(UUID roomId, UUID host, Instant start, Instant end, List<UUID> attendees) {
        createMeetingUseCase.execute(
                new CreateMeetingCommand(
                        "회의", start, end, host, ORGANIZATION_ID, roomId, null, null, attendees, null, null, null));
    }

    // ─── ③ 타임라인 (전체 예약) ───

    @Test
    void boardGroupsReservationsByRoom() {
        UUID roomA = givenRoom("대 회의실");
        UUID roomB = givenRoom("소 회의실");
        reserve(roomA, UUID.randomUUID(), SLOT_START, SLOT_END, null);

        List<RoomReservationsResult> board =
                getRoomReservationsUseCase.getReservationBoard(DAY_START, DAY_END, null, null);

        assertThat(board).hasSize(2); // 빈 회의실도 행으로 포함
        RoomReservationsResult a =
                board.stream().filter(r -> r.roomId().equals(roomA)).findFirst().orElseThrow();
        RoomReservationsResult b =
                board.stream().filter(r -> r.roomId().equals(roomB)).findFirst().orElseThrow();
        assertThat(a.reservations()).hasSize(1);
        assertThat(a.reservations().get(0).scheduledAt()).isEqualTo(SLOT_START);
        assertThat(a.siteName()).isEqualTo("테헤란로");
        assertThat(b.reservations()).isEmpty();
    }

    @Test
    void boardIncludesAllUsersReservations() {
        UUID roomId = givenRoom("대 회의실");
        reserve(roomId, UUID.randomUUID(), SLOT_START, SLOT_END, null);
        reserve(
                roomId,
                UUID.randomUUID(),
                Instant.parse("2099-06-01T03:00:00Z"),
                Instant.parse("2099-06-01T04:00:00Z"),
                null);

        RoomReservationsResult room =
                getRoomReservationsUseCase
                        .getReservationBoard(DAY_START, DAY_END, null, null)
                        .get(0);
        // 서로 다른 주최자의 예약 2건이 모두 보인다
        assertThat(room.reservations()).hasSize(2);
    }

    @Test
    void boardRejectsInvalidWindow() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                getRoomReservationsUseCase.getReservationBoard(
                                        DAY_END, DAY_START, null, null));
        assertThat(exception.errorCode()).isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
    }

    // ─── ①② 내 예약 / 참석할 회의 ───

    @Test
    void myReservationsReturnsHostedRoomMeetingsWithRoomName() {
        UUID host = UUID.randomUUID();
        UUID roomId = givenRoom("대 회의실");
        reserve(roomId, host, SLOT_START, SLOT_END, null);

        List<ReservationItemResult> mine =
                getRoomReservationsUseCase.getMyReservations(host, MeetingListFilter.HOST);

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).roomName()).isEqualTo("대 회의실");
        assertThat(mine.get(0).hostUserId()).isEqualTo(host);
    }

    @Test
    void myReservationsInvitedReturnsMeetingsIAttend() {
        UUID host = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        UUID roomId = givenRoom("대 회의실");
        reserve(roomId, host, SLOT_START, SLOT_END, List.of(invitee));

        // 초대된 사람: 참석 탭 1건, 예약(주최) 탭 0건
        assertThat(getRoomReservationsUseCase.getMyReservations(invitee, MeetingListFilter.INVITED))
                .hasSize(1);
        assertThat(getRoomReservationsUseCase.getMyReservations(invitee, MeetingListFilter.HOST))
                .isEmpty();
    }

    @Test
    void myReservationsExcludesVideoOnlyMeetings() {
        UUID host = UUID.randomUUID();
        // 회의실 없는 화상회의
        createMeetingUseCase.execute(
                new CreateMeetingCommand(
                        "화상",
                        SLOT_START,
                        SLOT_END,
                        host,
                        ORGANIZATION_ID,
                        null,
                        "LIVEKIT",
                        "room-1",
                        null,
                        null,
                        null,
                        null));

        assertThat(getRoomReservationsUseCase.getMyReservations(host, MeetingListFilter.HOST))
                .isEmpty();
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
        JpaBuildingRepositoryAdapter.class,
        JpaSiteRepositoryAdapter.class,
        MeetingRoomReservationGuard.class,
        MeetingAttendeeOverlapGuard.class,
        MeetingAttendeeWriter.class,
        CreateMeetingUseCase.class,
        GetRoomReservationsUseCase.class
    })
    static class TestApplication {}
}
