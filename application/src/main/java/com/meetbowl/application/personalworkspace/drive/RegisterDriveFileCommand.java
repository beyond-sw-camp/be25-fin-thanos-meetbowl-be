package com.meetbowl.application.personalworkspace.drive;

import java.util.UUID;

/**
 * 개인 드라이브 파일 업로드 UseCase의 입력 모델이다.
 *
 * <p>storageKey와 신뢰할 Content-Type은 서버가 생성·판정한다. 파일 바이트는 DB가 아닌 Object Storage에만 저장한다.
 */
public record RegisterDriveFileCommand(
        UUID ownerUserId, UUID organizationId, String originalFileName, byte[] content) {

    public RegisterDriveFileCommand {
        content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }
}
