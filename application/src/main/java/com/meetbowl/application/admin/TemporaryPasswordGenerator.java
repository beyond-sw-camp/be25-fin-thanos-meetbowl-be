package com.meetbowl.application.admin;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

@Component
public class TemporaryPasswordGenerator {

    private static final int TEMPORARY_PASSWORD_LENGTH = 16;
    private static final char[] TEMPORARY_PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return generateRandomPassword();
    }

    public String generateDistinctFrom(String passwordHash, PasswordEncoder passwordEncoder) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String temporaryPassword = generateRandomPassword();
            if (!passwordEncoder.matches(temporaryPassword, passwordHash)) {
                return temporaryPassword;
            }
        }

        throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "임시 비밀번호를 생성할 수 없습니다.");
    }

    private String generateRandomPassword() {
        StringBuilder builder = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (int index = 0; index < TEMPORARY_PASSWORD_LENGTH; index++) {
            int randomIndex = secureRandom.nextInt(TEMPORARY_PASSWORD_ALPHABET.length);
            builder.append(TEMPORARY_PASSWORD_ALPHABET[randomIndex]);
        }
        return builder.toString();
    }
}
