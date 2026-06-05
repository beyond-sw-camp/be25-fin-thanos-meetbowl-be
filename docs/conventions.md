# api-server Conventions

## 목적

이 문서는 `meetbowl-be`에서 사용하는 공통 개발 규칙을 정의한다.

모든 개발자와 AI Agent는 기능 구현 시 본 문서의 규칙을 준수해야 한다.

---

# 1. 공통 원칙

- 일관성을 우선한다.
- 기존 패턴을 우선 사용한다.
- 동일한 문제는 동일한 방식으로 해결한다.
- 새로운 패턴은 충분한 이유가 있을 때만 도입한다.
- 코드 길이보다 가독성을 우선한다.
- 명확한 이름을 사용한다.
- 계층 경계와 모듈 의존 방향을 기능 구현보다 우선한다.

---

# 2. 멀티모듈 규칙

`meetbowl-be`는 Gradle 멀티모듈 기반 모듈러 모놀리스로 구성한다.

기본 모듈:

```text
app-api
application
domain
infrastructure
common
```

의존 방향:

```text
app-api → application
app-api → common
application → domain
application → common
infrastructure → domain
infrastructure → common
domain → common
```

금지:

```text
domain → infrastructure
application → infrastructure
controller → repository
controller → entity
기능 간 entity/repository 직접 참조
```

---

# 3. 패키지 규칙

각 모듈 내부는 기능별 패키지로 분리한다.

예시:

```text
app-api/meeting
application/meeting
domain/meeting
infrastructure/persistence/meeting
```

권장 기능 패키지:

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
```

패키지명은 모두 소문자를 사용한다.

---

# 4. Java 네이밍 규칙

## 클래스명

PascalCase 사용.

예시:

```java
User
Meeting
MeetingParticipant
CreateMeetingUseCase
MeetingResponse
MeetingRepositoryPort
```

## 메서드명 / 변수명

camelCase 사용.

예시:

```java
createMeeting()
findMeeting()
meetingId
organizationId
createdAt
```

## 상수명

UPPER_SNAKE_CASE 사용.

예시:

```java
MAX_FILE_SIZE
DEFAULT_PAGE_SIZE
ACCESS_TOKEN_EXPIRE_SECONDS
```

---

# 5. DTO 규칙

DTO는 계층별로 분리한다.

| 위치 | DTO | 예시 |
|---|---|---|
| app-api | Request DTO | `CreateMeetingRequest` |
| app-api | Response DTO | `MeetingDetailResponse` |
| application | Command DTO | `CreateMeetingCommand` |
| application | Query DTO | `FindMeetingQuery` |
| application | Result DTO | `MeetingResult` |
| infrastructure/messaging | Message DTO | `MeetingEndedMessage` |
| infrastructure/client | External DTO | `AiMinutesGenerateRequest` |

금지:

- Entity를 API 응답으로 직접 반환하지 않는다.
- API Request DTO를 UseCase 내부로 그대로 전달하지 않는다.
- Message DTO와 API DTO를 혼용하지 않는다.
- External Client DTO를 Domain/Application 계층에 노출하지 않는다.

변환 위치:

```text
Request DTO → Command: app-api
Result → Response DTO: app-api
Message DTO → Command: messaging adapter
Persistence Entity ↔ Domain: persistence adapter
External DTO ↔ Result/Command: client adapter
```

---

# 6. Controller 규칙

Controller는 HTTP 요청과 응답만 담당한다.

담당:

- Request DTO 검증
- 인증 사용자 정보 전달
- UseCase 호출
- Response DTO 반환

금지:

```text
비즈니스 규칙 작성
Repository 직접 호출
Entity 반환
트랜잭션 처리
try-catch로 공통 오류 응답 직접 생성
```

URL은 복수형 리소스를 사용한다.

권장:

```text
GET /meetings
POST /meetings
GET /users
POST /users
```

금지:

```text
/getMeeting
/createMeeting
/deleteMeeting
```

---

# 7. UseCase 규칙

UseCase는 하나의 사용자 행위 또는 시스템 행위를 표현한다.

예시:

```java
CreateMeetingUseCase
UpdateMeetingScheduleUseCase
EndMeetingUseCase
ApproveMinutesUseCase
SendMailUseCase
```

담당:

- 트랜잭션 경계
- 권한 검증 조율
- 도메인 모델 조립
- Port 호출
- 이벤트 발행 요청

UseCase는 Controller나 JPA Entity에 의존하지 않는다.

---

# 8. Domain 규칙

Domain은 업무 규칙의 중심이다.

예시 규칙:

- 같은 회의실은 같은 시간대에 중복 예약할 수 없다.
- 회의록은 검토자 승인 전 자동 공유할 수 없다.
- Guest는 회의 참여 외 내부 기능에 접근할 수 없다.
- User는 관리자 API에 접근할 수 없다.

Domain은 Infrastructure에 의존하지 않는다.

---

# 9. Repository / Port 규칙

Domain 또는 Application은 Repository Port에 의존한다.

예시:

```java
MeetingRepositoryPort
MinutesRepositoryPort
MailRepositoryPort
```

Infrastructure는 Port 구현체를 제공한다.

예시:

```java
JpaMeetingRepositoryAdapter
JpaMinutesRepositoryAdapter
```

Repository는 데이터 접근만 담당한다.

비즈니스 로직 작성 금지.

---

# 10. Entity / Database 규칙

## Entity 클래스명

단수형 사용.

권장:

```java
User
Meeting
MeetingParticipant
Mail
```

금지:

```java
Users
Meetings
```

## 테이블명 / 컬럼명

snake_case 사용.

예시:

```sql
meeting
meeting_participant
meeting_transcript
meeting_minutes
mail_attachment

