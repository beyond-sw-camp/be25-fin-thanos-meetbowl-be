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
        // 다중 문서 RAG 질의는 검색·리랭크·생성을 거치며 20초를 넘길 수 있다. 연결 실패는 빠르게 감지하되,
        // 정상 추론 중인 요청은 중간에 끊지 않도록 응답 대기 상한을 45초로 둔다.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        return RestClient.builder()
                .baseUrl(baseUrl)
                // Uvicorn 내부 API와 통신할 때 JDK HttpClient의 h2c upgrade/chunked 조합을 피하고
                // 예측 가능한 HTTP/1.1 요청 본문을 전송한다.
                .requestFactory(requestFactory)
                .defaultHeader(INTERNAL_TOKEN_HEADER, internalToken)
                .build();
    }
}
