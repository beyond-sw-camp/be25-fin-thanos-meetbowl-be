package com.meetbowl.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
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
import com.meetbowl.domain.organization.Affiliate;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.Department;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.organization.Position;
import com.meetbowl.domain.organization.PositionRepositoryPort;
import com.meetbowl.domain.organization.ReferenceStatus;
import com.meetbowl.domain.organization.Team;
import com.meetbowl.domain.organization.TeamRepositoryPort;

class AdminOrganizationMasterDataUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-06-12T00:00:00Z");
    private static final UUID AFFILIATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID POSITION_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private FakeAffiliateRepository affiliateRepository;
    private FakeDepartmentRepository departmentRepository;
    private FakeTeamRepository teamRepository;
    private FakePositionRepository positionRepository;
    private AdminOrganizationMasterDataUseCase useCase;

    @BeforeEach
    void setUp() {
        affiliateRepository = new FakeAffiliateRepository();
        departmentRepository = new FakeDepartmentRepository();
        teamRepository = new FakeTeamRepository();
        positionRepository = new FakePositionRepository();
        useCase =
                new AdminOrganizationMasterDataUseCase(
                        affiliateRepository,
                        departmentRepository,
                        teamRepository,
                        positionRepository);
    }

    @Test
    void affiliateCrudSuccess() {
        AdminOrganizationMasterDataUseCase.AffiliateResult created =
                useCase.createAffiliate(
                        new AdminOrganizationMasterDataUseCase.CreateAffiliateCommand(
                                "Platform", "PLT", "ACTIVE", 1));

        AdminOrganizationMasterDataUseCase.AffiliateResult updated =
                useCase.updateAffiliate(
                        new AdminOrganizationMasterDataUseCase.UpdateAffiliateCommand(
                                created.affiliateId(), "Platform Lab", "PLT-LAB", 2));

        AdminOrganizationMasterDataUseCase.AffiliateResult changedStatus =
                useCase.updateAffiliateStatus(
                        new AdminOrganizationMasterDataUseCase.UpdateAffiliateStatusCommand(
                                created.affiliateId(), "INACTIVE"));

        List<AdminOrganizationMasterDataUseCase.AffiliateResult> items = useCase.getAffiliates();

        assertEquals(1, items.size());
        assertEquals("Platform", created.name());
        assertEquals("Platform Lab", updated.name());
        assertEquals("INACTIVE", changedStatus.status());
    }

    @Test
    void departmentCrudSuccess() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));

        AdminOrganizationMasterDataUseCase.DepartmentResult created =
                useCase.createDepartment(
                        new AdminOrganizationMasterDataUseCase.CreateDepartmentCommand(
                                AFFILIATE_ID, "Engineering", "ENG", "ACTIVE", 1));

        AdminOrganizationMasterDataUseCase.DepartmentResult updated =
                useCase.updateDepartment(
                        new AdminOrganizationMasterDataUseCase.UpdateDepartmentCommand(
                                created.departmentId(),
                                AFFILIATE_ID,
                                "Core Engineering",
                                "CENG",
                                2));

        AdminOrganizationMasterDataUseCase.DepartmentResult changedStatus =
                useCase.updateDepartmentStatus(
                        new AdminOrganizationMasterDataUseCase.UpdateDepartmentStatusCommand(
                                created.departmentId(), "INACTIVE"));

        List<AdminOrganizationMasterDataUseCase.DepartmentResult> items = useCase.getDepartments();

        assertEquals(1, items.size());
        assertEquals("Engineering", created.name());
        assertEquals("Core Engineering", updated.name());
        assertEquals("INACTIVE", changedStatus.status());
    }

    @Test
    void teamCrudSuccess() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG"));

        AdminOrganizationMasterDataUseCase.TeamResult created =
                useCase.createTeam(
                        new AdminOrganizationMasterDataUseCase.CreateTeamCommand(
                                DEPARTMENT_ID, "Backend", "BE", "ACTIVE", 1));

        AdminOrganizationMasterDataUseCase.TeamResult updated =
                useCase.updateTeam(
                        new AdminOrganizationMasterDataUseCase.UpdateTeamCommand(
                                created.teamId(), DEPARTMENT_ID, "Platform Backend", "PBE", 2));

        AdminOrganizationMasterDataUseCase.TeamResult changedStatus =
                useCase.updateTeamStatus(
                        new AdminOrganizationMasterDataUseCase.UpdateTeamStatusCommand(
                                created.teamId(), "INACTIVE"));

        List<AdminOrganizationMasterDataUseCase.TeamResult> items = useCase.getTeams();

        assertEquals(1, items.size());
        assertEquals("Backend", created.name());
        assertEquals("Platform Backend", updated.name());
        assertEquals("INACTIVE", changedStatus.status());
    }

    @Test
    void positionCrudSuccess() {
        AdminOrganizationMasterDataUseCase.PositionResult created =
                useCase.createPosition(
                        new AdminOrganizationMasterDataUseCase.CreatePositionCommand(
                                "Manager", "MGR", "ACTIVE", 1));

        AdminOrganizationMasterDataUseCase.PositionResult updated =
                useCase.updatePosition(
                        new AdminOrganizationMasterDataUseCase.UpdatePositionCommand(
                                created.positionId(), "Senior Manager", "SMGR", 2));

        AdminOrganizationMasterDataUseCase.PositionResult changedStatus =
                useCase.updatePositionStatus(
                        new AdminOrganizationMasterDataUseCase.UpdatePositionStatusCommand(
                                created.positionId(), "INACTIVE"));

        List<AdminOrganizationMasterDataUseCase.PositionResult> items = useCase.getPositions();

        assertEquals(1, items.size());
        assertEquals("Manager", created.name());
        assertEquals("Senior Manager", updated.name());
        assertEquals("INACTIVE", changedStatus.status());
    }

    @Test
    void duplicateAffiliateCreateFails() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Platform", "PLT"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createAffiliate(
                                        new AdminOrganizationMasterDataUseCase
                                                .CreateAffiliateCommand(
                                                "platform", "PLT-2", "ACTIVE", 1)));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void duplicatePositionCreateFails() {
        positionRepository.save(position(POSITION_ID, "Manager", "MGR"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createPosition(
                                        new AdminOrganizationMasterDataUseCase
                                                .CreatePositionCommand(
                                                "manager", "MGR-2", "ACTIVE", 1)));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void createDepartmentFailsWhenAffiliateDoesNotExist() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createDepartment(
                                        new AdminOrganizationMasterDataUseCase
                                                .CreateDepartmentCommand(
                                                AFFILIATE_ID, "Engineering", "ENG", "ACTIVE", 1)));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }

    @Test
    void createTeamFailsWhenDepartmentDoesNotExist() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createTeam(
                                        new AdminOrganizationMasterDataUseCase.CreateTeamCommand(
                                                DEPARTMENT_ID, "Backend", "BE", "ACTIVE", 1)));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }

    private Affiliate affiliate(UUID id, String name, String code) {
        return new Affiliate(id, name, code, ReferenceStatus.ACTIVE, 1, NOW, NOW);
    }

    private Department department(UUID id, UUID affiliateId, String name, String code) {
        return new Department(
                id, affiliateId, null, name, code, ReferenceStatus.ACTIVE, 1, NOW, NOW);
    }

    private Team team(UUID id, UUID departmentId, String name, String code) {
        return new Team(id, departmentId, name, code, ReferenceStatus.ACTIVE, 1, NOW, NOW);
    }

    private Position position(UUID id, String name, String code) {
        return new Position(id, name, code, ReferenceStatus.ACTIVE, 1, NOW, NOW);
    }

    private static final class FakeAffiliateRepository implements AffiliateRepositoryPort {
        private final Map<UUID, Affiliate> affiliates = new ConcurrentHashMap<>();

        @Override
        public Affiliate save(Affiliate organization) {
            affiliates.put(organization.id(), organization);
            return organization;
        }

        @Override
        public Optional<Affiliate> findById(UUID organizationId) {
            return Optional.ofNullable(affiliates.get(organizationId));
        }

        @Override
        public List<Affiliate> findAll() {
            return List.copyOf(affiliates.values());
        }

        @Override
        public List<Affiliate> findAllByIds(Collection<UUID> organizationIds) {
            return organizationIds.stream()
                    .map(affiliates::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public boolean existsByName(String name) {
            return affiliates.values().stream()
                    .anyMatch(item -> item.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByCode(String code) {
            return affiliates.values().stream()
                    .anyMatch(item -> item.code().equalsIgnoreCase(code));
        }

        @Override
        public boolean existsByNameAndIdNot(String name, UUID affiliateId) {
            return affiliates.values().stream()
                    .anyMatch(
                            item ->
                                    !item.id().equals(affiliateId)
                                            && item.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByCodeAndIdNot(String code, UUID affiliateId) {
            return affiliates.values().stream()
                    .anyMatch(
                            item ->
                                    !item.id().equals(affiliateId)
                                            && item.code().equalsIgnoreCase(code));
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
        public Optional<Department> findById(UUID departmentId) {
            return Optional.ofNullable(departments.get(departmentId));
        }

        @Override
        public List<Department> findAll() {
            return List.copyOf(departments.values());
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
            return departments.values().stream()
                    .anyMatch(
                            item ->
                                    item.affiliateId().equals(affiliateId)
                                            && item.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByAffiliateIdAndNameAndIdNot(
                UUID affiliateId, String name, UUID departmentId) {
            return departments.values().stream()
                    .anyMatch(
                            item ->
                                    !item.id().equals(departmentId)
                                            && item.affiliateId().equals(affiliateId)
                                            && item.name().equalsIgnoreCase(name));
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
        public Optional<Team> findById(UUID teamId) {
            return Optional.ofNullable(teams.get(teamId));
        }

        @Override
        public List<Team> findAll() {
            return List.copyOf(teams.values());
        }

        @Override
        public List<Team> findAllByIds(Collection<UUID> teamIds) {
            return teamIds.stream().map(teams::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public boolean existsByDepartmentIdAndName(UUID departmentId, String name) {
            return teams.values().stream()
                    .anyMatch(
                            item ->
                                    item.departmentId().equals(departmentId)
                                            && item.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByDepartmentIdAndNameAndIdNot(
                UUID departmentId, String name, UUID teamId) {
            return teams.values().stream()
                    .anyMatch(
                            item ->
                                    !item.id().equals(teamId)
                                            && item.departmentId().equals(departmentId)
                                            && item.name().equalsIgnoreCase(name));
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
        public Optional<Position> findById(UUID positionId) {
            return Optional.ofNullable(positions.get(positionId));
        }

        @Override
        public List<Position> findAll() {
            return List.copyOf(positions.values());
        }

        @Override
        public List<Position> findAllByIds(Collection<UUID> positionIds) {
            return positionIds.stream()
                    .map(positions::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public boolean existsByName(String name) {
            return positions.values().stream().anyMatch(item -> item.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByCode(String code) {
            return positions.values().stream().anyMatch(item -> item.code().equalsIgnoreCase(code));
        }

        @Override
        public boolean existsByNameAndIdNot(String name, UUID positionId) {
            return positions.values().stream()
                    .anyMatch(
                            item ->
                                    !item.id().equals(positionId)
                                            && item.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByCodeAndIdNot(String code, UUID positionId) {
            return positions.values().stream()
                    .anyMatch(
                            item ->
                                    !item.id().equals(positionId)
                                            && item.code().equalsIgnoreCase(code));
        }
    }
}
