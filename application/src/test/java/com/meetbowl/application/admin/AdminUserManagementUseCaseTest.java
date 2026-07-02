package com.meetbowl.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.user.UserSearchReindexRequestDispatcher;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.admin.AdminAuditLog;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
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
import com.meetbowl.domain.user.UserSearchReindexRequestedEvent;
import com.meetbowl.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementUseCaseTest {

    private static final Instant ACTIVE_FROM = Instant.parse("2026-06-11T00:00:00Z");
    // 고정 clock(2026-06-23) 기준으로 기본 fixture 사용자는 현재 시점에 유효한 ACTIVE 상태여야 한다.
    private static final Instant ACTIVE_UNTIL = Instant.parse("2026-06-30T00:00:00Z");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-06-23T00:00:00Z"), ZoneOffset.UTC);

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private AffiliateRepositoryPort affiliateRepositoryPort;
    @Mock private DepartmentRepositoryPort departmentRepositoryPort;
    @Mock private TeamRepositoryPort teamRepositoryPort;
    @Mock private PositionRepositoryPort positionRepositoryPort;
    @Mock private AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;
    @Mock private TokenStateRepositoryPort tokenStateRepositoryPort;
    @Mock private UserSearchReindexRequestDispatcher userSearchReindexRequestDispatcher;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void createAdminUserSuccess() {
        AdminUserManagementUseCase useCase = useCase();
        UUID adminAffiliateId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        stubOrganizationReferences(adminAffiliateId, departmentId, teamId, positionId);
        given(userRepositoryPort.existsByLoginId("admin01")).willReturn(false);
        given(userRepositoryPort.findByEmail("admin01@example.com")).willReturn(Optional.empty());
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserManagementUseCase.CreateResult result =
                useCase.create(
                        new AdminUserManagementUseCase.CreateCommand(
                                "admin01",
                                "Admin One",
                                "admin01@example.com",
                                "ADMIN",
                                "ACTIVE",
                                adminAffiliateId,
                                departmentId,
                                teamId,
                                positionId,
                                ACTIVE_FROM,
                                ACTIVE_UNTIL,
                                UUID.randomUUID(),
                                "Root Admin",
                                "127.0.0.1",
                                "JUnit"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals(UserRole.ADMIN, savedUser.getValue().role());
        assertEquals(adminAffiliateId, savedUser.getValue().affiliateId());
        assertEquals(UserRole.ADMIN.name(), result.user().role());
        assertTrue(savedUser.getValue().initialPasswordChangeRequired());
        ArgumentCaptor<AdminAuditLog> logCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepositoryPort).save(logCaptor.capture());
        assertEquals("admin01", logCaptor.getValue().targetLoginId());
        assertEquals("Admin One", logCaptor.getValue().targetName());
        verifyUserReindexPublished("USER_CREATED", result.userId());
    }

    @Test
    void createUserSuccessReturnsTemporaryPasswordAndHash() {
        AdminUserManagementUseCase useCase = useCase();
        UUID adminAffiliateId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        stubOrganizationReferences(adminAffiliateId, departmentId, teamId, positionId);
        given(userRepositoryPort.existsByLoginId("user01")).willReturn(false);
        given(userRepositoryPort.findByEmail("user01@example.com")).willReturn(Optional.empty());
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserManagementUseCase.CreateResult result =
                useCase.create(
                        new AdminUserManagementUseCase.CreateCommand(
                                "user01",
                                "User One",
                                "user01@example.com",
                                "USER",
                                "ACTIVE",
                                adminAffiliateId,
                                departmentId,
                                teamId,
                                positionId,
                                ACTIVE_FROM,
                                ACTIVE_UNTIL,
                                UUID.randomUUID(),
                                "Root Admin",
                                "127.0.0.1",
                                "JUnit"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals("1234", result.temporaryPassword());
        assertTrue(
                passwordEncoder.matches(
                        result.temporaryPassword(), savedUser.getValue().passwordHash()));
        assertTrue(savedUser.getValue().initialPasswordChangeRequired());
        assertEquals(UserRole.USER.name(), result.user().role());
        assertEquals(UserStatus.ACTIVE.name(), result.user().status());
        assertEquals(adminAffiliateId, result.user().affiliateId());
    }

    @Test
    void createUserIgnoresInactiveRequestStatus() {
        AdminUserManagementUseCase useCase = useCase();
        UUID adminAffiliateId = UUID.randomUUID();
        stubAffiliate(adminAffiliateId, "Affiliate");
        given(userRepositoryPort.existsByLoginId("user02")).willReturn(false);
        given(userRepositoryPort.findByEmail("user02@example.com")).willReturn(Optional.empty());
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserManagementUseCase.CreateResult result =
                useCase.create(
                        new AdminUserManagementUseCase.CreateCommand(
                                "user02",
                                "User Two",
                                "user02@example.com",
                                "USER",
                                "INACTIVE",
                                adminAffiliateId,
                                null,
                                null,
                                null,
                                ACTIVE_FROM,
                                ACTIVE_UNTIL,
                                UUID.randomUUID(),
                                "Root Admin",
                                "127.0.0.1",
                                "JUnit"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals(UserStatus.ACTIVE, savedUser.getValue().status());
        assertEquals(UserStatus.ACTIVE.name(), result.user().status());
    }

    @Test
    void createFailsWhenDepartmentBelongsToDifferentAffiliate() {
        AdminUserManagementUseCase useCase = useCase();
        UUID adminAffiliateId = UUID.randomUUID();
        UUID otherAffiliateId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        stubAffiliate(adminAffiliateId, "Affiliate");
        stubDepartment(departmentId, otherAffiliateId, "Department");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.create(
                                        new AdminUserManagementUseCase.CreateCommand(
                                                "user01",
                                                "User One",
                                                "user01@example.com",
                                                "USER",
                                                "ACTIVE",
                                                adminAffiliateId,
                                                departmentId,
                                                null,
                                                null,
                                                ACTIVE_FROM,
                                                ACTIVE_UNTIL,
                                                UUID.randomUUID(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void updateFailsWhenTeamBelongsToDifferentDepartment() {
        AdminUserManagementUseCase useCase = useCase();
        User current = createUser("current", "current@example.com", UserRole.USER);
        UUID affiliateId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID otherDepartmentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        stubAffiliate(affiliateId, "Affiliate");
        stubDepartment(departmentId, affiliateId, "Department");
        stubTeam(teamId, otherDepartmentId, "Team");
        given(userRepositoryPort.findById(current.id())).willReturn(Optional.of(current));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.update(
                                        new AdminUserManagementUseCase.UpdateCommand(
                                                current.id(),
                                                "Updated User",
                                                "updated@example.com",
                                                "USER",
                                                affiliateId,
                                                departmentId,
                                                teamId,
                                                null,
                                                ACTIVE_FROM,
                                                ACTIVE_UNTIL,
                                                UUID.randomUUID(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void createSucceedsWithValidAffiliateDepartmentTeamCombination() {
        AdminUserManagementUseCase useCase = useCase();
        UUID adminAffiliateId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        stubAffiliate(adminAffiliateId, "Affiliate");
        stubDepartment(departmentId, adminAffiliateId, "Department");
        stubTeam(teamId, departmentId, "Team");
        given(userRepositoryPort.existsByLoginId("user01")).willReturn(false);
        given(userRepositoryPort.findByEmail("user01@example.com")).willReturn(Optional.empty());
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserManagementUseCase.CreateResult result =
                useCase.create(
                        new AdminUserManagementUseCase.CreateCommand(
                                "user01",
                                "User One",
                                "user01@example.com",
                                "USER",
                                "ACTIVE",
                                adminAffiliateId,
                                departmentId,
                                teamId,
                                null,
                                ACTIVE_FROM,
                                ACTIVE_UNTIL,
                                UUID.randomUUID(),
                                "Root Admin",
                                "127.0.0.1",
                                "JUnit"));

        assertEquals("user01", result.user().loginId());
    }

    @Test
    void createFailsWhenAdminAffiliateIsMissing() {
        AdminUserManagementUseCase useCase = useCase();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.create(
                                        new AdminUserManagementUseCase.CreateCommand(
                                                "user01",
                                                "User One",
                                                "user01@example.com",
                                                "USER",
                                                "ACTIVE",
                                                null,
                                                null,
                                                null,
                                                null,
                                                ACTIVE_FROM,
                                                ACTIVE_UNTIL,
                                                UUID.randomUUID(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void createFailsWhenLoginIdAlreadyExists() {
        AdminUserManagementUseCase useCase = useCase();
        UUID affiliateId = UUID.randomUUID();
        stubAffiliate(affiliateId, "Affiliate");
        given(userRepositoryPort.existsByLoginId("dup")).willReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.create(
                                        new AdminUserManagementUseCase.CreateCommand(
                                                "dup",
                                                "User",
                                                "dup@example.com",
                                                "USER",
                                                "ACTIVE",
                                                affiliateId,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                UUID.randomUUID(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void createFailsWhenEmailAlreadyExists() {
        AdminUserManagementUseCase useCase = useCase();
        UUID affiliateId = UUID.randomUUID();
        stubAffiliate(affiliateId, "Affiliate");
        given(userRepositoryPort.existsByLoginId("user01")).willReturn(false);
        given(userRepositoryPort.findByEmail("user01@example.com"))
                .willReturn(
                        Optional.of(createUser("existing", "user01@example.com", UserRole.USER)));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.create(
                                        new AdminUserManagementUseCase.CreateCommand(
                                                "user01",
                                                "User",
                                                "user01@example.com",
                                                "USER",
                                                "ACTIVE",
                                                affiliateId,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                UUID.randomUUID(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void listSuccessReturnsPagedUsersWithTotalPagesAndBatchLookups() {
        AdminUserManagementUseCase useCase = useCase();
        User first = createUser("first", "first@example.com", UserRole.USER);
        User second = createUser("second", "second@example.com", UserRole.ADMIN);
        given(userRepositoryPort.search(
                        org.mockito.ArgumentMatchers.eq("name"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq(1),
                        org.mockito.ArgumentMatchers.eq(1)))
                .willReturn(new Paged<>(List.of(first, second), 2));
        stubBatchOrganizationReferences(List.of(first, second));

        AdminUserManagementUseCase.PageResult result =
                useCase.search(new AdminUserManagementUseCase.SearchCommand(" name ", null, 1, 1));

        assertEquals(2, result.items().size());
        assertEquals(2, result.totalElements());
        assertEquals(2, result.totalPages());
        assertEquals("first", result.items().get(0).loginId());
        assertEquals("ADMIN", result.items().get(1).role());
        verify(userRepositoryPort)
                .search(
                        org.mockito.ArgumentMatchers.eq("name"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq(1),
                        org.mockito.ArgumentMatchers.eq(1));
        verify(affiliateRepositoryPort).findAllByIds(any());
        verify(departmentRepositoryPort).findAllByIds(any());
        verify(teamRepositoryPort).findAllByIds(any());
        verify(positionRepositoryPort).findAllByIds(any());
        verify(affiliateRepositoryPort, never()).findById(any());
        verify(departmentRepositoryPort, never()).findById(any());
        verify(teamRepositoryPort, never()).findById(any());
        verify(positionRepositoryPort, never()).findById(any());
    }

    @Test
    void getSuccessReturnsUserDetail() {
        AdminUserManagementUseCase useCase = useCase();
        User user = createUser("detail", "detail@example.com", UserRole.ADMIN);
        stubOrganizationReferencesForUser(user);
        given(userRepositoryPort.findById(user.id())).willReturn(Optional.of(user));

        AdminUserManagementUseCase.UserSummary result = useCase.get(user.id());

        assertEquals(user.id(), result.userId());
        assertEquals("detail", result.loginId());
        assertEquals("ADMIN", result.role());
    }

    @Test
    void updateSuccessChangesEditableFieldsIncludingRole() {
        AdminUserManagementUseCase useCase = useCase();
        User current = createUser("current", "current@example.com", UserRole.USER);
        UUID affiliateId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        stubOrganizationReferences(affiliateId, departmentId, teamId, positionId);
        given(userRepositoryPort.findById(current.id())).willReturn(Optional.of(current));
        given(userRepositoryPort.findByEmail("updated@example.com")).willReturn(Optional.empty());
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserManagementUseCase.UserSummary result =
                useCase.update(
                        new AdminUserManagementUseCase.UpdateCommand(
                                current.id(),
                                "Updated User",
                                "updated@example.com",
                                "ADMIN",
                                affiliateId,
                                departmentId,
                                teamId,
                                positionId,
                                ACTIVE_FROM.plusSeconds(86400),
                                ACTIVE_UNTIL.plusSeconds(86400),
                                UUID.randomUUID(),
                                "Root Admin",
                                "127.0.0.1",
                                "JUnit"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals("Updated User", savedUser.getValue().name());
        assertEquals("updated@example.com", savedUser.getValue().email());
        assertEquals(UserRole.ADMIN, savedUser.getValue().role());
        assertEquals("ADMIN", result.role());
        ArgumentCaptor<AdminAuditLog> logCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepositoryPort).save(logCaptor.capture());
        assertEquals("current", logCaptor.getValue().targetLoginId());
        assertEquals("Updated User", logCaptor.getValue().targetName());
        verifyUserReindexPublished("USER_UPDATED", current.id());
    }

    @Test
    void updateUsesCurrentAffiliateWhenAffiliateIdIsOmitted() {
        AdminUserManagementUseCase useCase = useCase();
        User current = createUser("current", "current@example.com", UserRole.USER);
        UUID departmentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        stubOrganizationReferences(current.affiliateId(), departmentId, teamId, positionId);
        given(userRepositoryPort.findById(current.id())).willReturn(Optional.of(current));
        given(userRepositoryPort.findByEmail("updated@example.com")).willReturn(Optional.empty());
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        useCase.update(
                new AdminUserManagementUseCase.UpdateCommand(
                        current.id(),
                        "Updated User",
                        "updated@example.com",
                        "USER",
                        null,
                        departmentId,
                        teamId,
                        positionId,
                        ACTIVE_FROM,
                        ACTIVE_UNTIL,
                        UUID.randomUUID(),
                        "Root Admin",
                        "127.0.0.1",
                        "JUnit"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals(current.affiliateId(), savedUser.getValue().affiliateId());
    }

    @Test
    void updateStatusToActiveSuccess() {
        assertStatusUpdateSuccess("ACTIVE", UserStatus.ACTIVE);
    }

    @Test
    void updateStatusToInactiveSuccess() {
        assertStatusUpdateSuccess("INACTIVE", UserStatus.INACTIVE);
    }

    @Test
    void updateStatusFailsWhenLockedRequested() {
        AdminUserManagementUseCase useCase = useCase();
        User current = createUser("status", "status@example.com", UserRole.ADMIN);
        given(userRepositoryPort.findById(current.id())).willReturn(Optional.of(current));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.updateStatus(
                                        new AdminUserManagementUseCase.UpdateStatusCommand(
                                                current.id(),
                                                "LOCKED",
                                                UUID.randomUUID(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void updateStatusFailsWhenAdminTriesToInactivateSelf() {
        AdminUserManagementUseCase useCase = useCase();
        User current = createUser("self-admin", "self@example.com", UserRole.ADMIN);
        given(userRepositoryPort.findById(current.id())).willReturn(Optional.of(current));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.updateStatus(
                                        new AdminUserManagementUseCase.UpdateStatusCommand(
                                                current.id(),
                                                "INACTIVE",
                                                current.id(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void deleteSuccessSoftDeletesUserAndWritesAuditLog() {
        AdminUserManagementUseCase useCase = useCase();
        User current = createUser("delete-user", "delete@example.com", UserRole.USER);
        stubOrganizationReferencesForUser(current);
        given(userRepositoryPort.findByIdIncludingDeleted(current.id()))
                .willReturn(Optional.of(current));
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserManagementUseCase.UserSummary result =
                useCase.delete(
                        new AdminUserManagementUseCase.DeleteCommand(
                                current.id(),
                                UUID.randomUUID(),
                                "Root Admin",
                                "127.0.0.1",
                                "JUnit"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals(UserStatus.ACTIVE, savedUser.getValue().status());
        assertEquals(Instant.parse("2026-06-23T00:00:00Z"), savedUser.getValue().deletedAt());
        assertEquals("deleted+" + current.id() + "@deleted.local", savedUser.getValue().email());
        assertEquals("INACTIVE", result.status());
        verify(tokenStateRepositoryPort).revokeUserSessions(eq(current.id()), any());
        verifyUserReindexPublished("USER_DELETED", current.id());

        ArgumentCaptor<AdminAuditLog> logCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepositoryPort).save(logCaptor.capture());
        assertEquals("DELETE", logCaptor.getValue().actionName());
        assertEquals(current.id(), logCaptor.getValue().targetId());
        assertEquals("delete-user", logCaptor.getValue().targetLoginId());
        assertEquals("User Name", logCaptor.getValue().targetName());
    }

    @Test
    void deleteFailsWhenTargetIsCurrentAdmin() {
        AdminUserManagementUseCase useCase = useCase();
        User current = createUser("self-admin", "self@example.com", UserRole.ADMIN);
        stubOrganizationReferencesForUser(current);
        given(userRepositoryPort.findByIdIncludingDeleted(current.id()))
                .willReturn(Optional.of(current));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.delete(
                                        new AdminUserManagementUseCase.DeleteCommand(
                                                current.id(),
                                                current.id(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void deleteFailsWhenUserIsAlreadyInactive() {
        AdminUserManagementUseCase useCase = useCase();
        User current =
                User.of(
                        UUID.randomUUID(),
                        "inactive-user",
                        passwordEncoder.encode("password"),
                        "Inactive User",
                        "inactive@example.com",
                        UserRole.USER,
                        UserStatus.INACTIVE,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        false,
                        ACTIVE_FROM,
                        ACTIVE_UNTIL,
                        FIXED_CLOCK.instant(),
                        ACTIVE_FROM,
                        ACTIVE_FROM);
        stubOrganizationReferencesForUser(current);
        given(userRepositoryPort.findByIdIncludingDeleted(current.id()))
                .willReturn(Optional.of(current));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.delete(
                                        new AdminUserManagementUseCase.DeleteCommand(
                                                current.id(),
                                                UUID.randomUUID(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void createAuditSnapshotSerializesQuotesAndNewlinesAndExcludesPasswords() throws Exception {
        AdminUserManagementUseCase useCase = useCase();
        UUID affiliateId = UUID.randomUUID();
        stubAffiliate(affiliateId, "Affiliate");
        given(userRepositoryPort.existsByLoginId("quoted")).willReturn(false);
        given(userRepositoryPort.findByEmail("line\nbreak@example.com"))
                .willReturn(Optional.empty());
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        useCase.create(
                new AdminUserManagementUseCase.CreateCommand(
                        "quoted",
                        "Name \"Quoted\"\nLine",
                        "line\nbreak@example.com",
                        "USER",
                        "ACTIVE",
                        affiliateId,
                        null,
                        null,
                        null,
                        ACTIVE_FROM,
                        ACTIVE_UNTIL,
                        UUID.randomUUID(),
                        "Root Admin",
                        "127.0.0.1",
                        "JUnit"));

        ArgumentCaptor<AdminAuditLog> logCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepositoryPort).save(logCaptor.capture());
        String afterValue = logCaptor.getValue().afterValue();
        JsonNode jsonNode = objectMapper.readTree(afterValue);
        assertEquals("Name \"Quoted\"\nLine", jsonNode.get("name").asText());
        assertEquals("line\nbreak@example.com", jsonNode.get("email").asText());
        assertFalse(afterValue.contains("temporaryPassword"));
        assertFalse(afterValue.contains("rawPassword"));
        assertFalse(afterValue.contains("1234"));
    }

    @Test
    void getFailsWhenUserDoesNotExist() {
        AdminUserManagementUseCase useCase = useCase();
        UUID userId = UUID.randomUUID();
        given(userRepositoryPort.findById(userId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.get(userId));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode());
    }

    @Test
    void updateFailsWhenUserDoesNotExist() {
        AdminUserManagementUseCase useCase = useCase();
        UUID userId = UUID.randomUUID();
        given(userRepositoryPort.findById(userId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.update(
                                        new AdminUserManagementUseCase.UpdateCommand(
                                                userId,
                                                "User",
                                                "user@example.com",
                                                "USER",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                UUID.randomUUID(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode());
    }

    @Test
    void updateStatusFailsWhenUserDoesNotExist() {
        AdminUserManagementUseCase useCase = useCase();
        UUID userId = UUID.randomUUID();
        given(userRepositoryPort.findById(userId)).willReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.updateStatus(
                                        new AdminUserManagementUseCase.UpdateStatusCommand(
                                                userId,
                                                "ACTIVE",
                                                UUID.randomUUID(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode());
    }

    @Test
    void updateFailsWhenTargetIsSystemAccount() {
        AdminUserManagementUseCase useCase = useCase();
        User systemUser = createUser("system", "system@example.com", UserRole.SYSTEM);
        given(userRepositoryPort.findById(systemUser.id())).willReturn(Optional.of(systemUser));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.update(
                                        new AdminUserManagementUseCase.UpdateCommand(
                                                systemUser.id(),
                                                "System",
                                                "system@example.com",
                                                "ADMIN",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                UUID.randomUUID(),
                                                "Root Admin",
                                                "127.0.0.1",
                                                "JUnit")));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }

    private void assertStatusUpdateSuccess(String requestedStatus, UserStatus expectedStatus) {
        AdminUserManagementUseCase useCase = useCase();
        User current = createUser("status", "status@example.com", UserRole.ADMIN);
        given(userRepositoryPort.findById(current.id())).willReturn(Optional.of(current));
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserManagementUseCase.UserSummary result =
                useCase.updateStatus(
                        new AdminUserManagementUseCase.UpdateStatusCommand(
                                current.id(),
                                requestedStatus,
                                UUID.randomUUID(),
                                "Root Admin",
                                "127.0.0.1",
                                "JUnit"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals(expectedStatus, savedUser.getValue().status());
        assertEquals(expectedStatus.name(), result.status());
        verify(tokenStateRepositoryPort).revokeUserSessions(eq(current.id()), any());
        ArgumentCaptor<AdminAuditLog> logCaptor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepositoryPort).save(logCaptor.capture());
        assertEquals("status", logCaptor.getValue().targetLoginId());
        assertEquals("User Name", logCaptor.getValue().targetName());
        verifyUserReindexPublished("USER_STATUS_UPDATED", current.id());
    }

    private AdminUserManagementUseCase useCase() {
        return new AdminUserManagementUseCase(
                userRepositoryPort,
                affiliateRepositoryPort,
                departmentRepositoryPort,
                teamRepositoryPort,
                positionRepositoryPort,
                passwordEncoder,
                adminAuditLogRepositoryPort,
                tokenStateRepositoryPort,
                objectMapper,
                userSearchReindexRequestDispatcher,
                FIXED_CLOCK);
    }

    private void verifyUserReindexPublished(String reason, UUID userId) {
        ArgumentCaptor<UserSearchReindexRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(UserSearchReindexRequestedEvent.class);
        verify(userSearchReindexRequestDispatcher).publishAfterCommit(eventCaptor.capture());
        assertEquals(reason, eventCaptor.getValue().reason());
        assertEquals(List.of(userId), eventCaptor.getValue().userIds());
    }

    private User createUser(String loginId, String email, UserRole role) {
        return User.of(
                UUID.randomUUID(),
                loginId,
                passwordEncoder.encode("password"),
                "User Name",
                email,
                role,
                UserStatus.ACTIVE,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                false,
                ACTIVE_FROM,
                ACTIVE_UNTIL,
                ACTIVE_FROM,
                ACTIVE_FROM);
    }

    private void stubOrganizationReferences(
            UUID affiliateId, UUID departmentId, UUID teamId, UUID positionId) {
        stubAffiliate(affiliateId, "Affiliate");
        stubDepartment(departmentId, affiliateId, "Department");
        stubTeam(teamId, departmentId, "Team");
        stubPosition(positionId, affiliateId, "Position");
    }

    private void stubOrganizationReferencesForUser(User user) {
        stubOrganizationReferences(
                user.affiliateId(), user.departmentId(), user.teamId(), user.positionId());
    }

    private void stubBatchOrganizationReferences(List<User> users) {
        given(affiliateRepositoryPort.findAllByIds(any()))
                .willReturn(
                        users.stream()
                                .map(User::affiliateId)
                                .distinct()
                                .map(id -> affiliate(id, "Affiliate-" + id))
                                .toList());
        given(departmentRepositoryPort.findAllByIds(any()))
                .willReturn(
                        users.stream()
                                .map(User::departmentId)
                                .distinct()
                                .map(id -> department(id, UUID.randomUUID(), "Department-" + id))
                                .toList());
        given(teamRepositoryPort.findAllByIds(any()))
                .willReturn(
                        users.stream()
                                .map(User::teamId)
                                .distinct()
                                .map(id -> team(id, UUID.randomUUID(), "Team-" + id))
                                .toList());
        given(positionRepositoryPort.findAllByIds(any()))
                .willReturn(
                        users.stream()
                                .map(User::positionId)
                                .distinct()
                                .map(id -> position(id, users.get(0).affiliateId(), "Position-" + id))
                                .toList());
    }

    private void stubAffiliate(UUID affiliateId, String name) {
        given(affiliateRepositoryPort.findById(affiliateId))
                .willReturn(Optional.of(affiliate(affiliateId, name)));
    }

    private void stubDepartment(UUID departmentId, UUID affiliateId, String name) {
        given(departmentRepositoryPort.findById(departmentId))
                .willReturn(Optional.of(department(departmentId, affiliateId, name)));
    }

    private void stubTeam(UUID teamId, UUID departmentId, String name) {
        given(teamRepositoryPort.findById(teamId))
                .willReturn(Optional.of(team(teamId, departmentId, name)));
    }

    private void stubPosition(UUID positionId, UUID affiliateId, String name) {
        if (positionId != null) {
            given(positionRepositoryPort.findById(positionId))
                    .willReturn(Optional.of(position(positionId, affiliateId, name)));
        }
    }

    private Affiliate affiliate(UUID affiliateId, String name) {
        return new Affiliate(
                affiliateId, name, "AFF", ReferenceStatus.ACTIVE, 1, Instant.now(), Instant.now());
    }

    private Department department(UUID departmentId, UUID affiliateId, String name) {
        return new Department(
                departmentId,
                affiliateId,
                null,
                name,
                "DEP",
                ReferenceStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now());
    }

    private Team team(UUID teamId, UUID departmentId, String name) {
        return new Team(
                teamId,
                departmentId,
                name,
                "TEAM",
                ReferenceStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now());
    }

    private Position position(UUID positionId, UUID affiliateId, String name) {
        return new Position(
                positionId,
                affiliateId,
                name,
                "POS",
                ReferenceStatus.ACTIVE,
                1,
                Instant.now(),
                Instant.now());
    }
}
