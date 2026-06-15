package com.meetbowl.domain.document;

import java.util.List;
import java.util.UUID;

/** 승인된 회의록을 열람할 수 있는 회의 참석자 사용자 ID를 제공하는 Port다. */
public interface MeetingMinutesAccessScopePort {

    List<UUID> findReadableUserIds(UUID meetingId);
}
