# Meetbowl api-server API 명세서

## 1. 역할

`meetbowl-be`는 Meetbowl의 메인 백엔드 서버다.

담당 범위:

- 인증/인가
- 사용자/조직 관리
- 회의실/회의 예약
- 회의 정보 관리
- 게스트 초대/참여 제어
- STT Final Transcript 저장
- 회의록 조회/검토/승인/공유
- 내부 메일
- 개인 워크스페이스
- 공유 워크스페이스
- 관리자 페이지
- 알림
- AI/STT 서버와의 내부 연동

프론트엔드는 원칙적으로 `meetbowl-be`만 직접 호출한다.

## 2. 공통 규칙

### Base URL

```text
/api/v1
```

### 인증 헤더

```http
Authorization: Bearer {accessToken}
```

### 내부 인증 헤더

```http
X-Internal-Token: {internalToken}
```

- 내부 서버 전용 API와 System 전용 API는 `X-Internal-Token`으로만 인증한다.
- `SYSTEM` 역할의 JWT 발급과 일반 로그인은 허용하지 않는다.
- 내부 토큰은 일반 사용자, 관리자, Guest API 인증에 사용할 수 없다.

### 공통 성공 응답

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

### 공통 실패 응답

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "오류 메시지",
    "details": []
  }
}
```

### Validation 실패 응답

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

### 권한 표기

| 권한 | 의미 |
|---|---|
| User | 일반 사용자 |
| Admin | 관리자 시스템 계정 |
| Host | 회의 주최자 |
| Participant | 회의 참석자 |
| Guest | 초대 코드/URL 기반 회의 참여자 |
| Reviewer | 회의록 검토자 |
| Owner | 리소스 소유자 |
| Member | 공유 워크스페이스 구성원 |
| System | 시스템 내부 호출 |
| Internal | 서버 간 내부 호출 |

---

## 3. 주요 Error Code

| Code | HTTP | 설명 |
|---|---:|---|
| `COMMON_INVALID_REQUEST` | 400 | 잘못된 요청 |
| `COMMON_UNAUTHORIZED` | 401 | 인증 필요 |
| `COMMON_FORBIDDEN` | 403 | 권한 부족 |
| `COMMON_NOT_FOUND` | 404 | 리소스 없음 |
| `COMMON_CONFLICT` | 409 | 상태 충돌 |
| `AUTH_INVALID_CREDENTIALS` | 401 | 로그인 정보 오류 |
| `AUTH_TOKEN_EXPIRED` | 401 | 토큰 만료 |
| `AUTH_REFRESH_TOKEN_INVALID` | 401 | Refresh Token이 유효하지 않거나 이미 사용됨 |
| `AUTH_INITIAL_PASSWORD_CHANGE_REQUIRED` | 403 | 초기 비밀번호 변경 필요 |
| `USER_NOT_FOUND` | 404 | 사용자 없음 |
| `MEETING_NOT_FOUND` | 404 | 회의 없음 |
| `MEETING_ROOM_ALREADY_RESERVED` | 409 | 회의실 중복 예약 |
| `MEETING_ROOM_UNAVAILABLE` | 409 | 사용 제한된 회의실(isAvailable=false) 예약 불가 |
| `MEETING_FORBIDDEN_GUEST_ACCESS` | 403 | 게스트 접근 불가 |
| `MINUTES_REVIEW_REQUIRED` | 409 | 검토자 승인 필요 |
| `MINUTES_ALREADY_APPROVED` | 409 | 이미 승인된 회의록 |
| `MAIL_NOT_FOUND` | 404 | 메일 없음 |
| `MAIL_FORBIDDEN_ACCESS` | 403 | 메일 접근 불가 |
| `MAIL_IDEMPOTENCY_CONFLICT` | 409 | 동일한 멱등성 키의 요청 내용 충돌 |
| `FILE_INVALID_EXTENSION` | 415 | 허용되지 않은 파일 형식 |
| `FILE_SIZE_EXCEEDED` | 413 | 파일 크기 초과 |
| `AI_RAG_ACCESS_DENIED` | 403 | AI 자료 접근 권한 없음 |
| `STT_SESSION_NOT_FOUND` | 404 | STT 세션 없음 |

---

## 4. Auth API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/auth/login` | 로그인 | Public |
| POST | `/auth/logout` | 로그아웃 | User/Admin |
| POST | `/auth/token/refresh` | Access/Refresh Token 재발급 및 Refresh Token Rotation | Public |
| POST | `/auth/password/change-initial` | 최초 로그인 초기 비밀번호 변경 | User |
| POST | `/auth/password/reset-request` | 비밀번호 재설정 요청 | User |
| POST | `/admin/users/{userId}/password/reset` | 관리자가 비밀번호를 `1234`로 초기화 | Admin |
| GET | `/auth/me` | 현재 로그인 사용자 정보 조회 | User/Admin |

로그인 성공 시 짧은 수명의 JWT Access Token과 opaque Refresh Token을 발급한다.

- 로그인 응답 `data.user.initialPasswordChangeRequired`는 비밀번호 변경 필요 여부를 나타낸다.

- Refresh Token은 원문을 저장하지 않고 SHA-256 해시를 Redis에 TTL과 함께 저장한다.
- Token 재발급 시 기존 Refresh Token을 폐기하고 새 Refresh Token을 발급한다.
- 로그아웃 시 Refresh Token을 폐기하고 현재 Access Token의 `jti`를 남은 만료 시간 동안 Redis blacklist에 저장한다.
- Access Token은 서명, 만료 시간과 함께 `issuer=meetbowl`을 검증하며 `iat`가 없는 토큰은 허용하지 않는다.
- 관리자에 의한 사용자 상태 변경과 비밀번호 초기화 시 해당 사용자의 모든 Refresh Token을 폐기하고, 변경 시각 이전에 발급된 Access Token을 거부한다.
- 초기 비밀번호 변경이 필요한 사용자는 `initialPasswordChangeRequired: true`인 제한 Access Token만 발급받으며
  Refresh Token은 발급받지 않는다.
