package com.meetbowl.infrastructure.persistence.auth;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.meetbowl.application.auth.JwtTokenProvider;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Component
public class NimbusJwtTokenProvider implements JwtTokenProvider {

    private final byte[] secret;
    private final long expirationSeconds;

    public NimbusJwtTokenProvider(
            @Value("${meetbowl.security.jwt.secret}") String secret,
            @Value("${meetbowl.security.jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.secret = secret.getBytes();
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public String createToken(String subject, Map<String, Object> claims) {
        try {
            JWSSigner signer = new MACSigner(secret);
            Instant now = Instant.now();

            JWTClaimsSet.Builder claimsBuilder =
                    new JWTClaimsSet.Builder()
                            .issuer("meetbowl")
                            .issueTime(Date.from(now))
                            .expirationTime(Date.from(now.plusSeconds(expirationSeconds)))
                            .subject(subject);

            claims.forEach(claimsBuilder::claim);

            SignedJWT signedJWT =
                    new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build());

            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("JWT generation failed", e);
        }
    }

    @Override
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
