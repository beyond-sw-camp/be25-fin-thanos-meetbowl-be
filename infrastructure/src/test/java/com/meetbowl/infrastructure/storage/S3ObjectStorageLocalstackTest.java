package com.meetbowl.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/** 실제 LocalStack S3에 파일을 저장하고 다시 읽어 Object Storage 연결을 검증한다. */
@EnabledIfEnvironmentVariable(named = "RUN_S3_E2E", matches = "true")
class S3ObjectStorageLocalstackTest {

    @Test
    void uploadAndDownloadFile() {
        String bucket = "meetbowl-files";
        String key = "e2e/" + UUID.randomUUID() + "/sample.txt";
        byte[] content = "Meetbowl LocalStack S3".getBytes();

        try (S3Client client = localstackClient()) {
            ensureBucket(client, bucket);
            S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(client, bucket);

            adapter.upload(key, "text/plain", content);
            byte[] downloaded =
                    client.getObjectAsBytes(
                                    GetObjectRequest.builder().bucket(bucket).key(key).build())
                            .asByteArray();

            assertArrayEquals(content, downloaded);
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }
    }

    private S3Client localstackClient() {
        return S3Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .endpointOverride(URI.create("http://localhost:4566"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.builder()
                                        .accessKeyId("test")
                                        .secretAccessKey("test")
                                        .build()))
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private void ensureBucket(S3Client client, String bucket) {
        try {
            client.headBucket(builder -> builder.bucket(bucket));
        } catch (NoSuchBucketException exception) {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }
}
