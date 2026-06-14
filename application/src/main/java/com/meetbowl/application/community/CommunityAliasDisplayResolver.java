package com.meetbowl.application.community;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.meetbowl.domain.community.CommunityAlias;
import com.meetbowl.domain.community.CommunityAliasRepositoryPort;

/**
 * 목록/Hot 화면에서 작성자 userId 묶음을 "익명N" 표시명으로 배치 변환한다. 작성자당 한 번씩 조회하면 N+1이 되므로, 페이지에 등장하는 userId를 한 번에
 * 조회해 매핑한다.
 */
@Component
public class CommunityAliasDisplayResolver {

    /**
     * 별칭이 조회되지 않을 때의 안전 표시값이다. 글/댓글 작성 시 항상 별칭을 발급하므로 정상 흐름에선 쓰이지 않지만, 데이터 정합성 깨짐(예: 별칭 행 유실)에도 실제
     * userId가 노출되지 않도록 둔다.
     */
    public static final String FALLBACK_DISPLAY_NAME = "익명";

    private final CommunityAliasRepositoryPort communityAliasRepositoryPort;

    public CommunityAliasDisplayResolver(
            CommunityAliasRepositoryPort communityAliasRepositoryPort) {
        this.communityAliasRepositoryPort = communityAliasRepositoryPort;
    }

    /** 주어진 사용자들의 userId → "익명N" 표시명 매핑을 반환한다. */
    public Map<UUID, String> displayNames(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return communityAliasRepositoryPort.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(CommunityAlias::userId, CommunityAlias::displayName));
    }
}
