package com.meetbowl.infrastructure.persistence.community;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.community.CommunityCategory;
import com.meetbowl.domain.community.Post;
import com.meetbowl.domain.community.PostRepositoryPort;

/** Post domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaPostRepositoryAdapter implements PostRepositoryPort {

    private final SpringDataPostRepository springDataPostRepository;

    public JpaPostRepositoryAdapter(SpringDataPostRepository springDataPostRepository) {
        this.springDataPostRepository = springDataPostRepository;
    }

    @Override
    public Post save(Post post) {
        return springDataPostRepository.save(PostEntity.from(post)).toDomain();
    }

    @Override
    public Optional<Post> findById(UUID id) {
        return springDataPostRepository.findById(id).map(PostEntity::toDomain);
    }

    @Override
    public Paged<Post> findPage(CommunityCategory category, int page, int size) {
        PageRequest pageRequest =
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostEntity> result =
                category == null
                        ? springDataPostRepository.findAll(pageRequest)
                        : springDataPostRepository.findByCategory(category, pageRequest);
        return new Paged<>(
                result.getContent().stream().map(PostEntity::toDomain).toList(),
                result.getTotalElements());
    }

    @Override
    public void deleteById(UUID id) {
        springDataPostRepository.deleteById(id);
    }
}