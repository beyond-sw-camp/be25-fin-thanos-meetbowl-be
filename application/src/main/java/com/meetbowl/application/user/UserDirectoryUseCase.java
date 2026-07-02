package com.meetbowl.application.user;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.organization.TeamRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserStatus;

@Service
public class UserDirectoryUseCase {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final UserRepositoryPort userRepositoryPort;
    private final AffiliateRepositoryPort affiliateRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final PositionRepositoryPort positionRepositoryPort;
    private final Clock clock;

    public UserDirectoryUseCase(
            UserRepositoryPort userRepositoryPort,
            AffiliateRepositoryPort affiliateRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort,
            Clock clock) {
        this.userRepositoryPort = userRepositoryPort;
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResult search(SearchCommand command) {
        int page = normalizePage(command.page());
        int size = normalizeSize(command.size());
        // 검색 API는 별도 status를 주지 않으면 화면 기본값과 동일하게 활성 사용자만 보여준다.
        UserStatus status =
                command.status() == null ? UserStatus.ACTIVE : parseStatus(command.status());
        Instant dayStart = todayStart();
        Instant nextDayStart = dayStart.plusSeconds(24 * 60 * 60);

        Paged<User> result =
                userRepositoryPort.search(
                        normalizeKeyword(command.keyword()),
                        command.affiliateId(),
                        command.departmentId(),
                        command.teamId(),
                        command.positionId(),
                        status,
                        dayStart,
                        nextDayStart,
                        page,
                        size);

        NameLookups lookups = loadNameLookups(result.content());
        List<UserDirectorySummary> items =
                result.content().stream().map(user -> toSummary(user, lookups)).toList();

        return new PageResult(
                items,
                page,
                size,
                result.totalElements(),
                calculateTotalPages(result.totalElements(), size));
    }

    @Transactional(readOnly = true)
    public UserDirectorySummary getSummary(UUID userId) {
        // 조직도에서 특정 회원을 클릭했을 때 보여줄 요약 정보이므로, 존재하지 않으면 즉시 404로 처리한다.
        User user =
                userRepositoryPort
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        NameLookups lookups = loadNameLookups(List.of(user));
        return toSummary(user, lookups);
    }

    private UserDirectorySummary toSummary(User user, NameLookups lookups) {
        return new UserDirectorySummary(
                user.id(),
                user.loginId(),
                user.name(),
                user.email(),
                user.role().name(),
                user.effectiveStatusAt(Instant.now(clock)).name(),
                user.affiliateId(),
                user.departmentId(),
                user.teamId(),
                user.positionId(),
                lookups.affiliateNames().get(user.affiliateId()),
                lookups.departmentNames().get(user.departmentId()),
                lookups.teamNames().get(user.teamId()),
                lookups.positionNames().get(user.positionId()));
    }

    private Instant todayStart() {
        return Instant.now(clock)
                .atZone(BUSINESS_ZONE)
                .toLocalDate()
                .atStartOfDay(BUSINESS_ZONE)
                .toInstant();
    }

    // 검색 결과 전체에서 조직 ID를 모아 한 번씩만 조회해 조직명 매핑 시 N+1을 피한다.
    private NameLookups loadNameLookups(List<User> users) {
        return new NameLookups(
                toNameMap(
                        affiliateRepositoryPort.findAllByIds(extractIds(users, User::affiliateId)),
                        Affiliate::id,
                        Affiliate::name),
                toNameMap(
                        departmentRepositoryPort.findAllByIds(
                                extractIds(users, User::departmentId)),
                        Department::id,
                        Department::name),
                toNameMap(
                        teamRepositoryPort.findAllByIds(extractIds(users, User::teamId)),
                        Team::id,
                        Team::name),
                toNameMap(
                        positionRepositoryPort.findAllByIds(extractIds(users, User::positionId)),
                        Position::id,
                        Position::name));
    }

    private Set<UUID> extractIds(List<User> users, Function<User, UUID> idExtractor) {
        return users.stream()
                .map(idExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private <T> Map<UUID, String> toNameMap(
            List<T> items, Function<T, UUID> idExtractor, Function<T, String> nameExtractor) {
        return items.stream().collect(Collectors.toMap(idExtractor, nameExtractor));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        // JPA 쿼리에서는 null이면 검색 조건을 생략하도록 작성했기 때문에 공백 문자열도 null로 통일한다.
        return keyword.trim();
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return 1;
        }
        if (page < 1) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Page must be at least 1.");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return 20;
        }
        if (size < 1 || size > 100) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Size must be between 1 and 100.");
        }
        return size;
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "Unsupported user status.");
        }
    }

    private int calculateTotalPages(long totalElements, int size) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }

    public record SearchCommand(
            String keyword,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            String status,
            Integer page,
            Integer size) {}

    public record UserDirectorySummary(
            UUID userId,
            String loginId,
            String name,
            String email,
            String role,
            String status,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            String affiliate,
            String department,
            String team,
            String position) {}

    public record PageResult(
            List<UserDirectorySummary> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {}

    private record NameLookups(
            Map<UUID, String> affiliateNames,
            Map<UUID, String> departmentNames,
            Map<UUID, String> teamNames,
            Map<UUID, String> positionNames) {}
}
