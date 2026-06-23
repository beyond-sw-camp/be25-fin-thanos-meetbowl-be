package com.meetbowl.infrastructure.stt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * STT 내부 API 호출에 필요한 설정값이다.
 *
 * <p>프론트는 STT 서버를 직접 호출하지 않으므로, 회의 입장 시 필요한 내부 토큰과 base URL은 서버 설정으로만 관리한다.
 */
@ConfigurationProperties(prefix = "meetbowl.stt")
public class SttSessionProperties {

    private String baseUrl;
    private String internalToken;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }
}
