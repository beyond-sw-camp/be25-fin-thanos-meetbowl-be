package com.meetbowl.infrastructure.meeting;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.meetbowl.application.meeting.MeetingOrganizationResolver;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

/** 사용자 도메인의 조직 정보를 회의 종료 흐름에 노출하는 Adapter다. */
@Component
public class DefaultMeetingOrganizationResolver implements MeetingOrganizationResolver {

    private final UserRepositoryPort userRepositoryPort;

    public DefaultMeetingOrganizationResolver(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public UUID resolveByHostUserId(UUID hostUserId) {
        User host =
                userRepositoryPort
                        .findById(hostUserId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (host.affiliateId() == null) {
            throw new BusinessException(
                    ErrorCode.MEETING_ORGANIZATION_REQUIRED, "회의 주최자의 조직 정보가 필요합니다.");
        }
        return host.affiliateId();
    }
}
