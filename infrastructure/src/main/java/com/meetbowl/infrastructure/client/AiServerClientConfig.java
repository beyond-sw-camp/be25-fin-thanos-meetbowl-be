package com.meetbowl.infrastructure.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
        // AI 응답이 멈춰도 무한 대기하지 않도록 연결 3초, 응답 20초 상한을 둔다(초과 시 에러로 떨어짐).
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder()
                .baseUrl(baseUrl)
                // Uvicorn 내부 API와 통신할 때 JDK HttpClient의 h2c upgrade/chunked 조합을 피하고
                // 예측 가능한 HTTP/1.1 요청 본문을 전송한다.
                .requestFactory(requestFactory)
                .defaultHeader(INTERNAL_TOKEN_HEADER, internalToken)
                .build();
    }
}
