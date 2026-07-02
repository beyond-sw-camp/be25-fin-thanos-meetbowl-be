package com.meetbowl.application.community;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.meetbowl.domain.community.CommunityCommentListItem;

/**
 * 커뮤니티 작성자 표기 정책을 계산한다.
 *
 * <p>게시글 작성자는 항상 "글쓴이"로 보이고, 댓글 작성자는 게시글별로 다시 번호를 매긴다. 같은 글 안에서는 같은 사용자가 동일 번호를 재사용하고,
 * 게시글 작성자가 댓글을 달면 그 댓글도 "글쓴이"로 유지한다.
 */
@Component
public class CommunityAliasPolicy {

    public static final String POST_AUTHOR_ALIAS = "글쓴이";

    public String postAuthorAlias() {
        return POST_AUTHOR_ALIAS;
    }

    public String commentAuthorAlias(
            UUID postAuthorUserId, UUID commentAuthorUserId, List<CommunityCommentListItem> comments) {
        if (postAuthorUserId != null && postAuthorUserId.equals(commentAuthorUserId)) {
            return POST_AUTHOR_ALIAS;
        }
        return commentAliases(postAuthorUserId, comments).getOrDefault(commentAuthorUserId, "익명");
    }

    public Map<UUID, String> commentAliases(
            UUID postAuthorUserId, List<CommunityCommentListItem> comments) {
        Map<UUID, String> aliases = new LinkedHashMap<>();
        int nextAliasNo = 1;
        for (CommunityCommentListItem comment : comments) {
            UUID authorUserId = comment.authorUserId();
            if (authorUserId == null || aliases.containsKey(authorUserId)) {
                continue;
            }
            if (postAuthorUserId != null && postAuthorUserId.equals(authorUserId)) {
                aliases.put(authorUserId, POST_AUTHOR_ALIAS);
                continue;
            }
            aliases.put(authorUserId, "익명" + nextAliasNo++);
        }
        return aliases;
    }
}
