package com.meetbowl.infrastructure.persistence.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserStatus;

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

    @Override
    public Paged<User> search(
            String keyword,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            UserStatus status,
            int page,
            int size) {
        // 이름순 정렬을 기본으로 두어 조직도/수신자 선택 UI에서 안정적인 검색 결과를 제공한다.
        PageRequest pageRequest =
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "name", "createdAt"));
        var result =
                springDataUserRepository.searchUsers(
                        keyword,
                        affiliateId,
                        departmentId,
                        teamId,
                        positionId,
                        status,
                        pageRequest);
        return new Paged<>(
                result.getContent().stream().map(UserEntity::toDomain).toList(),
                result.getTotalElements());
    }

    @Override
    public List<User> findAllByAffiliateId(UUID affiliateId) {
        return springDataUserRepository.findByAffiliateId(affiliateId).stream()
                .map(UserEntity::toDomain)
                .toList();
    }
}
