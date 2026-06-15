# Meetbowl api-server 아키텍처

## 1. 목적

`meetbowl-be`는 Meetbowl의 메인 백엔드 서버다.

사용자 인증, 회원·조직, 회의실 예약, 회의, 회의록, 내부 메일, 워크스페이스, 관리자 기능의 중심 API를 제공한다.

프론트엔드는 기본적으로 `meetbowl-be`만 직접 호출한다.

`meetbowl-ai`, `meetbowl-stt`는 일반 사용자가 직접 호출하지 않는 내부 서버로 취급한다.

---

## 2. 아키텍처 형태

`meetbowl-be`는 단일 배포 Spring Boot 애플리케이션이다.

단, 내부 구조는 Gradle 멀티모듈 기반 모듈러 모놀리스로 구성한다.

목표:

- 계층별 책임을 모듈 의존성으로 강제한다.
- Controller에서 Repository를 직접 호출하지 못하게 한다.
- Domain이 Infrastructure에 의존하지 못하게 한다.
- 기능별 패키지를 유지해 향후 기능별 모듈 분리가 가능하게 한다.
- 협업 시 작업 위치와 책임 경계를 명확히 한다.

---

## 3. 권장 멀티모듈 구조

```text
meetbowl-be/
  settings.gradle
  build.gradle

  app-api/
  application/
  domain/
  infrastructure/
  common/
```

### app-api

Spring Boot 실행 모듈이다.

담당:

- Controller
- API Request/Response DTO
- Security 설정
- Web Config
- GlobalExceptionHandler
- OpenAPI/Swagger 설정
- UTC `Clock` 등 공통 런타임 Bean 설정
- Application bootstrap

### application

UseCase와 업무 조율 계층이다.

담당:

- UseCase
- 트랜잭션 경계
- 권한 검증 조율
- 여러 도메인 조립
- Port 호출
- 이벤트 발행 요청

### domain

도메인 규칙의 중심이다.

담당:

- Domain Model
- Value Object
- Domain Service
- Repository Port
- Event Port
- 업무 규칙

### infrastructure

외부 시스템 연동 구현체다.

담당:

- JPA Entity
- Spring Data JPA Repository
- Repository Adapter
- Redis Adapter
- 공통 RabbitMQ Envelope Publisher
- 도메인별 RabbitMQ Publisher/Consumer Adapter
- Object Storage Adapter
- ai-server Client
- stt-server Client

### common

공통 기반 모듈이다.

담당:

- 공통 응답
- 공통 예외
- ErrorCode
- 시간/ID 유틸
- 이벤트 이름 계약 상수
- Event Envelope
- 공통 로깅 상수

---

## 4. 모듈 의존 방향

허용 의존:

```text
app-api → application
app-api → common

application → domain
application → common

infrastructure → domain
infrastructure → common

domain → common
common → no dependency
```

금지 의존:

```text
domain → infrastructure
domain → app-api
application → infrastructure
application → app-api
common → 다른 모듈
```

필요 시 `application`은 domain port를 호출하고, 실제 구현은 `infrastructure` adapter가 제공한다.

---

## 5. 기능별 패키지 구조

각 모듈 내부는 기능별 패키지로 분리한다.

예시:

```text
app-api/src/main/java/com/meetbowl/api/meeting
application/src/main/java/com/meetbowl/application/meeting
domain/src/main/java/com/meetbowl/domain/meeting
infrastructure/src/main/java/com/meetbowl/infrastructure/persistence/meeting
```

권장 도메인 구분:

```text
auth
user
organization
meetingroom
meeting
transcript
minutes
mail
workspace
sharedworkspace
admin
notification
audit
file
common
```

기능 간 직접 Entity/Repository 참조는 금지한다.

다른 기능과 협력해야 하면 UseCase, Port, Event를 사용한다.

---

## 6. 책임 범위

`meetbowl-be`가 담당하는 책임은 다음과 같다.

- JWT 기반 로그인, 로그아웃, 인증 처리
- Admin/User/Guest 권한 제어
- 회원 계정 생성, 수정, 상태 관리
- 조직 기준 정보 관리
- 회의실 등록, 조회, 예약, 취소
- 회의 생성, 수정, 조회, 종료
- 회의 일정 수정 알림
- 회의 참석자 및 게스트 접근 제어
- STT Final Transcript 저장
- AI 회의록 초안 저장
- 회의록 검토자 지정
- 회의록 검토 요청/재알림
- 회의록 수정, 승인, 공유
- 내부 메일 작성, 발송, 조회, 삭제, 백업
- 개인 워크스페이스 일정, 백업, 메모 관리
- 공유 워크스페이스 파일 및 버전 관리
- 관리자 대시보드, 정책, 작업 로그 관리
- RabbitMQ 이벤트 발행 및 소비
- MariaDB 데이터 정합성 보장

`meetbowl-be`는 AI 추론, 실시간 STT 처리, LiveKit 음성 트랙 직접 처리, 벡터 검색 직접 수행을 담당하지 않는다.

---

## 7. 외부 시스템 관계

```text
meetbowl-fe
  ↓ REST API / realtime UI
meetbowl-be
  ↓ MariaDB
  ↓ Redis
  ↓ RabbitMQ
  ↓ Object Storage
  ↓ meetbowl-ai
  ↓ meetbowl-stt
```

