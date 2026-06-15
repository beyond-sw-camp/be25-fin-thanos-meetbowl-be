package com.meetbowl.api.messaging;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
