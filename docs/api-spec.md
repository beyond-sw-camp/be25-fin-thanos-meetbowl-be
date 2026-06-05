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

---

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
| `AUTH_INITIAL_PASSWORD_CHANGE_REQUIRED` | 403 | 초기 비밀번호 변경 필요 |
| `USER_NOT_FOUND` | 404 | 사용자 없음 |
| `MEETING_NOT_FOUND` | 404 | 회의 없음 |
| `MEETING_ROOM_ALREADY_RESERVED` | 409 | 회의실 중복 예약 |
| `MEETING_FORBIDDEN_GUEST_ACCESS` | 403 | 게스트 접근 불가 |
| `MINUTES_REVIEW_REQUIRED` | 409 | 검토자 승인 필요 |
| `MINUTES_ALREADY_APPROVED` | 409 | 이미 승인된 회의록 |
| `MAIL_FORBIDDEN_ACCESS` | 403 | 메일 접근 불가 |
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
| POST | `/auth/password/change-initial` | 최초 로그인 초기 비밀번호 변경 | User |
| POST | `/auth/password/reset-request` | 비밀번호 재설정 요청 | User |
| POST | `/auth/password/reset-by-admin` | 관리자가 비밀번호 초기화 | Admin |
| GET | `/auth/me` | 현재 로그인 사용자 정보 조회 | User/Admin |

Admin 계정은 공유 시스템 계정으로 운영될 수 있으므로 동일 Admin 계정의 동시 접속은 하나의 세션만 유지한다.

---

## 5. User / Organization API

### 사용자

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/users/me` | 내 정보 조회 | User/Admin |
| PATCH | `/users/me` | 내 프로필 수정 | User/Admin |
| GET | `/users` | 회원 목록 조회 | Admin |
| GET | `/users/{userId}` | 회원 상세 조회 | Admin |
| POST | `/users` | 회원 계정 생성 | Admin |
| PATCH | `/users/{userId}` | 회원 정보 수정 | Admin |
| PATCH | `/users/{userId}/status` | 회원 활성/비활성 관리 | Admin |
| POST | `/users/import` | 엑셀 기반 회원 일괄 업로드 | Admin |
| GET | `/users/export` | 회원/조직도 엑셀 다운로드 | Admin |
| GET | `/users/{userId}/simple-profile` | 조직도용 간단 회원 정보 조회 | User/Admin |
| GET | `/users/recipients/search` | 메일 수신자 및 소속 정보 검색 | User/Admin |

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
| POST | `/organizations/duties` | 직책 등록 | Admin |
| PATCH | `/organizations/duties/{dutyId}` | 직책 수정 | Admin |
| DELETE | `/organizations/duties/{dutyId}` | 직책 삭제 | Admin |
| POST | `/organizations/jobs` | 직무 등록 | Admin |
| PATCH | `/organizations/jobs/{jobId}` | 직무 수정 | Admin |
| DELETE | `/organizations/jobs/{jobId}` | 직무 삭제 | Admin |

---

## 6. User Settings API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/settings/me` | 개인 설정 조회 | User/Admin |
| PATCH | `/settings/me` | 회의 알림 시간, 자동 백업 여부, 자동 백업 시각 수정 | User/Admin |

---

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
| POST | `/meetings/{meetingId}/join` | 회의 참여 정보 조회 | Participant/Guest |
| POST | `/meetings/{meetingId}/leave` | 회의 퇴장 | Participant/Guest |
| POST | `/meetings/{meetingId}/end` | 회의 종료 | Host/Admin/System |
| POST | `/meetings/{meetingId}/invite-link` | 회의 초대 코드/URL 생성 | Host |
| POST | `/meetings/guest-join` | 게스트 초대 코드로 회의 참여 | Public |
| POST | `/meetings/{meetingId}/recording/start` | 회의 녹음 시작 | Host/System |
| POST | `/meetings/{meetingId}/recording/stop` | 회의 녹음 종료 | Host/System |
| POST | `/meetings/{meetingId}/attachments` | 회의 참고자료 첨부파일 등록 | Host/Admin |
| GET | `/meetings/{meetingId}/attachments` | 회의 참고자료 첨부파일 목록 조회 | Participant/Admin |
| GET | `/meetings/{meetingId}/attachments/{attachmentId}` | 회의 참고자료 첨부파일 다운로드 | Participant/Admin |
| DELETE | `/meetings/{meetingId}/attachments/{attachmentId}` | 회의 참고자료 첨부파일 삭제 | Host/Admin |
| GET | `/meetings/{meetingId}/caption-language` | 현재 자막 표시 언어 조회 | Participant/Guest |
| PATCH | `/meetings/{meetingId}/caption-language` | KOR/ENG 자막 표시 언어 변경 | Participant/Guest |

회의 생성/수정 시 회의록 검토자를 지정할 수 있다.

