package com.meetbowl.domain.storage;

/** Object Storage에서 내려받은 파일 원본과 응답에 필요한 메타데이터다. */
public record StoredObject(byte[] content, String contentType, long contentLength) {}
