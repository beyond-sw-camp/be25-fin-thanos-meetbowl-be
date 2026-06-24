package com.meetbowl.api.common;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = firstNonBlankToken(request.getHeader(ApiHeaders.X_FORWARDED_FOR));
        if (forwardedFor != null) {
            return forwardedFor;
        }

        String realIp = normalize(request.getHeader(ApiHeaders.X_REAL_IP));
        if (realIp != null) {
            return realIp;
        }

        String remoteAddr = normalize(request.getRemoteAddr());
        return remoteAddr == null ? "unknown" : remoteAddr;
    }

    private static String firstNonBlankToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (String token : value.split(",")) {
            String normalized = normalize(token);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
