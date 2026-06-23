package com.meetbowl.infrastructure.persistence.community;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.community.CommentLike;
import com.meetbowl.domain.community.CommentLikeRepositoryPort;

/** CommentLike domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaCommentLikeRepositoryAdapter implements CommentLikeRepositoryPort {

    private final SpringDataCommentLikeRepository springDataCommentLikeRepository;

    public JpaCommentLikeRepositoryAdapter(
            SpringDataCommentLikeRepository springDataCommentLikeRepository) {
        this.springDataCommentLikeRepository = springDataCommentLikeRepository;
    }

    @Override
    public CommentLike save(CommentLike commentLike) {
        return springDataCommentLikeRepository.save(CommentLikeEntity.from(commentLike)).toDomain();
    }

    @Override
    public boolean existsByCommentIdAndUserId(UUID commentId, UUID userId) {
        return springDataCommentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }

    @Override
    public Set<UUID> findLikedCommentIds(UUID userId, Collection<UUID> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(
                springDataCommentLikeRepository.findLikedCommentIds(userId, commentIds));
    }

    @Override
    @Transactional
    public void deleteByCommentIdAndUserId(UUID commentId, UUID userId) {
        springDataCommentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
    }

    @Override
    public long countByCommentId(UUID commentId) {
        return springDataCommentLikeRepository.countByCommentId(commentId);
    }

    @Override
    @Transactional
    public void deleteByCommentId(UUID commentId) {
        springDataCommentLikeRepository.deleteByCommentId(commentId);
    }
}
