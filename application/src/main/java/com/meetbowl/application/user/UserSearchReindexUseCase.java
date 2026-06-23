package com.meetbowl.application.user;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.user.UserSearchIndexPort;

@Service
public class UserSearchReindexUseCase {

    private final UserSearchIndexPort userSearchIndexPort;

    public UserSearchReindexUseCase(UserSearchIndexPort userSearchIndexPort) {
        this.userSearchIndexPort = userSearchIndexPort;
    }

    @Transactional(readOnly = true)
    public void execute(Command command) {
        if (command.reindexAll()) {
            userSearchIndexPort.reindexAll();
            return;
        }

        Set<UUID> deduplicatedUserIds = new LinkedHashSet<>(command.userIds());
        if (!deduplicatedUserIds.isEmpty()) {
            // 같은 이벤트가 중복 전달되더라도 최종 문서는 upsert 결과만 남도록 사용자별 재색인을 반복 허용한다.
            deduplicatedUserIds.forEach(userSearchIndexPort::indexUser);
            return;
        }

        if (command.affiliateId() != null) {
            userSearchIndexPort.reindexByAffiliateId(command.affiliateId());
            return;
        }
        if (command.departmentId() != null) {
            userSearchIndexPort.reindexByDepartmentId(command.departmentId());
            return;
        }
        if (command.teamId() != null) {
            userSearchIndexPort.reindexByTeamId(command.teamId());
            return;
        }
        if (command.positionId() != null) {
            userSearchIndexPort.reindexByPositionId(command.positionId());
            return;
        }

        throw new IllegalArgumentException("회원 검색 재색인 대상이 지정되지 않았습니다.");
    }

    public record Command(
            boolean reindexAll,
            Set<UUID> userIds,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId) {

        public Command {
            userIds = userIds == null ? Set.of() : Set.copyOf(userIds);
        }
    }
}
