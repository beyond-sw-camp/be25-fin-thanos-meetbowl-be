package com.meetbowl.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ElasticsearchUserSearchProperties.class)
public class ElasticsearchUserSearchConfig {

    @Bean(name = "userSearchElasticsearchRestClient")
    RestClient userSearchElasticsearchRestClient(ElasticsearchUserSearchProperties properties) {
        return RestClient.builder().baseUrl(properties.baseUrl()).build();
    }
}
