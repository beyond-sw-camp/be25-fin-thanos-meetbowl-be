package com.meetbowl.application.community;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.community.CommunityAlias;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostRepositoryPort;

/**
 * 게시글 등록 UseCase다.
 *
 * <p>흐름: (1) 작성자의 익명 별칭을 확보한다(첫 활동이면 발급, 아니면 재사용 — 동시성은 {@link CommunityAliasResolver}가 유니크 제약+재시도로
 * 처리). (2) 게시글을 저장한다(제목/내용/카테고리 필수 검증은 도메인 {@link Post#create}에서 수행). (3) 등록 직후라 좋아요/댓글 수는 0으로
 * 응답한다.
 *
 * <p>익명성 주의: 응답에는 실제 작성자 userId를 절대 노출하지 않고 "익명N" 표시명만 내린다. 별칭은 사용자당 전역 고정(글·댓글 공통)이라 같은 사용자의 활동이
 * 같은 번호로 묶인다 .
 */
@Service
public class CreatePostUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final CommunityAliasResolver communityAliasResolver;

    public CreatePostUseCase(
            PostRepositoryPort postRepositoryPort, CommunityAliasResolver communityAliasResolver) {
        this.postRepositoryPort = postRepositoryPort;
        this.communityAliasResolver = communityAliasResolver;
    }

    @Transactional
    public PostResult execute(CreatePostCommand command) {
        // 별칭 발급은 REQUIRES_NEW로 독립 커밋된다. 별칭은 "첫 활동" 기준 영구 부여이므로 이후 게시글 저장이 실패해도 보존된다.
        CommunityAlias alias = communityAliasResolver.resolve(command.authorUserId());

        // 카테고리 문자열(enum 이름/한글 라벨)을 도메인 카테고리로 해석한다. 알 수 없는 값이면 400으로 막힌다.
        Post post =
                Post.create(
                        CommunityCategory.from(command.category()),
                        command.title(),
                        command.content(),
                        command.authorUserId());
        Post saved = postRepositoryPort.save(post);

        return PostResult.of(saved, alias.displayName());
    }
}
