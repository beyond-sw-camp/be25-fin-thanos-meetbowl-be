package com.meetbowl.infrastructure.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

/** LocalStack과 실제 AWS S3를 같은 코드로 사용하도록 S3Client를 구성한다. */
@Configuration
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(
            @Value("${meetbowl.s3.region:ap-northeast-2}") String region,
            @Value("${meetbowl.s3.endpoint:}") String endpoint,
            @Value("${meetbowl.s3.access-key:}") String accessKey,
            @Value("${meetbowl.s3.secret-key:}") String secretKey) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));

        if (!endpoint.isBlank()) {
            // LocalStack은 localhost 경로에 버킷명을 넣는 path-style 주소가 필요하다.
            builder.endpointOverride(URI.create(endpoint));
            builder.serviceConfiguration(
                    S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }

        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.builder()
                                    .accessKeyId(accessKey)
                                    .secretAccessKey(secretKey)
                                    .build()));
        } else {
            // 운영에서는 환경변수 키 대신 ECS/EC2 IAM Role 등 AWS 기본 자격증명 체인을 사용한다.
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