- 관리자 비밀번호 초기화 응답에는 초기 비밀번호 원문 `1234`가 1회 포함되며, 이후에는 저장되지 않는다.
- 서비스 기본 관리자 계정은 seed 초기화 과정에서 `admin` 한 개를 제공하며, `한화 시스템` 계열사에 소속된다. `prod` 배포 시 함께 생성한다. 관리자 회원 관리 API로 추가 생성하거나 변경하지 않는다.
- 관리자 회원 관리 API가 생성하는 계정은 항상 `USER`이며, 기존 `ADMIN`·`SYSTEM` 계정은 수정, 상태 변경, 비밀번호 초기화 대상에서 제외한다.
- 제한 Access Token은 `/auth/password/change-initial`에만 사용할 수 있다.
- 초기 비밀번호 변경 완료 시 제한 Access Token을 폐기하고 정상 Access/Refresh Token을 발급한다.
- 시스템 계정은 로그인과 Refresh Token 재발급을 사용할 수 없으며 내부 토큰 인증만 사용한다.

---

## 5. User / Organization API

### 사용자

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/users/me` | 내 정보 조회 | User/Admin |
| PATCH | `/users/me` | 내 프로필 수정 | User/Admin |
| PATCH | `/users/me/password` | 내 비밀번호 변경 | User/Admin |
| GET | `/users` | 회원 목록 조회 | Admin |
| GET | `/users/{userId}` | 회원 상세 조회 | Admin |
| POST | `/users` | 회원 계정 생성 | Admin |
| PATCH | `/users/{userId}` | 회원 정보 수정 | Admin |
| PATCH | `/users/{userId}/status` | 회원 활성/비활성 관리 | Admin |
| POST | `/users/import` | 엑셀 기반 회원 일괄 업로드 | Admin |
| GET | `/users/export` | 회원/조직도 엑셀 다운로드 | Admin |
| GET | `/users/{userId}/simple-profile` | 조직도용 간단 회원 정보 조회 | User/Admin |
| GET | `/users/recipients/search` | 메일 수신자 및 소속 정보 검색 | User/Admin |

- 관리자 회원 계정 생성 시 초기 비밀번호는 항상 `1234`이며, DB에는 PasswordEncoder로 암호화된 해시만 저장한다.
- 관리자 회원 계정 생성 및 관리자 비밀번호 초기화 대상 사용자는 `initialPasswordChangeRequired: true`로 저장한다.
- 관리자 회원 계정 생성 시 `affiliateId`는 요청으로 받지 않으며, 현재 로그인한 관리자의 계열사(`organizationId`)를 자동 상속한다.
- 회원 생성 요청의 `departmentId`, `teamId`, `positionId`는 상속된 계열사 체계와 일치해야 하며, 다른 계열사의 부서/팀/직급을 지정하면 거절한다.
- 로컬 seed 계정 `admin`, `user1`, `user2`는 모두 `1234`를 사용하지만 `initialPasswordChangeRequired`를 강제로 `true`로 만들지 않는다.
- seed 계정은 모두 `한화 시스템` 계열사에 소속된다.
- 운영 `prod` 배포에서도 같은 seed 계정 구성을 재사용한다.

### 조직 기준 정보

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/organizations` | 조직 기준 정보 조회 | User/Admin |
| POST | `/organizations/departments` | 부서 등록 | Admin |
| PATCH | `/organizations/departments/{departmentId}` | 부서 수정 | Admin |
| DELETE | `/organizations/departments/{departmentId}` | 부서 삭제 | Admin |
| POST | `/organizations/positions` | 직급 등록 | Admin |
| PATCH | `/organizations/positions/{positionId}` | 직급 수정 | Admin |
| DELETE | `/organizations/positions/{positionId}` | 직급 삭제 | Admin |

- 직급은 계열사 소속 기준정보다. 직급 등록/수정 요청에는 `affiliateId`가 포함되어야 하며, 같은 직급명과 정렬 순서는 같은 계열사 안에서만 중복을 막는다.

---

## 6. User Settings API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/settings/me` | 개인 설정 조회 | User/Admin |
| PATCH | `/settings/me` | 회의 알림 시간, 회의록 미검토 알림 주기 수정 | User/Admin |

---

### `/settings/me` fields

`GET /api/v1/users/me/settings` and `PATCH /api/v1/users/me/settings` use the fields below.

```json
{
  "meetingStartReminderMinutes": 10,
  "minutesReviewReminderMinutes": 60
}
```

- `minutesReviewReminderMinutes` is the reminder interval for unreviewed meeting minutes.
- Allowed values are `60`, `120`, `180`, `240`.

## 7. Meeting Room API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/meeting-rooms` | 회의실 목록 조회 | User/Admin |
| GET | `/meeting-rooms/status` | 회의실별 현재 상태 조회 | User/Admin |
| GET | `/meeting-rooms/reservations` | 전체 회의실 예약 현황 조회 | User/Admin |
| GET | `/meeting-rooms/reservations/me` | 내 회의실 예약 현황 조회 | User/Admin |
| POST | `/meeting-rooms/reservations` | 회의실 예약 생성 | User/Admin |
| PATCH | `/meeting-rooms/reservations/{reservationId}` | 회의실 예약 수정 | Host/Admin |
| DELETE | `/meeting-rooms/reservations/{reservationId}` | 회의실 예약 취소 | Host/Admin |

