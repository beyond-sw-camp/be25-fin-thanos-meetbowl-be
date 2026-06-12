package com.meetbowl.infrastructure.livekit;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.meetbowl.domain.meeting.LiveKitTokenIssueCommand;
import com.meetbowl.domain.meeting.LiveKitTokenIssueResult;
import com.meetbowl.domain.meeting.LiveKitTokenIssuer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * LiveKit이 요구하는 HS256 JWT를 생성한다.
 *
 * <p>브라우저에서 secret을 들고 HMAC 서명을 하지 않도록, API key/secret을 아는 책임은 이 서버 adapter에만 둔다.
 */
@Component
public class NimbusLiveKitTokenIssuer implements LiveKitTokenIssuer {

    private final LiveKitTokenProperties properties;

    public NimbusLiveKitTokenIssuer(LiveKitTokenProperties properties) {
        this.properties = properties;
    }

    @Override
    public LiveKitTokenIssueResult issue(LiveKitTokenIssueCommand command) {
        try {
            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plusSeconds(properties.getTokenExpirationSeconds());
            JWSSigner signer =
                    new MACSigner(properties.getApiSecret().getBytes(StandardCharsets.UTF_8));

            JWTClaimsSet claims =
                    new JWTClaimsSet.Builder()
                            // LiveKit access token은 iss에 API key를 넣어 서버가 발급 주체를 검증한다.
                            .issuer(properties.getApiKey())
                            .subject(command.participantIdentity())
                            .claim("name", command.participantName())
                            // 클라이언트와 서버 시계 차이 때문에 즉시 접속 실패하지 않도록 약간 과거 시각을 허용한다.
                            .notBeforeTime(Date.from(issuedAt.minusSeconds(30)))
                            .issueTime(Date.from(issuedAt))
                            .expirationTime(Date.from(expiresAt))
                            .claim(
                                    "video",
                                    Map.of(
                                            "room", command.roomName(),
                                            "roomJoin", true,
                                            "canPublish", true,
                                            "canSubscribe", true,
                                            "canPublishData", true))
                            .build();

            SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJwt.sign(signer);

            return new LiveKitTokenIssueResult(
                    properties.getServerUrl(), signedJwt.serialize(), issuedAt, expiresAt);
        } catch (Exception exception) {
            throw new IllegalStateException("LiveKit join token generation failed", exception);
        }
    }
}