회의 일정 수정 시 기존 참석자와 새 참석자에게 알림을 발송한다.

Guest는 해당 회의 참여에 필요한 API에만 접근할 수 있다.

---

## 9. Transcript / STT Result API

`meetbowl-be`는 STT 자체를 수행하지 않는다. STT 처리는 `meetbowl-stt`가 담당하고, `meetbowl-be`는 최종 결과 저장과 조회를 담당한다.

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/meetings/{meetingId}/transcripts` | 회의 STT 원문 조회 | Participant/Admin |
| POST | `/internal/meetings/{meetingId}/transcripts/final` | STT Final Transcript 저장 | Internal |
| POST | `/internal/meetings/{meetingId}/recordings` | 회의 녹음 파일 메타데이터 저장 | Internal |

Final Transcript 저장과 녹음 파일 메타데이터 저장의 운영 기본 경로는 RabbitMQ 이벤트 소비다.

- Final Transcript 저장: `transcript.final.created`
- 녹음 파일 메타데이터 저장: `recording.completed`

내부 API는 장애 대응, 수동 재처리, 테스트 용도로만 사용한다.

---

## 10. Meeting Minutes API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/meetings/{meetingId}/minutes/generate` | AI 회의록 초안 생성 요청 | Host/Admin |
| GET | `/meetings/{meetingId}/minutes` | 회의록 조회 | Participant/Admin |
| PATCH | `/meetings/{meetingId}/minutes` | 회의록 검토 및 수정 | Reviewer |
| POST | `/meetings/{meetingId}/minutes/approve` | 회의록 공유 수락 | Reviewer |
| POST | `/meetings/{meetingId}/minutes/review-remind` | 검토 지연 재알림 발송 | System/Admin |
| POST | `/meetings/{meetingId}/minutes/share/participants` | 참석자에게 회의록 내부 메일 공유 | System |
| POST | `/meetings/{meetingId}/minutes/share` | 미참석자에게 회의록 공유 | Participant |
| GET | `/minutes` | 회의록 목록/검색 | User/Admin |
| GET | `/minutes/deletion-scheduled` | 삭제 예정 회의록 조회 | Admin |
| PATCH | `/minutes/{minutesId}/deletion-status` | 삭제 예정 회의록 상태 관리 | Admin |
| GET | `/admin/minutes/settings` | 회의록 자동 발송 설정 조회 | Admin |
| PATCH | `/admin/minutes/settings` | 회의록 자동 발송 여부 설정 | Admin |
| POST | `/internal/meetings/{meetingId}/minutes/draft` | ai-server가 생성한 회의록 초안 저장 | Internal |

AI 회의록 초안 저장의 운영 기본 경로는 RabbitMQ `minutes.generated` 이벤트 소비다.

내부 API는 장애 대응, 수동 재처리, 테스트 용도로만 사용한다.

회의록 상태:

```text
DRAFT
IN_REVIEW
APPROVED
SHARED
DELETION_SCHEDULED
```

검토자 승인 전 자동 공유는 금지한다.

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
| GET | `/workspace/backups/search` | 백업 자료 검색 | User/Admin |
| POST | `/workspace/backups/{backupId}/bookmark` | 백업 자료 북마크 등록 | User/Admin |
| DELETE | `/workspace/backups/{backupId}/bookmark` | 백업 자료 북마크 해제 | User/Admin |
| GET | `/workspace/address-book` | 개인/조직 주소록 조회 | User/Admin |
| POST | `/workspace/address-book` | 개인 주소록 등록 | User/Admin |
| PATCH | `/workspace/address-book/{contactId}` | 개인 주소록 수정 | Owner |
| DELETE | `/workspace/address-book/{contactId}` | 개인 주소록 삭제 | Owner |
| GET | `/workspace/drive/files` | 개인 드라이브 파일 목록 조회 | User/Admin |
| POST | `/workspace/drive/files` | 개인 드라이브 파일 업로드 | User/Admin |
| GET | `/workspace/drive/files/{fileId}` | 개인 드라이브 파일 다운로드 | Owner |
| DELETE | `/workspace/drive/files/{fileId}` | 개인 드라이브 파일 삭제 | Owner |
| GET | `/workspace/memos` | 개인 메모 목록 조회 | User/Admin |
| POST | `/workspace/memos` | 개인 메모 작성 | User/Admin |
| PATCH | `/workspace/memos/{memoId}` | 개인 메모 수정 | Owner |
| DELETE | `/workspace/memos/{memoId}` | 개인 메모 삭제 | Owner |
| POST | `/workspace/calendar/google/connect` | 구글 캘린더 연동 연결 | User/Admin |
| DELETE | `/workspace/calendar/google/connect` | 구글 캘린더 연동 해제 | Owner |
| GET | `/shared-workspaces` | 접근 가능한 공유 워크스페이스 조회 | User/Admin |
| POST | `/shared-workspaces` | 공유 워크스페이스 생성 | User/Admin |
| GET | `/shared-workspaces/{spaceId}` | 공유 워크스페이스 상세 조회 | Member |
| POST | `/shared-workspaces/{spaceId}/invite-link` | 초대 링크 생성 | Owner |
| POST | `/shared-workspaces/join` | 초대 링크로 참여 | User/Admin |
| PATCH | `/shared-workspaces/{spaceId}/audience` | 전 직원 공유 대상 설정 | Owner/Admin |
| GET | `/shared-workspaces/{spaceId}/files` | 공유 자료 목록 조회 | Member |
| POST | `/shared-workspaces/{spaceId}/files` | 공유 자료 업로드 | Member |
| GET | `/shared-workspaces/{spaceId}/files/{fileId}` | 공유 자료 다운로드 | Member |
| POST | `/shared-workspaces/{spaceId}/files/{fileId}/versions` | 새 버전 업로드 | Member |
| GET | `/shared-workspaces/{spaceId}/files/{fileId}/versions` | 파일 버전 목록 조회 | Member |

