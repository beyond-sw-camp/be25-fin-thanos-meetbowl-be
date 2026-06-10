package com.meetbowl.application.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.auth.TokenStateRepositoryPort;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

@Service
public class ChangeInitialPasswordUseCase {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;
    private static final int MAXIMUM_PASSWORD_LENGTH = 100;

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenIssuer authTokenIssuer;
    private final TokenStateRepositoryPort tokenStateRepositoryPort;
    private final TransactionOperations transactionOperations;

    public ChangeInitialPasswordUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoder passwordEncoder,
            AuthTokenIssuer authTokenIssuer,
            TokenStateRepositoryPort tokenStateRepositoryPort,
            TransactionOperations transactionOperations) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.authTokenIssuer = authTokenIssuer;
        this.tokenStateRepositoryPort = tokenStateRepositoryPort;
        this.transactionOperations = transactionOperations;
    }

    public IssuedTokens execute(ChangeInitialPasswordCommand command) {
        validateNewPassword(command.newPassword());
        User changedUser =
                Objects.requireNonNull(
                        transactionOperations.execute(status -> changePassword(command)));
        revokeAccessToken(command.accessTokenId(), command.accessTokenExpiresAt());
        return authTokenIssuer.issue(changedUser);
    }

    private User changePassword(ChangeInitialPasswordCommand command) {
        User user =
                userRepositoryPort
                        .findById(command.userId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.initialPasswordChangeRequired()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "초기 비밀번호 변경이 필요한 계정이 아닙니다.");
        }
        if (passwordEncoder.matches(command.newPassword(), user.passwordHash())) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        User changedUser =
                user.completeInitialPasswordChange(passwordEncoder.encode(command.newPassword()));
        return userRepositoryPort.save(changedUser);
    }

    private void validateNewPassword(String newPassword) {
        if (newPassword == null
                || newPassword.length() < MINIMUM_PASSWORD_LENGTH
                || newPassword.length() > MAXIMUM_PASSWORD_LENGTH) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "새 비밀번호는 8자 이상 100자 이하여야 합니다.");
        }
    }

    private void revokeAccessToken(String accessTokenId, Instant accessTokenExpiresAt) {
        Duration remaining = Duration.between(Instant.now(), accessTokenExpiresAt);
        if (!remaining.isNegative() && !remaining.isZero()) {
            tokenStateRepositoryPort.revokeAccessToken(accessTokenId, remaining);
        }
    }
}
