package com.meetbowl.infrastructure.persistence.user;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;
import com.meetbowl.infrastructure.search.user.ElasticsearchUserSearchAdapter;

@Repository
public class JpaUserRepositoryAdapter implements UserRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(JpaUserRepositoryAdapter.class);

    private final SpringDataUserRepository springDataUserRepository;
    private final ElasticsearchUserSearchAdapter elasticsearchUserSearchAdapter;

    public JpaUserRepositoryAdapter(
            SpringDataUserRepository springDataUserRepository,
            ElasticsearchUserSearchAdapter elasticsearchUserSearchAdapter) {
        this.springDataUserRepository = springDataUserRepository;
        this.elasticsearchUserSearchAdapter = elasticsearchUserSearchAdapter;
    }

    @Override
    public User save(User user) {
        return springDataUserRepository.save(UserEntity.from(user)).toDomain();
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return springDataUserRepository.findByIdAndDeletedAtIsNull(userId).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByIdIncludingDeleted(UUID userId) {
        return springDataUserRepository.findById(userId).map(UserEntity::toDomain);
    }

    @Override
    public List<User> findAll() {
        return springDataUserRepository.findAll().stream()
                .map(UserEntity::toDomain)
                .filter(user -> !user.isDeleted())
                .toList();
    }

    @Override
    public Optional<User> findByLoginId(String loginId) {
        return springDataUserRepository.findByLoginIdAndDeletedAtIsNull(loginId).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmailAndDeletedAtIsNull(email).map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return springDataUserRepository.existsByLoginIdAndDeletedAtIsNull(loginId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public List<User> findAllForExcelExportByRoles(Set<UserRole> roles) {
        return springDataUserRepository.findAllForExcelExportByRoles(roles).stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public Paged<User> findPage(String keyword, int page, int size) {
        return searchWithFallback(
                () -> elasticsearchUserSearchAdapter.searchAdmin(keyword, page, size),
                () -> {
                    PageRequest pageRequest =
                            PageRequest.of(
                                    page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                    var result = springDataUserRepository.searchByKeyword(keyword, pageRequest);
                    return new Paged<>(
                            result.getContent().stream().map(UserEntity::toDomain).toList(),
                            result.getTotalElements());
                });
    }

    @Override
    public Paged<User> search(
            String keyword,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            UserStatus status,
            Instant dayStart,
            Instant nextDayStart,
            int page,
            int size) {
        return searchWithFallback(
                () ->
                        elasticsearchUserSearchAdapter.searchDirectory(
                                keyword,
                                affiliateId,
                                departmentId,
                                teamId,
                                positionId,
                                status == null ? null : status.name(),
                                dayStart,
                                nextDayStart,
                                page,
                                size),
                () -> {
                    // 날짜 경계(activeFrom/activeUntil)와 삭제 제외 조건을 DB fallback에서도 동일하게 적용한다.
                    PageRequest pageRequest =
                            PageRequest.of(
                                    page - 1,
                                    size,
                                    Sort.by(Sort.Direction.ASC, "name", "createdAt"));
                    var result =
                            springDataUserRepository.searchUsers(
                                    keyword,
                                    affiliateId,
                                    departmentId,
                                    teamId,
                                    positionId,
                                    status,
                                    dayStart,
                                    nextDayStart,
                                    pageRequest);
                    return new Paged<>(
                            result.getContent().stream().map(UserEntity::toDomain).toList(),
                            result.getTotalElements());
                });
    }

    @Override
    public List<User> findAllByAffiliateId(UUID affiliateId) {
        return springDataUserRepository.findByAffiliateIdAndDeletedAtIsNull(affiliateId).stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public List<User> findAllByDepartmentId(UUID departmentId) {
        return springDataUserRepository.findByDepartmentIdAndDeletedAtIsNull(departmentId).stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public List<User> findAllByTeamId(UUID teamId) {
        return springDataUserRepository.findByTeamIdAndDeletedAtIsNull(teamId).stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public List<User> findAllByPositionId(UUID positionId) {
        return springDataUserRepository.findByPositionIdAndDeletedAtIsNull(positionId).stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    private Paged<User> searchWithFallback(
            Supplier<ElasticsearchUserSearchAdapter.SearchIdsPage> elasticsearchSearch,
            Supplier<Paged<User>> dbFallback) {
        try {
            ElasticsearchUserSearchAdapter.SearchIdsPage result = elasticsearchSearch.get();
            return toPagedUsers(result);
        } catch (RuntimeException exception) {
            // Elasticsearch 寃???ㅽ뙣 ??API ?꾩껜瑜?源⑥? ?딄린 ?꾪빐 DB LIKE 寃?됱쑝濡?利됱떆 ?고쉶?쒕떎.
            log.warn(
                    "User search fallback to DB due to Elasticsearch failure: {}",
                    exception.getMessage());
            return dbFallback.get();
        }
    }

    private Paged<User> toPagedUsers(ElasticsearchUserSearchAdapter.SearchIdsPage result) {
        if (result.userIds().isEmpty()) {
            return new Paged<>(List.of(), result.totalElements());
        }

        Map<UUID, User> usersById =
                springDataUserRepository.findAllById(result.userIds()).stream()
                        .map(UserEntity::toDomain)
                        .filter(user -> !user.isDeleted())
                        .collect(Collectors.toMap(User::id, user -> user));

        List<User> orderedUsers =
                result.userIds().stream().map(usersById::get).filter(user -> user != null).toList();
        return new Paged<>(orderedUsers, result.totalElements());
    }
}
