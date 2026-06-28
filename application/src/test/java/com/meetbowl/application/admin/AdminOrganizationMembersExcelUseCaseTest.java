package com.meetbowl.application.admin;

import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.AffiliateRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.DepartmentRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.PositionRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.TeamRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.UserRow;
import static com.meetbowl.application.admin.excel.OrganizationMembersExcelRows.WorkbookRows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.admin.excel.OrganizationMembersExcelWorkbookMapper;
import com.meetbowl.application.user.UserSearchReindexRequestDispatcher;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.admin.AuditResult;
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
import com.meetbowl.domain.user.UserSearchReindexEventPublisherPort;
import com.meetbowl.domain.user.UserSearchReindexRequestedEvent;
import com.meetbowl.domain.user.UserStatus;

class AdminOrganizationMembersExcelUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-06-18T00:00:00Z");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID AFFILIATE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID DEPARTMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID POSITION_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");

    private FakeAffiliateRepository affiliateRepository;
    private FakeDepartmentRepository departmentRepository;
    private FakeTeamRepository teamRepository;
    private FakePositionRepository positionRepository;
    private FakeUserRepository userRepository;
    private FakeAdminAuditLogRepository auditLogRepository;
    private FakeUserSearchReindexEventPublisher eventPublisher;
    private UserSearchReindexRequestDispatcher dispatcher;
    private PasswordEncoder passwordEncoder;
    private ObjectMapper objectMapper;
    private OrganizationMembersExcelWorkbookMapper workbookMapper;
    private AdminOrganizationMembersExcelUseCase useCase;

    @BeforeEach
    void setUp() {
        affiliateRepository = new FakeAffiliateRepository();
        departmentRepository = new FakeDepartmentRepository();
        teamRepository = new FakeTeamRepository();
        positionRepository = new FakePositionRepository();
        userRepository = new FakeUserRepository();
        auditLogRepository = new FakeAdminAuditLogRepository();
        eventPublisher = new FakeUserSearchReindexEventPublisher();
        dispatcher = new UserSearchReindexRequestDispatcher(eventPublisher);
        passwordEncoder = new BCryptPasswordEncoder();
        objectMapper = new ObjectMapper().findAndRegisterModules();
        workbookMapper = new OrganizationMembersExcelWorkbookMapper();

        AdminOrganizationMembersExcelApplyService applyService =
                new AdminOrganizationMembersExcelApplyService(
                        affiliateRepository,
                        departmentRepository,
                        teamRepository,
                        positionRepository,
                        userRepository,
                        passwordEncoder,
                        objectMapper,
                        auditLogRepository,
                        dispatcher,
                        workbookMapper);
        AdminOrganizationMembersExcelAuditService auditService =
                new AdminOrganizationMembersExcelAuditService(auditLogRepository, objectMapper);
        useCase =
                new AdminOrganizationMembersExcelUseCase(
                        affiliateRepository,
                        departmentRepository,
                        teamRepository,
                        positionRepository,
                        userRepository,
                        workbookMapper,
                        applyService,
                        auditService);
    }

    @Test
    void importSuccessCreatesAndUpdatesOrganizationMembersAndPublishesReindexEvent() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "한화시스템", "HSC", 10));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "서비스개발부", "DEV", 10));
        teamRepository.save(team(TEAM_ID, DEPARTMENT_ID, "플랫폼개발팀", "PLATFORM", 10));
        positionRepository.save(position(POSITION_ID, "대리", "STAFF", 10));
        userRepository.save(
                user(USER_ID, "legacy", "legacy@meetbowl.local", UserRole.USER, UserStatus.ACTIVE));

        AdminOrganizationMembersExcelUseCase.ImportResult result =
                useCase.importExcel(
                        new AdminOrganizationMembersExcelUseCase.ImportCommand(
                                workbookBytes(
                                        new WorkbookRows(
                                                List.of(
                                                        new AffiliateRow(
                                                                0,
                                                                AFFILIATE_ID.toString(),
                                                                "한화시스템",
                                                                "HSC-NEW",
                                                                "ACTIVE"),
                                                        new AffiliateRow(
                                                                0, "", "한화생명", "HLI", "ACTIVE")),
                                                List.of(
                                                        new DepartmentRow(
                                                                0,
                                                                DEPARTMENT_ID.toString(),
                                                                "한화시스템",
                                                                "서비스개발본부",
                                                                "DEV-HQ",
                                                                "2",
                                                                "ACTIVE"),
                                                        new DepartmentRow(
                                                                0, "", "한화생명", "경영지원부", "MGMT", "1",
                                                                "ACTIVE")),
                                                List.of(
                                                        new TeamRow(
                                                                0,
                                                                TEAM_ID.toString(),
                                                                "한화시스템",
                                                                "서비스개발본부",
                                                                "플랫폼아키텍처팀",
                                                                "PLATFORM-ARCH",
                                                                "3",
                                                                "ACTIVE"),
                                                        new TeamRow(
                                                                0, "", "한화생명", "경영지원부", "운영관리팀",
                                                                "OPS", "1", "ACTIVE")),
                                                List.of(
                                                        new PositionRow(
                                                                0,
                                                                POSITION_ID.toString(),
                                                                "한화시스템",
                                                                "과장",
                                                                "MANAGER",
                                                                "4",
                                                                "ACTIVE"),
                                                        new PositionRow(
                                                                0,
                                                                "",
                                                                "한화생명",
                                                                "부장",
                                                                "GENERAL_MANAGER",
                                                                "5",
                                                                "ACTIVE")),
                                                List.of(
                                                        new UserRow(
                                                                0,
                                                                USER_ID.toString(),
                                                                "legacy",
                                                                "레거시 사용자",
                                                                "legacy-updated@meetbowl.local",
                                                                "한화시스템",
                                                                "서비스개발본부",
                                                                "플랫폼아키텍처팀",
                                                                "과장",
                                                                "ADMIN",
                                                                "INACTIVE"),
                                                        new UserRow(
                                                                0,
                                                                "",
                                                                "new-user",
                                                                "신규 사용자",
                                                                "new-user@meetbowl.local",
                                                                "한화생명",
                                                                "경영지원부",
                                                                "운영관리팀",
                                                                "부장",
                                                                "USER",
                                                                "ACTIVE")))),
                                "organization-members.xlsx",
                                ADMIN_ID,
                                "Admin",
                                "127.0.0.1",
                                "JUnit"));

        assertEquals(1, result.createdAffiliates());
        assertEquals(1, result.updatedAffiliates());
        assertEquals(1, result.createdDepartments());
        assertEquals(1, result.updatedDepartments());
        assertEquals(1, result.createdTeams());
        assertEquals(1, result.updatedTeams());
        assertEquals(1, result.createdPositions());
        assertEquals(1, result.updatedPositions());
        assertEquals(1, result.createdUsers());
        assertEquals(1, result.updatedUsers());

        Department updatedDepartment = departmentRepository.findById(DEPARTMENT_ID).orElseThrow();
        Team updatedTeam = teamRepository.findById(TEAM_ID).orElseThrow();
        Position updatedPosition = positionRepository.findById(POSITION_ID).orElseThrow();
        User updatedUser = userRepository.findById(USER_ID).orElseThrow();
        User createdUser = userRepository.findByLoginId("new-user").orElseThrow();

        assertEquals("서비스개발본부", updatedDepartment.name());
        assertEquals(2, updatedDepartment.sortOrder());
        assertEquals("플랫폼아키텍처팀", updatedTeam.name());
        assertEquals(3, updatedTeam.sortOrder());
        assertEquals("과장", updatedPosition.name());
        assertEquals(4, updatedPosition.sortOrder());
        assertEquals("legacy-updated@meetbowl.local", updatedUser.email());
        assertEquals(UserRole.ADMIN, updatedUser.role());
        assertEquals(UserStatus.INACTIVE, updatedUser.status());
        assertEquals("new-user", createdUser.loginId());
        assertTrue(createdUser.initialPasswordChangeRequired());
        assertTrue(passwordEncoder.matches("1234", createdUser.passwordHash()));

        assertEquals("ORGANIZATION_MEMBERS_EXCEL_IMPORTED", eventPublisher.lastEvent.reason());
        assertTrue(eventPublisher.lastEvent.reindexAll());
        assertEquals(AuditResult.SUCCESS, auditLogRepository.lastSaved.result());
    }

    @Test
    void importFailsWhenRequiredColumnMissing() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.importExcel(
                                        commandWithBytes(
                                                removeHeader(
                                                        workbookBytes(validWorkbook()), "회원", 8))));

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.errorCode());
        assertEquals("회원", exception.details().get(0).sheetName());
        assertEquals("role", exception.details().get(0).field());
        assertEquals(AuditResult.FAILURE, auditLogRepository.lastSaved.result());
    }

    @Test
    void importFailsWhenRoleOrStatusIsInvalid() {
        WorkbookRows rows =
                new WorkbookRows(
                        List.of(new AffiliateRow(0, "", "한화시스템", "HSC", "ACTIVE")),
                        List.of(new DepartmentRow(0, "", "한화시스템", "서비스개발부", "DEV", "1", "ACTIVE")),
                        List.of(
                                new TeamRow(
                                        0,
                                        "",
                                        "한화시스템",
                                        "서비스개발부",
                                        "플랫폼개발팀",
                                        "PLATFORM",
                                        "1",
                                        "ACTIVE")),
                        List.of(
                                new PositionRow(
                                        0, "", "한화시스템", "대리", "STAFF", "1", "ACTIVE")),
                        List.of(
                                new UserRow(
                                        0,
                                        "",
                                        "user01",
                                        "사용자",
                                        "user01@meetbowl.local",
                                        "한화시스템",
                                        "서비스개발부",
                                        "플랫폼개발팀",
                                        "대리",
                                        "GUEST",
                                        "LOCKED")));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.importExcel(commandWithBytes(workbookBytes(rows))));

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.errorCode());
        assertEquals(2, exception.details().size());
    }

    @Test
    void importFailsWhenSortNumberIsNotNumericAndDoesNotPersistAnything() {
        WorkbookRows rows =
                new WorkbookRows(
                        List.of(new AffiliateRow(0, "", "한화시스템", "HSC", "ACTIVE")),
                        List.of(
                                new DepartmentRow(
                                        0, "", "한화시스템", "서비스개발부", "DEV", "abc", "ACTIVE")),
                        List.of(),
                        List.of(),
                        List.of());

        assertThrows(
                BusinessException.class,
                () -> useCase.importExcel(commandWithBytes(workbookBytes(rows))));

        assertTrue(departmentRepository.findAll().isEmpty());
        assertEquals(AuditResult.FAILURE, auditLogRepository.lastSaved.result());
    }

    @Test
    void importFailsWhenLoginIdDuplicatedInSameWorkbook() {
        WorkbookRows rows =
                new WorkbookRows(
                        List.of(new AffiliateRow(0, "", "한화시스템", "HSC", "ACTIVE")),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new UserRow(
                                        0,
                                        "",
                                        "dup-user",
                                        "사용자1",
                                        "dup1@meetbowl.local",
                                        "",
                                        "",
                                        "",
                                        "",
                                        "USER",
                                        "ACTIVE"),
                                new UserRow(
                                        0,
                                        "",
                                        "dup-user",
                                        "사용자2",
                                        "dup2@meetbowl.local",
                                        "",
                                        "",
                                        "",
                                        "",
                                        "USER",
                                        "ACTIVE")));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> useCase.importExcel(commandWithBytes(workbookBytes(rows))));

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.errorCode());
        assertEquals("loginId", exception.details().get(0).field());
        assertTrue(userRepository.findAll().isEmpty());
    }

    @Test
    void importGeneratesDepartmentTeamAndPositionCodesWhenNewRowsLeaveCodeBlank() {
        affiliateRepository.save(affiliate(AFFILIATE_ID, "한화시스템", "HSC", 1));
        departmentRepository.save(department(DEPARTMENT_ID, AFFILIATE_ID, "기존부서", "D001", 1));
        teamRepository.save(team(TEAM_ID, DEPARTMENT_ID, "기존팀", "T001", 1));
        positionRepository.save(position(POSITION_ID, "기존직급", "P001", 1));

        WorkbookRows rows =
                new WorkbookRows(
                        List.of(),
                        List.of(new DepartmentRow(0, "", "한화시스템", "서비스개발부", "", "2", "ACTIVE")),
                        List.of(new TeamRow(0, "", "한화시스템", "서비스개발부", "플랫폼개발팀", "", "3", "ACTIVE")),
                        List.of(new PositionRow(0, "", "한화시스템", "대리", "", "4", "ACTIVE")),
                        List.of());

        AdminOrganizationMembersExcelUseCase.ImportResult result =
                useCase.importExcel(commandWithBytes(workbookBytes(rows)));

        assertEquals(1, result.createdDepartments());
        assertEquals(1, result.createdTeams());
        assertEquals(1, result.createdPositions());
        assertTrue(
                departmentRepository.findAll().stream()
                        .anyMatch(
                                department ->
                                        department.name().equals("서비스개발부")
                                                && department.code().equals("D002")));
        assertTrue(
                teamRepository.findAll().stream()
                        .anyMatch(
                                team ->
                                        team.name().equals("플랫폼개발팀")
                                                && team.code().equals("T002")));
        assertTrue(
                positionRepository.findAll().stream()
                        .anyMatch(
                                position ->
                                        position.name().equals("대리")
                                                && position.code().equals("P002")));
    }

    private WorkbookRows validWorkbook() {
        return new WorkbookRows(
                List.of(new AffiliateRow(0, "", "한화시스템", "HSC", "ACTIVE")),
                List.of(new DepartmentRow(0, "", "한화시스템", "서비스개발부", "DEV", "1", "ACTIVE")),
                List.of(new TeamRow(0, "", "한화시스템", "서비스개발부", "플랫폼개발팀", "PLATFORM", "1", "ACTIVE")),
                List.of(new PositionRow(0, "", "한화시스템", "대리", "STAFF", "1", "ACTIVE")),
                List.of(
                        new UserRow(
                                0,
                                "",
                                "user01",
                                "사용자",
                                "user01@meetbowl.local",
                                "한화시스템",
                                "서비스개발부",
                                "플랫폼개발팀",
                                "대리",
                                "USER",
                                "ACTIVE")));
    }

    private AdminOrganizationMembersExcelUseCase.ImportCommand commandWithBytes(byte[] bytes) {
        return new AdminOrganizationMembersExcelUseCase.ImportCommand(
                bytes, "organization-members.xlsx", ADMIN_ID, "Admin", "127.0.0.1", "JUnit");
    }

    private byte[] workbookBytes(WorkbookRows rows) {
        return workbookMapper.write(rows);
    }

    private byte[] removeHeader(byte[] bytes, String sheetName, int columnIndex) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row header = workbook.getSheet(sheetName).getRow(3);
            header.getCell(columnIndex).setCellValue("");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private Affiliate affiliate(UUID id, String name, String code, Integer sortOrder) {
        return new Affiliate(id, name, code, ReferenceStatus.ACTIVE, sortOrder, NOW, NOW);
    }

    private Department department(
            UUID id, UUID affiliateId, String name, String code, Integer sortOrder) {
        return new Department(
                id, affiliateId, null, name, code, ReferenceStatus.ACTIVE, sortOrder, NOW, NOW);
    }

    private Team team(UUID id, UUID departmentId, String name, String code, Integer sortOrder) {
        return new Team(id, departmentId, name, code, ReferenceStatus.ACTIVE, sortOrder, NOW, NOW);
    }

    private Position position(UUID id, String name, String code, Integer sortOrder) {
        return new Position(id, AFFILIATE_ID, name, code, ReferenceStatus.ACTIVE, sortOrder, NOW, NOW);
    }

    private User user(UUID id, String loginId, String email, UserRole role, UserStatus status) {
        return User.of(
                id,
                loginId,
                passwordEncoder.encode("password"),
                "기존 사용자",
                email,
                role,
                status,
                AFFILIATE_ID,
                DEPARTMENT_ID,
                POSITION_ID,
                TEAM_ID,
                false,
                null,
                null,
                NOW,
                NOW);
    }

    private static final class FakeAffiliateRepository implements AffiliateRepositoryPort {
        private final Map<UUID, Affiliate> values = new ConcurrentHashMap<>();

        @Override
        public Affiliate save(Affiliate organization) {
            values.put(organization.id(), organization);
            return organization;
        }

        @Override
        public Optional<Affiliate> findById(UUID organizationId) {
            return Optional.ofNullable(values.get(organizationId));
        }

        @Override
        public List<Affiliate> findAll() {
            return values.values().stream().toList();
        }

        @Override
        public List<Affiliate> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public List<Affiliate> findAllByIds(Collection<UUID> organizationIds) {
            return organizationIds.stream()
                    .map(values::get)
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
        private final Map<UUID, Department> values = new ConcurrentHashMap<>();

        @Override
        public Department save(Department department) {
            values.put(department.id(), department);
            return department;
        }

        @Override
        public void deleteById(UUID departmentId) {
            values.remove(departmentId);
        }

        @Override
        public Optional<Department> findById(UUID departmentId) {
            return Optional.ofNullable(values.get(departmentId));
        }

        @Override
        public List<Department> findAll() {
            return values.values().stream().toList();
        }

        @Override
        public List<Department> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public List<Department> findAllByIds(Collection<UUID> departmentIds) {
            return departmentIds.stream()
                    .map(values::get)
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
        private final Map<UUID, Team> values = new ConcurrentHashMap<>();

        @Override
        public Team save(Team team) {
            values.put(team.id(), team);
            return team;
        }

        @Override
        public void deleteById(UUID teamId) {
            values.remove(teamId);
        }

        @Override
        public Optional<Team> findById(UUID teamId) {
            return Optional.ofNullable(values.get(teamId));
        }

        @Override
        public List<Team> findAll() {
            return values.values().stream().toList();
        }

        @Override
        public List<Team> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public List<Team> findAllByIds(Collection<UUID> teamIds) {
            return teamIds.stream().map(values::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public List<Team> findAllByDepartmentId(UUID departmentId) {
            return values.values().stream()
                    .filter(item -> item.departmentId().equals(departmentId))
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
        private final Map<UUID, Position> values = new ConcurrentHashMap<>();

        @Override
        public Position save(Position position) {
            values.put(position.id(), position);
            return position;
        }

        @Override
        public void deleteById(UUID positionId) {
            values.remove(positionId);
        }

        @Override
        public Optional<Position> findById(UUID positionId) {
            return Optional.ofNullable(values.get(positionId));
        }

        @Override
        public List<Position> findAll() {
            return values.values().stream().toList();
        }

        @Override
        public List<Position> findAllForExcelExport() {
            return findAll();
        }

        @Override
        public List<Position> findAllByIds(Collection<UUID> positionIds) {
            return positionIds.stream()
                    .map(values::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public List<Position> findAllByAffiliateId(UUID affiliateId) {
            return values.values().stream()
                    .filter(item -> java.util.Objects.equals(item.affiliateId(), affiliateId))
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

    private static final class FakeUserRepository implements UserRepositoryPort {
        private final Map<UUID, User> values = new ConcurrentHashMap<>();

        @Override
        public User save(User user) {
            values.put(user.id(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(values.get(userId));
        }

        @Override
        public List<User> findAll() {
            return values.values().stream().toList();
        }

        @Override
        public Optional<User> findByLoginId(String loginId) {
            return values.values().stream()
                    .filter(user -> user.loginId().equals(loginId))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return values.values().stream().filter(user -> user.email().equals(email)).findFirst();
        }

        @Override
        public List<User> findAllForExcelExportByRoles(java.util.Set<UserRole> roles) {
            return values.values().stream().filter(user -> roles.contains(user.role())).toList();
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
            throw new UnsupportedOperationException();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public List<User> findAllByAffiliateId(UUID affiliateId) {
            return List.of();
        }

        @Override
        public List<User> findAllByDepartmentId(UUID departmentId) {
            return List.of();
        }

        @Override
        public List<User> findAllByTeamId(UUID teamId) {
            return List.of();
        }

        @Override
        public List<User> findAllByPositionId(UUID positionId) {
            return List.of();
        }
    }

    private static final class FakeAdminAuditLogRepository implements AdminAuditLogRepositoryPort {
        private AdminAuditLog lastSaved;

        @Override
        public AdminAuditLog save(AdminAuditLog adminAuditLog) {
            this.lastSaved = adminAuditLog;
            return adminAuditLog;
        }

        @Override
        public Optional<AdminAuditLog> findById(UUID auditLogId) {
            return Optional.empty();
        }

        @Override
        public Paged<AdminAuditLog> findPage(
                com.meetbowl.domain.admin.AdminAuditLogSearchCondition condition) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeUserSearchReindexEventPublisher
            implements UserSearchReindexEventPublisherPort {
        private UserSearchReindexRequestedEvent lastEvent;

        @Override
        public void publish(UserSearchReindexRequestedEvent event) {
            this.lastEvent = event;
        }
    }
}
