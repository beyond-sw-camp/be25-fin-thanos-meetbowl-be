package com.meetbowl.infrastructure.stt;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meeting.MeetingRealtimeSessionStopper;

/**
 * 회의 종료 시 STT 세션도 함께 멈추도록 내부 API를 호출하는 adapter다.
 *
 * <p>다른 참석자 화면 종료 브로드캐스트와 마지막 자막 flush는 STT 서버가 담당하므로, BE는 회의 종료 후 이 adapter로 정리 요청만 보낸다.
 */
@Component
public class HttpMeetingRealtimeSessionStopper implements MeetingRealtimeSessionStopper {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;

    private final ObjectMapper objectMapper;
    private final SttSessionProperties properties;

    public HttpMeetingRealtimeSessionStopper(
            ObjectMapper objectMapper, SttSessionProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void stop(UUID meetingId) {
        HttpURLConnection connection = null;
        try {
            connection =
                    (HttpURLConnection)
                            URI.create(
                                            properties.getBaseUrl().replaceAll("/+$", "")
                                                    + "/sessions/meetings/"
                                                    + meetingId
                                                    + "/stop")
                                    .toURL()
                                    .openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestProperty(INTERNAL_TOKEN_HEADER, properties.getInternalToken());

            int statusCode = connection.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                return;
            }

            String responseBody = readResponseBody(connection, statusCode);
            String message = extractMessage(responseBody);
            throw new BusinessException(
                    ErrorCode.STT_PROVIDER_UNAVAILABLE,
                    "STT 세션 종료에 실패했습니다. " + message);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.STT_PROVIDER_UNAVAILABLE,
                    "STT 세션 종료에 실패했습니다. STT 서버 연결 상태를 확인하세요.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponseBody(HttpURLConnection connection, int statusCode) {
        try {
            InputStream stream =
                    statusCode >= 400
                            ? connection.getErrorStream()
                            : connection.getInputStream();
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
}
