package com.meetbowl.api.admin.dto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.admin.AdminAuditLogResult;

final class AdminAuditLogDisplayFormatter {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA).withZone(DISPLAY_ZONE);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss", Locale.KOREA).withZone(DISPLAY_ZONE);

    private static final Map<String, String> ACTION_LABELS = createActionLabels();
    private static final Map<String, String> TARGET_TYPE_LABELS = createTargetTypeLabels();
    private static final Map<String, String> USER_FIELD_LABELS = createUserFieldLabels();
    private static final Map<String, String> POLICY_FIELD_LABELS = createPolicyFieldLabels();
    private static final Map<String, String> FALLBACK_FIELD_LABELS = createFallbackFieldLabels();

    private static final Set<String> SENSITIVE_KEYS =
            Set.of(
                    "password",
                    "passwordHash",
                    "temporaryPassword",
                    "accessToken",
                    "refreshToken",
                    "jwt",
                    "apiKey",
                    "token",
                    "resetToken",
                    "mailBody");

    private static final Set<String> INTERNAL_FIELDS =
            Set.of(
                    "id",
                    "createdAt",
                    "updatedAt",
                    "deletedAt",
                    "modifiedAt",
                    "lastModifiedAt",
                    "version",
                    "createdBy",
                    "updatedBy");

    private static final Set<String> DATE_ONLY_FIELDS =
            Set.of("activeFrom", "activeUntil", "startedAt", "endedAt", "date");

    private static final Set<String> POLICY_TARGET_TYPES =
            Set.of("MAIL_RETENTION_POLICY", "RETENTION_POLICY", "MAIL_POLICY");

    private AdminAuditLogDisplayFormatter() {}

    static DisplayInfo format(AdminAuditLogResult result, ObjectMapper objectMapper) {
        JsonNode beforeNode = readTree(result.beforeSnapshot(), objectMapper);
        JsonNode afterNode = readTree(result.afterSnapshot(), objectMapper);
        String actionLabel = ACTION_LABELS.getOrDefault(result.actionType(), result.actionType());
        String targetTypeLabel =
                TARGET_TYPE_LABELS.getOrDefault(result.targetType(), result.targetType());
        List<AdminAuditLogResponse.DisplayChangeItemResponse> displayChangeItems =
                buildDisplayChangeItems(result, beforeNode, afterNode);

        return new DisplayInfo(actionLabel, targetTypeLabel, actionLabel, displayChangeItems);
    }

    private static List<AdminAuditLogResponse.DisplayChangeItemResponse> buildDisplayChangeItems(
            AdminAuditLogResult result, JsonNode beforeNode, JsonNode afterNode) {
        if ("USER".equals(result.targetType())) {
            return buildUserChangeItems(result, beforeNode, afterNode);
        }
        if ("ORGANIZATION_MEMBER_EXCEL".equals(result.targetType())
                || "ORGANIZATION_EXCEL".equals(result.targetType())) {
            return buildExcelChangeItems(result, prefer(afterNode, beforeNode));
        }
        if (POLICY_TARGET_TYPES.contains(result.targetType())) {
            return buildPolicyChangeItems(beforeNode, afterNode);
        }
        return buildFallbackItems(prefer(afterNode, beforeNode));
    }

    private static List<AdminAuditLogResponse.DisplayChangeItemResponse> buildUserChangeItems(
            AdminAuditLogResult result, JsonNode beforeNode, JsonNode afterNode) {
        if (isPasswordResetAction(result.actionType())) {
            return List.of(
                    new AdminAuditLogResponse.DisplayChangeItemResponse(
                            "작업 내용", null, null, "회원 비밀번호를 초기화했습니다."));
        }
        if ("USER_PASSWORD_RESET_REQUEST".equals(result.actionType())) {
            return List.of(
                    new AdminAuditLogResponse.DisplayChangeItemResponse(
                            "작업 내용", null, null, "비밀번호 재설정 요청이 접수되었습니다."));
        }

        List<AdminAuditLogResponse.DisplayChangeItemResponse> items = new ArrayList<>();
        JsonNode sourceBefore = objectNodeOrNull(beforeNode);
        JsonNode sourceAfter = objectNodeOrNull(afterNode);

        for (Map.Entry<String, String> entry : USER_FIELD_LABELS.entrySet()) {
            String field = entry.getKey();
            String label = entry.getValue();
            String beforeValue = formatNodeValue(field, sourceBefore == null ? null : sourceBefore.get(field));
            String afterValue = formatNodeValue(field, sourceAfter == null ? null : sourceAfter.get(field));

            if ("USER_CREATE".equals(result.actionType()) && afterValue != null) {
                items.add(new AdminAuditLogResponse.DisplayChangeItemResponse(label, null, null, afterValue));
                continue;
            }
            if ("USER_DELETE".equals(result.actionType()) && beforeValue != null) {
                items.add(new AdminAuditLogResponse.DisplayChangeItemResponse(label, null, null, beforeValue));
                continue;
            }
            if (!equalsDisplayValue(beforeValue, afterValue)
                    && (beforeValue != null || afterValue != null)) {
                items.add(
                        new AdminAuditLogResponse.DisplayChangeItemResponse(
                                label,
                                beforeValue == null ? "-" : beforeValue,
                                afterValue == null ? "-" : afterValue,
                                formatDiffValue(beforeValue, afterValue)));
            }
        }

        if (!items.isEmpty()) {
            return items;
        }
        return buildFailureOrFallbackItems(afterNode, beforeNode);
    }

    private static List<AdminAuditLogResponse.DisplayChangeItemResponse> buildExcelChangeItems(
            AdminAuditLogResult result, JsonNode sourceNode) {
        JsonNode source = objectNodeOrNull(sourceNode);
        if (source == null) {
            return List.of();
        }

        List<AdminAuditLogResponse.DisplayChangeItemResponse> items = new ArrayList<>();
        addValueItem(items, "파일명", textValue(source.get("fileName")));
        addValueItem(items, "작업 결과", "FAILED".equals(result.result()) ? "실패" : "성공");

        String failureMessage = firstText(source, "message", "failureReason");
        if (failureMessage != null) {
            addValueItem(items, "실패 사유", failureMessage);
        }

        JsonNode resultNode = source.get("result");
        if (resultNode != null && resultNode.isObject()) {
            int processedCount = 0;
            for (String fieldName :
                    List.of(
                            "createdAffiliates",
                            "updatedAffiliates",
                            "createdDepartments",
                            "updatedDepartments",
                            "createdTeams",
                            "updatedTeams",
                            "createdPositions",
                            "updatedPositions",
                            "createdUsers",
                            "updatedUsers")) {
                processedCount += intValue(resultNode.get(fieldName));
            }
            addValueItem(items, "처리 건수", String.valueOf(processedCount));
        }

        JsonNode errorCountNode = source.get("errorCount");
        if (errorCountNode != null && errorCountNode.isNumber()) {
            addValueItem(items, "오류 건수", errorCountNode.asText());
        }

        if (!items.isEmpty()) {
            return items;
        }
        return buildFailureOrFallbackItems(sourceNode, null);
    }

    private static List<AdminAuditLogResponse.DisplayChangeItemResponse> buildPolicyChangeItems(
            JsonNode beforeNode, JsonNode afterNode) {
        List<AdminAuditLogResponse.DisplayChangeItemResponse> items = new ArrayList<>();
        JsonNode sourceBefore = objectNodeOrNull(beforeNode);
        JsonNode sourceAfter = objectNodeOrNull(afterNode);

        for (Map.Entry<String, String> entry : POLICY_FIELD_LABELS.entrySet()) {
            String field = entry.getKey();
            String label = entry.getValue();
            String beforeValue = formatNodeValue(field, sourceBefore == null ? null : sourceBefore.get(field));
            String afterValue = formatNodeValue(field, sourceAfter == null ? null : sourceAfter.get(field));
            if (!equalsDisplayValue(beforeValue, afterValue)
                    && (beforeValue != null || afterValue != null)) {
                items.add(
                        new AdminAuditLogResponse.DisplayChangeItemResponse(
                                label,
                                beforeValue == null ? "-" : beforeValue,
                                afterValue == null ? "-" : afterValue,
                                formatDiffValue(beforeValue, afterValue)));
            }
        }

        if (!items.isEmpty()) {
            return items;
        }
        return buildFailureOrFallbackItems(afterNode, beforeNode);
    }

    private static List<AdminAuditLogResponse.DisplayChangeItemResponse> buildFailureOrFallbackItems(
            JsonNode preferredNode, JsonNode fallbackNode) {
        JsonNode source = prefer(preferredNode, fallbackNode);
        JsonNode objectNode = objectNodeOrNull(source);
        if (objectNode != null) {
            String message = firstText(objectNode, "message", "failureReason");
            if (message != null) {
                return List.of(
                        new AdminAuditLogResponse.DisplayChangeItemResponse(
                                "실패 사유", null, null, message));
            }
        }
        return buildFallbackItems(source);
    }

    private static List<AdminAuditLogResponse.DisplayChangeItemResponse> buildFallbackItems(
            JsonNode source) {
        JsonNode objectNode = objectNodeOrNull(source);
        if (objectNode == null) {
            String value = formatNodeValue("details", source);
            if (value == null) {
                return List.of();
            }
            return List.of(
                    new AdminAuditLogResponse.DisplayChangeItemResponse(
                            "작업 내용", null, null, value));
        }

        List<AdminAuditLogResponse.DisplayChangeItemResponse> items = new ArrayList<>();
        // 표시용 상세에는 운영자가 이해할 수 있는 값만 남기고 내부 관리 필드는 걸러낸다.
        objectNode.fields()
                .forEachRemaining(
                        entry -> {
                            String key = entry.getKey();
                            if (items.size() >= 6 || isSensitiveKey(key) || isInternalField(key)) {
                                return;
                            }
                            String value = formatNodeValue(key, entry.getValue());
                            if (value != null) {
                                items.add(
                                        new AdminAuditLogResponse.DisplayChangeItemResponse(
                                                prettifyLabel(key),
                                                null,
                                                null,
                                                value));
                            }
                        });
        return items;
    }

    private static JsonNode readTree(String snapshot, ObjectMapper objectMapper) {
        if (snapshot == null || snapshot.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(snapshot);
        } catch (Exception exception) {
            return null;
        }
    }

    private static JsonNode prefer(JsonNode primary, JsonNode secondary) {
        return primary != null && !primary.isNull() ? primary : secondary;
    }

    private static JsonNode objectNodeOrNull(JsonNode node) {
        return node != null && node.isObject() ? node : null;
    }

    private static String formatNodeValue(String fieldName, JsonNode node) {
        if (node == null || node.isNull() || isSensitiveKey(fieldName) || isInternalField(fieldName)) {
            return null;
        }
        if (node.isTextual()) {
            return formatTextValue(fieldName, node.asText());
        }
        if (node.isBoolean()) {
            return node.asBoolean() ? "예" : "아니오";
        }
        if (node.isNumber()) {
            return formatNumericValue(fieldName, node);
        }
        if (node.isObject() || node.isArray()) {
            return null;
        }
        return node.asText();
    }

    private static String formatTextValue(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("status".equals(fieldName)) {
            return switch (value) {
                case "ACTIVE" -> "활성";
                case "INACTIVE" -> "비활성";
                case "LOCKED" -> "잠김";
                default -> value;
            };
        }
        if ("role".equals(fieldName)) {
            return switch (value) {
                case "ADMIN" -> "관리자";
                case "USER" -> "회원";
                default -> value;
            };
        }
        if (looksLikeIsoInstant(value)) {
            return formatInstant(fieldName, Instant.parse(value));
        }
        return value;
    }

    private static String formatNumericValue(String fieldName, JsonNode node) {
        if ("retentionDays".equals(fieldName) || fieldName.endsWith("RetentionDays")) {
            return node.asInt() + "일";
        }

        double numericValue = node.asDouble();
        if (looksLikeEpochField(fieldName, numericValue)) {
            long epochMillis = numericValue >= 1_000_000_000_000L
                    ? (long) numericValue
                    : (long) (numericValue * 1000L);
            return formatInstant(fieldName, Instant.ofEpochMilli(epochMillis));
        }

        if (Math.floor(numericValue) == numericValue) {
            return String.valueOf((long) numericValue);
        }
        return node.asText();
    }

    private static String formatInstant(String fieldName, Instant instant) {
        if (DATE_ONLY_FIELDS.contains(fieldName)
                || fieldName.endsWith("From")
                || fieldName.endsWith("Until")
                || fieldName.endsWith("Date")) {
            return DATE_FORMATTER.format(instant);
        }
        return DATE_TIME_FORMATTER.format(instant);
    }

    private static boolean looksLikeIsoInstant(String value) {
        return value.endsWith("Z") && value.contains("T");
    }

    private static boolean looksLikeEpochField(String fieldName, double value) {
        if (isInternalField(fieldName)) {
            return false;
        }
        return (fieldName.endsWith("At")
                        || fieldName.endsWith("From")
                        || fieldName.endsWith("Until")
                        || fieldName.endsWith("Date")
                        || fieldName.endsWith("Time"))
                && value >= 1_000_000_000L;
    }

    private static boolean equalsDisplayValue(String beforeValue, String afterValue) {
        return beforeValue == null ? afterValue == null : beforeValue.equals(afterValue);
    }

    private static String formatDiffValue(String beforeValue, String afterValue) {
        return (beforeValue == null ? "-" : beforeValue)
                + " -> "
                + (afterValue == null ? "-" : afterValue);
    }

    private static void addValueItem(
            List<AdminAuditLogResponse.DisplayChangeItemResponse> items, String label, String value) {
        if (value != null && !value.isBlank()) {
            items.add(new AdminAuditLogResponse.DisplayChangeItemResponse(label, null, null, value));
        }
    }

    private static String textValue(JsonNode node) {
        return node != null && !node.isNull() && !node.asText().isBlank() ? node.asText() : null;
    }

    private static String firstText(JsonNode source, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = textValue(source.get(fieldName));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static int intValue(JsonNode node) {
        return node != null && node.isNumber() ? node.asInt() : 0;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(key)
                || normalized.contains("token")
                || normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("jwt");
    }

    private static boolean isInternalField(String key) {
        return key != null && INTERNAL_FIELDS.contains(key);
    }

    private static boolean isPasswordResetAction(String actionType) {
        return "USER_PASSWORD_RESET".equals(actionType)
                || "PASSWORD_RESET".equals(actionType)
                || "USER_PASSWORD_INITIALIZE".equals(actionType)
                || "ADMIN_PASSWORD_RESET".equals(actionType);
    }

    private static String prettifyLabel(String key) {
        return FALLBACK_FIELD_LABELS.getOrDefault(key, key);
    }

    private static Map<String, String> createActionLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("USER_CREATE", "회원 생성");
        labels.put("USER_UPDATE", "회원 수정");
        labels.put("USER_DELETE", "회원 삭제");
        labels.put("USER_STATUS_CHANGE", "회원 상태 변경");
        labels.put("USER_STATUS_CHANGED", "회원 상태 변경");
        labels.put("USER_ACTIVATE", "회원 상태 변경");
        labels.put("USER_DEACTIVATE", "회원 상태 변경");
        labels.put("USER_LOCK", "회원 상태 변경");
        labels.put("USER_UNLOCK", "회원 상태 변경");
        labels.put("PASSWORD_RESET", "회원 비밀번호 초기화");
        labels.put("USER_PASSWORD_RESET", "회원 비밀번호 초기화");
        labels.put("USER_PASSWORD_INITIALIZE", "회원 비밀번호 초기화");
        labels.put("ADMIN_PASSWORD_RESET", "회원 비밀번호 초기화");
        labels.put("USER_PASSWORD_RESET_REQUEST", "비밀번호 재설정 요청");
        labels.put("ORGANIZATION_MEMBER_EXCEL_IMPORT", "조직/회원 엑셀 업로드");
        labels.put("ORGANIZATION_MEMBER_EXCEL_DOWNLOAD", "조직/회원 엑셀 다운로드");
        labels.put("ORGANIZATION_EXCEL_IMPORT", "조직/회원 엑셀 업로드");
        labels.put("ORGANIZATION_EXCEL_DOWNLOAD", "조직/회원 엑셀 다운로드");
        labels.put("AFFILIATE_CREATE", "조직 생성");
        labels.put("AFFILIATE_UPDATE", "조직 수정");
        labels.put("AFFILIATE_DELETE", "조직 삭제");
        labels.put("AFFILIATE_STATUS_CHANGE", "조직 상태 변경");
        labels.put("AFFILIATE_UPDATE_STATUS", "조직 상태 변경");
        labels.put("DEPARTMENT_CREATE", "부서 생성");
        labels.put("DEPARTMENT_UPDATE", "부서 수정");
        labels.put("DEPARTMENT_DELETE", "부서 삭제");
        labels.put("DEPARTMENT_STATUS_CHANGE", "부서 상태 변경");
        labels.put("DEPARTMENT_UPDATE_STATUS", "부서 상태 변경");
        labels.put("TEAM_CREATE", "팀 생성");
        labels.put("TEAM_UPDATE", "팀 수정");
        labels.put("TEAM_DELETE", "팀 삭제");
        labels.put("TEAM_STATUS_CHANGE", "팀 상태 변경");
        labels.put("TEAM_UPDATE_STATUS", "팀 상태 변경");
        labels.put("POSITION_CREATE", "직급 생성");
        labels.put("POSITION_UPDATE", "직급 수정");
        labels.put("POSITION_DELETE", "직급 삭제");
        labels.put("POSITION_STATUS_CHANGE", "직급 상태 변경");
        labels.put("POSITION_UPDATE_STATUS", "직급 상태 변경");
        labels.put("MEETING_ROOM_CREATE", "회의실 생성");
        labels.put("MEETING_ROOM_UPDATE", "회의실 수정");
        labels.put("MEETING_ROOM_DELETE", "회의실 삭제");
        labels.put("MEETING_ROOM_STATUS_CHANGE", "회의실 상태 변경");
        labels.put("RETENTION_POLICY_UPDATE", "회의록 보관 정책 수정");
        labels.put("MAIL_POLICY_UPDATE", "메일 정책 수정");
        labels.put("MAIL_RETENTION_POLICY_UPDATE", "메일 보관 정책 수정");
        labels.put("ADMIN_PERMISSION_CREATE", "관리자 권한 생성");
        labels.put("ADMIN_PERMISSION_UPDATE", "관리자 권한 수정");
        labels.put("ADMIN_PERMISSION_DELETE", "관리자 권한 삭제");
        return labels;
    }

    private static Map<String, String> createTargetTypeLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("USER", "회원");
        labels.put("ORGANIZATION", "조직");
        labels.put("AFFILIATE", "조직");
        labels.put("DEPARTMENT", "부서");
        labels.put("TEAM", "팀");
        labels.put("POSITION", "직급");
        labels.put("MEETING_ROOM", "회의실");
        labels.put("RETENTION_POLICY", "보관 정책");
        labels.put("MAIL_POLICY", "메일 정책");
        labels.put("MAIL_RETENTION_POLICY", "메일 보관 정책");
        labels.put("ORGANIZATION_MEMBER_EXCEL", "조직/회원 엑셀");
        labels.put("ORGANIZATION_EXCEL", "조직/회원 엑셀");
        return labels;
    }

    private static Map<String, String> createUserFieldLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("status", "상태");
        labels.put("activeFrom", "활성 시작일");
        labels.put("activeUntil", "활성 종료일");
        labels.put("role", "역할");
        return labels;
    }

    private static Map<String, String> createPolicyFieldLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("retentionDays", "보관 기간");
        labels.put("inboxRetentionDays", "보관 기간");
        labels.put("autoDeleteEnabled", "자동 삭제");
        return labels;
    }

    private static Map<String, String> createFallbackFieldLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("fileName", "파일명");
        labels.put("message", "메시지");
        labels.put("failureReason", "실패 사유");
        labels.put("errorCount", "오류 건수");
        labels.put("requestSource", "요청 경로");
        labels.put("initialPasswordChangeRequired", "초기 비밀번호 변경 필요");
        labels.put("retentionDays", "보관 기간");
        labels.put("autoDeleteEnabled", "자동 삭제");
        return labels;
    }

    record DisplayInfo(
            String actionLabel,
            String targetTypeLabel,
            String displayTitle,
            List<AdminAuditLogResponse.DisplayChangeItemResponse> displayChangeItems) {}
}
