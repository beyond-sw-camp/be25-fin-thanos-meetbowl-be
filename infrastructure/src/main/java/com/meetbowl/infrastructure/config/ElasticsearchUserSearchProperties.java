package com.meetbowl.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meetbowl.elasticsearch")
public record ElasticsearchUserSearchProperties(
        String baseUrl, String userIndexName, boolean autoCreateIndex, int reindexBatchSize) {}
