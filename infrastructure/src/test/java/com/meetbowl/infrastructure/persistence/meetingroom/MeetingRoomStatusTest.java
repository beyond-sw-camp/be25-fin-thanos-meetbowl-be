package com.meetbowl.infrastructure.persistence.meetingroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
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
import com.meetbowl.application.meeting.MeetingAttendeeWriter;
import com.meetbowl.application.meeting.MeetingResult;
import com.meetbowl.application.meeting.MeetingRoomReservationGuard;
import com.meetbowl.application.meetingroom.GetMeetingRoomStatusUseCase;
import com.meetbowl.application.meetingroom.RoomAvailabilityStatus;
import com.meetbowl.application.meetingroom.RoomStatusQuery;
import com.meetbowl.application.meetingroom.RoomStatusResult;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.Meeting;
import com.meetbowl.domain.meeting.MeetingRepositoryPort;
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

/** 회의실 현황 조회(F3)를 실제 DB(H2)로 검증한다. 상태는 점유 회의의 status로 판정한다. */
@SpringBootTest(classes = MeetingRoomStatusTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:room-status-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class MeetingRoomStatusTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final Instant FROM = Instant.parse("2099-05-01T01:00:00Z");
    private static final Instant TO = Instant.parse("2099-05-01T02:00:00Z");

    @Autowired private GetMeetingRoomStatusUseCase getMeetingRoomStatusUseCase;
    @Autowired private CreateMeetingUseCase createMeetingUseCase;
    @Autowired private MeetingRepositoryPort meetingRepositoryPort;
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
                        .save(Site.of(null, UUID.randomUUID(), "판교 사옥", "성남시"))
                        .id();
        buildingId = buildingRepositoryPort.save(Building.of(null, siteId, "A동")).id();
    }

    private UUID givenRoom(String name, boolean available) {
        return meetingRoomRepositoryPort
                .save(MeetingRoom.of(null, buildingId, name, 3, "본관", 8, available))
                .id();
    }

    private RoomStatusResult statusOf(UUID roomId) {
        return getMeetingRoomStatusUseCase
                .execute(new RoomStatusQuery(FROM, TO, null, null))
                .stream()
                .filter(result -> result.roomId().equals(roomId))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void emptyRoomIsAvailable() {
        UUID roomId = givenRoom("빈 회의실", true);
        assertThat(statusOf(roomId).status()).isEqualTo(RoomAvailabilityStatus.AVAILABLE);
    }

    @Test
    void roomWithScheduledMeetingIsReserved() {
        UUID roomId = givenRoom("예약된 회의실", true);
        createMeetingUseCase.execute(
                new CreateMeetingCommand(
                        "회의", FROM, TO, UUID.randomUUID(), ORGANIZATION_ID, roomId, null, null, null, null, null, null));

        RoomStatusResult status = statusOf(roomId);
        assertThat(status.status()).isEqualTo(RoomAvailabilityStatus.RESERVED);
        assertThat(status.currentMeeting()).isNotNull();
        assertThat(status.currentMeeting().scheduledAt()).isEqualTo(FROM);
    }

    @Test
    void roomWithInProgressMeetingIsInUse() {
        UUID roomId = givenRoom("사용 중 회의실", true);
        MeetingResult created =
                createMeetingUseCase.execute(
                        new CreateMeetingCommand(
                                "회의",
                                FROM,
                                TO,
                                UUID.randomUUID(),
                                ORGANIZATION_ID,
                                roomId,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null));
        // 회의를 실제 진행 상태로 전환해 저장 → IN_USE로 보여야 한다
        Meeting meeting = meetingRepositoryPort.findById(created.meetingId()).orElseThrow();
        meetingRepositoryPort.save(meeting.start(FROM));

        assertThat(statusOf(roomId).status()).isEqualTo(RoomAvailabilityStatus.IN_USE);
    }

    @Test
    void unavailableRoomIsUnavailable() {
        UUID roomId = givenRoom("점검 중 회의실", false);
        assertThat(statusOf(roomId).status()).isEqualTo(RoomAvailabilityStatus.UNAVAILABLE);
    }

    @Test
    void nonOverlappingMeetingLeavesRoomAvailable() {
        UUID roomId = givenRoom("다른 시간 회의실", true);
        // 03~04시 회의 → 조회 창(01~02시)과 안 겹침
        Instant otherStart = Instant.parse("2099-05-01T03:00:00Z");
        Instant otherEnd = Instant.parse("2099-05-01T04:00:00Z");
        createMeetingUseCase.execute(
                new CreateMeetingCommand(
                        "회의",
                        otherStart,
                        otherEnd,
                        UUID.randomUUID(),
                        ORGANIZATION_ID,
                        roomId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        assertThat(statusOf(roomId).status()).isEqualTo(RoomAvailabilityStatus.AVAILABLE);
    }

    @Test
    void rejectsInvalidWindow() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                getMeetingRoomStatusUseCase.execute(
                                        new RoomStatusQuery(TO, FROM, null, null)));
        assertThat(exception.errorCode()).isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
    }

    @Test
    void includesSiteAndBuildingNames() {
        UUID roomId = givenRoom("이름 회의실", true);
        RoomStatusResult status = statusOf(roomId);
        assertThat(status.siteName()).isEqualTo("판교 사옥");
        assertThat(status.buildingName()).isEqualTo("A동");
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
        GetMeetingRoomStatusUseCase.class
    })
    static class TestApplication {}
}
