package com.meetbowl.application.community;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostLikeRepositoryPort;
import com.meetbowl.domain.community.PostRepositoryPort;

/**
 * 게시글 수정 UseCase다. 작성자 본인만 수정할 수 있고, 수정 항목은 카테고리·제목·내용뿐이다.
 *
 * <p>조회수/좋아요수/댓글수는 수정 대상이 아니다: viewCount·작성자는 도메인 {@link Post#change}가 보존하고, 좋아요/댓글 수는 이 UseCase가
 * 건드리지 않고 응답 표시용으로 현재 값을 읽기만 한다.
 */
@Service
public class UpdatePostUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final PostLikeRepositoryPort postLikeRepositoryPort;
    private final CommentRepositoryPort commentRepositoryPort;
    private final CommunityAliasDisplayResolver aliasDisplayResolver;

    public UpdatePostUseCase(
            PostRepositoryPort postRepositoryPort,
            PostLikeRepositoryPort postLikeRepositoryPort,
            CommentRepositoryPort commentRepositoryPort,
            CommunityAliasDisplayResolver aliasDisplayResolver) {
        this.postRepositoryPort = postRepositoryPort;
        this.postLikeRepositoryPort = postLikeRepositoryPort;
        this.commentRepositoryPort = commentRepositoryPort;
        this.aliasDisplayResolver = aliasDisplayResolver;
    }

    @Transactional
    public PostResult execute(UpdatePostCommand command) {
        Post post =
                postRepositoryPort
                        .findById(command.postId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없습니다."));

        // 작성자 본인만 수정 가능. 실제 userId 비교는 도메인이 보관한 authorUserId로 한다(응답엔 노출 안 함).
        if (!post.isAuthoredBy(command.requesterId())) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "작성자만 게시글을 수정할 수 있습니다.");
        }

        // 카테고리/제목/내용 검증과 작성자·조회수 보존은 도메인 change에서 수행된다.
        Post changed =
                post.change(
                        CommunityCategory.from(command.category()),
                        command.title(),
                        command.content());
        Post saved = postRepositoryPort.save(changed);

        // 좋아요/댓글 수는 수정으로 바뀌지 않는다. 응답 표시용으로 현재 값만 읽는다.
        long likeCount = postLikeRepositoryPort.countByPostId(saved.id());
        long commentCount = commentRepositoryPort.countByPostId(saved.id());
        // 작성자는 글을 쓴 적이 있으므로 별칭이 이미 존재한다(첫 활동 시 발급됨).
        String authorAlias =
                aliasDisplayResolver
                        .displayNames(Set.of(command.requesterId()))
                        .getOrDefault(
                                command.requesterId(),
                                CommunityAliasDisplayResolver.FALLBACK_DISPLAY_NAME);

        return PostResult.of(saved, authorAlias, likeCount, commentCount);
    }
}
