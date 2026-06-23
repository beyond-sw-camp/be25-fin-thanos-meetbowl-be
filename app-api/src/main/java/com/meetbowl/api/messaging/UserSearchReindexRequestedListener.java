package com.meetbowl.api.messaging;

import java.util.LinkedHashSet;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.meetbowl.application.user.UserSearchReindexUseCase;
import com.meetbowl.common.event.EventEnvelope;
import com.meetbowl.common.event.EventTypes;
import com.meetbowl.common.event.user.UserSearchReindexRequestedMessage;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/** 회원/조직 수정 후 발행된 검색 재색인 요청을 수신해 기존 Elasticsearch 재색인 유스케이스로 위임한다. */
@Component
public class UserSearchReindexRequestedListener {

    private final ObjectMapper objectMapper;
    private final UserSearchReindexUseCase userSearchReindexUseCase;

    public UserSearchReindexRequestedListener(
            ObjectMapper objectMapper, UserSearchReindexUseCase userSearchReindexUseCase) {
        this.objectMapper = objectMapper;
        this.userSearchReindexUseCase = userSearchReindexUseCase;
    }

    @RabbitListener(queues = RabbitMqMessagingConfig.USER_SEARCH_REINDEX_QUEUE)
    public void consume(byte[] body) {
        EventEnvelope<UserSearchReindexRequestedMessage> envelope = readEnvelope(body);
        if (!EventTypes.USER_SEARCH_REINDEX_REQUESTED.equals(envelope.eventType())) {
            throw new IllegalArgumentException("지원하지 않는 이벤트입니다: " + envelope.eventType());
        }

        UserSearchReindexRequestedMessage payload = envelope.payload();
        userSearchReindexUseCase.execute(
                new UserSearchReindexUseCase.Command(
                        payload.reindexAll(),
                        new LinkedHashSet<>(payload.userIds()),
                        payload.affiliateId(),
                        payload.departmentId(),
                        payload.teamId(),
                        payload.positionId()));
    }

    private EventEnvelope<UserSearchReindexRequestedMessage> readEnvelope(byte[] body) {
        try {
            JavaType envelopeType =
                    objectMapper
                            .getTypeFactory()
                            .constructParametricType(
                                    EventEnvelope.class, UserSearchReindexRequestedMessage.class);
            return objectMapper.readValue(body, envelopeType);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "user.search.reindex.requested 이벤트를 읽을 수 없습니다.", exception);
        }
    }
}
