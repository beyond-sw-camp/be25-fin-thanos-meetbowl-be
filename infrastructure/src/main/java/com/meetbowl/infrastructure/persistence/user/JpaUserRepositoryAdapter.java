package com.meetbowl.infrastructure.persistence.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

@Repository
public class JpaUserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository springDataUserRepository;

    public JpaUserRepositoryAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public User save(User user) {
        return springDataUserRepository.save(UserEntity.from(user)).toDomain();
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return springDataUserRepository.findById(userId).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByLoginId(String loginId) {
        return springDataUserRepository.findByLoginId(loginId).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email).map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return springDataUserRepository.existsByLoginId(loginId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }

    @Override
    public Paged<User> findPage(String keyword, int page, int size) {
        PageRequest pageRequest =
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = springDataUserRepository.searchByKeyword(keyword, pageRequest);
        return new Paged<>(
                result.getContent().stream().map(UserEntity::toDomain).toList(),
                result.getTotalElements());
    }
}