### 관리자 회의실 관리

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/admin/meeting-rooms` | 회의실 등록 | Admin |
| PATCH | `/admin/meeting-rooms/{roomId}` | 회의실 수정 | Admin |
| DELETE | `/admin/meeting-rooms/{roomId}` | 회의실 삭제 | Admin |
| POST | `/admin/meeting-room-sites` | 사이트/건물 등록 | Admin |
| PATCH | `/admin/meeting-room-sites/{siteId}` | 사이트/건물 수정 | Admin |
| DELETE | `/admin/meeting-room-sites/{siteId}` | 사이트/건물 삭제 | Admin |

---

## 8. Meeting API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/meetings` | 회의 생성 | User/Admin |
| GET | `/meetings` | 내가 생성/참석하는 회의 목록 조회 | User/Admin |
| GET | `/meetings/{meetingId}` | 회의 상세 조회 | Participant/Admin |
| PATCH | `/meetings/{meetingId}` | 회의 일정, 회의실, 참석자, 검토자 수정 | Host/Admin |
| DELETE | `/meetings/{meetingId}` | 회의 취소 | Host/Admin |
| POST | `/meetings/{meetingId}/join` | LiveKit 회의 참여 정보 조회 | Participant/Guest |
| POST | `/meetings/{meetingId}/end` | 회의 종료 확정 | Host |
| POST | `/meetings/{meetingId}/transfer-host` | 회의 관리자 이전 | Host |
| POST | `/meetings/{meetingId}/invite-link` | 회의 초대 코드/URL 생성 | Host |
| POST | `/meetings/guest-join` | 게스트 초대 코드로 회의 참여 | Public |

회의 생성/수정 시 회의록 검토자를 지정할 수 있다.

회의 일정 수정 시 기존 참석자와 새 참석자에게 알림을 발송한다.

Guest는 해당 회의 참여에 필요한 API에만 접근할 수 있다.

### POST `/meetings/{meetingId}/join`

프론트엔드는 이 API를 통해서만 LiveKit join info/token을 발급받는다.

- 회의가 `ENDED` 상태면 `409 MEETING_ALREADY_ENDED`를 반환한다.
- 회의가 `CANCELLED` 상태면 `409 COMMON_CONFLICT`를 반환한다.
- 예약 회의는 `scheduledAt` 15분 전부터만 입장할 수 있으며, 더 이르면 `409 MEETING_JOIN_TOO_EARLY`를 반환한다.

- 브라우저는 LiveKit API Secret을 보유하지 않는다.
- 응답 `livekitUrl`, `token`, `roomName`을 그대로 사용해 `room.connect()`를 호출한다.
- 인증 사용자가 있으면 서버가 `user-{userId}` 규칙으로 participant identity를 결정한다.
- 인증 연동 전 개발 화면에서는 요청 `participantIdentity`를 fallback 값으로 사용할 수 있다.
- 응답에는 현재 회의 주최자 `hostUserId`도 포함되어 회의 화면에서 관리자 UI를 제어할 수 있다.

요청 예시:

```json
{
  "displayName": "이지연",
  "participantIdentity": "u-user"
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "meetingId": "3ef5f58f-50b2-4f0b-97bf-42e79d91ac39",
    "roomName": "meeting-3ef5f58f-50b2-4f0b-97bf-42e79d91ac39",
    "livekitUrl": "http://localhost:7880",
    "hostUserId": "31f73d71-c04e-4410-a98c-fdc15e918091",
    "participantIdentity": "u-user",
    "participantName": "이지연",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "issuedAt": "2026-06-12T01:00:00Z",
    "expiresAt": "2026-06-12T02:00:00Z"
  },
  "message": null
}
```

---

## 9. Transcript / STT Result API