created_at
updated_at
meeting_id
organization_id
```

## 기본키

모든 주요 테이블은 `id`를 PK로 사용한다.

```sql
id UUID PRIMARY KEY
```

## 외래키

```text
{target}_id
```

예시:

```text
user_id
meeting_id
organization_id
```

## Enum

DB 저장 시 문자열을 사용한다.

```java
@Enumerated(EnumType.STRING)
```

ORDINAL 저장 금지.

---

# 11. API 응답 규칙

공통 성공 응답:

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

공통 실패 응답:

```json
{
  "success": false,
  "error": {
    "code": "MEETING_NOT_FOUND",
    "message": "회의를 찾을 수 없습니다.",
    "details": []
  }
}
```

Validation 실패:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "요청 값이 올바르지 않습니다.",
    "details": [
      {
        "field": "startedAt",
        "reason": "시작 시간은 종료 시간보다 이전이어야 합니다."
      }
    ]
  }
}
```

---

# 12. 예외 / 에러 코드 규칙

예상 가능한 업무 오류는 `BusinessException` 계열로 처리한다.

권장 구조:

```java
BusinessException
  - ErrorCode
  - message
  - details
```

예외 종류:

```text
BusinessException
ValidationException
AuthenticationException
AuthorizationException
ExternalServiceException
```

ErrorCode 예시:

```text
COMMON_INVALID_REQUEST
COMMON_UNAUTHORIZED
COMMON_FORBIDDEN
COMMON_NOT_FOUND
COMMON_CONFLICT
COMMON_INTERNAL_ERROR

AUTH_INVALID_CREDENTIALS
AUTH_TOKEN_EXPIRED
AUTH_INITIAL_PASSWORD_CHANGE_REQUIRED

USER_NOT_FOUND
USER_ALREADY_EXISTS

MEETING_NOT_FOUND
MEETING_ROOM_ALREADY_RESERVED
MEETING_FORBIDDEN_GUEST_ACCESS

MINUTES_NOT_FOUND
MINUTES_REVIEW_REQUIRED
MINUTES_ALREADY_APPROVED

MAIL_NOT_FOUND
MAIL_FORBIDDEN_ACCESS

FILE_INVALID_EXTENSION
FILE_SIZE_EXCEEDED

AI_RAG_ACCESS_DENIED
AI_RESPONSE_VALIDATION_FAILED

STT_SESSION_NOT_FOUND
STT_PROVIDER_UNAVAILABLE
```

GlobalExceptionHandler에서 모든 예외를 공통 실패 응답으로 변환한다.

알 수 없는 예외는 `COMMON_INTERNAL_ERROR`로 응답하고 내부 로그에만 상세를 남긴다.

---

# 13. 파일 규칙

파일명 저장 시 UUID 또는 UUID 기반 object key를 사용한다.

예시:

```text
recordings/{meetingId}/{uuid}.webm
attachments/{mailId}/{uuid}.pdf
```

원본 파일명은 별도 컬럼에 저장한다.

예시:

```sql
original_file_name
stored_file_name
object_key
mime_type
size_bytes
```

파일 원본은 DB에 저장하지 않는다.

---

# 14. 트랜잭션 규칙

원칙:

- UseCase에서 트랜잭션 관리
- Controller에서 트랜잭션 금지
- Repository에서 업무 트랜잭션 경계 설정 금지

권장:

```java
@Transactional
public void execute() {
}
```

외부 API 호출은 DB 트랜잭션 내부에서 길게 수행하지 않는다.

---

# 15. 로깅 규칙

금지:

```java
e.printStackTrace();
System.out.println();
```

권장:

```java
log.info()
log.warn()
log.error()
```

로그 금지 대상:

```text
비밀번호
JWT
Refresh Token
API Key
메일 본문
회의 원문 전체
개인정보
```

---

# 16. 테스트 규칙

테스트 클래스명:

```java
CreateMeetingUseCaseTest
MeetingRepositoryAdapterTest
MeetingControllerTest
```

테스트 메서드명:

```java
createMeeting_success()
createMeeting_fail_when_duplicate()
approveMinutes_fail_when_not_reviewer()
```

패턴:

```text
행위_결과
행위_결과_when_조건
```

테스트 범위:

| 계층 | 테스트 |
|---|---|
| domain | 도메인 규칙 단위 테스트 |
| application | UseCase 테스트 |
| infrastructure | Adapter / Repository 테스트 |
| app-api | Controller / Security 테스트 |

---

# 17. Git 규칙

브랜치:

```text
feat/meeting-create
fix/login-error
refactor/meeting-usecase
docs/api-spec
```

커밋:

```text
feat: 회의 생성 기능 추가
fix: 로그인 오류 수정
refactor: 회의 UseCase 분리
test: 회의 생성 테스트 추가
docs: API 문서 수정
```

---

# 18. 금지사항

- Entity를 API DTO로 사용
- DTO에 비즈니스 로직 작성
- Controller에서 Repository 호출
- Controller에서 트랜잭션 처리
- Repository에서 비즈니스 로직 작성
- Domain에서 Infrastructure 참조
- Application에서 JPA Entity 직접 참조
- 다른 도메인 Entity 직접 참조
- 다른 도메인 Repository 직접 호출
- `printStackTrace` 사용
- `System.out.println` 사용
- Enum ORDINAL 저장
- API DTO와 Message DTO 혼용
- 검토자 승인 전 회의록 자동 공유
- Guest에게 내부 메일/워크스페이스/관리자 기능 노출
