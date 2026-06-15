package com.meetbowl.api.minutes;

import java.util.List;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

/** 루트 event-contract의 minutes.generated payload를 표현하는 RabbitMQ message DTO다. */
public record MinutesGeneratedMessage(
        UUID meetingId,
        UUID organizationId,
        UUID reviewerUserId,
        String status,
        String summary,
        List<Object> agendaItems,
        List<String> decisions,
        List<Object> actionItems,
        JsonNode editorContent,
        String model,
        String promptVersion) {}
