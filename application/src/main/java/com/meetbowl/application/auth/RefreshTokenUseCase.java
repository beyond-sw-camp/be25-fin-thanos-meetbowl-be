package com.meetbowl.application.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

@Service
@Transactional(readOnly = true)
public class RefreshTokenUseCase {

    private final TokenStateRepositoryPort tokenStateRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuthTokenIssuer authTokenIssuer;

    public RefreshTokenUseCase(
            TokenStateRepositoryPort tokenStateRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            AuthTokenIssuer authTokenIssuer) {
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.authTokenIssuer = authTokenIssuer;
    }

    public IssuedTokens execute(RefreshTokenCommand command) {
        UUID userId =
                tokenStateRepositoryPort
                        .consumeRefreshToken(RefreshTokenHasher.hash(command.refreshToken()))
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        User user =
                userRepositoryPort
                        .findById(userId)
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        if (!user.canLoginAt(Instant.now())) {
            throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "로그인할 수 없는 계정 상태입니다.");
        }

        return authTokenIssuer.issue(user);
    }
}
