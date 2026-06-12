# 개인 워크스페이스 API 설계 및 구현

## 목적

`docs/api-spec.md`의 `12. Workspace API` 중 개인 워크스페이스 영역에 대한
Application(UseCase) 계층과 app-api(Controller) 계층을 구현한다.

도메인 모델, Repository Port, JPA Entity, Persistence Adapter는
`tasks/personal-workspace-domain.md` 작업으로 이미 존재한다. 본 작업은 그 위에
UseCase / Command / Result / Controller / Request / Response DTO를 추가해
실제 HTTP API를 노출한다.

관련 요구사항: 개인 일정, 동료 일정 구독, 백업 자료/북마크, 개인 드라이브, 개인 메모.

## 관련 문서

- `../AGENTS.md`
- `../docs/architecture.md`, `../docs/conventions.md`
- `docs/architecture.md`, `docs/conventions.md`, `docs/api-spec.md`
- `tasks/personal-workspace-domain.md`

## 설계 원칙

- 계층 흐름: Controller → UseCase → Domain → Repository Port → Infrastructure Adapter.
- 컨트롤러는 `com.meetbowl.domain..`, `com.meetbowl.infrastructure..`에 의존하지 않는다.
  (ArchUnit `ApiArchitectureTest`로 강제됨) → API DTO/Result는 도메인 enum 대신 String을 노출한다.
- API Request DTO를 UseCase에 직접 전달하지 않고 Command로 변환한다.
- 인증 사용자 식별은 `@CurrentUser AuthenticatedUser`로 받고, 소유자(Owner) 권한은 UseCase에서 검증한다.
- 모든 엔드포인트는 `@RequireUserOrAdmin`으로 User/Admin만 접근하도록 제한한다. (Guest 노출 금지)
- 시간 값은 `Instant`/UTC를 사용하고, 서버 생성 시각이 필요한 흐름은 주입한 `Clock`을 사용한다.

## API 계약

기본 경로: `/api/v1/workspace`

### 개인 일정 (calendar)

| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/workspace/calendar?from=&to=` | 조회 기간 내 내 일정 + 구독한 동료 일정 조회 | User/Admin |
| POST | `/workspace/calendar/events` | 개인 일정 등록 | User/Admin |
| PATCH | `/workspace/calendar/events/{eventId}` | 개인 일정 수정 | Owner |
| DELETE | `/workspace/calendar/events/{eventId}` | 개인 일정 삭제 | Owner |
| GET | `/workspace/calendar/subscriptions` | 동료 일정 구독 목록 조회 | User/Admin |
| POST | `/workspace/calendar/subscriptions` | 동료 일정 구독 등록 | User/Admin |
| DELETE | `/workspace/calendar/subscriptions/{subscriptionId}` | 동료 일정 구독 해제 | Owner |

- `GET /workspace/calendar`는 `findVisibleByUserIdAndPeriod`를 사용해 본인 일정과 구독한
  동료 일정을 함께 반환한다. `from`/`to`는 ISO-8601 UTC이며 `from < to`여야 한다.
- 회의에서 투영된 일정(`source = MEETING`)은 개인 캘린더에서 직접 수정/삭제할 수 없다.
  (도메인이 `COMMON_FORBIDDEN`으로 차단)

### 백업 자료 (backups)

| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/workspace/backups` | 백업 자료 목록 조회 | User/Admin |
| GET | `/workspace/backups/search?keyword=` | 백업 자료 검색 | User/Admin |
| POST | `/workspace/backups/{backupId}/bookmark` | 북마크 등록 | Owner |
| DELETE | `/workspace/backups/{backupId}/bookmark` | 북마크 해제 | Owner |

- 목록/검색 응답에는 사용자의 북마크 여부(`bookmarked`)를 함께 내려준다.
- 북마크 등록/해제는 멱등하게 동작한다. (중복 등록/없는 북마크 해제는 성공 처리)

### 개인 드라이브 (drive)

| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/workspace/drive/files` | 활성 파일 목록 조회 | User/Admin |
| POST | `/workspace/drive/files` | 파일 메타데이터 등록 | User/Admin |
| GET | `/workspace/drive/files/{fileId}` | 파일 다운로드 정보 조회 | Owner |
| DELETE | `/workspace/drive/files/{fileId}` | 파일 삭제(soft delete) | Owner |

- Object Storage 바이너리 업로드 어댑터는 아직 없으므로, 본 작업은 파일 메타데이터만
  저장한다. 업로드 시 서버가 `personal-drive/{userId}/{uuid}` 형식의 `storageKey`를 생성한다.
  실제 바이너리 업로드/다운로드(presigned URL)는 Object Storage Adapter 연동 후속 작업이다.

### 개인 메모 (memos)

| Method | Path | 설명 | 권한 |
|---|---|---|---|
| GET | `/workspace/memos` | 메모 목록 조회 | User/Admin |
| POST | `/workspace/memos` | 메모 작성 | User/Admin |
| PATCH | `/workspace/memos/{memoId}` | 메모 수정 | Owner |
| DELETE | `/workspace/memos/{memoId}` | 메모 삭제 | Owner |

## 구현 파일

### application

`application/src/main/java/com/meetbowl/application/personalworkspace`

- `calendar/`: `CalendarEventResult`, `GetCalendarUseCase`, `CreateCalendarEventCommand`,
  `CreateCalendarEventUseCase`, `UpdateCalendarEventCommand`, `UpdateCalendarEventUseCase`,
  `DeleteCalendarEventUseCase`, `CalendarSubscriptionResult`, `GetCalendarSubscriptionsUseCase`,
  `SubscribeCalendarCommand`, `SubscribeCalendarUseCase`, `UnsubscribeCalendarUseCase`
- `backup/`: `BackupResult`, `GetBackupsUseCase`, `SearchBackupsUseCase`,
  `AddBackupBookmarkUseCase`, `RemoveBackupBookmarkUseCase`
- `drive/`: `DriveFileResult`, `GetDriveFilesUseCase`, `RegisterDriveFileCommand`,
  `RegisterDriveFileUseCase`, `GetDriveFileUseCase`, `DeleteDriveFileUseCase`
- `memo/`: `MemoResult`, `GetMemosUseCase`, `CreateMemoCommand`, `CreateMemoUseCase`,
  `UpdateMemoCommand`, `UpdateMemoUseCase`, `DeleteMemoUseCase`

### app-api

`app-api/src/main/java/com/meetbowl/api/personalworkspace`

- `WorkspaceCalendarController`, `WorkspaceBackupController`,
  `WorkspaceDriveController`, `WorkspaceMemoController`
- 각 Controller의 `dto/` 패키지에 Request/Response DTO

## 검증

- `./gradlew :application:test :app-api:test`
- ArchUnit 아키텍처 테스트(api/application) 통과

## 후속 작업

- 드라이브 실제 바이너리 업로드/다운로드를 Object Storage Adapter와 연결 (multipart, presigned URL)
- 회의 생성/수정/취소 흐름과 개인 캘린더 투영 동기화 UseCase 연동
- 백업 자료 생성 흐름(메일/회의록/드라이브 → 백업) UseCase 연동
- 목록 API 페이지네이션이 필요해지면 `PageResponse` 적용
