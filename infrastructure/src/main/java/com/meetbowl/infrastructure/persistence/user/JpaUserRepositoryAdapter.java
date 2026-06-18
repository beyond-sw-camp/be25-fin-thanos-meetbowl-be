package com.meetbowl.infrastructure.persistence.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        return springDataUserRepository.findById(userId).map(UserEntity::toDomain);
    }

    @Override
    public List<User> findAll() {
        return springDataUserRepository.findAll().stream().map(UserEntity::toDomain).toList();
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
                                page,
                                size),
                () -> {
                    // 검색 장애 시 기존 DB 정렬/필터 규칙을 그대로 유지해 호출자 계약을 지킨다.
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
                                    pageRequest);
                    return new Paged<>(
                            result.getContent().stream().map(UserEntity::toDomain).toList(),
                            result.getTotalElements());
                });
    }

    @Override
    public List<User> findAllByAffiliateId(UUID affiliateId) {
        return springDataUserRepository.findByAffiliateId(affiliateId).stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public List<User> findAllByDepartmentId(UUID departmentId) {
        return springDataUserRepository.findByDepartmentId(departmentId).stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public List<User> findAllByTeamId(UUID teamId) {
        return springDataUserRepository.findByTeamId(teamId).stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public List<User> findAllByPositionId(UUID positionId) {
        return springDataUserRepository.findByPositionId(positionId).stream()
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
            // Elasticsearch 검색 실패 시 API 전체를 깨지 않기 위해 DB LIKE 검색으로 즉시 우회한다.
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

        // ES 점수순 결과를 유지한 채 기존 도메인 User 매핑을 재사용하기 위해 ID 기준으로 다시 조립한다.
        Map<UUID, User> usersById =
                springDataUserRepository.findAllById(result.userIds()).stream()
                        .map(UserEntity::toDomain)
                        .collect(Collectors.toMap(User::id, user -> user));

        List<User> orderedUsers =
                result.userIds().stream().map(usersById::get).filter(user -> user != null).toList();
        return new Paged<>(orderedUsers, result.totalElements());
    }
}