`meetbowl-be`는 STT 자체를 수행하지 않는다. STT 처리는 `meetbowl-stt`가 담당하고, `meetbowl-be`는 최종 결과 저장과 조회를 담당한다.

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/meetings/{meetingId}/transcripts` | 회의 STT 원문 조회 | Participant/Admin |
| POST | `/internal/meetings/{meetingId}/transcripts/final` | STT Final Transcript 저장 | Internal |
| POST | `/internal/meetings/{meetingId}/end` | STT/시스템 기준 회의 종료 처리 | Internal |

Final Transcript 저장과 녹음 파일 메타데이터 저장의 운영 기본 경로는 RabbitMQ 이벤트 소비다.

- Final Transcript 저장: `transcript.final.created`
- 녹음 파일 메타데이터 저장: `recording.completed`

내부 API는 장애 대응, 수동 재처리, 테스트 용도로만 사용한다.

### GET `/meetings/{meetingId}/transcripts`

한 회의의 최종 STT 원문을 sequence 순서대로 조회한다.

- `segments`는 발화 단위 리스트다.
- `fullText`는 같은 회의의 `sourceText`를 순서대로 이어 붙인 전체 원문이다.
- 중간 `STREAMING`은 저장되지 않고 `FINALIZED`만 내려간다.

응답 예시:

```json
{
  "success": true,
  "data": {
    "meetingId": "3ef5f58f-50b2-4f0b-97bf-42e79d91ac39",
    "fullText": "첫 문장\n둘째 문장",
    "segments": [
      {
        "segmentId": "segment-1",
        "sequence": 1,
        "language": "KO",
        "sourceText": "첫 문장",
        "startedAtMs": 0,
        "endedAtMs": 500
      },
      {
        "segmentId": "segment-2",
        "sequence": 2,
        "language": "KO",
        "sourceText": "둘째 문장",
        "startedAtMs": 600,
        "endedAtMs": 1000
      }
    ]
  },
  "message": null
}
```

### POST `/internal/meetings/{meetingId}/end`

STT 서버나 내부 시스템이 세션 종료를 기준으로 회의를 종료 상태로 정리할 때 사용한다.

- 내부 토큰 인증이 필요하다.
- 회의 상태를 먼저 `ENDED`로 정리한다.
- 같은 DB 트랜잭션에 `meeting.ended` Outbox를 저장하고 `meetingEndedEventQueued=true`를 반환한다.
- Outbox Scheduler가 RabbitMQ publisher confirm을 확인하며 발행하고, 실패하면 지수 backoff로 재시도한다.
- 이미 종료된 회의는 멱등하게 처리하고 이벤트를 다시 발행하지 않는다.

---

## 10. Meeting Minutes API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/meetings/{meetingId}/minutes/generate` | AI 회의록 초안 생성 요청 | Host/Admin |
| GET | `/meetings/{meetingId}/minutes` | 회의록 조회 | Participant/Admin |
| PATCH | `/meetings/{meetingId}/minutes` | 회의록 요약/본문 검토 및 수정 | Reviewer |
| POST | `/meetings/{meetingId}/minutes/approve` | 회의록 승인, 참석자 자동 내부 메일 공유 및 AI 색인 요청 | Reviewer |
| POST | `/meetings/{meetingId}/minutes/share` | 미참석자에게 회의록 추가 공유 | Participant |
| GET | `/minutes` | 회의록 목록/검색 | User/Admin |
| POST | `/minutes/{minutesId}/favorite` | 개인 워크스페이스 회의록 즐겨찾기 등록 | User/Admin |
| DELETE | `/minutes/{minutesId}/favorite` | 개인 워크스페이스 회의록 즐겨찾기 해제 | User/Admin |

AI 회의록 초안 저장의 운영 기본 경로는 RabbitMQ `minutes.generated` 이벤트 소비다.

AI 서버는 RabbitMQ `meeting.ended`를 받은 뒤 시스템 전용
`GET /api/v1/internal/meetings/{meetingId}/minutes-generation-context`로 생성 Context를 조회한다.

- `X-Internal-Token` SYSTEM 인증이 필요하다.
- 회의 제목, 조직, Host, Reviewer, 참석자, 실제 시작·종료 시각을 반환한다.
- MariaDB의 Final Transcript segment를 sequence 순으로 정렬한 `segments`와, 이를 줄바꿈으로 결합한 `rawTranscript`를 함께 반환한다.
- Final Transcript가 없거나 Reviewer·조직 정보가 없으면 Context를 반환하지 않는다.
- 원문은 AI 입력으로만 사용하며 `minutes.content`에는 AI가 생성한 Tiptap 초안만 저장한다.
- `minutes.generated`는 `eventId` inbox로 멱등 처리하며 `DRAFT`만 재생성 결과로 교체할 수 있다.

`GET /minutes`는 `keyword` query parameter로 `summary`, `content`를 검색할 수 있다.
목록 응답에는 사용자별 즐겨찾기 여부를 나타내는 `favorite` 필드가 포함된다.
목록 항목은 `minutesId`, `meetingId`, `reviewerUserId`, `status`, `summary`, `approvedAt`, `favorite`와
화면 표시용 회의 메타데이터 `meetingTitle`, `meetingStartedAt`, `meetingEndedAt`, `attendeeCount`,
`reviewerName`, `reviewerDepartment`를 반환한다. `GET /meetings/{meetingId}/minutes` 상세 응답도 같은
회의 메타데이터와 `content` Tiptap JSON 문자열을 함께 반환한다.

내부 API는 장애 대응, 수동 재처리, 테스트 용도로만 사용한다.

회의록 상태:

```text
DRAFT
IN_REVIEW
APPROVED
SHARED
DELETION_SCHEDULED
```

검토자 승인 전 자동 공유와 수동 공유는 금지한다.

`POST /meetings/{meetingId}/minutes/approve` 성공 시 `meetbowl-be`는 승인된 회의록 본문을 담은 RabbitMQ
`document.index.requested` 이벤트를 발행한다. 회의·참석자 테이블이 구현되기 전에는 임시 제목 `회의록`을 사용하고,
`accessScope.userIds`에는 지정 검토자만 포함해 권한 없는 AI 검색 노출을 방지한다.
문서 타입 분리를 위해 회의록 이벤트에는 `metadata.meetingId`, `metadata.approvedAt`를 함께 담는다.
승인이 완료되면 회의 참여자에게 `MINUTES_SHARE` 내부 메일을 자동 발송한다. 메일 발신자는 승인자이며,
메일 도메인의 자기 자신 발송 금지 규칙 때문에 승인자는 자동 수신자에서 제외한다.

`POST /meetings/{meetingId}/minutes/share`는 승인 이후 회의에 참여하지 않은 사용자에게 회의록을 별도로 보내는
수동 공유 API다. 요청 본문은 `recipientUserIds`, `subject`, `body`, `idempotencyKey`를 포함한다.
수신자에 회의 참여자가 포함되면 `COMMON_INVALID_REQUEST`로 거절한다. 회의 참여자는 승인 시 자동 공유 대상이기 때문이다.

---

