package com.meetbowl.domain.storage;

import java.io.InputStream;

/**
 * Object Storage에서 내려받은 파일 원본 스트림과 응답에 필요한 메타데이터다.
 *
 * <p>원본을 byte[]로 메모리에 적재하면 대용량/동시 다운로드 시 OOM 위험이 있어, 스트림으로 흘려보낸다. {@code content}는 호출 측(컨트롤러)이 전송을
 * 마친 뒤 닫을 책임을 진다.
 */
public record StoredObject(InputStream content, String contentType, long contentLength) {}
