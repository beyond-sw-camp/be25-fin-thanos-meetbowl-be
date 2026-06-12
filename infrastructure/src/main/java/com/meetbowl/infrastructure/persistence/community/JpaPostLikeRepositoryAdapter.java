package com.meetbowl.infrastructure.persistence.community;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.community.PostLike;
import com.meetbowl.domain.community.PostLikeRepositoryPort;

/** PostLike domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaPostLikeRepositoryAdapter implements PostLikeRepositoryPort {

    private final SpringDataPostLikeRepository springDataPostLikeRepository;

    public JpaPostLikeRepositoryAdapter(SpringDataPostLikeRepository springDataPostLikeRepository) {
        this.springDataPostLikeRepository = springDataPostLikeRepository;
    }

    @Override
    public PostLike save(PostLike postLike) {
        return springDataPostLikeRepository.save(PostLikeEntity.from(postLike)).toDomain();
    }

    @Override
    public boolean existsByPostIdAndUserId(UUID postId, UUID userId) {
        return springDataPostLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    @Override
    @Transactional
    public void deleteByPostIdAndUserId(UUID postId, UUID userId) {
        springDataPostLikeRepository.deleteByPostIdAndUserId(postId, userId);
    }

    @Override
    public long countByPostId(UUID postId) {
        return springDataPostLikeRepository.countByPostId(postId);
    }

    @Override
    @Transactional
    public void deleteByPostId(UUID postId) {
        springDataPostLikeRepository.deleteByPostId(postId);
    }
}
