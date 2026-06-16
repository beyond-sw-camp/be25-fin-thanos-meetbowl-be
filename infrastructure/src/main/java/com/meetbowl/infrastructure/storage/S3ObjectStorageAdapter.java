package com.meetbowl.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.storage.ObjectStoragePort;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** 개인/공유 파일 원본을 S3 호환 저장소에 업로드하는 Adapter다. */
@Component
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectStorageAdapter(
            S3Client s3Client, @Value("${meetbowl.s3.bucket:meetbowl-files}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void upload(String storageKey, String contentType, byte[] content) {
        try {
            // Content-Type을 객체 메타데이터에 남겨 AI가 파일 형식에 맞는 추출 방식을 선택할 수 있게 한다.
            PutObjectRequest request =
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageKey)
                            .contentType(contentType)
                            .contentLength((long) content.length)
                            .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (SdkException exception) {
            // 파일 원본이나 자격증명 등 민감 정보는 로그·응답에 노출하지 않는다.
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "파일 저장소 업로드에 실패했습니다.");
        }
    }
}
