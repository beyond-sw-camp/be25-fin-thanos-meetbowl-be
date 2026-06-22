package com.meetbowl.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.storage.ObjectStoragePort;
import com.meetbowl.domain.storage.StoredObject;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** 개인/공유 파일 원본을 S3 호환 저장소에 업로드하고 다운로드하는 Adapter다. */
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

    @Override
    public StoredObject download(String storageKey) {
        try {
            // 권한 검증은 Application 계층에서 끝낸 뒤 이 Adapter에는 확정된 storageKey만 전달한다.
            GetObjectRequest request =
                    GetObjectRequest.builder().bucket(bucket).key(storageKey).build();
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);
            GetObjectResponse response = responseBytes.response();
            byte[] content = responseBytes.asByteArray();
            String contentType =
                    response.contentType() == null || response.contentType().isBlank()
                            ? "application/octet-stream"
                            : response.contentType();
            long contentLength =
                    response.contentLength() == null ? content.length : response.contentLength();
            return new StoredObject(content, contentType, contentLength);
        } catch (NoSuchKeyException exception) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "파일 원본을 찾을 수 없습니다.");
        } catch (SdkException exception) {
            // S3 상세 오류, 버킷명, 자격증명 등은 클라이언트 응답에 노출하지 않는다.
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "파일 저장소 다운로드에 실패했습니다.");
        }
    }
}
