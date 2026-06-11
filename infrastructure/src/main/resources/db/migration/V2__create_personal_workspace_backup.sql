-- 개인 워크스페이스 백업 도메인 스키마.
-- 로컬은 ddl-auto=update로 자동 생성되지만, 운영(ddl-auto=validate)에서도 존재하도록 마이그레이션으로 명시한다.
-- 이미 ddl-auto로 생성된 로컬 DB에도 안전하게 적용되도록 IF NOT EXISTS 가드를 둔다.

CREATE TABLE IF NOT EXISTS personal_workspace_backups (
    id BINARY(16) NOT NULL,
    owner_user_id BINARY(16) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id BINARY(16) NOT NULL,
    title VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NULL,
    body MEDIUMTEXT NULL,
    backed_up_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_personal_workspace_backup_source UNIQUE (owner_user_id, source_type, source_id),
    INDEX idx_personal_workspace_backup_owner (owner_user_id)
);

-- 기존(로컬 ddl-auto) 환경에는 body 컬럼이 없을 수 있어 보강한다.
ALTER TABLE personal_workspace_backups ADD COLUMN IF NOT EXISTS body MEDIUMTEXT NULL AFTER summary;

CREATE TABLE IF NOT EXISTS personal_workspace_backup_bookmarks (
    id BINARY(16) NOT NULL,
    owner_user_id BINARY(16) NOT NULL,
    backup_id BINARY(16) NOT NULL,
    bookmarked_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_personal_workspace_backup_bookmark UNIQUE (owner_user_id, backup_id)
);

-- 첨부 스냅샷. 원본 메일/첨부는 보존 정책으로 삭제되므로 백업과 독립적으로 보관한다.
-- object_key는 파일 실체(S3 객체)를 가리키며, 같은 원본을 여러 유저가 백업할 수 있어 유니크 제약은 두지 않는다.
CREATE TABLE IF NOT EXISTS personal_workspace_backup_attachments (
    id BINARY(16) NOT NULL,
    backup_id BINARY(16) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pw_backup_attachment_backup
        FOREIGN KEY (backup_id) REFERENCES personal_workspace_backups (id),
    INDEX idx_pw_backup_attachment_backup (backup_id)
);
