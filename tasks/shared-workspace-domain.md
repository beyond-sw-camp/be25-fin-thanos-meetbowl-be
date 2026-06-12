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
- 공유 파일 최초 등록 시 `v.1.0.0` 버전 이력을 파일 메타데이터와 같은 트랜잭션에서 자동 생성한다.
- 이후 버전은 `major.minor.patch` 형식으로 수정자가 지정하며 현재 버전보다 큰 값만 허용한다.
- 버전 저장 시 파일 행을 잠금 조회하고 요청의 기대 버전과 DB 현재 버전을 다시 비교한다.
- 기대 버전이 다르면 `409 Conflict`로 최신 버전을 확인한 후 재시도하도록 안내한다.
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
- 추방 또는 자진 탈퇴한 멤버는 기존 멤버 행을 `ACTIVE`로 재활성화해 다시 초대할 수 있다.
- 퇴사자는 재초대할 수 없다. 이 검증은 사용자 도메인의 재직 상태 조회 Port를 사용하는 초대 UseCase에서 수행한다.
- 공유 파일은 원본 파일이 아니라 파일명, 크기, Content-Type, 저장 경로 메타데이터만 가진다.
- 공유 파일 삭제는 `deletedAt` 기반 soft delete로 표현한다.
- 새 파일 버전은 현재 버전보다 커야 한다.
- 파일 버전 변경 메모는 원본 파일을 변경하지 않고 버전 메타데이터만 수정한다.
- 삭제된 워크스페이스와 파일은 일반 조회 및 변경 대상에서 제외한다.
- 공유 워크스페이스 기본 공개 범위는 `MEMBERS_ONLY`이며 생성자와 활성 참여자만 접근한다.
- `ORGANIZATION` 공개 전환은 별도 Owner/Admin 권한 검증을 통과한 경우에만 허용한다.

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
- 사용자 도메인 구현 후 재직 상태 조회 Port를 연결해 퇴사자 초대를 차단
- 공유 파일 업로드는 Object Storage Adapter와 연결
- 공유 워크스페이스 전 직원 공개 범위는 조직/관리자 정책과 연결
- 배포 준비 시 스키마 버전 관리 도구를 결정하고 운영용 변경 SQL 작성
- 운영용 변경 SQL에 워크스페이스-멤버, 워크스페이스-파일, 파일-버전 FK와 조회 인덱스 포함
