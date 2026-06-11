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
        // 초기 비밀번호 변경 절차를 하나의 트랜잭션으로 묶어 처리한다.
        validateNewPassword(command.newPassword());
        User changedUser =
                Objects.requireNonNull(
                        transactionOperations.execute(status -> changePassword(command)));
        revokeAccessToken(command.accessTokenId(), command.accessTokenExpiresAt());
        return authTokenIssuer.issue(changedUser);
    }

    private User changePassword(ChangeInitialPasswordCommand command) {
        // 아직 초기 비밀번호 변경이 필요한 사용자만 이 경로로 진입할 수 있다.
        User user =
                userRepositoryPort
                        .findById(command.userId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.initialPasswordChangeRequired()) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "珥덇린 鍮꾨?踰덊샇 蹂寃쎌씠 ?꾩슂??怨꾩젙???꾨떃?덈떎.");
        }
        if (passwordEncoder.matches(command.newPassword(), user.passwordHash())) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "??鍮꾨?踰덊샇??湲곗〈 鍮꾨?踰덊샇? ?щ씪???⑸땲??");
        }

        User changedUser =
                user.completeInitialPasswordChange(passwordEncoder.encode(command.newPassword()));
        return userRepositoryPort.save(changedUser);
    }

    private void validateNewPassword(String newPassword) {
        // 일반 비밀번호 변경과 동일한 비밀번호 정책을 적용한다.
        if (newPassword == null
                || newPassword.length() < MINIMUM_PASSWORD_LENGTH
                || newPassword.length() > MAXIMUM_PASSWORD_LENGTH) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "??鍮꾨?踰덊샇??8???댁긽 100???댄븯?ъ빞 ?⑸땲??");
        }
    }

    private void revokeAccessToken(String accessTokenId, Instant accessTokenExpiresAt) {
        // 변경 직후에는 제한된 접근 토큰이 더 이상 사용되지 않도록 폐기한다.
        Duration remaining = Duration.between(Instant.now(), accessTokenExpiresAt);
        if (!remaining.isNegative() && !remaining.isZero()) {
            tokenStateRepositoryPort.revokeAccessToken(accessTokenId, remaining);
        }
    }
}