## 11. Internal Mail API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/mails/inbox` | 받은 메일함 조회 | User/Admin |
| GET | `/mails/sent` | 보낸 메일함 조회 | User/Admin |
| GET | `/mails/trash` | 휴지통 조회 | User/Admin |
| GET | `/mails/{mailId}` | 메일 상세 조회 | Owner |
| POST | `/mails` | 내부 메일 발송 | User/Admin |
| POST | `/mails/announcements` | 공지 메일 발송 | Admin |
| PATCH | `/mails/{mailId}/read` | 읽음/안읽음 상태 변경 | Owner |
| DELETE | `/mails/{mailId}` | 메일 휴지통 이동 | Owner |
| POST | `/mails/{mailId}/restore` | 휴지통 메일 복구 | Owner |
| DELETE | `/mails/{mailId}/permanent` | 메일 영구 삭제 | Owner |
| GET | `/mails/search` | 메일 검색 | User/Admin |
| POST | `/mails/backup` | 선택 메일 백업 | User/Admin |
| POST | `/mails/{mailId}/attachments` | 첨부파일 업로드 | Sender |
| GET | `/mails/{mailId}/attachments/{attachmentId}` | 첨부파일 다운로드 | Owner |
| POST | `/internal/mails/send` | 시스템 내부 메일 발송 요청 | Internal/System |

### 11.1 구현 범위

현재 사용자 API는 발송, 받은/보낸/휴지통 목록, 상세 조회, 읽음 상태 변경, 휴지통 이동, 복구, 영구 삭제, 선택 메일 백업, 메일 검색, 공지 메일 발송을 제공한다.
첨부파일 업로드/다운로드와 시스템 내부 발송 API는 Object Storage 및 RabbitMQ Adapter 계약과 함께 후속 구현한다.

#### 메일 검색

`GET /api/v1/mails/search?q={keyword}&page=1&size=20`

- `q`는 필수이며 공백만 입력하면 `COMMON_INVALID_REQUEST`로 거절한다.
- 현재 사용자가 소유한 메일함 항목 중 제목 또는 본문에 키워드를 포함한 항목을 대소문자 구분 없이 검색한다.
- 휴지통과 영구 삭제 항목은 검색 대상에서 제외한다.
- 응답은 받은/보낸 메일함 목록과 동일한 페이지 형식을 사용한다.

#### 공지 메일 발송

`POST /api/v1/mails/announcements` (Admin)

```json
{
  "subject": "전사 공지",
  "body": "공지 내용",
  "bodyType": "TEXT",
  "idempotencyKey": "uuid"
}
```

- 수신자는 요청 본문으로 받지 않고, 발신 관리자와 같은 조직(affiliate)의 활성 사용자로 서버가 계산한다.
- 발신자 본인과 시스템 계정은 수신자에서 제외한다. 수신 가능 사용자가 없으면 `COMMON_INVALID_REQUEST`로 거절한다.
- 메일 유형은 `ANNOUNCEMENT`로 고정하고, 일반 메일과 동일하게 발송 즉시 `SENT` 상태로 저장한다.
- 같은 `idempotencyKey`로 내용이 동일하면 기존 발송 결과를 반환하고, 다르면 `MAIL_IDEMPOTENCY_CONFLICT`를 반환한다.

### 11.2 메일 발송

`POST /api/v1/mails`

```json
{
  "recipientUserIds": ["uuid"],
  "subject": "회의 자료 공유",
  "body": "자료를 확인해 주세요.",
  "bodyType": "TEXT",
  "relatedResourceType": null,
  "relatedResourceId": null,
  "idempotencyKey": "uuid"
}
```

- 일반 사용자 발송의 `mailType`은 `NORMAL`로 고정한다.
- 수신자는 발신자와 같은 조직에 속한 활성 User/Admin 계정이어야 한다.
- 발신자를 수신자 목록에 포함할 수 없다.
- `relatedResourceType`과 `relatedResourceId`는 함께 지정하거나 모두 생략한다.
- 같은 `idempotencyKey`와 동일한 요청은 기존 발송 결과를 반환한다.
- 같은 `idempotencyKey`로 다른 내용을 요청하면 `MAIL_IDEMPOTENCY_CONFLICT`를 반환한다.
- 성공 시 메일 상태는 `SENT`이며, 발신자의 `SENT` 항목과 수신자의 `INBOX` 항목을 같은 트랜잭션에서 생성한다.

```json
{
  "success": true,
  "data": {
    "mailId": "uuid",
    "deliveryStatus": "SENT",
    "requestedAt": "2026-06-10T03:00:00Z"
  }
}
```

### 11.3 메일함 목록

`GET /api/v1/mails/inbox?page=1&size=20`

`GET /api/v1/mails/sent?page=1&size=20`

`GET /api/v1/mails/trash?page=1&size=20`

- `page`는 1부터 시작하며 기본값은 1이다.
- `size` 기본값은 20, 최댓값은 100이다.
- 받은/보낸 메일함은 휴지통 및 영구 삭제 항목을 제외한다.
- 휴지통은 받은/보낸 메일함 유형을 모두 포함하며 영구 삭제 항목은 제외한다.
- 최신 메일함 항목 순서로 반환한다.

### 11.4 메일 상세 및 상태 변경

- `GET /api/v1/mails/{mailId}`는 현재 사용자가 소유한 메일함 항목이 있을 때만 반환한다.
- `PATCH /api/v1/mails/{mailId}/read` 요청은 `{ "read": true }` 형식이며 받은 메일에만 허용한다.
- `DELETE /api/v1/mails/{mailId}`는 현재 사용자의 메일함 항목만 휴지통으로 이동한다.
- `POST /api/v1/mails/{mailId}/restore`는 현재 사용자의 휴지통 항목만 복구한다.
- `DELETE /api/v1/mails/{mailId}/permanent`는 휴지통에 있는 현재 사용자의 항목만 영구 삭제 표시한다.
- 다른 사용자의 메일함 상태나 공용 메일 본문은 변경하지 않는다.

