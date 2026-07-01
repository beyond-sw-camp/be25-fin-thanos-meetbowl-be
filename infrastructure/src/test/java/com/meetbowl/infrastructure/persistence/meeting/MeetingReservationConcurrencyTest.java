package com.meetbowl.infrastructure.persistence.meeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.application.meeting.CreateMeetingCommand;
import com.meetbowl.application.meeting.CreateMeetingUseCase;
import com.meetbowl.application.meeting.MeetingAttendeeWriter;
import com.meetbowl.application.meeting.MeetingAttendeeOverlapGuard;
import com.meetbowl.application.meeting.MeetingExternalInviteeSyncService;
import com.meetbowl.application.meeting.MeetingResult;
import com.meetbowl.application.meeting.MeetingRoomReservationGuard;
import com.meetbowl.application.meeting.SendMeetingExternalInvitationMailUseCase;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meetingroom.MeetingRoom;
import com.meetbowl.domain.meetingroom.MeetingRoomRepositoryPort;
import com.meetbowl.infrastructure.config.InfrastructureConfig;
import com.meetbowl.infrastructure.persistence.meetingroom.JpaMeetingRoomRepositoryAdapter;
import com.meetbowl.infrastructure.persistence.meetingroom.JpaRoomBlockRepositoryAdapter;
import com.meetbowl.infrastructure.persistence.meetingroom.MeetingRoomJpaConfig;
import com.meetbowl.infrastructure.persistence.meetingroom.SpringDataMeetingRoomRepository;

/**
 * 회의실 중복 예약 방지의 동시성 검증 테스트다.
 *
 * <p>같은 회의실·겹치는 시간대를 여러 스레드가 동시에 예약하면, 회의실 행 비관적 잠금이 검사~저장 구간을 직렬화하므로 정확히 하나만 성공하고 나머지는 {@link
 * ErrorCode#MEETING_ROOM_ALREADY_RESERVED}로 실패해야 한다. 잠금/트랜잭션 동작이 실제 DB에 의존하므로 운영(MariaDB)과 동작이 100%
 * 동일하지는 않다(H2 검증). 추후 Testcontainers MariaDB로 재검증한다.
 */
@SpringBootTest(classes = MeetingReservationConcurrencyTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:meeting-reservation-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class MeetingReservationConcurrencyTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2099-03-01T01:00:00Z");
    private static final Instant END = Instant.parse("2099-03-01T02:00:00Z");

    @Autowired private CreateMeetingUseCase createMeetingUseCase;
    @Autowired private MeetingRoomRepositoryPort meetingRoomRepositoryPort;
    @Autowired private SpringDataMeetingRepository springDataMeetingRepository;
    @Autowired private SpringDataMeetingRoomRepository springDataMeetingRoomRepository;

    @BeforeEach
    void cleanUp() {
        // 인메모리 DB가 테스트 간 공유되므로 각 테스트는 깨끗한 상태에서 시작한다.
        springDataMeetingRepository.deleteAll();
        springDataMeetingRoomRepository.deleteAll();
    }

    private UUID givenRoom() {
        MeetingRoom room =
                meetingRoomRepositoryPort.save(
                        MeetingRoom.of(null, UUID.randomUUID(), "회의실 A", 3, "본관", 8, true));
        return room.id();
    }

    private CreateMeetingCommand command(UUID roomId, UUID hostId, Instant start, Instant end) {
        return new CreateMeetingCommand(
                "회의", start, end, hostId, ORGANIZATION_ID, roomId, null, null, null, null, null, null);
    }

    @Test
    void onlyOneSucceedsWhenSameRoomAndTimeReservedConcurrently() throws InterruptedException {
        UUID roomId = givenRoom();
        UUID hostId = UUID.randomUUID();
        int threadCount = 8;

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch fire = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        List<ErrorCode> failures = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            pool.submit(
                    () -> {
                        ready.countDown();
                        try {
                            fire.await();
                            createMeetingUseCase.execute(command(roomId, hostId, START, END));
                            successCount.incrementAndGet();
                        } catch (BusinessException e) {
                            failures.add(e.errorCode());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
        }

        ready.await(); // 모든 스레드가 대기선에 도달
        fire.countDown(); // 동시에 출발
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // 정확히 하나만 성공, 나머지는 모두 회의실 중복 예약으로 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failures)
                .hasSize(threadCount - 1)
                .containsOnly(ErrorCode.MEETING_ROOM_ALREADY_RESERVED);
        // 실제로 저장된 회의도 하나뿐
        assertThat(springDataMeetingRepository.count()).isEqualTo(1);
    }

    @Test
    void overlappingReservationIsRejected() {
        UUID roomId = givenRoom();
        UUID hostId = UUID.randomUUID();
        createMeetingUseCase.execute(command(roomId, hostId, START, END)); // 1~2시

        // 1:30~2:30 → 기존 1~2시와 겹침
        Instant overlapStart = Instant.parse("2099-03-01T01:30:00Z");
        Instant overlapEnd = Instant.parse("2099-03-01T02:30:00Z");
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                createMeetingUseCase.execute(
                                        command(roomId, hostId, overlapStart, overlapEnd)));

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.MEETING_ROOM_ALREADY_RESERVED);
    }

    @Test
    void boundaryTouchingReservationIsAllowed() {
        UUID roomId = givenRoom();
        UUID hostId = UUID.randomUUID();
        createMeetingUseCase.execute(command(roomId, hostId, START, END)); // 1~2시

        // 2~3시 (시작 == 이전 종료) → 경계가 맞닿을 뿐 겹치지 않으므로 허용
        Instant nextEnd = Instant.parse("2099-03-01T03:00:00Z");
        MeetingResult result = createMeetingUseCase.execute(command(roomId, hostId, END, nextEnd));

        assertThat(result.meetingId()).isNotNull();
        assertThat(springDataMeetingRepository.count()).isEqualTo(2);
    }

    @Test
    void differentRoomsDoNotConflict() {
        UUID roomA = givenRoom();
        UUID roomB = givenRoom();
        UUID hostId = UUID.randomUUID();

        createMeetingUseCase.execute(command(roomA, hostId, START, END));
        // 다른 회의실의 같은 시간대는 충돌하지 않는다
        MeetingResult result = createMeetingUseCase.execute(command(roomB, hostId, START, END));

        assertThat(result.meetingId()).isNotNull();
        assertThat(springDataMeetingRepository.count()).isEqualTo(2);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        MeetingJpaConfig.class,
        MeetingRoomJpaConfig.class,
        JpaMeetingRepositoryAdapter.class,
        JpaMeetingAttendeeRepositoryAdapter.class,
        JpaMeetingExternalInviteeRepositoryAdapter.class,
        JpaMeetingRoomRepositoryAdapter.class,
        JpaRoomBlockRepositoryAdapter.class,
        MeetingRoomReservationGuard.class,
        MeetingAttendeeOverlapGuard.class,
        MeetingExternalInviteeSyncService.class,
        MeetingAttendeeWriter.class,
        CreateMeetingUseCase.class
    })
    static class TestApplication {

        @Bean
        SendMeetingExternalInvitationMailUseCase sendMeetingExternalInvitationMailUseCase() {
            return mock(SendMeetingExternalInvitationMailUseCase.class);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}
