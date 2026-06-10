package com.meetbowl.domain.sharedworkspace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

public record DocumentVersion(int major, int minor, int patch)
        implements Comparable<DocumentVersion> {

    public static final DocumentVersion INITIAL = new DocumentVersion(1, 0, 0);

    private static final Pattern VERSION_PATTERN =
            Pattern.compile("^(?:v\\.)?(\\d+)\\.(\\d+)\\.(\\d+)$");

    public DocumentVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST, "문서 버전은 0 이상의 숫자로 구성해야 합니다.");
        }
    }

    public static DocumentVersion parse(String value) {
        if (value == null) {
            throw invalidFormat();
        }

        Matcher matcher = VERSION_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw invalidFormat();
        }

        try {
            return new DocumentVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        } catch (NumberFormatException exception) {
            throw invalidFormat();
        }
    }

    @Override
    public int compareTo(DocumentVersion other) {
        int majorComparison = Integer.compare(major, other.major);
        if (majorComparison != 0) {
            return majorComparison;
        }
        int minorComparison = Integer.compare(minor, other.minor);
        if (minorComparison != 0) {
            return minorComparison;
        }
        return Integer.compare(patch, other.patch);
    }

    public String value() {
        return major + "." + minor + "." + patch;
    }

    public String displayValue() {
        return "v." + value();
    }

    private static BusinessException invalidFormat() {
        return new BusinessException(
                ErrorCode.COMMON_INVALID_REQUEST, "문서 버전은 v.1.0.0 또는 1.0.0 형식이어야 합니다.");
    }
}
