package com.meetbowl.api.meeting;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meetbowl.api.common.ApiPaths;
import com.meetbowl.api.common.BaseController;
import com.meetbowl.api.common.auth.AuthenticatedUser;
import com.meetbowl.api.common.auth.CurrentUser;
import com.meetbowl.api.meeting.dto.MeetingTranscriptResponse;
import com.meetbowl.application.transcript.GetMeetingTranscriptResult;
import com.meetbowl.application.transcript.GetMeetingTranscriptUseCase;
import com.meetbowl.common.response.ApiResponse;

/**
 * 회의별 최종 STT 원문 조회 API다.
 *
 * <p>한 회의의 segment 리스트와 조합된 전체 원문을 함께 내려줘, 화면과 후처리 기능이 같은 API를 재사용할 수 있게 한다.
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + "/meetings")
public class MeetingTranscriptController extends BaseController {

    private final GetMeetingTranscriptUseCase getMeetingTranscriptUseCase;

    public MeetingTranscriptController(GetMeetingTranscriptUseCase getMeetingTranscriptUseCase) {
        this.getMeetingTranscriptUseCase = getMeetingTranscriptUseCase;
    }

    @GetMapping("/{meetingId}/transcripts")
    public ApiResponse<MeetingTranscriptResponse> getMeetingTranscripts(
            @PathVariable UUID meetingId, @CurrentUser AuthenticatedUser currentUser) {
        GetMeetingTranscriptResult result =
                getMeetingTranscriptUseCase.execute(
                        meetingId, currentUser.userId(), currentUser.isAdmin());
        return ok(MeetingTranscriptResponse.from(result));
    }
}
