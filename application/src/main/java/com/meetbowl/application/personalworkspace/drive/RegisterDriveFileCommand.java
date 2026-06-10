package com.meetbowl.application.personalworkspace.drive;

import java.util.UUID;

/**
 * 개인 드라이브 파일 메타데이터 등록 UseCase의 입력 모델이다.
 *
 * <p>storageKey는 클라이언트가 정하지 않고 서버가 생성하므로 Command에 포함하지 않는다.
 */
public record RegisterDriveFileCommand(
        UUID ownerUserId, String originalFileName, String contentType, long sizeBytes) {}
