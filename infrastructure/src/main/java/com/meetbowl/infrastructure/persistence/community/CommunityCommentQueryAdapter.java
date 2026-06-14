package com.meetbowl.infrastructure.persistence.community;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.community.CommunityCommentListItem;
import com.meetbowl.domain.community.CommunityCommentQueryPort;

/**
 * 커뮤니티 댓글 조회 전용 포트의 JPA 구현 adapter다. 좋아요 수 집계가 포함된 프로젝션 쿼리를 Spring Data에 위임하고, 도메인 조회 모델로의 변환은 이
 * 경계에서만 수행한다.
 */
@Repository
public class CommunityCommentQueryAdapter implements CommunityCommentQueryPort {

    private final SpringDataCommentRepository springDataCommentRepository;

    public CommunityCommentQueryAdapter(SpringDataCommentRepository springDataCommentRepository) {
        this.springDataCommentRepository = springDataCommentRepository;
    }

    @Override
    public List<CommunityCommentListItem> findByPostId(UUID postId) {
        return springDataCommentRepository.findCommentListByPostId(postId);
    }
}
