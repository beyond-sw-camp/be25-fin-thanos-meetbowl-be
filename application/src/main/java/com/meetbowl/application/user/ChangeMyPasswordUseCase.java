package com.meetbowl.application.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.user.User;
import com.meetbowl.domain.user.UserRepositoryPort;

@Service
@Transactional
public class ChangeMyPasswordUseCase {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;
    private static final int MAXIMUM_PASSWORD_LENGTH = 100;

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    public ChangeMyPasswordUseCase(
            UserRepositoryPort userRepositoryPort, PasswordEncoder passwordEncoder) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoder = passwordEncoder;
    }

    public void execute(ChangeMyPasswordCommand command) {
        // 새 비밀번호는 길이와 확인값부터 먼저 검증한다.
        validateNewPassword(command.newPassword(), command.newPasswordConfirm());

        User user =
                userRepositoryPort
                        .findById(command.userId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 현재 비밀번호가 맞는지 확인한 뒤, 같은 비밀번호 재사용도 막는다.
        if (!passwordEncoder.matches(command.currentPassword(), user.passwordHash())) {
            throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
        }
        if (passwordEncoder.matches(command.newPassword(), user.passwordHash())) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "새 비밀번호는 현재 비밀번호와 다르게 설정해야 합니다.");
        }

        userRepositoryPort.save(user.changePassword(passwordEncoder.encode(command.newPassword())));
    }

    private void validateNewPassword(String newPassword, String newPasswordConfirm) {
        // 정책상 허용 길이와 확인값 일치 여부를 함께 체크한다.
        if (newPassword == null
                || newPassword.length() < MINIMUM_PASSWORD_LENGTH
                || newPassword.length() > MAXIMUM_PASSWORD_LENGTH) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "새 비밀번호는 8자 이상 100자 이하여야 합니다.");
        }
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다.");
        }
    }
}
