package com.meetbowl.api.messaging;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.meetbowl.common.event.EventTypes;

/**
 * API 서버가 소비/발행하는 RabbitMQ 기본 topology를 선언한다.
 *
 * <p>이번 작업에서는 transcript 저장 consumer가 안정적으로 뜰 수 있도록 최소 exchange/queue/binding만 명시한다.
 */
@Configuration
public class RabbitMqMessagingConfig {

    public static final String TOPIC_EXCHANGE = "meetbowl.topic";
    public static final String DLX_EXCHANGE = "meetbowl.dlx";
    public static final String TRANSCRIPT_FINAL_SAVE_QUEUE = "api.transcript.final.save";
    public static final String USER_SEARCH_REINDEX_QUEUE = "api.user.search.reindex";
    public static final String MINUTES_GENERATED_QUEUE = "api.minutes.generated";
    public static final String AI_DOCUMENT_INDEX_QUEUE = "ai.index.document";
    public static final String AI_DOCUMENT_INDEX_REMOVED_QUEUE = "ai.index.document.removed";

    @Bean
    TopicExchange meetbowlTopicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange meetbowlDlxExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    Queue transcriptFinalSaveQueue() {
        return new Queue(
                TRANSCRIPT_FINAL_SAVE_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-queue-type",
                        "quorum",
                        "x-delivery-limit",
                        3,
                        "x-dead-letter-exchange",
                        DLX_EXCHANGE,
                        "x-dead-letter-routing-key",
                        "dlq.transcript.final.created"));
    }

    @Bean
    Binding transcriptFinalSaveBinding(
            Queue transcriptFinalSaveQueue, TopicExchange meetbowlTopicExchange) {
        return BindingBuilder.bind(transcriptFinalSaveQueue)
                .to(meetbowlTopicExchange)
                .with("transcript.final.created");
    }

    @Bean
    Queue transcriptFinalSaveDeadLetterQueue() {
        return new Queue("dlq.api.transcript.final.save", true);
    }

    @Bean
    Binding transcriptFinalSaveDeadLetterBinding(
            Queue transcriptFinalSaveDeadLetterQueue, TopicExchange meetbowlDlxExchange) {
        return BindingBuilder.bind(transcriptFinalSaveDeadLetterQueue)
                .to(meetbowlDlxExchange)
                .with("dlq.transcript.final.created");
    }

    @Bean
    Queue minutesGeneratedQueue(
            @Value("${meetbowl.rabbitmq.minutes-generated-queue:api.minutes.generated}")
                    String queueName) {
        return new Queue(
                queueName,
                true,
                false,
                false,
                Map.of(
                        "x-queue-type",
                        "quorum",
                        "x-delivery-limit",
                        3,
                        "x-dead-letter-exchange",
                        DLX_EXCHANGE,
                        "x-dead-letter-routing-key",
                        "dlq.minutes.generated"));
    }

    @Bean
    Binding minutesGeneratedBinding(
            Queue minutesGeneratedQueue, TopicExchange meetbowlTopicExchange) {
        // AI 서버보다 BE가 먼저 떠도 회의록 초안 저장 큐와 binding을 보장한다.
        return BindingBuilder.bind(minutesGeneratedQueue)
                .to(meetbowlTopicExchange)
                .with("minutes.generated");
    }

    @Bean
    Queue minutesGeneratedDeadLetterQueue() {
        return new Queue("dlq.api.minutes.generated", true);
    }

    @Bean
    Binding minutesGeneratedDeadLetterBinding(
            Queue minutesGeneratedDeadLetterQueue, TopicExchange meetbowlDlxExchange) {
        return BindingBuilder.bind(minutesGeneratedDeadLetterQueue)
                .to(meetbowlDlxExchange)
                .with("dlq.minutes.generated");
    }

    @Bean
    Queue userSearchReindexQueue() {
        return new Queue(
                USER_SEARCH_REINDEX_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-queue-type",
                        "quorum",
                        "x-delivery-limit",
                        3,
                        "x-dead-letter-exchange",
                        DLX_EXCHANGE,
                        "x-dead-letter-routing-key",
                        "dlq.user.search.reindex.requested"));
    }

    @Bean
    Binding userSearchReindexBinding(
            Queue userSearchReindexQueue, TopicExchange meetbowlTopicExchange) {
        return BindingBuilder.bind(userSearchReindexQueue)
                .to(meetbowlTopicExchange)
                .with("user.search.reindex.requested");
    }

    @Bean
    Queue userSearchReindexDeadLetterQueue() {
        return new Queue("dlq.api.user.search.reindex", true);
    }

    @Bean
    Binding userSearchReindexDeadLetterBinding(
            Queue userSearchReindexDeadLetterQueue, TopicExchange meetbowlDlxExchange) {
        return BindingBuilder.bind(userSearchReindexDeadLetterQueue)
                .to(meetbowlDlxExchange)
                .with("dlq.user.search.reindex.requested");
    }

    @Bean
    Queue aiDocumentIndexQueue() {
        // 파일 업로드는 BE가 성공 응답을 먼저 반환하고 색인은 비동기로 처리한다. 따라서 AI 서버가 나중에 떠도
        // document.index.requested 이벤트가 유실되지 않도록 producer인 BE도 AI 색인 큐를 미리 선언한다.
        return new Queue(
                AI_DOCUMENT_INDEX_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange",
                        DLX_EXCHANGE,
                        "x-dead-letter-routing-key",
                        "dlq.document.index.requested"));
    }

    @Bean
    Binding aiDocumentIndexBinding(
            Queue aiDocumentIndexQueue, TopicExchange meetbowlTopicExchange) {
        return BindingBuilder.bind(aiDocumentIndexQueue)
                .to(meetbowlTopicExchange)
                .with(EventTypes.DOCUMENT_INDEX_REQUESTED);
    }

    @Bean
    Queue aiDocumentIndexDeadLetterQueue() {
        return new Queue("dlq.ai.index.document", true);
    }

    @Bean
    Binding aiDocumentIndexDeadLetterBinding(
            Queue aiDocumentIndexDeadLetterQueue, TopicExchange meetbowlDlxExchange) {
        return BindingBuilder.bind(aiDocumentIndexDeadLetterQueue)
                .to(meetbowlDlxExchange)
                .with("dlq.document.index.requested");
    }

    @Bean
    Queue aiDocumentIndexRemovedQueue() {
        // 파일 삭제 이벤트도 AI 서버 미기동 중 발행될 수 있으므로 제거 큐까지 같은 기준으로 보장한다.
        return new Queue(
                AI_DOCUMENT_INDEX_REMOVED_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange",
                        DLX_EXCHANGE,
                        "x-dead-letter-routing-key",
                        "dlq.document.index.removed"));
    }

    @Bean
    Binding aiDocumentIndexRemovedBinding(
            Queue aiDocumentIndexRemovedQueue, TopicExchange meetbowlTopicExchange) {
        return BindingBuilder.bind(aiDocumentIndexRemovedQueue)
                .to(meetbowlTopicExchange)
                .with(EventTypes.DOCUMENT_INDEX_REMOVED);
    }

    @Bean
    Queue aiDocumentIndexRemovedDeadLetterQueue() {
        return new Queue("dlq.ai.index.document.removed", true);
    }

    @Bean
    Binding aiDocumentIndexRemovedDeadLetterBinding(
            Queue aiDocumentIndexRemovedDeadLetterQueue, TopicExchange meetbowlDlxExchange) {
        return BindingBuilder.bind(aiDocumentIndexRemovedDeadLetterQueue)
                .to(meetbowlDlxExchange)
                .with("dlq.document.index.removed");
    }
}
