package com.meetbowl.infrastructure.persistence.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

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
    public List<User> findAllByAffiliateId(UUID affiliateId) {
        return springDataUserRepository.findByAffiliateId(affiliateId).stream()
                .map(UserEntity::toDomain)
                .toList();
    }
}
