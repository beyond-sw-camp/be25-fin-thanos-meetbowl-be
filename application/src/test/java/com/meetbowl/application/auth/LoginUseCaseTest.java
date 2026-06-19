package com.meetbowl.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.organization.AffiliateRepositoryPort;
import com.meetbowl.domain.organization.DepartmentRepositoryPort;
import com.meetbowl.domain.organization.PositionRepositoryPort;
import com.meetbowl.domain.organization.TeamRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;
import com.meetbowl.domain.user.UserRole;
import com.meetbowl.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    private LoginUseCase loginUseCase;

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthTokenIssuer authTokenIssuer;
    @Mock private AffiliateRepositoryPort affiliateRepositoryPort;
    @Mock private DepartmentRepositoryPort departmentRepositoryPort;
    @Mock private TeamRepositoryPort teamRepositoryPort;
    @Mock private PositionRepositoryPort positionRepositoryPort;
    @Mock private TokenStateRepositoryPort tokenStateRepositoryPort;

    @BeforeEach
    void setUp() {
        loginUseCase =
                new LoginUseCase(
                        userRepositoryPort,
                        passwordEncoder,
                        authTokenIssuer,
                        affiliateRepositoryPort,
                        departmentRepositoryPort,
                        teamRepositoryPort,
                        positionRepositoryPort,
                        tokenStateRepositoryPort);
    }

    @Test
    @DisplayName("login success - user")
    void loginSuccessUser() {
        String loginId = "user1";
        String password = "password";
        User user = createUser(loginId, "hash", UserRole.USER);
        LoginCommand command = new LoginCommand(loginId, password);

        given(userRepositoryPort.findByLoginId(loginId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, "hash")).willReturn(true);
        given(authTokenIssuer.issue(any()))
                .willReturn(new IssuedTokens("access", "refresh", "Bearer", 900L, 1209600L));

        LoginResult result = loginUseCase.execute(command);

        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
    }

    @Test
    void adminSecondLoginFailsWhenActiveRefreshTokenExists() {
        String loginId = "admin";
        User admin = createUser(loginId, "hash", UserRole.ADMIN);
        given(userRepositoryPort.findByLoginId(loginId)).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("password", "hash")).willReturn(true);
        given(tokenStateRepositoryPort.hasActiveRefreshToken(admin.id())).willReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> loginUseCase.execute(new LoginCommand(loginId, "password")));

        assertEquals(ErrorCode.AUTH_ADMIN_ALREADY_LOGGED_IN, exception.errorCode());
    }

    @Test
    void userLoginIsNotBlockedByAnotherActiveSession() {
        String loginId = "user1";
        User user = createUser(loginId, "hash", UserRole.USER);
        given(userRepositoryPort.findByLoginId(loginId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", "hash")).willReturn(true);
        given(authTokenIssuer.issue(user))
                .willReturn(new IssuedTokens("access", "refresh", "Bearer", 900L, 1209600L));

        LoginResult result = loginUseCase.execute(new LoginCommand(loginId, "password"));

        assertEquals("refresh", result.refreshToken());
    }

    @Test
    @DisplayName("inactive user login fails")
    void loginFailInactiveUser() {
        String loginId = "user1";
        User user =
                User.of(
                        UUID.randomUUID(),
                        loginId,
                        "hash",
                        "name",
                        "email",
                        UserRole.USER,
                        UserStatus.INACTIVE,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        null);
        given(userRepositoryPort.findByLoginId(loginId)).willReturn(Optional.of(user));

        assertThrows(
                BusinessException.class,
                () -> loginUseCase.execute(new LoginCommand(loginId, "password")));
    }

    @Test
    void loginInitialPasswordUser_issuesRestrictedToken() {
        String loginId = "user1";
        User user = createUser(loginId, "hash", UserRole.USER, true);
        given(userRepositoryPort.findByLoginId(loginId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", "hash")).willReturn(true);
        given(authTokenIssuer.issueInitialPasswordChangeToken(user))
                .willReturn(new IssuedTokens("restricted", null, "Bearer", 900L, 0L));

        LoginResult result = loginUseCase.execute(new LoginCommand(loginId, "password"));

        assertEquals("restricted", result.accessToken());
        assertEquals(null, result.refreshToken());
        assertEquals(true, result.user().initialPasswordChangeRequired());
        verify(authTokenIssuer).issueInitialPasswordChangeToken(user);
    }

    @Test
    void loginSystemAccount_failsAfterCredentialValidation() {
        String loginId = "system1";
        User user = createUser(loginId, "hash", UserRole.SYSTEM);
        given(userRepositoryPort.findByLoginId(loginId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", "hash")).willReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> loginUseCase.execute(new LoginCommand(loginId, "password")));

        assertEquals(ErrorCode.COMMON_UNAUTHORIZED, exception.errorCode());
    }

    private User createUser(String loginId, String hash, UserRole role) {
        return createUser(loginId, hash, role, false);
    }

    private User createUser(
            String loginId, String hash, UserRole role, boolean initialPasswordChangeRequired) {
        return User.of(
                UUID.randomUUID(),
                loginId,
                hash,
                "name",
                "email",
                role,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                initialPasswordChangeRequired,
                null,
                null,
                null,
                null);
    }
}
