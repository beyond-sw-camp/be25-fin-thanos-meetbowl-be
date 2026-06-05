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

## Profiles

기본 profile은 `local`이다. Flyway는 아직 사용하지 않고, 초기 개발 단계에서는 JPA `ddl-auto`를 profile별로 분리한다.

```text
local: ddl-auto=update
test: ddl-auto=create-drop
prod: ddl-auto=validate
```

로컬 MariaDB 접속 정보는 환경 변수로 덮어쓸 수 있다.

```bash
MEETBOWL_DB_URL=jdbc:mariadb://localhost:3306/meetbowl
MEETBOWL_DB_USERNAME=meetbowl
MEETBOWL_DB_PASSWORD=meetbowl
```

## Samples

`sampletask`는 실제 기능이 아니라 계층 구조 참고용 예시다.

```text
sample: InMemory adapter 예시
sample-jpa: JPA Entity / Spring Data Repository / Adapter 예시
```

기본 profile에서는 샘플 Controller와 샘플 JPA scan이 켜지지 않는다.
