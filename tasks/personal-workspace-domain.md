# 개인 워크스페이스 도메인 모델 및 엔티티 설계

## 목적

`docs/api-spec.md`의 PersonalWorkspace API 중 개인 워크스페이스 영역을 구현하기 위한 도메인 모델, Repository Port, JPA Entity, Persistence Adapter 기반을 추가한다.

대상 기능:

- 개인 일정 조회/등록/수정/삭제
- 동료 일정 구독 조회/등록/해제
- 백업 자료 조회/검색
- 백업 자료 북마크 등록/해제
- 개인 드라이브 파일 조회/업로드/다운로드/삭제
- 개인 메모 조회/작성/수정/삭제

## 관련 문서

- `../AGENTS.md`
- `../docs/architecture.md`
- `../docs/conventions.md`
- `docs/architecture.md`
- `docs/conventions.md`
- `docs/api-spec.md`

## 설계 원칙

- 개인 워크스페이스는 `meetbowl-be`의 업무 도메인으로 `personalworkspace` 패키지에 둔다.
- 도메인 모델은 JPA Entity를 알지 않는다.
- Application 계층은 JPA Repository가 아니라 Domain Repository Port에 의존한다.
- Infrastructure 계층에서 JPA Entity와 Domain Model 간 변환을 담당한다.
- 다른 기능의 Entity/Repository를 직접 참조하지 않고 `userId`, `meetingId`, `mailId`, `minutesId` 등 UUID 식별자로만 연결한다.
- 파일 원본은 DB에 저장하지 않고 S3 호환 스토리지의 `storageKey`와 파일 메타데이터만 저장한다.
- Enum은 `EnumType.STRING`으로 저장하고 ordinal 저장은 사용하지 않는다.

## 도메인 모델

추가 위치:

- `domain/src/main/java/com/meetbowl/domain/personalworkspace`

모델:

- `PersonalWorkspaceCalendarEvent`
- `PersonalWorkspaceCalendarSubscription`
- `PersonalWorkspaceBackup`
- `PersonalWorkspaceBackupBookmark`
- `PersonalWorkspaceDriveFile`
- `PersonalWorkspaceMemo`

Enum:

- `CalendarEventSource`
- `PersonalWorkspaceBackupSourceType`

Repository Port:

- `PersonalWorkspaceCalendarEventRepositoryPort`
- `PersonalWorkspaceCalendarSubscriptionRepositoryPort`
- `PersonalWorkspaceBackupRepositoryPort`
- `PersonalWorkspaceBackupBookmarkRepositoryPort`
- `PersonalWorkspaceDriveFileRepositoryPort`
- `PersonalWorkspaceMemoRepositoryPort`

## Persistence 모델

추가 위치:

- `infrastructure/src/main/java/com/meetbowl/infrastructure/persistence/personalworkspace`

JPA Entity:

- `PersonalWorkspaceCalendarEventEntity`
- `PersonalWorkspaceCalendarSubscriptionEntity`
- `PersonalWorkspaceBackupEntity`
- `PersonalWorkspaceBackupBookmarkEntity`
- `PersonalWorkspaceDriveFileEntity`
- `PersonalWorkspaceMemoEntity`

Spring Data Repository:

- `SpringDataPersonalWorkspaceCalendarEventRepository`
- `SpringDataPersonalWorkspaceCalendarSubscriptionRepository`
- `SpringDataPersonalWorkspaceBackupRepository`
- `SpringDataPersonalWorkspaceBackupBookmarkRepository`
- `SpringDataPersonalWorkspaceDriveFileRepository`
- `SpringDataPersonalWorkspaceMemoRepository`

Adapter:

- `JpaPersonalWorkspaceCalendarEventRepositoryAdapter`
- `JpaPersonalWorkspaceCalendarSubscriptionRepositoryAdapter`
- `JpaPersonalWorkspaceBackupRepositoryAdapter`
- `JpaPersonalWorkspaceBackupBookmarkRepositoryAdapter`
- `JpaPersonalWorkspaceDriveFileRepositoryAdapter`
- `JpaPersonalWorkspaceMemoRepositoryAdapter`

JPA 설정:

- `PersonalWorkspaceJpaConfig`

## 주요 테이블

- `personal_workspace_calendar_events`
- `personal_workspace_calendar_subscriptions`
- `personal_workspace_backups`
- `personal_workspace_backup_bookmarks`
- `personal_workspace_drive_files`
- `personal_workspace_memos`

## 검증한 도메인 규칙

- 개인 일정 제목은 필수다.
- 개인 일정 시작 시각은 종료 시각보다 이전이어야 한다.
- 회의 기반 일정은 원본 회의 식별자인 `sourceId`가 필요하다.
- 회의 기반 일정은 회의 정보의 사용자별 투영 데이터이며 개인 캘린더에서 직접 수정하거나 삭제하지 않는다.
- 동일 사용자에게 동일 회의 일정이 중복 생성되지 않도록 `(owner_user_id, source, source_id)` 유니크 제약을 둔다.
- 자기 자신의 일정은 구독할 수 없다.
- 개인 드라이브 파일은 원본 파일이 아니라 파일명, 크기, Content-Type, 저장 경로 메타데이터만 가진다.
- 개인 드라이브 파일 삭제는 `deletedAt` 기반 soft delete로 표현한다.
- 메모 수정 시각은 생성 시각보다 이전일 수 없다.

## 테스트

추가 테스트:

- `domain/src/test/java/com/meetbowl/domain/personalworkspace/PersonalWorkspaceCalendarEventTest.java`
- `domain/src/test/java/com/meetbowl/domain/personalworkspace/PersonalWorkspaceCalendarSubscriptionTest.java`
- `domain/src/test/java/com/meetbowl/domain/personalworkspace/PersonalWorkspaceDriveFileTest.java`
- `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/personalworkspace/JpaPersonalWorkspaceRepositoryAdapterTest.java`

실행 결과:

```text
./gradlew :domain:spotlessApply :infrastructure:spotlessApply :domain:test :infrastructure:test
BUILD SUCCESSFUL

./gradlew build
BUILD SUCCESSFUL
```

## 후속 작업

- 회의 생성·수정·취소 흐름과 개인 캘린더 투영 동기화 UseCase 추가
- PersonalWorkspace API별 UseCase/Command/Result 추가
- Controller Request/Response DTO 추가
- Owner 권한 검증 적용
- 파일 업로드는 Object Storage Adapter와 연결
- DB 마이그레이션 도구가 정해지면 위 테이블 기준 migration 작성