### 11.5 선택 메일 백업

`POST /api/v1/mails/backup`

```json
{
  "mailIds": ["uuid"]
}
```

- 현재 사용자가 소유한 받은/보낸 메일만 개인 워크스페이스 백업으로 등록한다.
- 같은 메일을 다시 요청하면 기존 백업을 반환해 멱등하게 처리한다.
- 생성 결과는 `GET /api/v1/workspace/backups`에서 `sourceType: MAIL`로 조회된다.

---

## 12. Workspace API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/workspace/calendar` | 개인 일정 조회 | User/Admin |
| POST | `/workspace/calendar/events` | 개인 일정 등록 | User/Admin |
| PATCH | `/workspace/calendar/events/{eventId}` | 개인 일정 수정 | Owner |
| DELETE | `/workspace/calendar/events/{eventId}` | 개인 일정 삭제 | Owner |
| GET | `/workspace/calendar/subscriptions` | 구독한 동료 일정 목록 조회 | User/Admin |
| POST | `/workspace/calendar/subscriptions` | 동료 일정 구독 등록 | User/Admin |
| DELETE | `/workspace/calendar/subscriptions/{subscriptionId}` | 동료 일정 구독 해제 | Owner |
| GET | `/workspace/backups` | 백업 자료 조회 | User/Admin |
| GET | `/workspace/backups/{backupId}` | 본인 백업 자료 상세·본문 조회 | User/Admin |
| GET | `/workspace/backups/search` | 백업 자료 검색 | User/Admin |
| POST | `/workspace/backups/{backupId}/bookmark` | 백업 자료 북마크 등록 | User/Admin |
| DELETE | `/workspace/backups/{backupId}/bookmark` | 백업 자료 북마크 해제 | User/Admin |
| GET | `/workspace/drive/files` | 개인 드라이브 파일 목록 조회 | User/Admin |
| POST | `/workspace/drive/files` | 개인 드라이브 파일 업로드 | User/Admin |
| GET | `/workspace/drive/files/{fileId}` | 개인 드라이브 파일 메타데이터 조회 | Owner |
| GET | `/workspace/drive/files/{fileId}/download` | 개인 드라이브 파일 다운로드 | Owner |
| GET | `/workspace/drive/files/{fileId}/preview` | 개인 드라이브 파일 미리보기 | Owner |
| DELETE | `/workspace/drive/files/{fileId}` | 개인 드라이브 파일 삭제 | Owner |
| GET | `/workspace/memos` | 개인 메모 목록 조회 | User/Admin |
| POST | `/workspace/memos` | 개인 메모 작성 | User/Admin |
| PATCH | `/workspace/memos/{memoId}` | 개인 메모 수정 | Owner |
| DELETE | `/workspace/memos/{memoId}` | 개인 메모 삭제 | Owner |

개인 캘린더에서 직접 수정·삭제할 수 있는 대상은 사용자가 작성한 개인 일정으로 제한한다. 회의에서 파생된 일정은 회의 정보가 기준이므로 회의 생성·수정·취소 흐름을 통해서만 변경한다.

### 12.1 개인 드라이브 파일 업로드

`POST /api/v1/workspace/drive/files`

- 요청 형식은 `multipart/form-data`이며 파일 파트 이름은 `file`이다.
- 허용 확장자는 PDF, PNG, JPG/JPEG, DOCX, TXT이며 최대 크기는 20MB다.
- 서버는 확장자를 기준으로 신뢰할 Content-Type을 결정하고 파일 원본을 S3 호환 Object Storage에 저장한다.
- MariaDB에는 파일 원본을 저장하지 않고 파일명, Content-Type, 크기, `storageKey` 등 메타데이터만 저장한다.
- 저장 성공 후 `document.index.requested` 이벤트를 발행한다. 파일 본문은 이벤트에 싣지 않고 `storageKey`와 `contentType`을 전달하며, AI 서버가 파일을 내려받아 텍스트 추출·임베딩·Qdrant 색인을 수행한다.
- 개인 드라이브 파일의 `accessScope.userIds`에는 소유자만 포함한다. 조직 미소속 사용자는 `organizationId: null`로 발행할 수 있다.
- 허용되지 않은 형식은 `FILE_INVALID_EXTENSION`, 20MB 초과 파일은 `FILE_SIZE_EXCEEDED`로 거절한다.
- 다운로드와 미리보기는 모두 소유자 권한 검사를 통과한 뒤 S3 원본을 서버가 읽어 반환한다.
- `/download` 응답은 `Content-Disposition: attachment`, `/preview` 응답은 `Content-Disposition: inline`을 사용한다.

