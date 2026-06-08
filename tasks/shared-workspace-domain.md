# 공유 워크스페이스 도메인 모델 및 엔티티 설계

## 목적

`docs/api-spec.md`의 SharedWorkspace API 중 공유 워크스페이스 영역을 구현하기 위한 도메인 모델, Repository Port, JPA Entity, Persistence Adapter 기반을 추가한다.

대상 기능:

- 공유 워크스페이스 생성/조회/수정/삭제
- 공유 워크스페이스 멤버 초대/조회/추방/탈퇴
- 공유 워크스페이스 공개 범위 변경
- 공유 자료 조회/업로드/다운로드/삭제
- 공유 자료 버전 업로드/조회
- 공유 자료 버전 변경 메모 수정

## 관련 문서

- `../AGENTS.md`
- `../docs/architecture.md`
- `../docs/conventions.md`
- `docs/architecture.md`
- `docs/conventions.md`
- `docs/api-spec.md`

## 설계 원칙

- 공유 워크스페이스는 `meetbowl-be`의 업무 도메인으로 `sharedworkspace` 패키지에 둔다.
- 도메인 모델은 JPA Entity를 알지 않는다.
- Application 계층은 JPA Repository가 아니라 Domain Repository Port에 의존한다.
- Infrastructure 계층에서 JPA Entity와 Domain Model 간 변환을 담당한다.
- 다른 기능의 Entity/Repository를 직접 참조하지 않고 `organizationId`, `userId`, `workspaceId`, `fileId` 등 UUID 식별자로만 연결한다.
- 파일 원본은 DB에 저장하지 않고 S3 호환 스토리지의 `storageKey`와 파일 메타데이터만 저장한다.
- 공유 파일은 새 버전이 업로드되어도 기존 버전과 변경 이력을 보존한다.
- Enum은 `EnumType.STRING`으로 저장하고 ordinal 저장은 사용하지 않는다.

## 도메인 모델

추가 위치:

- `domain/src/main/java/com/meetbowl/domain/sharedworkspace`

모델:

- `SharedWorkspace`
- `SharedWorkspaceMember`
- `SharedWorkspaceFile`
- `SharedWorkspaceFileVersion`

Enum:

- `SharedWorkspaceVisibility`
- `SharedWorkspaceMemberRole`
- `SharedWorkspaceMemberStatus`

Repository Port:

- `SharedWorkspaceRepositoryPort`
- `SharedWorkspaceMemberRepositoryPort`
- `SharedWorkspaceFileRepositoryPort`
- `SharedWorkspaceFileVersionRepositoryPort`

## Persistence 모델

추가 위치:

- `infrastructure/src/main/java/com/meetbowl/infrastructure/persistence/sharedworkspace`

JPA Entity:

- `SharedWorkspaceEntity`
- `SharedWorkspaceMemberEntity`
- `SharedWorkspaceFileEntity`
- `SharedWorkspaceFileVersionEntity`

Spring Data Repository:

- `SpringDataSharedWorkspaceRepository`
- `SpringDataSharedWorkspaceMemberRepository`
- `SpringDataSharedWorkspaceFileRepository`
- `SpringDataSharedWorkspaceFileVersionRepository`

Adapter:

- `JpaSharedWorkspaceRepositoryAdapter`
- `JpaSharedWorkspaceMemberRepositoryAdapter`
- `JpaSharedWorkspaceFileRepositoryAdapter`
- `JpaSharedWorkspaceFileVersionRepositoryAdapter`

JPA 설정:

- `SharedWorkspaceJpaConfig`

## 주요 테이블

- `shared_workspaces`
- `shared_workspace_members`
- `shared_workspace_files`
- `shared_workspace_file_versions`

## 검증한 도메인 규칙

- 공유 워크스페이스 조직 ID와 소유자 ID는 필수다.
- 공유 워크스페이스 이름은 필수다.
- 공유 워크스페이스 공개 범위는 `MEMBERS_ONLY` 또는 `ORGANIZATION`으로 관리한다.
- 소유자 멤버는 제거할 수 없다.
- 공유 파일은 원본 파일이 아니라 파일명, 크기, Content-Type, 저장 경로 메타데이터만 가진다.
- 공유 파일 삭제는 `deletedAt` 기반 soft delete로 표현한다.
- 새 파일 버전 번호는 현재 버전보다 커야 한다.
- 파일 버전 변경 메모는 원본 파일을 변경하지 않고 버전 메타데이터만 수정한다.

## 테스트

추가 테스트:

- `domain/src/test/java/com/meetbowl/domain/sharedworkspace/SharedWorkspaceTest.java`
- `infrastructure/src/test/java/com/meetbowl/infrastructure/persistence/sharedworkspace/JpaSharedWorkspaceRepositoryAdapterTest.java`

실행 결과:

```text
./gradlew :domain:spotlessApply :infrastructure:spotlessApply :domain:test :infrastructure:test
BUILD SUCCESSFUL

./gradlew build
BUILD SUCCESSFUL
```

## 후속 작업

- SharedWorkspace API별 UseCase/Command/Result 추가
- Controller Request/Response DTO 추가
- Owner/Member/Admin 권한 검증 적용
- 공유 파일 업로드는 Object Storage Adapter와 연결
- 공유 워크스페이스 전 직원 공개 범위는 조직/관리자 정책과 연결
- DB 마이그레이션 도구가 정해지면 위 테이블 기준 migration 작성
