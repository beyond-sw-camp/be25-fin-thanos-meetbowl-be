package com.meetbowl.application.sharedworkspace;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.sharedworkspace.SharedWorkspace;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMember;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceMemberRepositoryPort;
import com.meetbowl.domain.sharedworkspace.SharedWorkspaceRepositoryPort;

/**
 * 공유 워크스페이스를 생성한다. 생성자는 즉시 OWNER 멤버가 되어야 이후 권한 검증이 멤버십 단일 기준으로 일관되므로, 같은 트랜잭션에서 소유자 멤버 행을 함께 만든다.
 */
@Service
public class CreateSharedWorkspaceUseCase {

    private final SharedWorkspaceRepositoryPort workspaceRepositoryPort;
    private final SharedWorkspaceMemberRepositoryPort memberRepositoryPort;
    private final Clock clock;

    public CreateSharedWorkspaceUseCase(
            SharedWorkspaceRepositoryPort workspaceRepositoryPort,
            SharedWorkspaceMemberRepositoryPort memberRepositoryPort,
            Clock clock) {
        this.workspaceRepositoryPort = workspaceRepositoryPort;
        this.memberRepositoryPort = memberRepositoryPort;
        this.clock = clock;
    }

    @Transactional
    public SharedWorkspaceResult execute(CreateSharedWorkspaceCommand command) {
        Instant now = Instant.now(clock);
        SharedWorkspace workspace =
                workspaceRepositoryPort.save(
                        SharedWorkspace.create(
                                command.organizationId(),
                                command.ownerUserId(),
                                command.name(),
                                command.description(),
                                now));
        memberRepositoryPort.save(
                SharedWorkspaceMember.owner(workspace.id(), command.ownerUserId(), now));
        return SharedWorkspaceResult.from(workspace);
    }
}