| GET | `/shared-workspaces` | 접근 가능한 공유 워크스페이스 조회 | User/Admin |
| POST | `/shared-workspaces` | 공유 워크스페이스 생성 | User/Admin |
| DELETE | `/shared-workspaces/{spaceId}` | 공유 워크스페이스 삭제 | Owner |
| GET | `/shared-workspaces/{spaceId}` | 공유 워크스페이스 상세 조회 | Member |
| PATCH | `/shared-workspaces/{spaceId}` | 공유 워크스페이스 정보 수정 | Owner |
| POST | `/shared-workspaces/{spaceId}/members` | 공유 워크스페이스 멤버 초대 | Owner |
| GET | `/shared-workspaces/{spaceId}/members` | 멤버 목록 조회 | Member |
| DELETE | `/shared-workspaces/{spaceId}/members/{userId}` | 멤버 추방/탈퇴 | Owner/Self |
| PATCH | `/shared-workspaces/{spaceId}/audience` | 전 직원 공유 대상 설정 | Owner/Admin |
| GET | `/shared-workspaces/{spaceId}/files` | 공유 자료 목록 조회 | Member |
| POST | `/shared-workspaces/{spaceId}/files` | 공유 자료 업로드 | Member |
| GET | `/shared-workspaces/{spaceId}/files/{fileId}` | 공유 자료 메타데이터 조회 | Member |
| GET | `/shared-workspaces/{spaceId}/files/{fileId}/download` | 공유 자료 다운로드 | Member |
| GET | `/shared-workspaces/{spaceId}/files/{fileId}/preview` | 공유 자료 미리보기 | Member |
| DELETE | `/shared-workspaces/{spaceId}/files/{fileId}` | 공유 자료 삭제 | Member/Owner |
| POST | `/shared-workspaces/{spaceId}/files/{fileId}/versions` | 새 버전 업로드 | Member |
| GET | `/shared-workspaces/{spaceId}/files/{fileId}/versions` | 파일 버전 목록 조회 | Member |
| PATCH | `/shared-workspaces/{spaceId}/files/{fileId}/versions/{versionId}` | 버전 변경 내용 메모 수정 | Member |
공유 워크스페이스 파일은 새 버전이 업로드되어도 기존 버전과 변경 이력을 보존한다.

### 12.2 공유 자료 업로드/새 버전 업로드

`POST /api/v1/shared-workspaces/{spaceId}/files`, `POST /api/v1/shared-workspaces/{spaceId}/files/{fileId}/versions`

- 두 API 모두 요청 형식은 `multipart/form-data`이며 파일 파트 이름은 `file`이다. 새 버전 업로드는 `expectedCurrentVersion`, `newVersion`, `changeMemo`(선택)를 form 필드로 함께 받는다.
- 허용 확장자는 PDF, PNG, JPG/JPEG, DOCX, TXT이며 최대 크기는 20MB다. Content-Type은 서버가 확장자로 결정한다.
- 파일 원본은 S3 호환 Object Storage에 저장하고, DB에는 메타데이터(파일명, Content-Type, 크기, `storageKey`)만 저장한다. 버전마다 별도 `storageKey`를 두어 이전 버전 원본도 보존한다.
- 저장 성공 후 `document.index.requested`를 발행한다. 색인 단위는 파일(`documentId=fileId`)이라 새 버전이 올라오면 같은 문서를 최신 내용으로 교체 색인한다.
- 공유 자료이므로 `accessScope.sharedWorkspaceIds`에 워크스페이스 ID를 담아 멤버 전원이 검색할 수 있게 한다.
- 다운로드와 미리보기는 모두 워크스페이스 멤버 권한 검사를 통과한 뒤 S3 원본을 서버가 읽어 반환한다.
- `/download` 응답은 `Content-Disposition: attachment`, `/preview` 응답은 `Content-Disposition: inline`을 사용한다.

---

## 13. AI Chatbot Gateway API

프론트엔드는 `meetbowl-ai`를 직접 호출하지 않는다.

챗봇 요청은 `meetbowl-be`가 인증/권한 검증 후 `meetbowl-ai`로 위임한다.

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/ai/chat/messages` | 현재 화면의 대화 문맥으로 챗봇 질의 | User/Admin |

`meetbowl-be`는 사용자 권한 context를 구성해 `meetbowl-ai`에 전달한다.

`meetbowl-be`는 챗봇 질문마다 현재 인증 사용자의 권한 context를 다시 계산해 `meetbowl-ai`에 전달한다.

챗봇 대화는 영속 데이터가 아니다. 프론트엔드는 현재 챗봇 화면의 메모리에서만 질문, 답변, citation을 유지하고 후속 질문에 필요한 범위만 `messageHistory`로 전송한다. `meetbowl-be`와 `meetbowl-ai`는 요청 처리 중에만 이를 사용하며 MariaDB, Qdrant, Redis, 로그에 저장하지 않는다. 사용자가 챗봇 화면을 나가거나 새로고침하거나 브라우저 탭을 닫으면 대화는 즉시 폐기되며 복구 API를 제공하지 않는다.

검색 대상은 다음으로 제한한다.

- 사용자가 수동 또는 자동으로 백업한 메일
- 사용자가 Host 또는 Participant인 회의 중 상태가 `APPROVED` 또는 `SHARED`인 회의록
- 사용자가 소유한 개인 메모
- 사용자가 소유한 개인 드라이브 파일
- 사용자가 현재 Owner 또는 Member인 공유 워크스페이스의 파일 버전

개인 자료는 인증 사용자 ID를 기준으로 전체 검색하고, 공유 자료는 현재 접근 가능한 공유 워크스페이스 ID 목록으로 제한한다. 공유 워크스페이스 권한을 잃으면 다음 질문부터 검색 대상에서 제외한다.

권한 없는 자료는 검색과 답변에 포함하면 안 된다.

---

## 14. Admin / Notification API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/notifications` | 내 알림 목록 | User/Admin |
| PATCH | `/notifications/{notificationId}/read` | 알림 읽음 처리 | Owner |
| PATCH | `/notifications/read-all` | 알림 전체 읽음 처리 | User/Admin |
| POST | `/internal/notifications/meeting-reminders` | 회의 시작 전 알림 발송 | System |
| POST | `/internal/notifications/meeting-updated` | 회의 일정 수정 알림 발송 | System |
| POST | `/internal/notifications/minutes-review` | 회의록 검토 요청 알림 발송 | System |
| POST | `/internal/notifications/minutes-review/remind` | 회의록 검토 지연 추가 알림 | System |
| GET | `/admin/dashboard` | 관리자 대시보드 지표 조회 | Admin |
| GET | `/admin/dashboard/summary` | 관리자 대시보드 요약 정보 조회 | Admin |
| GET | `/admin/meeting-rooms/reservations` | 관리자 전체 회의실 예약 현황 조회 | Admin |
| PATCH | `/admin/meeting-rooms/{roomId}/availability` | 회의실 사용 가능 여부/제한 설정 | Admin |
| GET | `/admin/meeting-sites` | 사이트 목록 조회 | Admin |
| POST | `/admin/meeting-sites` | 사이트 등록 | Admin |
| PATCH | `/admin/meeting-sites/{siteId}` | 사이트 수정 | Admin |
| DELETE | `/admin/meeting-sites/{siteId}` | 사이트 삭제 | Admin |
| GET | `/admin/meeting-buildings` | 건물 목록 조회 | Admin |
| POST | `/admin/meeting-buildings` | 건물 등록 | Admin |
| PATCH | `/admin/meeting-buildings/{buildingId}` | 건물 수정 | Admin |
| DELETE | `/admin/meeting-buildings/{buildingId}` | 건물 삭제 | Admin |
| GET | `/admin/mail-retention-policies` | 메일 보관 기간 정책 조회 | Admin |
| PATCH | `/admin/mail-retention-policies` | 받은/보낸/휴지통 메일 보관 기간 및 자동 삭제 기준 설정 | Admin |
| GET | `/admin/audit-logs` | 관리자 작업 로그 조회 | Admin |
| GET | `/admin/audit-logs/{auditLogId}` | 관리자 작업 로그 상세 조회 | Admin |

