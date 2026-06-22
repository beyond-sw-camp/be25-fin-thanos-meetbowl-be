package com.meetbowl.application.admin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 부서/팀/직급 코드를 서버 정책으로만 채번한다.
 *
 * <p>이름 기반 코드는 충돌과 변경 여파가 커서 쓰지 않고, prefix + 숫자 3자리 이상 형식으로만 생성한다. 기존에 사용된 최대 번호 다음 값부터 시작해
 * 삭제/비활성화된 코드도 가능하면 재사용하지 않는다.
 */
final class OrganizationCodeGenerator {

    private static final Pattern NUMERIC_SUFFIX_PATTERN =
            Pattern.compile("^(?<prefix>[A-Z]+)(?<seq>\\d+)$");

    private final String prefix;
    private final Set<String> reservedCodes;
    private int nextSequence;

    private OrganizationCodeGenerator(String prefix, Collection<String> existingCodes) {
        this.prefix = prefix;
        this.reservedCodes = new HashSet<>();
        this.nextSequence = 1;

        for (String existingCode : existingCodes) {
            String normalized = normalize(existingCode);
            if (normalized == null) {
                continue;
            }
            reservedCodes.add(normalized);

            Matcher matcher = NUMERIC_SUFFIX_PATTERN.matcher(normalized);
            if (!matcher.matches() || !prefix.equals(matcher.group("prefix"))) {
                continue;
            }
            nextSequence = Math.max(nextSequence, Integer.parseInt(matcher.group("seq")) + 1);
        }
    }

    static OrganizationCodeGenerator forDepartmentCodes(Collection<String> existingCodes) {
        return new OrganizationCodeGenerator("D", existingCodes);
    }

    static OrganizationCodeGenerator forTeamCodes(Collection<String> existingCodes) {
        return new OrganizationCodeGenerator("T", existingCodes);
    }

    static OrganizationCodeGenerator forPositionCodes(Collection<String> existingCodes) {
        return new OrganizationCodeGenerator("P", existingCodes);
    }

    String nextCode() {
        while (true) {
            // 001, 002처럼 짧은 형식을 유지하되 1000 이상도 자연스럽게 이어지게 한다.
            String candidate = "%s%03d".formatted(prefix, nextSequence++);
            if (reservedCodes.add(candidate)) {
                return candidate;
            }
        }
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