### MariaDB

관계형 업무 데이터의 기준 저장소다.

저장 대상:

- 사용자
- 조직 기준 정보
- 회의실
- 회의 예약
- 회의
- 회의 참석자
- Guest 초대 코드
- STT Final Transcript
- 회의록
- 회의록 검토/승인 이력
- 내부 메일
- 첨부파일 메타데이터
- 워크스페이스 메타데이터
- 관리자 작업 로그

### Redis

짧은 수명의 상태와 캐시에 사용한다.

사용 예시:

- Admin 단일 세션 제어
- Refresh Token 또는 세션 상태
- 짧은 TTL의 초대 코드
- 알림 상태 캐시

Redis는 주요 업무 데이터의 영구 저장소로 사용하지 않는다.

### RabbitMQ

반드시 처리되어야 하는 비동기 작업 큐로 사용한다.

사용 예시:

- 회의 종료 후 AI 회의록 생성 요청
- STT Final Transcript 저장 요청
- 회의록 생성 완료 이벤트

### Object Storage

파일 원본 저장소다.

저장 대상:

- 메일 첨부파일
- 공유 워크스페이스 파일 버전

DB에는 파일 메타데이터와 object key만 저장한다.

---

## 8. 주요 처리 흐름

### 회의 생성/수정

```text
meetbowl-fe
  ↓ POST /meetings or PATCH /meetings/{meetingId}
app-api
  ↓
application meeting usecase
  ↓ 회의실 중복 검증
  ↓ 참석자 권한 검증
  ↓ 검토자 지정
  ↓ Meeting 저장
  ↓ MeetingParticipant 저장
  ↓ 개인 일정 자동 등록/수정
  ↓ 알림 예약 또는 이벤트 발행
```

### 회의 종료 후 회의록 생성

```text
회의 종료
  ↓
meetbowl-be
  ↓ meeting.ended 이벤트 발행
RabbitMQ
  ↓
meetbowl-ai
  ↓ AI 회의록 초안 생성
  ↓ minutes.generated 이벤트 발행
RabbitMQ
  ↓
meetbowl-be
  ↓ 회의록 초안 저장
  ↓ 검토자 알림 발송
```

### 회의록 검토/공유

```text
Reviewer
  ↓ PATCH /meetings/{meetingId}/minutes
meetbowl-be
  ↓ 회의록 요약/본문 수정
Reviewer
  ↓ POST /meetings/{meetingId}/minutes/approve
meetbowl-be
  ↓ 회의록 상태 APPROVED 변경
  ↓ document.index.requested 이벤트 발행
RabbitMQ
  ↓
meetbowl-ai
  ↓ 회의록 임베딩 및 Qdrant 색인
```

검토자 승인 전 AI 검색/RAG용 색인은 금지한다.

---

## 9. 권한 구조

기본 Role:

```text
Admin
User
Guest
```

리소스 단위 권한:

```text
Host
Participant
Reviewer
Owner
Member
System
Internal
```

예시:

- Admin은 관리자 페이지와 운영 기능에 접근할 수 있다.
- User는 회의, 메일, 워크스페이스 기능에 접근할 수 있다.
- Guest는 초대받은 회의 참여 기능만 사용할 수 있다.
- Reviewer는 지정된 회의록을 검토하고 승인할 수 있다.
- Owner는 본인이 생성한 리소스를 수정/삭제할 수 있다.

권한 검증은 Controller가 아니라 UseCase 또는 Security Layer에서 수행한다.

---

## 10. 이벤트 처리 원칙

이벤트 이름과 payload는 루트 `docs/event-contract.md`를 따른다.

이벤트를 임의로 변경하지 않는다.

이벤트 변경이 필요하면 다음을 함께 수정한다.

- root `docs/event-contract.md`
- root `docs/requirements-trace.md`
- `meetbowl-be/docs/api-spec.md`
- `meetbowl-ai/docs/api-spec.md`
- `meetbowl-stt/docs/api-spec.md`
- 관련 테스트

Consumer는 중복 메시지를 처리해도 문제가 없도록 멱등성을 보장한다.

---

## 11. 트랜잭션 원칙

하나의 업무 데이터 변경은 하나의 UseCase에서 트랜잭션으로 묶는다.

예시:

- 회의 생성 + 참석자 저장 + 개인 일정 등록
- 회의록 승인 + 공유 상태 변경 + 공유 이벤트 발행
- 메일 발송 + 수신자별 메일함 생성

외부 API 호출은 DB 트랜잭션 내부에서 길게 수행하지 않는다.

필요하면 Outbox 또는 이벤트 기반으로 분리한다.

---

## 12. 금지 사항

- Controller에 비즈니스 로직 작성 금지
- Controller에서 Repository 직접 호출 금지
- Entity를 API DTO로 사용 금지
- API DTO와 메시지 DTO 혼용 금지
- Domain이 Infrastructure에 의존 금지
- Application이 JPA Entity에 직접 의존 금지
- 도메인 간 Repository 직접 참조 금지
- 이벤트 payload 임의 변경 금지
- 파일 원본을 DB에 저장 금지
- Guest에게 내부 메일, 워크스페이스, 관리자 기능 노출 금지
- 검토자 승인 전 회의록 자동 공유 금지
