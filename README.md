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
