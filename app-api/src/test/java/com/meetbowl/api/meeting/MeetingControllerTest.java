package com.meetbowl.api.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.AuthenticatedUserRole;
import com.meetbowl.api.meeting.dto.JoinMeetingRequest;
import com.meetbowl.api.meeting.dto.JoinMeetingResponse;
import com.meetbowl.application.meeting.JoinMeetingCommand;
import com.meetbowl.application.meeting.JoinMeetingResult;
import com.meetbowl.application.meeting.JoinMeetingUseCase;
import com.meetbowl.common.response.ApiResponse;

class MeetingControllerTest {

    @Test
    void joinMeeting은UseCase결과를응답Envelope로감싼다() {
        UUID meetingId = UUID.fromString("3ef5f58f-50b2-4f0b-97bf-42e79d91ac39");
        MeetingController controller =
                new MeetingController(
                        new StubJoinMeetingUseCase(
                                new JoinMeetingResult(
                                        meetingId,
                                        "meeting-3ef5f58f-50b2-4f0b-97bf-42e79d91ac39",
                                        "http://localhost:7880",
                                        "user-31f73d71-c04e-4410-a98c-fdc15e918091",
                                        "이지연",
                                        "issued-token",
                                        Instant.parse("2026-06-12T01:00:00Z"),
                                        Instant.parse("2026-06-12T02:00:00Z"))));

        ApiResponse<JoinMeetingResponse> response =
                controller.joinMeeting(
                        meetingId,
                        new AuthenticatedUser(
                                UUID.fromString("31f73d71-c04e-4410-a98c-fdc15e918091"),
                                null,
                                AuthenticatedUserRole.USER,
                                "이지연"),
                        new JoinMeetingRequest("이지연", "frontend-id"));

        assertTrue(response.success());
        assertEquals("http://localhost:7880", response.data().livekitUrl());
        assertEquals("issued-token", response.data().token());
        assertEquals(meetingId, response.data().meetingId());
    }

    private static final class StubJoinMeetingUseCase extends JoinMeetingUseCase {

        private final JoinMeetingResult result;

        private StubJoinMeetingUseCase(JoinMeetingResult result) {
            super(null, null);
            this.result = result;
        }

        @Override
        public JoinMeetingResult execute(JoinMeetingCommand command) {
            return result;
        }
    }
}
