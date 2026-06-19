package com.meetbowl.application.sharedworkspace;

import java.util.UUID;

/**
 * 공유 자료 업로드 입력이다. 파일 원본 바이트를 받아 서버가 Object Storage에 저장하고, 신뢰할 Content-Type과 storageKey는 서버가
 * 판정·생성한다. organizationId는 색인 메타데이터용으로 업로더의 조직을 담는다.
 */
public record UploadSharedWorkspaceFileCommand(
        UUID workspaceId,
        UUID uploaderUserId,
        UUID organizationId,
        String originalFileName,
        byte[] content) {

    public UploadSharedWorkspaceFileCommand {
        content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }
}