관리자 작업 로그는 작업 일시, 작업 내용, 결과, 접속 IP 주소를 포함한다.

### 14.1 관리자 대시보드 회의실 시간대 요약 기준

- `meetingRoomSummary.timeSlotUsage`는 KST 기준 오늘 24개 시간 슬롯 각각에 대해 "해당 시간에 시작한 예약 건수"를 반환한다.
- `meetingRoomSummary.timeSlotOccupancyUsage`는 KST 기준 오늘 24개 시간 슬롯 각각에 대해 "해당 시간 슬롯과 겹치는 점유 회의실 수"를 반환한다.
- `meetingRoomSummary.weekdayReservationUsage`는 KST 기준 이번 주(월~일)에 "해당 요일에 시작한 예약 건수"를 요일별로 반환한다.
- `meetingRoomSummary.inUseMeetingRoomCount`와 `meetingRoomSummary.siteBuildingUsage`는 현재 시각 기준 `IN_PROGRESS`뿐 아니라 `RESERVED`까지 포함한 "현재 시간대 점유 회의실" 기준으로 집계한다.
- 프론트엔드는 두 시계열을 각각 `예약 시작 빈도`, `상세 점유 현황` 비교 그래프로 사용한다.
- 프론트엔드는 `weekdayReservationUsage`를 `요일별 예약 분포` 막대 그래프로 사용한다.

### 14.2 메일 보관 정책 자동 적용

- `autoDeleteEnabled=false`이면 보관 기간 정책은 저장만 되고 메일함 상태를 변경하지 않는다.
- `autoDeleteEnabled=true`이면 서버 스케줄러가 KST 기준 주기적으로 정책을 적용한다.
- 스케줄러는 `meetbowl.mail.retention.batch-size` 설정값 기준으로 만료 항목을 나누어 처리한다. 기본값은 `500`이다.
- 받은/보낸 메일함 항목은 각 보관 기간을 초과하면 바로 영구 삭제하지 않고 휴지통으로 이동한다.
- 휴지통 항목은 휴지통 보관 기간을 초과하면 현재 사용자 메일함 항목에 영구 삭제 시각을 기록한다.
- 공용 메일 본문과 다른 수신자의 메일함 항목은 자동 삭제로 직접 제거하지 않는다.

---

## 15. 내부 연동 원칙

- `meetbowl-be`는 MariaDB의 유일한 쓰기 주체다.
- `meetbowl-ai`와 `meetbowl-stt`의 저장 요청은 운영 기본 경로로 RabbitMQ 이벤트를 사용한다.
- 내부 API는 장애 대응, 수동 재처리, 테스트 용도로만 사용한다.
- 프론트엔드는 `meetbowl-ai`, `meetbowl-stt`의 내부 API를 직접 호출하지 않는다.
- 실시간 자막, 실시간 피드백, 실시간 채팅은 LiveKit DataChannel을 기본으로 한다.
- 서버 내부 실시간 AI 피드백 흐름은 Redis Stream을 사용한다.
- 반드시 처리되어야 하는 비동기 작업은 RabbitMQ를 사용한다.
### Password Reset Request APIs

| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | `/password-reset-requests` | Create a password reset request and persist it with `PENDING` status when the login ID and email match an existing `USER` account. | Public |
| GET | `/admin/password-reset-requests?status=PENDING` | List password reset requests for the admin notification/management screen. | Admin |
| GET | `/admin/notifications/count` | Return the count of unprocessed password reset requests (`PENDING`). | Admin |
| POST | `/admin/password-reset-requests/{requestId}/approve` | Approve the request, reset the target user's password to `1234`, revoke active sessions, and change status to `APPROVED`. | Admin |
| POST | `/admin/password-reset-requests/{requestId}/reject` | Reject the request and change status to `REJECTED`. | Admin |

- Request list items include requester name, login ID, email, requested time, and status.
- Approved or rejected requests cannot be processed again.
- Approval/rejection actions are recorded in admin audit logs.
- Responses and logs must not expose password hashes, plain passwords, JWTs, refresh tokens, or similar sensitive values.
