package com.meetbowl.infrastructure.persistence.community;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.domain.community.Comment;
import com.meetbowl.domain.community.CommentRepositoryPort;

/** Comment domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaCommentRepositoryAdapter implements CommentRepositoryPort {

    private final SpringDataCommentRepository springDataCommentRepository;

    public JpaCommentRepositoryAdapter(SpringDataCommentRepository springDataCommentRepository) {
        this.springDataCommentRepository = springDataCommentRepository;
    }

    @Override
    public Comment save(Comment comment) {
        return springDataCommentRepository.save(CommentEntity.from(comment)).toDomain();
    }

    @Override
    public Optional<Comment> findById(UUID id) {
        return springDataCommentRepository.findById(id).map(CommentEntity::toDomain);
    }

    @Override
    public List<Comment> findByPostId(UUID postId) {
        return springDataCommentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommentEntity::toDomain)
                .toList();
    }

    @Override
    public long countByPostId(UUID postId) {
        return springDataCommentRepository.countByPostId(postId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataCommentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByPostId(UUID postId) {
        springDataCommentRepository.deleteByPostId(postId);
    }
}