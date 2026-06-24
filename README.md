# meetbowl-be

Meetbowl 메인 백엔드 서버입니다.

## Module Structure

```text
app-api         Spring Boot 실행 모듈, Controller, API DTO, Web/Security 설정
application     UseCase, 트랜잭션 경계, 업무 조율
domain          Domain Model, Value Object, Repository/Event Port
infrastructure  Persistence, Messaging, Redis, Object Storage, 외부 서버 Adapter
common          공통 응답, 예외, Event Envelope, 시간/ID 유틸
```

의존 방향은 `docs/architecture.md`와 `docs/conventions.md`를 기준으로 한다.

## Commands

```bash
./gradlew clean build
./gradlew :app-api:bootRun
```

Docker image build:

```bash
docker build -t meetbowl-be:local .
```

## Profiles

기본 profile은 `local`이다. 운영/로컬 스키마는 Flyway migration으로 관리하고, 테스트만 H2
`create-drop`을 유지한다.

```text
local: flyway migrate + ddl-auto=validate
test: ddl-auto=create-drop
prod: flyway migrate + ddl-auto=validate
prod: flyway migrate + ddl-auto=validate + 1회성 초기 계정(admin/user1/user2) 생성
```

로컬 MariaDB 접속 정보는 환경 변수로 덮어쓸 수 있다.

```bash
MEETBOWL_DB_URL=jdbc:mariadb://localhost:3306/meetbowl
MEETBOWL_DB_USERNAME=meetbowl
MEETBOWL_DB_PASSWORD=meetbowl
```

## Flyway

운영 기준 baseline migration은 `app-api/src/main/resources/db/migration`에서 관리한다.

현재 JPA 매핑 기준 SQL을 baseline으로 추출해야 할 때는 schema export profile을 사용한다.

```bash
./gradlew :app-api:bootRun --args='--spring.profiles.active=schema-export'
```

생성 위치:

```text
app-api/build/generated-schema/meetbowl-baseline.sql
```

추출한 SQL은 검토 후 `V1__baseline.sql`로 고정한다.

## Samples

`sampletask`는 실제 기능이 아니라 계층 구조 참고용 예시다.

```text
sample: InMemory adapter 예시
sample-jpa: JPA Entity / Spring Data Repository / Adapter 예시
```

기본 profile에서는 샘플 Controller와 샘플 JPA scan이 켜지지 않는다.

## Security

API 인증은 JWT Bearer Token을 기본으로 한다.

```http
Authorization: Bearer {accessToken}
```

Access Token은 짧은 수명의 JWT이며, Refresh Token과 로그아웃된 Access Token 상태는 Redis에서 TTL로 관리한다.

로컬 JWT 검증 secret은 환경 변수로 덮어쓸 수 있다. `prod` 프로필에서는 공개되지 않은
32 bytes 이상의 `MEETBOWL_JWT_SECRET` 환경 변수가 필수다.

```bash
MEETBOWL_JWT_SECRET=<random-secret-of-at-least-32-bytes>
```

초기 JWT claim 기준은 다음과 같다.

```text
sub: 사용자 UUID
organizationId: 조직 UUID
role: USER | ADMIN | GUEST | SYSTEM
displayName: 표시 이름
initialPasswordChangeRequired: 초기 비밀번호 변경 제한 토큰 여부
```

Controller는 JWT를 직접 파싱하지 않고 `@CurrentUser AuthenticatedUser`를 사용한다.

내부 서버 전용 API는 `X-Internal-Token` 헤더를 사용하며, 일반 로그인으로 `SYSTEM` JWT를
발급하지 않는다. `prod` 프로필에서는 공개되지 않은 32 bytes 이상의
`MEETBOWL_INTERNAL_TOKEN` 환경 변수가 필수다.

운영 배포 직후 기본 계정이 필요하면 `SPRING_PROFILES_ACTIVE=prod`로 실행한다.
`prod`는 운영 설정을 그대로 쓰면서 `admin`, `user1`, `user2`를 `1234` 비밀번호로 채우고,
동일 계정이 있으면 다시 만들지 않는다.

## Architecture Checks

`./gradlew check`는 ArchUnit으로 모듈 의존 방향을 검증한다.

```text
domain -> app-api/application/infrastructure 금지
application -> app-api/infrastructure 금지
app-api -> domain/infrastructure 금지
infrastructure -> app-api/application 금지
Controller -> Repository 직접 의존 금지
```
