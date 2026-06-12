package com.meetbowl.infrastructure.persistence.community;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA 전용 repository다. application/domain 계층에서는 이 타입을 직접 참조하지 않는다. */
public interface SpringDataPostLikeRepository extends JpaRepository<PostLikeEntity, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    void deleteByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    void deleteByPostId(UUID postId);
}
