package com.meetbowl.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 내부 AI 서버(meetbowl-ai) 호출용 RestClient를 구성한다.
 *
 * <p>meetbowl-ai는 사용자 JWT를 검증하지 않고 서버 간 내부 토큰으로만 인증하므로, 모든 요청에 공통 내부 토큰 헤더를 기본 부착한다. 토큰 값은 로그로 남기지
 * 않는다.
 */
@Configuration
public class AiServerClientConfig {

    static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Bean
    RestClient aiServerRestClient(
            @Value("${meetbowl.ai.base-url:http://localhost:8000}") String baseUrl,
            @Value("${meetbowl.security.internal-token}") String internalToken) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(INTERNAL_TOKEN_HEADER, internalToken)
                .build();
    }
}
