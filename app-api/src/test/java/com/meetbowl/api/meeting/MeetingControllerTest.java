package com.meetbowl.api.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.api.meeting.dto.JoinMeetingRequest;
import com.meetbowl.api.meeting.dto.JoinMeetingResponse;
import com.meetbowl.application.meeting.JoinMeetingResult;
import com.meetbowl.application.meeting.JoinMeetingUseCase;
import com.meetbowl.common.response.ApiResponse;

class MeetingControllerTest {

    @Test
    void joinMeetingWrapsUseCaseResultWithApiEnvelope() {
        UUID meetingId = UUID.fromString("3ef5f58f-50b2-4f0b-97bf-42e79d91ac39");
        JoinMeetingUseCase joinMeetingUseCase = mock(JoinMeetingUseCase.class);
        given(joinMeetingUseCase.execute(any()))
                .willReturn(
                        new JoinMeetingResult(
                                meetingId,
                                "meeting-3ef5f58f-50b2-4f0b-97bf-42e79d91ac39",
                                "http://localhost:7880",
                                UUID.fromString("31f73d71-c04e-4410-a98c-fdc15e918091"),
                                "user-31f73d71-c04e-4410-a98c-fdc15e918091",
                                "테스터",
                                "issued-token",
                                Instant.parse("2026-06-12T01:00:00Z"),
                                Instant.parse("2026-06-12T02:00:00Z")));

        MeetingController controller =
                new MeetingController(null, null, null, null, null, null, joinMeetingUseCase);

        ApiResponse<JoinMeetingResponse> response =
                controller.joinMeeting(
                        meetingId,
                        new AuthenticatedUser(
                                UUID.fromString("31f73d71-c04e-4410-a98c-fdc15e918091"),
                                UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
                                AuthenticatedUserRole.USER,
                                "테스터"),
                        new JoinMeetingRequest("테스터", "frontend-id"));

        assertTrue(response.success());
        assertEquals("http://localhost:7880", response.data().livekitUrl());
        assertEquals("issued-token", response.data().token());
        assertEquals(meetingId, response.data().meetingId());
    }
}
