# Meetbowl API Server AGENTS

## 목적

본 문서는 `meetbowl-be`에서 작업하는 모든 개발자와 AI Agent가 반드시 따라야 하는 규칙을 정의한다.

---

## 필수 문서

작업 전 반드시 아래 문서를 읽는다.

```text
../AGENTS.md
../docs/architecture.md
../docs/conventions.md
../docs/event-contract.md
../docs/communication-decision.md
docs/architecture.md
docs/conventions.md
docs/api-spec.md
```

---

## 역할

`meetbowl-be`는 Meetbowl의 업무 시스템이다.

담당 기능:

- 인증
- 사용자
- 조직
- 회의실
- 회의
- 게스트 초대/참여 권한
- STT Final Transcript 저장
- 회의록 조회/검토/승인/공유
- 내부 메일
- 개인 워크스페이스
- 공유 워크스페이스
- 관리자 기능
- 알림
- RabbitMQ 이벤트 발행/소비
- MariaDB 데이터 정합성 보장

---

## 아키텍처 기준

`meetbowl-be`는 단일 배포 Spring Boot 애플리케이션이지만 내부는 Gradle 멀티모듈 기반 모듈러 모놀리스로 개발한다.

기본 모듈은 다음을 따른다.

```text
app-api
application
domain
infrastructure
common
```

각 모듈 내부는 기능별 패키지로 분리한다.

예시:

```text
application/meeting
application/minutes
domain/meeting
domain/minutes
infrastructure/persistence/meeting
infrastructure/messaging
```

---

## 계층 규칙

반드시 아래 흐름을 따른다.

```text
Controller
↓
UseCase
↓
Domain
↓
Repository Port
↓
Infrastructure Adapter
```

금지:

```text
Controller → Repository 직접 호출
Controller → Entity 반환
Domain → Infrastructure 의존
Application → JPA Entity 직접 의존
기능 간 Entity/Repository 직접 참조
```

기능 간 협력은 UseCase, Port, Event를 통해 수행한다.

---

## 데이터 소유권

`meetbowl-be`만 MariaDB에 접근할 수 있다.

다른 서버의 저장소에 직접 접근하지 않는다.

금지:

```text
Qdrant 직접 접근
LiveKit 직접 접근
ai-server DB 접근
stt-server DB 접근
```

Qdrant 검색은 `meetbowl-ai`에 요청한다.

LiveKit 음성 처리는 `meetbowl-stt`에 위임한다.

---

## 통신 규칙

통신 방식은 루트 `docs/communication-decision.md`를 따른다.

- 사용자 요청/조회/즉시 응답은 REST API를 사용한다.
- 반드시 처리되어야 하는 비동기 작업은 RabbitMQ를 사용한다.
- 서버 내부 실시간 이벤트 흐름은 Redis Stream을 사용한다.
- 회의 화면의 실시간 자막, 실시간 피드백, 실시간 채팅 전달은 LiveKit DataChannel을 기본으로 한다.

“REST보다 이벤트 우선”처럼 무조건적인 기준을 사용하지 않는다. 기능의 응답성, 저장 보장, 재시도 필요 여부에 따라 통신 방식을 선택한다.

---

## 이벤트 규칙

RabbitMQ 이벤트는 루트 `docs/event-contract.md`를 따른다.

임의 이벤트 추가 금지.

이벤트 변경이 필요하면 루트 `docs/event-contract.md`와 관련 서버 `docs/api-spec.md`를 함께 수정한다.

---

## DTO / 에러 규칙

DTO와 에러 응답은 루트 `docs/conventions.md`와 `meetbowl-be/docs/conventions.md`를 따른다.

필수:

- API DTO와 Message DTO를 분리한다.
- Request DTO를 UseCase 내부 계약으로 사용하지 않는다.
- Entity를 API 응답으로 반환하지 않는다.
- 모든 예외는 GlobalExceptionHandler에서 공통 실패 응답으로 변환한다.
- Controller에서 try-catch로 응답을 직접 만들지 않는다.

---

## 금지 사항

- STT 처리 구현
- LLM 호출 구현
- 임베딩 구현
- Qdrant 직접 접근
- LiveKit audio track 직접 처리
- 다른 서버 DB 접근
- 검토자 승인 전 회의록 자동 공유
- Guest에게 내부 메일, 워크스페이스, 관리자 기능 노출

---

## 구현 원칙

- 기능 구현 시 기존 패턴을 우선 따른다.
- 새로운 프레임워크나 구조를 임의 도입하지 않는다.
- 모든 API는 `docs/api-spec.md`를 기준으로 구현한다.
- 복잡한 기능은 필요 시 `tasks/*.md`로 작업 범위를 정리한다.
