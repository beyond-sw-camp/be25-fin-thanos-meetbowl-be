package com.meetbowl.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.user.UserSearchReindexRequestDispatcher;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;
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
import com.meetbowl.domain.user.UserSearchReindexEventPublisherPort;
import com.meetbowl.domain.user.UserSearchReindexRequestedEvent;
import com.meetbowl.domain.user.UserStatus;

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
    private UserRepositoryPort userRepositoryPort;
    private FakeAdminAuditLogRepository auditLogRepository;
    private FakeUserSearchReindexEventPublisherPort userSearchReindexEventPublisherPort;
    private UserSearchReindexRequestDispatcher userSearchReindexRequestDispatcher;
    private AdminOrganizationMasterDataUseCase useCase;

    @BeforeEach
    void setUp() {
        affiliateRepository = new FakeAffiliateRepository();
        departmentRepository = new FakeDepartmentRepository();
        teamRepository = new FakeTeamRepository(departmentRepository);
        positionRepository = new FakePositionRepository();
        userRepositoryPort = mock(UserRepositoryPort.class);
        auditLogRepository = new FakeAdminAuditLogRepository();
        userSearchReindexEventPublisherPort = new FakeUserSearchReindexEventPublisherPort();
        userSearchReindexRequestDispatcher =
                new UserSearchReindexRequestDispatcher(userSearchReindexEventPublisherPort);
        useCase =
                new AdminOrganizationMasterDataUseCase(
                        affiliateRepository,
                        departmentRepository,
                        teamRepository,
                        positionRepository,
                        userRepositoryPort,
                        auditLogRepository,
                        new ObjectMapper().findAndRegisterModules(),
                        userSearchReindexRequestDispatcher);
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
                                created.affiliateId(),
                                "Platform Lab",
                                "PLT-LAB",
                                2,
                                UUID.randomUUID()));

        AdminOrganizationMasterDataUseCase.AffiliateResult changedStatus =
                useCase.updateAffiliateStatus(
                        new AdminOrganizationMasterDataUseCase.UpdateAffiliateStatusCommand(
                                created.affiliateId(), "INACTIVE"));

        List<AdminOrganizationMasterDataUseCase.AffiliateResult> items = useCase.getAffiliates();

        assertEquals(1, items.size());
        assertEquals("Platform", created.name());
        assertEquals("Platform Lab", updated.name());
        assertEquals("INACTIVE", changedStatus.status());
        assertEquals(
                created.affiliateId(), userSearchReindexEventPublisherPort.lastEvent.affiliateId());
    }

    @Test
    void departmentCrudSuccess() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));

        AdminOrganizationMasterDataUseCase.DepartmentResult created =
                useCase.createDepartment(
                        new AdminOrganizationMasterDataUseCase.CreateDepartmentCommand(
                                AFFILIATE_ID, "Engineering", "ACTIVE", 1));

        AdminOrganizationMasterDataUseCase.DepartmentResult updated =
                useCase.updateDepartment(
                        new AdminOrganizationMasterDataUseCase.UpdateDepartmentCommand(
                                created.departmentId(),
                                AFFILIATE_ID,
                                "Core Engineering",
                                2,
                                UUID.randomUUID()));

        AdminOrganizationMasterDataUseCase.DepartmentResult changedStatus =
                useCase.updateDepartmentStatus(
                        new AdminOrganizationMasterDataUseCase.UpdateDepartmentStatusCommand(
                                created.departmentId(), "INACTIVE"));

        List<AdminOrganizationMasterDataUseCase.DepartmentResult> items = useCase.getDepartments();

        assertEquals(1, items.size());
        assertEquals("Engineering", created.name());
        assertEquals("D001", created.code());
        assertEquals("Core Engineering", updated.name());
        assertEquals("D001", updated.code());
        assertEquals("INACTIVE", changedStatus.status());
        assertEquals(
                created.departmentId(),
                userSearchReindexEventPublisherPort.lastEvent.departmentId());
    }

    @Test
    void teamCrudSuccess() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG"));

        AdminOrganizationMasterDataUseCase.TeamResult created =
                useCase.createTeam(
                        new AdminOrganizationMasterDataUseCase.CreateTeamCommand(
                                DEPARTMENT_ID, "Backend", "ACTIVE", 1));

        AdminOrganizationMasterDataUseCase.TeamResult updated =
                useCase.updateTeam(
                        new AdminOrganizationMasterDataUseCase.UpdateTeamCommand(
                                created.teamId(),
                                DEPARTMENT_ID,
                                "Platform Backend",
                                2,
                                UUID.randomUUID()));

        AdminOrganizationMasterDataUseCase.TeamResult changedStatus =
                useCase.updateTeamStatus(
                        new AdminOrganizationMasterDataUseCase.UpdateTeamStatusCommand(
                                created.teamId(), "INACTIVE"));

        List<AdminOrganizationMasterDataUseCase.TeamResult> items = useCase.getTeams();

        assertEquals(1, items.size());
        assertEquals("Backend", created.name());
        assertEquals("T001", created.code());
        assertEquals("Platform Backend", updated.name());
        assertEquals("T001", updated.code());
        assertEquals("INACTIVE", changedStatus.status());
        assertEquals(created.teamId(), userSearchReindexEventPublisherPort.lastEvent.teamId());
    }

    @Test
    void positionCrudSuccess() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        AdminOrganizationMasterDataUseCase.PositionResult created =
                useCase.createPosition(
                        new AdminOrganizationMasterDataUseCase.CreatePositionCommand(
                                AFFILIATE_ID, "Manager", "ACTIVE", 1));

        AdminOrganizationMasterDataUseCase.PositionResult updated =
                useCase.updatePosition(
                        new AdminOrganizationMasterDataUseCase.UpdatePositionCommand(
                                created.positionId(),
                                AFFILIATE_ID,
                                "Senior Manager",
                                2,
                                UUID.randomUUID()));

        AdminOrganizationMasterDataUseCase.PositionResult changedStatus =
                useCase.updatePositionStatus(
                        new AdminOrganizationMasterDataUseCase.UpdatePositionStatusCommand(
                                created.positionId(), "INACTIVE"));

        List<AdminOrganizationMasterDataUseCase.PositionResult> items = useCase.getPositions();

        assertEquals(1, items.size());
        assertEquals("Manager", created.name());
        assertEquals(AFFILIATE_ID, created.affiliateId());
        assertEquals("P001", created.code());
        assertEquals("Senior Manager", updated.name());
        assertEquals("P001", updated.code());
        assertEquals("INACTIVE", changedStatus.status());
        assertEquals(
                created.positionId(), userSearchReindexEventPublisherPort.lastEvent.positionId());
    }

    @Test
    void organizationCodesUseNextSequenceWithoutReusingGaps() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Platform", "D002"));
        departmentRepository.save(department(UUID.randomUUID(), AFFILIATE_ID, "Ops", "D001"));
        departmentRepository.save(department(UUID.randomUUID(), AFFILIATE_ID, "Sales", "D003"));
        departmentRepository.save(
                new Department(
                        UUID.randomUUID(),
                        AFFILIATE_ID,
                        null,
                        "Legacy",
                        "DX99",
                        ReferenceStatus.INACTIVE,
                        9,
                        NOW,
                        NOW));
        teamRepository.save(team(UUID.randomUUID(), DEPARTMENT_ID, "Core", "T002"));
        positionRepository.save(position(UUID.randomUUID(), "Lead", "P009"));

        AdminOrganizationMasterDataUseCase.DepartmentResult createdDepartment =
                useCase.createDepartment(
                        new AdminOrganizationMasterDataUseCase.CreateDepartmentCommand(
                                AFFILIATE_ID, "Engineering", "ACTIVE", 4));
        AdminOrganizationMasterDataUseCase.TeamResult createdTeam =
                useCase.createTeam(
                        new AdminOrganizationMasterDataUseCase.CreateTeamCommand(
                                DEPARTMENT_ID, "Backend", "ACTIVE", 2));
        AdminOrganizationMasterDataUseCase.PositionResult createdPosition =
                useCase.createPosition(
                        new AdminOrganizationMasterDataUseCase.CreatePositionCommand(
                                AFFILIATE_ID, "Manager", "ACTIVE", 10));

        assertEquals("D004", createdDepartment.code());
        assertEquals("T003", createdTeam.code());
        assertEquals("P010", createdPosition.code());
    }

    @Test
    void affiliateUpdateDoesNotPublishWhenOnlyCodeChanges() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Platform", "PLT"));

        useCase.updateAffiliate(
                new AdminOrganizationMasterDataUseCase.UpdateAffiliateCommand(
                        AFFILIATE_ID, "Platform", "PLT-NEW", 2, UUID.randomUUID()));

        assertEquals(0, userSearchReindexEventPublisherPort.publishCount);
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
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        positionRepository.save(position(POSITION_ID, "Manager", "MGR"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createPosition(
                                        new AdminOrganizationMasterDataUseCase
                                                .CreatePositionCommand(
                                                AFFILIATE_ID, "manager", "ACTIVE", 1)));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void createDepartmentFailsWhenSortOrderDuplicatedInSameAffiliate() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createDepartment(
                                        new AdminOrganizationMasterDataUseCase
                                                .CreateDepartmentCommand(
                                                AFFILIATE_ID, "Platform", "ACTIVE", 1)));

        assertEquals(ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED, exception.errorCode());
        assertEquals("이미 사용 중인 순서입니다. 다른 순서를 입력해 주세요.", exception.getMessage());
    }

    @Test
    void updateDepartmentFailsWhenSortOrderDuplicatedInSameAffiliate() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG"));
        UUID otherDepartmentId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        departmentRepository.save(
                new Department(
                        otherDepartmentId,
                        AFFILIATE_ID,
                        null,
                        "Platform",
                        "PLT",
                        ReferenceStatus.ACTIVE,
                        2,
                        NOW,
                        NOW));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.updateDepartment(
                                        new AdminOrganizationMasterDataUseCase
                                                .UpdateDepartmentCommand(
                                                otherDepartmentId,
                                                AFFILIATE_ID,
                                                "Platform",
                                                1,
                                                UUID.randomUUID())));

        assertEquals(ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED, exception.errorCode());
    }

    @Test
    void updateDepartmentSucceedsWhenKeepingOwnSortOrder() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG"));

        AdminOrganizationMasterDataUseCase.DepartmentResult result =
                useCase.updateDepartment(
                        new AdminOrganizationMasterDataUseCase.UpdateDepartmentCommand(
                                DEPARTMENT_ID,
                                AFFILIATE_ID,
                                "Engineering Platform",
                                1,
                                UUID.randomUUID()));

        assertEquals(1, result.sortOrder());
    }

    @Test
    void createDepartmentFailsWhenInactiveDepartmentUsesSameSortOrder() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(
                new Department(
                        DEPARTMENT_ID,
                        AFFILIATE_ID,
                        null,
                        "Legacy",
                        "LEG",
                        ReferenceStatus.INACTIVE,
                        3,
                        NOW,
                        NOW));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createDepartment(
                                        new AdminOrganizationMasterDataUseCase
                                                .CreateDepartmentCommand(
                                                AFFILIATE_ID, "Platform", "ACTIVE", 3)));

        assertEquals(ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED, exception.errorCode());
    }

    @Test
    void createDepartmentAllowsReusingSortOrderAfterDeletion() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG"));
        given(userRepositoryPort.findAllByDepartmentId(DEPARTMENT_ID)).willReturn(List.of());

        useCase.deleteDepartment(
                new AdminOrganizationMasterDataUseCase.DeleteDepartmentCommand(
                        DEPARTMENT_ID, UUID.randomUUID(), "Admin", "127.0.0.1", "JUnit"));

        AdminOrganizationMasterDataUseCase.DepartmentResult created =
                useCase.createDepartment(
                        new AdminOrganizationMasterDataUseCase.CreateDepartmentCommand(
                                AFFILIATE_ID, "Platform", "ACTIVE", 1));

        assertEquals(1, created.sortOrder());
    }

    @Test
    void createTeamFailsWhenSortOrderDuplicatedInSameAffiliateEvenIfDepartmentDiffers() {
        UUID otherDepartmentId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG"));
        departmentRepository.save(department(otherDepartmentId, AFFILIATE_ID, "Platform", "PLT"));
        teamRepository.save(team(TEAM_ID, DEPARTMENT_ID, "Backend", "BE"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createTeam(
                                        new AdminOrganizationMasterDataUseCase.CreateTeamCommand(
                                                otherDepartmentId, "Frontend", "ACTIVE", 1)));

        assertEquals(ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED, exception.errorCode());
    }

    @Test
    void updateTeamFailsWhenSortOrderDuplicatedInSameAffiliate() {
        UUID otherDepartmentId = UUID.fromString("00000000-0000-0000-0000-000000000103");
        UUID otherTeamId = UUID.fromString("00000000-0000-0000-0000-000000000104");
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG"));
        departmentRepository.save(department(otherDepartmentId, AFFILIATE_ID, "Platform", "PLT"));
        teamRepository.save(team(TEAM_ID, DEPARTMENT_ID, "Backend", "BE"));
        teamRepository.save(
                new Team(
                        otherTeamId,
                        otherDepartmentId,
                        "Frontend",
                        "FE",
                        ReferenceStatus.ACTIVE,
                        2,
                        NOW,
                        NOW));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.updateTeam(
                                        new AdminOrganizationMasterDataUseCase.UpdateTeamCommand(
                                                otherTeamId,
                                                otherDepartmentId,
                                                "Frontend",
                                                1,
                                                UUID.randomUUID())));

        assertEquals(ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED, exception.errorCode());
    }

    @Test
    void updateTeamSucceedsWhenKeepingOwnSortOrder() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG"));
        teamRepository.save(team(TEAM_ID, DEPARTMENT_ID, "Backend", "BE"));

        AdminOrganizationMasterDataUseCase.TeamResult result =
                useCase.updateTeam(
                        new AdminOrganizationMasterDataUseCase.UpdateTeamCommand(
                                TEAM_ID, DEPARTMENT_ID, "Platform Backend", 1, UUID.randomUUID()));

        assertEquals(1, result.sortOrder());
    }

    @Test
    void createTeamFailsWhenInactiveTeamUsesSameSortOrder() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG"));
        teamRepository.save(
                new Team(
                        TEAM_ID,
                        DEPARTMENT_ID,
                        "Legacy Team",
                        "LEG",
                        ReferenceStatus.INACTIVE,
                        4,
                        NOW,
                        NOW));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createTeam(
                                        new AdminOrganizationMasterDataUseCase.CreateTeamCommand(
                                                DEPARTMENT_ID, "Backend", "ACTIVE", 4)));

        assertEquals(ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED, exception.errorCode());
    }

    @Test
    void createPositionFailsWhenSortOrderDuplicated() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        positionRepository.save(position(POSITION_ID, "Manager", "MGR"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createPosition(
                                        new AdminOrganizationMasterDataUseCase
                                                .CreatePositionCommand(
                                                AFFILIATE_ID, "Director", "ACTIVE", 1)));

        assertEquals(ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED, exception.errorCode());
    }

    @Test
    void updatePositionFailsWhenSortOrderDuplicated() {
        UUID otherPositionId = UUID.fromString("00000000-0000-0000-0000-000000000105");
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        positionRepository.save(position(POSITION_ID, "Manager", "MGR"));
        positionRepository.save(
                new Position(
                        otherPositionId,
                        AFFILIATE_ID,
                        "Director",
                        "DIR",
                        ReferenceStatus.ACTIVE,
                        2,
                        NOW,
                        NOW));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.updatePosition(
                                        new AdminOrganizationMasterDataUseCase
                                                .UpdatePositionCommand(
                                                otherPositionId,
                                                AFFILIATE_ID,
                                                "Director",
                                                1,
                                                UUID.randomUUID())));

        assertEquals(ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED, exception.errorCode());
    }

    @Test
    void updatePositionSucceedsWhenKeepingOwnSortOrder() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        positionRepository.save(position(POSITION_ID, "Manager", "MGR"));

        AdminOrganizationMasterDataUseCase.PositionResult result =
                useCase.updatePosition(
                        new AdminOrganizationMasterDataUseCase.UpdatePositionCommand(
                                POSITION_ID, AFFILIATE_ID, "Senior Manager", 1, UUID.randomUUID()));

        assertEquals(1, result.sortOrder());
    }

    @Test
    void createPositionFailsWhenInactivePositionUsesSameSortOrder() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "Affiliate", "AFF"));
        positionRepository.save(
                new Position(
                        POSITION_ID,
                        AFFILIATE_ID,
                        "Legacy Manager",
                        "LMG",
                        ReferenceStatus.INACTIVE,
                        5,
                        NOW,
                        NOW));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.createPosition(
                                        new AdminOrganizationMasterDataUseCase
                                                .CreatePositionCommand(
                                                AFFILIATE_ID, "Manager", "ACTIVE", 5)));

        assertEquals(ErrorCode.ORGANIZATION_SORT_ORDER_DUPLICATED, exception.errorCode());
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
                                                AFFILIATE_ID, "Engineering", "ACTIVE", 1)));

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
                                                DEPARTMENT_ID, "Backend", "ACTIVE", 1)));

        assertEquals(ErrorCode.COMMON_NOT_FOUND, exception.errorCode());
    }

    @Test
    void deleteDepartmentSucceedsWhenNoChildTeamOrMemberExists() {
        Department department = department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG");
        departmentRepository.save(department);
        given(userRepositoryPort.findAllByDepartmentId(DEPARTMENT_ID)).willReturn(List.of());

        useCase.deleteDepartment(
                new AdminOrganizationMasterDataUseCase.DeleteDepartmentCommand(
                        DEPARTMENT_ID, UUID.randomUUID(), "Admin", "127.0.0.1", "JUnit"));

        assertEquals(true, departmentRepository.findById(DEPARTMENT_ID).isEmpty());
        assertEquals(AuditResult.SUCCESS, auditLogRepository.lastSaved.result());
    }

    @Test
    void deleteDepartmentFailsWhenChildTeamExists() {
        Department department = department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG");
        departmentRepository.save(department);
        teamRepository.save(team(TEAM_ID, DEPARTMENT_ID, "Backend", "BE"));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.deleteDepartment(
                                        new AdminOrganizationMasterDataUseCase
                                                .DeleteDepartmentCommand(
                                                DEPARTMENT_ID,
                                                UUID.randomUUID(),
                                                "Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
        assertEquals(AuditResult.FAILURE, auditLogRepository.lastSaved.result());
    }

    @Test
    void deleteDepartmentFailsWhenMemberExists() {
        Department department = department(DEPARTMENT_ID, AFFILIATE_ID, "Engineering", "ENG");
        departmentRepository.save(department);
        given(userRepositoryPort.findAllByDepartmentId(DEPARTMENT_ID))
                .willReturn(List.of(user("dept-user", DEPARTMENT_ID, null, null)));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.deleteDepartment(
                                        new AdminOrganizationMasterDataUseCase
                                                .DeleteDepartmentCommand(
                                                DEPARTMENT_ID,
                                                UUID.randomUUID(),
                                                "Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void deleteTeamSucceedsWhenNoMemberExists() {
        Team team = team(TEAM_ID, DEPARTMENT_ID, "Backend", "BE");
        teamRepository.save(team);
        given(userRepositoryPort.findAllByTeamId(TEAM_ID)).willReturn(List.of());

        useCase.deleteTeam(
                new AdminOrganizationMasterDataUseCase.DeleteTeamCommand(
                        TEAM_ID, UUID.randomUUID(), "Admin", "127.0.0.1", "JUnit"));

        assertEquals(true, teamRepository.findById(TEAM_ID).isEmpty());
        assertEquals(AuditResult.SUCCESS, auditLogRepository.lastSaved.result());
    }

    @Test
    void deleteTeamFailsWhenMemberExists() {
        Team team = team(TEAM_ID, DEPARTMENT_ID, "Backend", "BE");
        teamRepository.save(team);
        given(userRepositoryPort.findAllByTeamId(TEAM_ID))
                .willReturn(List.of(user("team-user", DEPARTMENT_ID, TEAM_ID, null)));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.deleteTeam(
                                        new AdminOrganizationMasterDataUseCase.DeleteTeamCommand(
                                                TEAM_ID,
                                                UUID.randomUUID(),
                                                "Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
        assertEquals(AuditResult.FAILURE, auditLogRepository.lastSaved.result());
    }

    @Test
    void deletePositionSucceedsWhenUnused() {
        Position position = position(POSITION_ID, "Manager", "MGR");
        positionRepository.save(position);
        given(userRepositoryPort.findAllByPositionId(POSITION_ID)).willReturn(List.of());

        useCase.deletePosition(
                new AdminOrganizationMasterDataUseCase.DeletePositionCommand(
                        POSITION_ID, UUID.randomUUID(), "Admin", "127.0.0.1", "JUnit"));

        assertEquals(true, positionRepository.findById(POSITION_ID).isEmpty());
        assertEquals(AuditResult.SUCCESS, auditLogRepository.lastSaved.result());
    }

    @Test
    void deletePositionFailsWhenMemberUsesIt() {
        Position position = position(POSITION_ID, "Manager", "MGR");
        positionRepository.save(position);
        given(userRepositoryPort.findAllByPositionId(POSITION_ID))
                .willReturn(List.of(user("position-user", DEPARTMENT_ID, TEAM_ID, POSITION_ID)));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.deletePosition(
                                        new AdminOrganizationMasterDataUseCase
                                                .DeletePositionCommand(
                                                POSITION_ID,
                                                UUID.randomUUID(),
                                                "Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
        assertEquals(AuditResult.FAILURE, auditLogRepository.lastSaved.result());
    }

    @Test
    void deleteDepartmentFailsWhenTargetDoesNotExist() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.deleteDepartment(
                                        new AdminOrganizationMasterDataUseCase
                                                .DeleteDepartmentCommand(
                                                DEPARTMENT_ID,
                                                UUID.randomUUID(),
                                                "Admin",
                                                "127.0.0.1",
                                                "JUnit")));

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
        return new Position(id, AFFILIATE_ID, name, code, ReferenceStatus.ACTIVE, 1, NOW, NOW);
    }

    private User user(String loginId, UUID departmentId, UUID teamId, UUID positionId) {
        return User.of(
                UUID.randomUUID(),
                loginId,
                "hash",
                "User",
                loginId + "@example.com",
                UserRole.USER,
                UserStatus.ACTIVE,
                AFFILIATE_ID,
                departmentId,
                positionId,
                teamId,
                false,
                NOW,
                NOW.plusSeconds(3600),
                NOW,
                NOW);
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
        public List<Affiliate> findAllForExcelExport() {
            return findAll();
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

        @Override
        public boolean existsByAffiliateIdAndSortOrder(UUID affiliateId, Integer sortOrder) {
            return departments.values().stream()
                    .anyMatch(
                            item ->
                                    item.affiliateId().equals(affiliateId)
                                            && java.util.Objects.equals(
                                                    item.sortOrder(), sortOrder));
        }

        @Override
        public boolean existsByAffiliateIdAndSortOrderAndIdNot(
                UUID affiliateId, Integer sortOrder, UUID departmentId) {
            return departments.values().stream()
                    .anyMatch(
                            item ->
                                    !item.id().equals(departmentId)
                                            && item.affiliateId().equals(affiliateId)
                                            && java.util.Objects.equals(
                                                    item.sortOrder(), sortOrder));
        }
    }

    private static final class FakeTeamRepository implements TeamRepositoryPort {
        private final Map<UUID, Team> teams = new ConcurrentHashMap<>();
        private final DepartmentRepositoryPort departmentRepository;

        private FakeTeamRepository(DepartmentRepositoryPort departmentRepository) {
            this.departmentRepository = departmentRepository;
        }

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
                    .filter(item -> item.departmentId().equals(departmentId))
                    .toList();
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

        @Override
        public boolean existsByAffiliateIdAndSortOrder(UUID affiliateId, Integer sortOrder) {
            return teams.values().stream()
                    .anyMatch(
                            item ->
                                    departmentRepository
                                                    .findById(item.departmentId())
                                                    .map(Department::affiliateId)
                                                    .orElseThrow()
                                                    .equals(affiliateId)
                                            && java.util.Objects.equals(
                                                    item.sortOrder(), sortOrder));
        }

        @Override
        public boolean existsByAffiliateIdAndSortOrderAndIdNot(
                UUID affiliateId, Integer sortOrder, UUID teamId) {
            return teams.values().stream()
                    .anyMatch(
                            item ->
                                    !item.id().equals(teamId)
                                            && departmentRepository
                                                    .findById(item.departmentId())
                                                    .map(Department::affiliateId)
                                                    .orElseThrow()
                                                    .equals(affiliateId)
                                            && java.util.Objects.equals(
                                                    item.sortOrder(), sortOrder));
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
                    .filter(item -> java.util.Objects.equals(item.affiliateId(), affiliateId))
                    .toList();
        }

        @Override
        public boolean existsByAffiliateIdAndName(UUID affiliateId, String name) {
            return positions.values().stream().anyMatch(item -> item.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsByCode(String code) {
            return positions.values().stream().anyMatch(item -> item.code().equalsIgnoreCase(code));
        }

        @Override
        public boolean existsByAffiliateIdAndNameAndIdNot(
                UUID affiliateId, String name, UUID positionId) {
            return positions.values().stream()
                    .anyMatch(
                            item ->
                                    !item.id().equals(positionId)
                                            && java.util.Objects.equals(
                                                    item.affiliateId(), affiliateId)
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

        @Override
        public boolean existsByAffiliateIdAndSortOrder(UUID affiliateId, Integer sortOrder) {
            return positions.values().stream()
                    .anyMatch(
                            item ->
                                    java.util.Objects.equals(item.affiliateId(), affiliateId)
                                            && java.util.Objects.equals(item.sortOrder(), sortOrder));
        }

        @Override
        public boolean existsByAffiliateIdAndSortOrderAndIdNot(
                UUID affiliateId, Integer sortOrder, UUID positionId) {
            return positions.values().stream()
                    .anyMatch(
                            item ->
                                    !item.id().equals(positionId)
                                            && java.util.Objects.equals(
                                                    item.affiliateId(), affiliateId)
                                            && java.util.Objects.equals(
                                                    item.sortOrder(), sortOrder));
        }
    }

    private static final class FakeAdminAuditLogRepository implements AdminAuditLogRepositoryPort {
        private AdminAuditLog lastSaved;

        @Override
        public AdminAuditLog save(AdminAuditLog adminAuditLog) {
            lastSaved = adminAuditLog;
            return adminAuditLog;
        }

        @Override
        public Optional<AdminAuditLog> findById(UUID auditLogId) {
            return Optional.ofNullable(lastSaved).filter(log -> log.id().equals(auditLogId));
        }

        @Override
        public com.meetbowl.domain.common.Paged<AdminAuditLog> findPage(
                com.meetbowl.domain.admin.AdminAuditLogSearchCondition condition) {
            return new com.meetbowl.domain.common.Paged<>(
                    lastSaved == null ? List.of() : List.of(lastSaved), lastSaved == null ? 0 : 1);
        }
    }

    private static final class FakeUserSearchReindexEventPublisherPort
            implements UserSearchReindexEventPublisherPort {

        private UserSearchReindexRequestedEvent lastEvent;
        private int publishCount;

        @Override
        public void publish(UserSearchReindexRequestedEvent event) {
            lastEvent = event;
            publishCount++;
        }
    }
}
