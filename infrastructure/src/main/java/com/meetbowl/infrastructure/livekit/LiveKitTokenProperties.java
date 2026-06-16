package com.meetbowl.infrastructure.livekit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LiveKit join token 발급에 필요한 설정값이다.
 *
 * <p>BE는 audio track을 다루지 않고, room join에 필요한 URL/API key/API secret만 소유한다.
 */
@ConfigurationProperties(prefix = "meetbowl.livekit")
public class LiveKitTokenProperties {

    private String serverUrl;
    private String apiKey;
    private String apiSecret;
    private long tokenExpirationSeconds = 3600;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public long getTokenExpirationSeconds() {
        return tokenExpirationSeconds;
    }

    public void setTokenExpirationSeconds(long tokenExpirationSeconds) {
        this.tokenExpirationSeconds = tokenExpirationSeconds;
    }
}
