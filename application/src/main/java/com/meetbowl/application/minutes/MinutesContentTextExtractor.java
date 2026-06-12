package com.meetbowl.application.minutes;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 승인된 Tiptap 회의록 JSON을 검색용 평문으로 변환한다. */
@Component
public class MinutesContentTextExtractor {

    private final ObjectMapper objectMapper;

    public MinutesContentTextExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extract(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(content);
            if (!root.isObject() || !"doc".equals(root.path("type").asText())) {
                return content;
            }
            StringBuilder builder = new StringBuilder();
            appendNodes(root.path("content"), builder);
            return normalize(builder.toString());
        } catch (JsonProcessingException exception) {
            return content;
        }
    }

    private void appendNodes(JsonNode nodes, StringBuilder builder) {
        if (!nodes.isArray()) {
            return;
        }
        for (JsonNode node : nodes) {
            appendNode(node, builder);
        }
    }

    private void appendNode(JsonNode node, StringBuilder builder) {
        String type = node.path("type").asText();
        if ("text".equals(type)) {
            builder.append(node.path("text").asText());
            return;
        }
        if ("hardBreak".equals(type)) {
            builder.append('\n');
            return;
        }
        if ("listItem".equals(type)) {
            builder.append("- ");
            appendNodes(node.path("content"), builder);
            appendBlockBreak(builder, 1);
            return;
        }
        if ("orderedList".equals(type)) {
            appendOrderedList(node.path("content"), builder);
            appendBlockBreak(builder, 2);
            return;
        }
        if ("blockquote".equals(type)) {
            appendQuotedBlock(node.path("content"), builder);
            appendBlockBreak(builder, 2);
            return;
        }

        appendNodes(node.path("content"), builder);
        if ("heading".equals(type) || "bulletList".equals(type)) {
            appendBlockBreak(builder, 2);
        } else if ("paragraph".equals(type)) {
            appendBlockBreak(builder, 1);
        }
    }

    private void appendOrderedList(JsonNode items, StringBuilder builder) {
        if (!items.isArray()) {
            return;
        }
        int index = 1;
        for (JsonNode item : items) {
            builder.append(index).append(". ");
            appendNodes(item.path("content"), builder);
            appendBlockBreak(builder, 1);
            index += 1;
        }
    }

    private void appendQuotedBlock(JsonNode content, StringBuilder builder) {
        StringBuilder quoted = new StringBuilder();
        appendNodes(content, quoted);
        String normalized = normalize(quoted.toString());
        if (normalized.isBlank()) {
            return;
        }
        String[] lines = normalized.split("\\n");
        for (String line : lines) {
            builder.append("> ").append(line).append('\n');
        }
    }

    private void appendBlockBreak(StringBuilder builder, int newlineCount) {
        int length = builder.length();
        if (length == 0) {
            return;
        }
        int trailingNewlines = 0;
        for (int index = length - 1; index >= 0 && builder.charAt(index) == '\n'; index -= 1) {
            trailingNewlines += 1;
        }
        while (trailingNewlines < newlineCount) {
            builder.append('\n');
            trailingNewlines += 1;
        }
    }

    private String normalize(String text) {
        return text.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").trim();
    }
}
