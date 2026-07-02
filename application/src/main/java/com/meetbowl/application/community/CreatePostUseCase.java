package com.meetbowl.application.community;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostRepositoryPort;

/**
 * 게시글 등록 UseCase다.
 *
 * <p>흐름: (1) 게시글을 저장한다(제목/내용/카테고리 필수 검증은 도메인 {@link Post#create}에서 수행). (2) 등록 직후라 좋아요/댓글 수는 0으로
 * 응답한다.
 *
 * <p>익명성 주의: 게시글 작성자는 항상 "글쓴이"로 표시하고 실제 작성자 userId는 절대 노출하지 않는다. 댓글 쪽 익명 번호는 게시글 단위로 다시 계산한다.
 */
@Service
public class CreatePostUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final CommunityAliasPolicy communityAliasPolicy;

    public CreatePostUseCase(
            PostRepositoryPort postRepositoryPort, CommunityAliasPolicy communityAliasPolicy) {
        this.postRepositoryPort = postRepositoryPort;
        this.communityAliasPolicy = communityAliasPolicy;
    }

    @Transactional
    public PostResult execute(CreatePostCommand command) {
        Post post =
                Post.create(
                        CommunityCategory.from(command.category()),
                        command.title(),
                        command.content(),
                        command.authorUserId());
        Post saved = postRepositoryPort.save(post);

        return PostResult.of(saved, communityAliasPolicy.postAuthorAlias());
    }
}
