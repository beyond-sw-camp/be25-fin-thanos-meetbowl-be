package com.meetbowl.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.organization.TeamRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

class UserDirectoryUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER2_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER3_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID AFFILIATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID POSITION_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
    private static final UUID AFFILIATE2_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID DEPARTMENT2_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID TEAM2_ID = UUID.fromString("00000000-0000-0000-0000-000000000023");
    private static final UUID POSITION2_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000024");
    private static final Instant NOW = Instant.parse("2026-06-23T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private FakeUserRepository userRepository;
    private UserDirectoryUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        FakeAffiliateRepository affiliateRepository = new FakeAffiliateRepository();
        FakeDepartmentRepository departmentRepository = new FakeDepartmentRepository();
        FakeTeamRepository teamRepository = new FakeTeamRepository();
        FakePositionRepository positionRepository = new FakePositionRepository();

        affiliateRepository.save(
                new Affiliate(
                        AFFILIATE_ID, "Affiliate", "AFF", ReferenceStatus.ACTIVE, 1, NOW, NOW));
        departmentRepository.save(
                new Department(
                        DEPARTMENT_ID,
                        AFFILIATE_ID,
                        null,
                        "Department",
                        "DEP",
                        ReferenceStatus.ACTIVE,
                        1,
                        NOW,
                        NOW));
        teamRepository.save(
                new Team(
                        TEAM_ID,
                        DEPARTMENT_ID,
                        "Team",
                        "TEAM",
                        ReferenceStatus.ACTIVE,
                        1,
                        NOW,
                        NOW));
        positionRepository.save(
                new Position(
                        POSITION_ID, AFFILIATE_ID, "Position", "POS", ReferenceStatus.ACTIVE, 1, NOW, NOW));
        affiliateRepository.save(
                new Affiliate(
                        AFFILIATE2_ID,
                        "Platform Affiliate",
                        "PLA",
                        ReferenceStatus.ACTIVE,
                        2,
                        NOW,
                        NOW));
        departmentRepository.save(
                new Department(
                        DEPARTMENT2_ID,
                        AFFILIATE2_ID,
                        null,
                        "Service Development",
                        "SVC",
                        ReferenceStatus.ACTIVE,
                        2,
                        NOW,
                        NOW));
        teamRepository.save(
                new Team(
                        TEAM2_ID,
                        DEPARTMENT2_ID,
                        "Core Platform Team",
                        "CORE",
                        ReferenceStatus.ACTIVE,
                        2,
                        NOW,
                        NOW));
        positionRepository.save(
                new Position(
                        POSITION2_ID,
                        AFFILIATE2_ID,
                        "Assistant Manager",
                        "AM",
                        ReferenceStatus.ACTIVE,
                        2,
                        NOW,
                        NOW));

        userRepository.save(
                createUser(
                        USER_ID,
                        "hong",
                        "Hong Gil Dong",
                        "hong@example.com",
                        UserStatus.ACTIVE,
                        AFFILIATE_ID,
                        DEPARTMENT_ID,
                        TEAM_ID,
                        POSITION_ID));
        userRepository.save(
                createUser(
                        USER2_ID,
                        "kim",
                        "Kim Tester",
                        "kim@example.com",
                        UserStatus.INACTIVE,
                        AFFILIATE_ID,
                        DEPARTMENT_ID,
                        TEAM_ID,
                        POSITION_ID));
        userRepository.save(
                createUser(
                        USER3_ID,
                        "emailuser",
                        "Lee User",
                        "recipient@example.com",
                        UserStatus.ACTIVE,
                        AFFILIATE2_ID,
                        DEPARTMENT2_ID,
                        TEAM2_ID,
                        POSITION2_ID));

        useCase =
                new UserDirectoryUseCase(
                        userRepository,
                        affiliateRepository,
                        departmentRepository,
                        teamRepository,
                        positionRepository,
                        FIXED_CLOCK);
    }

    @Test
    void searchByNameSuccess() {
        UserDirectoryUseCase.PageResult result =
                useCase.search(
                        new UserDirectoryUseCase.SearchCommand(
                                "Hong", null, null, null, null, null, 1, 20));

        assertEquals(1, result.items().size());
        assertEquals("Hong Gil Dong", result.items().get(0).name());
    }

    @Test
    void searchByEmailSuccess() {
        UserDirectoryUseCase.PageResult result =
                useCase.search(
                        new UserDirectoryUseCase.SearchCommand(
                                "recipient@example.com", null, null, null, null, null, 1, 20));

        assertEquals(1, result.items().size());
        assertEquals("recipient@example.com", result.items().get(0).email());
    }

    @Test
    void searchByOrganizationFiltersSuccess() {
        UserDirectoryUseCase.PageResult result =
                useCase.search(
                        new UserDirectoryUseCase.SearchCommand(
                                null,
                                AFFILIATE_ID,
                                DEPARTMENT_ID,
                                TEAM_ID,
                                POSITION_ID,
                                "ACTIVE",
                                1,
                                20));

        // status=ACTIVE 필터를 함께 주면 같은 조직의 비활성 사용자(USER2_ID)는 결과에서 제외되어야 한다.
        assertEquals(1, result.items().size());
        assertEquals("Affiliate", result.items().get(0).affiliate());
    }

    @Test
    void searchByOrganizationNamePartialSuccess() {
        UserDirectoryUseCase.PageResult result =
                useCase.search(
                        new UserDirectoryUseCase.SearchCommand(
                                " service ", null, null, null, null, null, 1, 20));

        assertEquals(1, result.items().size());
        assertEquals(USER3_ID, result.items().get(0).userId());
        assertEquals("Service Development", result.items().get(0).department());
    }

    @Test
    void searchDefaultsToActiveUsers() {
        UserDirectoryUseCase.PageResult result =
                useCase.search(
                        new UserDirectoryUseCase.SearchCommand(
                                null, null, null, null, null, null, 1, 20));

        assertEquals(2, result.items().size());
        assertEquals(
                List.of(USER_ID, USER3_ID),
                result.items().stream()
                        .map(UserDirectoryUseCase.UserDirectorySummary::userId)
                        .toList());
    }

    @Test
    void searchAppliesEffectiveStatusByDateBoundaries() {
        userRepository.save(
                createUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000004"),
                        "expired",
                        "Expired User",
                        "expired@example.com",
                        UserStatus.ACTIVE,
                        AFFILIATE_ID,
                        DEPARTMENT_ID,
                        TEAM_ID,
                        POSITION_ID,
                        Instant.parse("2026-06-19T00:00:00Z"),
                        Instant.parse("2026-06-21T00:00:00Z")));
        userRepository.save(
                createUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000005"),
                        "today-active",
                        "Today Active",
                        "today@example.com",
                        UserStatus.ACTIVE,
                        AFFILIATE_ID,
                        DEPARTMENT_ID,
                        TEAM_ID,
                        POSITION_ID,
                        Instant.parse("2026-06-19T00:00:00Z"),
                        Instant.parse("2026-06-23T00:00:00Z")));
        userRepository.save(
                createUser(
                        UUID.fromString("00000000-0000-0000-0000-000000000006"),
                        "future-user",
                        "Future User",
                        "future@example.com",
                        UserStatus.ACTIVE,
                        AFFILIATE_ID,
                        DEPARTMENT_ID,
                        TEAM_ID,
                        POSITION_ID,
                        Instant.parse("2026-06-24T00:00:00Z"),
                        null));

        UserDirectoryUseCase.PageResult activeResult =
                useCase.search(
                        new UserDirectoryUseCase.SearchCommand(
                                null, null, null, null, null, "ACTIVE", 1, 20));
        UserDirectoryUseCase.PageResult inactiveResult =
                useCase.search(
                        new UserDirectoryUseCase.SearchCommand(
                                null, null, null, null, null, "INACTIVE", 1, 20));

        assertEquals(
                List.of("hong", "emailuser", "today-active"),
                activeResult.items().stream()
                        .map(UserDirectoryUseCase.UserDirectorySummary::loginId)
                        .toList());
        assertEquals(
                List.of("expired", "future-user", "kim"),
                inactiveResult.items().stream()
                        .map(UserDirectoryUseCase.UserDirectorySummary::loginId)
                        .toList());
    }

    @Test
    void getSummarySuccess() {
        UserDirectoryUseCase.UserDirectorySummary result = useCase.getSummary(USER_ID);

        assertEquals(USER_ID, result.userId());
        assertEquals("hong", result.loginId());
        assertEquals("Affiliate", result.affiliate());
        assertEquals("Department", result.department());
        assertEquals("Team", result.team());
        assertEquals("Position", result.position());
    }

    @Test
    void getSummaryFailsWhenUserDoesNotExist() {
        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.getSummary(UUID.randomUUID()));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode());
    }

    private User createUser(
            UUID id,
            String loginId,
            String name,
            String email,
            UserStatus status,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId) {
        return createUser(
                id,
                loginId,
                name,
                email,
                status,
                affiliateId,
                departmentId,
                teamId,
                positionId,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z"));
    }

    private User createUser(
            UUID id,
            String loginId,
            String name,
            String email,
            UserStatus status,
            UUID affiliateId,
            UUID departmentId,
            UUID teamId,
            UUID positionId,
            Instant activeFrom,
            Instant activeUntil) {
        return User.of(
                id,
                loginId,
                "hash",
                name,
                email,
                UserRole.USER,
                status,
                affiliateId,
                departmentId,
                positionId,
                teamId,
                false,
                activeFrom,
                activeUntil,
                NOW,
                NOW);
    }

    private static final class FakeUserRepository implements UserRepositoryPort {
        private final Map<UUID, User> users = new ConcurrentHashMap<>();

        @Override
        public User save(User user) {
            users.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(users.values());
        }

        @Override
        public List<User> findAllForExcelExportByRoles(java.util.Set<UserRole> roles) {
            return users.values().stream().filter(user -> roles.contains(user.role())).toList();
        }

        @Override
        public Optional<User> findByLoginId(String loginId) {
            return users.values().stream()
                    .filter(user -> user.loginId().equals(loginId))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(user -> user.email().equals(email)).findFirst();
        }

        @Override
        public boolean existsByLoginId(String loginId) {
            return findByLoginId(loginId).isPresent();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public Paged<User> findPage(String keyword, int page, int size) {
            return new Paged<>(List.copyOf(users.values()), users.size());
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
            List<User> filtered =
                    users.values().stream()
                            .filter(user -> keyword == null || matchesKeyword(user, keyword))
                            .filter(
                                    user ->
                                            affiliateId == null
                                                    || affiliateId.equals(user.affiliateId()))
                            .filter(
                                    user ->
                                            departmentId == null
                                                    || departmentId.equals(user.departmentId()))
                            .filter(user -> teamId == null || teamId.equals(user.teamId()))
                            .filter(
                                    user ->
                                            positionId == null
                                                    || positionId.equals(user.positionId()))
                            .filter(
                                    user ->
                                            status == null
                                                    || user.effectiveStatusAt(dayStart)
                                                            .equals(status))
                            .filter(user -> !user.isDeleted())
                            .sorted(java.util.Comparator.comparing(User::name))
                            .toList();
            return new Paged<>(filtered, filtered.size());
        }

        private boolean matchesKeyword(User user, String keyword) {
            String normalized = keyword.toLowerCase();
            return user.name().toLowerCase().contains(normalized)
                    || user.email().toLowerCase().contains(normalized)
                    || user.loginId().toLowerCase().contains(normalized)
                    || organizationName(user.affiliateId()).contains(normalized)
                    || organizationName(user.departmentId()).contains(normalized)
                    || organizationName(user.teamId()).contains(normalized)
                    || organizationName(user.positionId()).contains(normalized);
        }

        private String organizationName(UUID id) {
            if (AFFILIATE_ID.equals(id)) {
                return "affiliate";
            }
            if (DEPARTMENT_ID.equals(id)) {
                return "department";
            }
            if (TEAM_ID.equals(id)) {
                return "team";
            }
            if (POSITION_ID.equals(id)) {
                return "position";
            }
            if (AFFILIATE2_ID.equals(id)) {
                return "platform affiliate";
            }
            if (DEPARTMENT2_ID.equals(id)) {
                return "service development";
            }
            if (TEAM2_ID.equals(id)) {
                return "core platform team";
            }
            if (POSITION2_ID.equals(id)) {
                return "assistant manager";
            }
            return "";
        }

        @Override
        public List<User> findAllByAffiliateId(UUID affiliateId) {
            return users.values().stream()
                    .filter(user -> affiliateId.equals(user.affiliateId()))
                    .toList();
        }

        @Override
        public List<User> findAllByDepartmentId(UUID departmentId) {
            return users.values().stream()
                    .filter(user -> departmentId.equals(user.departmentId()))
                    .toList();
        }

        @Override
        public List<User> findAllByTeamId(UUID teamId) {
            return users.values().stream().filter(user -> teamId.equals(user.teamId())).toList();
        }

        @Override
        public List<User> findAllByPositionId(UUID positionId) {
            return users.values().stream()
                    .filter(user -> positionId.equals(user.positionId()))
                    .toList();
        }
    }

    private static final class FakeAffiliateRepository implements AffiliateRepositoryPort {
        private final Map<UUID, Affiliate> affiliates = new ConcurrentHashMap<>();

        @Override
        public Affiliate save(Affiliate affiliate) {
            affiliates.put(affiliate.id(), affiliate);
            return affiliate;
        }

        @Override
        public Optional<Affiliate> findById(UUID affiliateId) {
            return Optional.ofNullable(affiliates.get(affiliateId));
        }

        @Override
        public List<Affiliate> findAll() {
            return List.copyOf(affiliates.values());
        }

        @Override
        public List<Affiliate> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public List<Affiliate> findAllByIds(Collection<UUID> affiliateIds) {
            return affiliateIds.stream()
                    .map(affiliates::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public boolean existsByName(String name) {
            return false;
        }

        @Override
        public boolean existsByCode(String code) {
            return false;
        }

        @Override
        public boolean existsByNameAndIdNot(String name, UUID affiliateId) {
            return false;
        }

        @Override
        public boolean existsByCodeAndIdNot(String code, UUID affiliateId) {
            return false;
        }
    }

    private static final class FakeDepartmentRepository implements DepartmentRepositoryPort {
        private final Map<UUID, Department> departments = new ConcurrentHashMap<>();

        @Override
        public Department save(Department department) {
            departments.put(department.id(), department);
            return department;
        }

        @Override
        public void deleteById(UUID departmentId) {
            departments.remove(departmentId);
        }

        @Override
        public Optional<Department> findById(UUID departmentId) {
            return Optional.ofNullable(departments.get(departmentId));
        }

        @Override
        public List<Department> findAll() {
            return List.copyOf(departments.values());
        }

        @Override
        public List<Department> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public List<Department> findAllByIds(Collection<UUID> departmentIds) {
            return departmentIds.stream()
                    .map(departments::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public boolean existsByAffiliateIdAndName(UUID affiliateId, String name) {
            return false;
        }

        @Override
        public boolean existsByAffiliateIdAndNameAndIdNot(
                UUID affiliateId, String name, UUID departmentId) {
            return false;
        }

        @Override
        public boolean existsByAffiliateIdAndSortOrder(UUID affiliateId, Integer sortOrder) {
            return false;
        }

        @Override
        public boolean existsByAffiliateIdAndSortOrderAndIdNot(
                UUID affiliateId, Integer sortOrder, UUID departmentId) {
            return false;
        }
    }

    private static final class FakeTeamRepository implements TeamRepositoryPort {
        private final Map<UUID, Team> teams = new ConcurrentHashMap<>();

        @Override
        public Team save(Team team) {
            teams.put(team.id(), team);
            return team;
        }

        @Override
        public void deleteById(UUID teamId) {
            teams.remove(teamId);
        }

        @Override
        public Optional<Team> findById(UUID teamId) {
            return Optional.ofNullable(teams.get(teamId));
        }

        @Override
        public List<Team> findAll() {
            return List.copyOf(teams.values());
        }

        @Override
        public List<Team> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public List<Team> findAllByIds(Collection<UUID> teamIds) {
            return teamIds.stream().map(teams::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public List<Team> findAllByDepartmentId(UUID departmentId) {
            return teams.values().stream()
                    .filter(team -> team.departmentId().equals(departmentId))
                    .toList();
        }

        @Override
        public boolean existsByDepartmentIdAndName(UUID departmentId, String name) {
            return false;
        }

        @Override
        public boolean existsByDepartmentIdAndNameAndIdNot(
                UUID departmentId, String name, UUID teamId) {
            return false;
        }

        @Override
        public boolean existsByAffiliateIdAndSortOrder(UUID affiliateId, Integer sortOrder) {
            return false;
        }

        @Override
        public boolean existsByAffiliateIdAndSortOrderAndIdNot(
                UUID affiliateId, Integer sortOrder, UUID teamId) {
            return false;
        }
    }

    private static final class FakePositionRepository implements PositionRepositoryPort {
        private final Map<UUID, Position> positions = new ConcurrentHashMap<>();

        @Override
        public Position save(Position position) {
            positions.put(position.id(), position);
            return position;
        }

        @Override
        public void deleteById(UUID positionId) {
            positions.remove(positionId);
        }

        @Override
        public Optional<Position> findById(UUID positionId) {
            return Optional.ofNullable(positions.get(positionId));
        }

        @Override
        public List<Position> findAll() {
            return List.copyOf(positions.values());
        }

        @Override
        public List<Position> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public List<Position> findAllByIds(Collection<UUID> positionIds) {
            return positionIds.stream()
                    .map(positions::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public List<Position> findAllByAffiliateId(UUID affiliateId) {
            return positions.values().stream()
                    .filter(position -> java.util.Objects.equals(position.affiliateId(), affiliateId))
                    .toList();
        }

        @Override
        public boolean existsByAffiliateIdAndName(UUID affiliateId, String name) {
            return false;
        }

        @Override
        public boolean existsByCode(String code) {
            return false;
        }

        @Override
        public boolean existsByAffiliateIdAndNameAndIdNot(
                UUID affiliateId, String name, UUID positionId) {
            return false;
        }

        @Override
        public boolean existsByCodeAndIdNot(String code, UUID positionId) {
            return false;
        }

        @Override
        public boolean existsByAffiliateIdAndSortOrder(UUID affiliateId, Integer sortOrder) {
            return false;
        }

        @Override
        public boolean existsByAffiliateIdAndSortOrderAndIdNot(
                UUID affiliateId, Integer sortOrder, UUID positionId) {
            return false;
        }
    }
}
