package com.meetbowl.application.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
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

@Service
@Transactional
public class LoginUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenIssuer authTokenIssuer;
    private final AffiliateRepositoryPort affiliateRepositoryPort;
    private final DepartmentRepositoryPort departmentRepositoryPort;
    private final TeamRepositoryPort teamRepositoryPort;
    private final PositionRepositoryPort positionRepositoryPort;
    private final TokenStateRepositoryPort tokenStateRepositoryPort;

    public LoginUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoder passwordEncoder,
            AuthTokenIssuer authTokenIssuer,
            AffiliateRepositoryPort affiliateRepositoryPort,
            DepartmentRepositoryPort departmentRepositoryPort,
            TeamRepositoryPort teamRepositoryPort,
            PositionRepositoryPort positionRepositoryPort,
            TokenStateRepositoryPort tokenStateRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.authTokenIssuer = authTokenIssuer;
        this.affiliateRepositoryPort = affiliateRepositoryPort;
        this.departmentRepositoryPort = departmentRepositoryPort;
        this.teamRepositoryPort = teamRepositoryPort;
        this.positionRepositoryPort = positionRepositoryPort;
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
    }

    public LoginResult execute(LoginCommand command) {
        User user =
                userRepositoryPort
                        .findByLoginId(command.loginId())
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ErrorCode.COMMON_UNAUTHORIZED,
                                                "아이디 또는 비밀번호가 올바르지 않습니다."));

        Instant now = Instant.now();
        if (!user.canLoginAt(now)) {
            throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "로그인할 수 없는 계정 상태입니다.");
        }

        if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
            throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if (user.isSystemAccount()) {
            throw new BusinessException(
                    ErrorCode.COMMON_UNAUTHORIZED, "시스템 계정은 내부 인증만 사용할 수 있습니다.");
        }
        // ADMIN 계정은 유효한 refresh token 이 남아 있으면 기존 세션이 살아 있는 것으로 보고,
        // 먼저 로그인한 관리자 세션은 유지한 채 후행 로그인만 차단한다.
        if (user.role().canAccessAdminApi()
                && tokenStateRepositoryPort.hasActiveRefreshToken(user.id())) {
            throw new BusinessException(ErrorCode.AUTH_ADMIN_ALREADY_LOGGED_IN);
        }

        IssuedTokens tokens =
                user.initialPasswordChangeRequired()
                        ? authTokenIssuer.issueInitialPasswordChangeToken(user)
                        : authTokenIssuer.issue(user);

        return new LoginResult(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.accessTokenExpiresIn(),
                tokens.refreshTokenExpiresIn(),
                new LoginResult.UserSummary(
                        user.id(),
                        user.loginId(),
                        user.name(),
                        user.email(),
                        user.role().name(),
                        user.status().name(),
                        getAffiliateName(user.affiliateId()),
                        getDepartmentName(user.departmentId()),
                        getTeamName(user.teamId()),
                        getPositionName(user.positionId()),
                        user.initialPasswordChangeRequired()));
    }

    private String getAffiliateName(UUID id) {
        return id == null
                ? null
                : affiliateRepositoryPort.findById(id).map(Affiliate::name).orElse(null);
    }

    private String getDepartmentName(UUID id) {
        return id == null
                ? null
                : departmentRepositoryPort.findById(id).map(Department::name).orElse(null);
    }

    private String getTeamName(UUID id) {
        return id == null ? null : teamRepositoryPort.findById(id).map(Team::name).orElse(null);
    }

    private String getPositionName(UUID id) {
        return id == null
                ? null
                : positionRepositoryPort.findById(id).map(Position::name).orElse(null);
    }
}
