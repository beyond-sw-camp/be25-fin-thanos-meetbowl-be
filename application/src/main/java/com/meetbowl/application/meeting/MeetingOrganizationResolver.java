package com.meetbowl.application.meeting;

import java.util.UUID;

/** 회의 주최자를 기준으로 회의가 속한 조직을 조회하는 기능 간 Port다. */
public interface MeetingOrganizationResolver {

    UUID resolveByHostUserId(UUID hostUserId);
}
