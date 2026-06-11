package com.meetbowl.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
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

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.common.Paged;
import com.meetbowl.domain.admin.AdminAuditLogRepositoryPort;
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

@ExtendWith(MockitoExtension.class)
class AdminUserManagementUseCaseTest {

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private AffiliateRepositoryPort affiliateRepositoryPort;
    @Mock private DepartmentRepositoryPort departmentRepositoryPort;
    @Mock private TeamRepositoryPort teamRepositoryPort;
    @Mock private PositionRepositoryPort positionRepositoryPort;
    @Mock private TemporaryPasswordGenerator temporaryPasswordGenerator;
    @Mock private AdminAuditLogRepositoryPort adminAuditLogRepositoryPort;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void createAdminUser_success_returnsTemporaryPassword_and_savesHashedInitialPassword() {
        AdminUserManagementUseCase useCase = useCase();
        UUID affiliateId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        stubOrganizationReferences(affiliateId, departmentId, teamId, positionId);
        given(userRepositoryPort.existsByLoginId("admin01")).willReturn(false);
        given(userRepositoryPort.findByEmail("admin01@example.com")).willReturn(Optional.empty());
        given(temporaryPasswordGenerator.generate()).willReturn("Temp1234Abcd5678");
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserManagementUseCase.CreateResult result =
                useCase.create(
                        new AdminUserManagementUseCase.CreateCommand(
                                "admin01",
                                "관리자",
                                "admin01@example.com",
                                "ADMIN",
                                "ACTIVE",
                                affiliateId,
                                departmentId,
                                teamId,
                                positionId,
                                Instant.parse("2026-06-11T00:00:00Z"),
                                Instant.parse("2026-06-12T00:00:00Z"),
                                UUID.randomUUID(),
                                "admin",
                                "127.0.0.1",
                                "Mozilla/5.0"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals("admin01", savedUser.getValue().loginId());
        assertTrue(savedUser.getValue().initialPasswordChangeRequired());
        assertTrue(
                passwordEncoder.matches(
                        "Temp1234Abcd5678", savedUser.getValue().passwordHash()));
        assertEquals("Temp1234Abcd5678", result.temporaryPassword());
        assertEquals(UserRole.ADMIN.name(), result.user().role());
        assertEquals(UserStatus.ACTIVE.name(), result.user().status());
    }

    @Test
    void createUser_success_returnsTemporaryPassword() {
        AdminUserManagementUseCase useCase = useCase();
        UUID affiliateId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        stubOrganizationReferences(affiliateId, departmentId, teamId, positionId);
        given(userRepositoryPort.existsByLoginId("user01")).willReturn(false);
        given(userRepositoryPort.findByEmail("user01@example.com")).willReturn(Optional.empty());
        given(temporaryPasswordGenerator.generate()).willReturn("Temp1234Abcd5678");
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserManagementUseCase.CreateResult result =
                useCase.create(
                        new AdminUserManagementUseCase.CreateCommand(
                                "user01",
                                "사용자",
                                "user01@example.com",
                                "USER",
                                "ACTIVE",
                                affiliateId,
                                departmentId,
                                teamId,
                                positionId,
                                null,
                                null,
                                UUID.randomUUID(),
                                "admin",
                                "127.0.0.1",
                                "Mozilla/5.0"));

        assertEquals("Temp1234Abcd5678", result.temporaryPassword());
        assertEquals(UserRole.USER.name(), result.user().role());
        assertTrue(result.user().initialPasswordChangeRequired());
    }

    @Test
    void createFailsWhenLoginIdAlreadyExists() {
        AdminUserManagementUseCase useCase = useCase();
        given(userRepositoryPort.existsByLoginId("admin01")).willReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.create(
                                        new AdminUserManagementUseCase.CreateCommand(
                                                "admin01",
                                                "관리자",
                                                "admin01@example.com",
                                                "ADMIN",
                                                "ACTIVE",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                UUID.randomUUID(),
                                                "admin",
                                                "127.0.0.1",
                                                "Mozilla/5.0")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void createFailsWhenEmailAlreadyExists() {
        AdminUserManagementUseCase useCase = useCase();
        given(userRepositoryPort.existsByLoginId("admin01")).willReturn(false);
        given(userRepositoryPort.findByEmail("admin01@example.com"))
                .willReturn(Optional.of(createUser("existing", "admin01@example.com")));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                useCase.create(
                                        new AdminUserManagementUseCase.CreateCommand(
                                                "admin01",
                                                "관리자",
                                                "admin01@example.com",
                                                "ADMIN",
                                                "ACTIVE",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                UUID.randomUUID(),
                                                "admin",
                                                "127.0.0.1",
                                                "Mozilla/5.0")));

        assertEquals(ErrorCode.COMMON_CONFLICT, exception.errorCode());
    }

    @Test
    void listSuccess_returnsPagedUsers() {
        AdminUserManagementUseCase useCase = useCase();
        User first = createUser("first", "first@example.com");
        User second = createUser("second", "second@example.com");
        stubOrganizationReferencesForUser(first);
        stubOrganizationReferencesForUser(second);
        given(userRepositoryPort.findPage("name", 1, 20))
                .willReturn(new Paged<>(List.of(first, second), 2));

        AdminUserManagementUseCase.PageResult result =
                useCase.search(new AdminUserManagementUseCase.SearchCommand("name", 1, 20));

        assertEquals(2, result.items().size());
        assertEquals(2, result.totalElements());
        assertEquals("first", result.items().get(0).loginId());
    }

    @Test
    void getSuccess_returnsUserDetail() {
        AdminUserManagementUseCase useCase = useCase();
        User user = createUser("detail", "detail@example.com");
        stubOrganizationReferencesForUser(user);
        given(userRepositoryPort.findById(user.id())).willReturn(Optional.of(user));

        AdminUserManagementUseCase.UserSummary result = useCase.get(user.id());

        assertEquals(user.id(), result.userId());
        assertEquals("detail", result.loginId());
    }

    @Test
    void updateSuccess_changesEditableFields() {
        AdminUserManagementUseCase useCase = useCase();
        User current = createUser("current", "current@example.com");
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
                                "수정된 이름",
                                "updated@example.com",
                                "ADMIN",
                                affiliateId,
                                departmentId,
                                teamId,
                                positionId,
                                Instant.parse("2026-06-13T00:00:00Z"),
                                Instant.parse("2026-06-14T00:00:00Z"),
                                UUID.randomUUID(),
                                "admin",
                                "127.0.0.1",
                                "Mozilla/5.0"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals("수정된 이름", savedUser.getValue().name());
        assertEquals("updated@example.com", savedUser.getValue().email());
        assertEquals(UserRole.ADMIN, savedUser.getValue().role());
        assertEquals(current.id(), result.userId());
    }

    @Test
    void updateStatusSuccess_changesUserStatus() {
        AdminUserManagementUseCase useCase = useCase();
        User current = createUser("status", "status@example.com");
        given(userRepositoryPort.findById(current.id())).willReturn(Optional.of(current));
        given(userRepositoryPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserManagementUseCase.UserSummary result =
                useCase.updateStatus(
                        new AdminUserManagementUseCase.UpdateStatusCommand(
                                current.id(),
                                "LOCKED",
                                UUID.randomUUID(),
                                "admin",
                                "127.0.0.1",
                                "Mozilla/5.0"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(savedUser.capture());
        assertEquals(UserStatus.LOCKED, savedUser.getValue().status());
        assertEquals("LOCKED", result.status());
    }

    @Test
    void getFailsWhenUserDoesNotExist() {
        AdminUserManagementUseCase useCase = useCase();
        UUID userId = UUID.randomUUID();
        given(userRepositoryPort.findById(userId)).willReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> useCase.get(userId));

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
                                                "이름",
                                                "user@example.com",
                                                "USER",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                UUID.randomUUID(),
                                                "admin",
                                                "127.0.0.1",
                                                "Mozilla/5.0")));

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
                                                "admin",
                                                "127.0.0.1",
                                                "Mozilla/5.0")));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode());
    }

    private AdminUserManagementUseCase useCase() {
        return new AdminUserManagementUseCase(
                userRepositoryPort,
                affiliateRepositoryPort,
                departmentRepositoryPort,
                teamRepositoryPort,
                positionRepositoryPort,
                passwordEncoder,
                temporaryPasswordGenerator,
                adminAuditLogRepositoryPort);
    }

    private User createUser(String loginId, String email) {
        return User.of(
                UUID.randomUUID(),
                loginId,
                passwordEncoder.encode("password"),
                "이름",
                email,
                UserRole.USER,
                UserStatus.ACTIVE,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                false,
                Instant.parse("2026-06-11T00:00:00Z"),
                Instant.parse("2026-06-12T00:00:00Z"),
                Instant.parse("2026-06-11T00:00:00Z"),
                Instant.parse("2026-06-11T00:00:00Z"));
    }

    private void stubOrganizationReferences(
            UUID affiliateId, UUID departmentId, UUID teamId, UUID positionId) {
        given(affiliateRepositoryPort.findById(affiliateId))
                .willReturn(
                        Optional.of(
                                new Affiliate(
                                        affiliateId,
                                        "계열사",
                                        "AFF",
                                        ReferenceStatus.ACTIVE,
                                        1,
                                        Instant.now(),
                                        Instant.now())));
        given(departmentRepositoryPort.findById(departmentId))
                .willReturn(
                        Optional.of(
                                new Department(
                                        departmentId,
                                        affiliateId,
                                        null,
                                        "부서",
                                        "DEP",
                                        ReferenceStatus.ACTIVE,
                                        1,
                                        Instant.now(),
                                        Instant.now())));
        given(teamRepositoryPort.findById(teamId))
                .willReturn(
                        Optional.of(
                                new Team(
                                        teamId,
                                        departmentId,
                                        "팀",
                                        "TEAM",
                                        ReferenceStatus.ACTIVE,
                                        1,
                                        Instant.now(),
                                        Instant.now())));
        given(positionRepositoryPort.findById(positionId))
                .willReturn(
                        Optional.of(
                                new Position(
                                        positionId,
                                        "직급",
                                        "POS",
                                        ReferenceStatus.ACTIVE,
                                        1,
                                        Instant.now(),
                                        Instant.now())));
    }

    private void stubOrganizationReferencesForUser(User user) {
        if (user.affiliateId() != null) {
            stubOrganizationReferences(
                    user.affiliateId(), user.departmentId(), user.teamId(), user.positionId());
        }
    }
}