공유 워크스페이스 파일은 새 버전이 업로드되어도 기존 버전과 변경 이력을 보존한다.

---

## 13. AI Chatbot Gateway API

프론트엔드는 `meetbowl-ai`를 직접 호출하지 않는다.

챗봇 요청은 `meetbowl-be`가 인증/권한 검증 후 `meetbowl-ai`로 위임한다.

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/ai/chat` | 권한 있는 자료 기반 AI 챗봇 질의 | User/Admin |
| GET | `/ai/chat/sessions` | 챗봇 대화 목록 조회 | User/Admin |
| GET | `/ai/chat/sessions/{sessionId}` | 챗봇 대화 상세 조회 | Owner |
| DELETE | `/ai/chat/sessions/{sessionId}` | 챗봇 대화 삭제 | Owner |

`meetbowl-be`는 사용자 권한 context를 구성해 `meetbowl-ai`에 전달한다.

권한 context에는 백업 메일, 즐겨찾기한 회의록, 개인 워크스페이스 자료, 사용자가 초대된 공유 워크스페이스 자료, 공유 워크스페이스 파일/버전 자료가 포함될 수 있다.

권한 없는 자료는 검색과 답변에 포함하면 안 된다.

---

## 14. Admin / Notification API

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| GET | `/notifications` | 내 알림 목록 | User/Admin |
| PATCH | `/notifications/{notificationId}/read` | 알림 읽음 처리 | Owner |
| POST | `/internal/notifications/meeting-reminders` | 회의 시작 전 알림 발송 | System |
| POST | `/internal/notifications/meeting-updated` | 회의 일정 수정 알림 발송 | System |
| POST | `/internal/notifications/minutes-review` | 회의록 검토 요청 알림 발송 | System |
| POST | `/internal/notifications/minutes-review/remind` | 회의록 검토 지연 추가 알림 | System |
| GET | `/admin/dashboard` | 관리자 대시보드 지표 조회 | Admin |
| GET | `/admin/dashboard/summary` | 관리자 대시보드 요약 정보 조회 | Admin |
| GET | `/admin/meeting-rooms/reservations` | 관리자 전체 회의실 예약 현황 조회 | Admin |
| PATCH | `/admin/meeting-rooms/{roomId}/availability` | 회의실 사용 가능 여부/제한 설정 | Admin |
| GET | `/admin/mail-retention-policies` | 메일 보관 기간 정책 조회 | Admin |
| PATCH | `/admin/mail-retention-policies` | 받은/보낸/휴지통 메일 보관 기간 및 자동 삭제 기준 설정 | Admin |
| GET | `/admin/audit-logs` | 관리자 작업 로그 조회 | Admin |
| GET | `/admin/audit-logs/{auditLogId}` | 관리자 작업 로그 상세 조회 | Admin |

관리자 작업 로그는 작업 일시, 작업 내용, 결과, 접속 IP 주소를 포함한다.

---

## 15. 내부 연동 원칙

- `meetbowl-be`는 MariaDB의 유일한 쓰기 주체다.
- `meetbowl-ai`와 `meetbowl-stt`의 저장 요청은 운영 기본 경로로 RabbitMQ 이벤트를 사용한다.
- 내부 API는 장애 대응, 수동 재처리, 테스트 용도로만 사용한다.
- 프론트엔드는 `meetbowl-ai`, `meetbowl-stt`의 내부 API를 직접 호출하지 않는다.
- 실시간 자막, 실시간 피드백, 실시간 채팅은 LiveKit DataChannel을 기본으로 한다.
- 채팅 내역 저장/조회가 필요하면 `meetbowl-be` REST API에서 권한 검사 후 처리한다.
- 서버 내부 실시간 AI 피드백 흐름은 Redis Stream을 사용한다.
- 반드시 처리되어야 하는 비동기 작업은 RabbitMQ를 사용한다.
