package com.meetbowl.infrastructure.stt;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.application.meeting.MeetingRealtimeSessionStarter;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

/**
 * 회의 입장 시 STT 세션을 내부 API로 보장하는 adapter다.
 *
 * <p>프론트가 회의 room에 붙기 전에 STT 서버 participant가 같은 room에 들어오도록 요청해, 첫 발화부터 자막이 나오도록 준비 시간을 앞당긴다.
 */
@Component
public class HttpMeetingRealtimeSessionStarter implements MeetingRealtimeSessionStarter {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;

    private final ObjectMapper objectMapper;
    private final SttSessionProperties properties;

    public HttpMeetingRealtimeSessionStarter(
            ObjectMapper objectMapper, SttSessionProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void ensureStarted(UUID meetingId, UUID organizationId, String roomName) {
        HttpURLConnection connection = null;
        try {
            byte[] requestBody =
                    objectMapper.writeValueAsBytes(
                            new EnsureStartedRequest(
                                    meetingId.toString(),
                                    organizationId.toString(),
                                    roomName,
                                    false));
            connection =
                    (HttpURLConnection)
                            URI.create(
                                            properties.getBaseUrl().replaceAll("/+$", "")
                                                    + "/sessions/ensure-started")
                                    .toURL()
                                    .openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty(INTERNAL_TOKEN_HEADER, properties.getInternalToken());
            // Fastify가 Content-Length와 실제 body 크기를 엄격히 보므로 길이를 고정해 전송한다.
            connection.setFixedLengthStreamingMode(requestBody.length);
            try (var outputStream = connection.getOutputStream()) {
                outputStream.write(requestBody);
            }

            int statusCode = connection.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                return;
            }

            String responseBody = readResponseBody(connection, statusCode);
            String message = extractMessage(responseBody);
            throw new BusinessException(
                    ErrorCode.STT_PROVIDER_UNAVAILABLE, "STT 세션 자동 시작에 실패했습니다. " + message);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.STT_PROVIDER_UNAVAILABLE,
                    "STT 세션 자동 시작에 실패했습니다. STT 서버 연결 상태를 확인하세요.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponseBody(HttpURLConnection connection, int statusCode) {
        try {
            InputStream stream =
                    statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) {
                return "";
            }
            try (stream) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    /** 내부 API 실패 응답이 공통 envelope를 따를 수 있으므로, 가능한 경우 서버 메시지를 그대로 사용자 오류에 반영한다. */
    private String extractMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");
            if (!message.isBlank()) {
                return message;
            }
            String plainMessage = root.path("message").asText("");
            if (!plainMessage.isBlank()) {
                return plainMessage;
            }
        } catch (Exception ignored) {
            // 응답이 JSON이 아니어도 상위 기본 메시지로 처리한다.
        }
        return "STT 내부 API 응답을 확인할 수 없습니다.";
    }

    private record EnsureStartedRequest(
            String meetingId, String organizationId, String roomName, boolean recordingEnabled) {}
}
