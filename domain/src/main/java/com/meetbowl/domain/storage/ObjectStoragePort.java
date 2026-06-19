package com.meetbowl.domain.storage;

/** 파일 원본을 S3 호환 Object Storage에 저장하는 출력 포트다. */
public interface ObjectStoragePort {

    void upload(String storageKey, String contentType, byte[] content);
}
