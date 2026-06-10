package com.meetbowl.application.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

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
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AffiliateRepositoryPort affiliateRepositoryPort;
    @Mock private DepartmentRepositoryPort departmentRepositoryPort;
    @Mock private TeamRepositoryPort teamRepositoryPort;
    @Mock private PositionRepositoryPort positionRepositoryPort;

    @BeforeEach
    void setUp() {
        loginUseCase =
                new LoginUseCase(
                        userRepositoryPort,
                        passwordEncoder,
                        jwtTokenProvider,
                        affiliateRepositoryPort,
                        departmentRepositoryPort,
                        teamRepositoryPort,
                        positionRepositoryPort);
    }

    @Test
    @DisplayName("로그인 성공 - 일반 사용자")
    void loginSuccessUser() {
        // given
        String loginId = "user1";
        String password = "password";
        User user = createUser(loginId, "hash", UserRole.USER);
        LoginCommand command = new LoginCommand(loginId, password);

        given(userRepositoryPort.findByLoginId(loginId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, "hash")).willReturn(true);
        given(jwtTokenProvider.createToken(anyString(), any())).willReturn("token");
        given(jwtTokenProvider.getExpirationSeconds()).willReturn(3600L);

        // when
        LoginResult result = loginUseCase.execute(command);

        // then
        assertEquals("token", result.accessToken());
    }

    @Test
    @DisplayName("비활성 사용자 로그인 실패")
    void loginFailInactiveUser() {
        // given
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

        // when & then
        assertThrows(
                BusinessException.class,
                () -> loginUseCase.execute(new LoginCommand(loginId, "password")));
    }

    private User createUser(String loginId, String hash, UserRole role) {
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
                false,
                null,
                null,
                null,
                null);
    }
}
