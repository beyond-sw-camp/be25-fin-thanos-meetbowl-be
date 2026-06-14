package com.meetbowl.application.community;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentRepositoryPort;
import com.meetbowl.domain.community.CommunityAlias;
import com.meetbowl.domain.community.PostRepositoryPort;

/**
 * 댓글 등록 UseCase다.
 *
 * <p>흐름: (1) 대상 게시글이 존재하는지 확인한다(없거나 삭제됐으면 404로 거부). (2) 작성자의 익명 별칭을 확보한다 — <b>게시글과 동일한 별칭 매핑을
 * 재사용</b>한다. 채번/재사용 로직은 {@link CommunityAliasResolver}에 있으며 여기서 새로 만들지 않는다(유저1이 글에서 "익명1"이었다면 댓글도
 * "익명1"). (3) 댓글을 저장한다(내용 필수 검증은 도메인 {@link Comment#create}에서 수행).
 *
 * <p>게시글의 댓글 수(commentCount)는 별도 카운터 컬럼이 아니라 댓글 행 수를 그때그때 집계해 산출하므로(목록/상세의 COUNT 쿼리), 댓글을 저장하면 카운트
 * 정합성이 자동으로 유지된다 — 따로 증가시킬 값이 없다.
 *
 * <p>익명성: 응답에는 실제 작성자 userId를 노출하지 않고 "익명N" 표시명만 내린다.
 */
@Service
public class CreateCommentUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final CommentRepositoryPort commentRepositoryPort;
    private final CommunityAliasResolver communityAliasResolver;

    public CreateCommentUseCase(
            PostRepositoryPort postRepositoryPort,
            CommentRepositoryPort commentRepositoryPort,
            CommunityAliasResolver communityAliasResolver) {
        this.postRepositoryPort = postRepositoryPort;
        this.commentRepositoryPort = commentRepositoryPort;
        this.communityAliasResolver = communityAliasResolver;
    }

    @Transactional
    public CommentResult execute(CreateCommentCommand command) {
        // 게시글이 없거나 삭제됐으면 댓글을 달 수 없다.
        if (postRepositoryPort.findById(command.postId()).isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없습니다.");
        }

        // 게시글 등록 때 만든 별칭을 그대로 재사용한다(없으면 첫 활동으로 발급). 별칭 채번은 중복 구현하지 않는다.
        CommunityAlias alias = communityAliasResolver.resolve(command.authorUserId());

        Comment saved =
                commentRepositoryPort.save(
                        Comment.create(
                                command.postId(), command.content(), command.authorUserId()));

        return CommentResult.of(saved, alias.displayName());
    }
}
