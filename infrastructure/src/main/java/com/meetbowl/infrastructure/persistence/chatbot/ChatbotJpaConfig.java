package com.meetbowl.infrastructure.persistence.chatbot;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackageClasses = ChatSessionEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataChatSessionRepository.class)
@Configuration
public class ChatbotJpaConfig {}
