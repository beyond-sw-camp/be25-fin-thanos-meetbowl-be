# Meetbowl stt-server API 명세서

## 1. 역할

`meetbowl-stt`는 Meetbowl의 실시간 음성 처리 서버다.

담당 범위:

- LiveKit 회의 오디오 트랙 수신
- STT Provider 연동
- 실시간 Interim/Partial Transcript 생성 (화면 자막용)
- Final Transcript 생성 및 전달
- 실시간 자막 / 번역 자막 전달 (KOR/ENG)
- AI 실시간 피드백 결과 전달
- 녹음 메타데이터 전달

### 통신 구조

`meetbowl-stt`는 프론트엔드의 REST 호출 대상이 아니다. 외부와 주고받는 흐름은 대부분 이벤트(LiveKit DataChannel / Redis Stream / RabbitMQ)다.

| 흐름 | 채널 |
|---|---|
| 세션 생성 트리거 | REST (`meetbowl-be` → stt) |
| 실시간 자막 / 피드백 / 상태 표시 | LiveKit DataChannel |
| AI 피드백 요청·결과 | Redis Stream |
| Final Transcript / 녹음 메타데이터 저장 | RabbitMQ |

사용자 대면 기능(회의 입장 토큰, 회의 원문 조회, 자막 언어 변경 요청)은 모두 `meetbowl-be`가 담당한다.

- 회의 입장/참여 토큰 발급, 권한 검증 → `meetbowl-be`
- 회의 원문 조회 → `meetbowl-be` `GET /meetings/{meetingId}/transcripts`
- 자막 표시 언어 변경 → 사용자는 `meetbowl-be`에 요청, `meetbowl-be`가 stt에 변경 지시(이벤트)

---

## 2. 공통 규칙

### Base URL

```text
/api/v1
```

### 내부 인증

```http
X-Internal-Token: {internalToken}
```

본 서버의 REST API는 내부 전용이며 `meetbowl-be`만 호출한다.

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
    "code": "STT_ERROR_CODE",
    "message": "오류 메시지",
    "details": []
  }
}
```

---

## 3. Error Code

| Code | HTTP | 설명 |
|---|---:|---|
| `STT_SESSION_NOT_FOUND` | 404 | STT 세션 없음 |
| `STT_PROVIDER_UNAVAILABLE` | 503 | STT Provider 장애 |
| `STT_STREAM_DISCONNECTED` | 503 | 음성 스트림 연결 끊김 |
| `STT_TRANSCRIPT_PUBLISH_FAILED` | 500 | Transcript 이벤트 발행 실패 |
| `STT_RECORDING_FAILED` | 500 | 녹음 처리 실패 |
| `STT_CAPTION_LANGUAGE_UNSUPPORTED` | 400 | 지원하지 않는 자막 언어 |

---

## 4. REST API

본 서버의 사용자 흐름은 이벤트 기반이므로 REST는 세션 생성 트리거 1개만 둔다. 세션 시작/종료, 자막 언어 변경, 녹음 시작/종료는 세션 생성 시 전달된 정보와 LiveKit Room 이벤트 및 Redis/RabbitMQ 이벤트로 처리한다.

| Method | Endpoint | 설명 | 호출 주체 |
|---|---|---|---|
| POST | `/sessions` | STT 세션 생성 및 처리 시작 | meetbowl-be |

#### Request

```json
{
  "meetingId": "uuid",
  "roomName": "livekit-room-name",
  "recordingEnabled": true,
  "sourceLanguage": "ko",
  "captionLanguages": ["ko", "en"],
  "participants": [
    {
      "userId": "uuid",
      "livekitIdentity": "user-uuid",
      "name": "홍길동"
    }
  ]
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "sessionId": "uuid",
    "meetingId": "uuid",
    "status": "PROCESSING"
  },
  "message": null
}
```

`status` 허용값: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` (ERD `stt_transcripts.stt_status` ENUM과 동일)

> 점검·장애 대응(세션 상태 조회, Final Transcript 강제 flush 등)이 필요한 경우 운영 단계에서 내부 전용 엔드포인트를 추가한다. MVP 기본 명세에는 포함하지 않는다.

---

## 5. LiveKit DataChannel Producer

`meetbowl-stt`는 사용자 화면 자막과 AI 실시간 피드백을 LiveKit DataChannel로 전달한다.

자막 표시 언어 전환(FR-042)은 별도 REST 없이 DataChannel `caption.language.changed` 이벤트로 반영한다.

회의 참여자 간 실시간 채팅은 `meetbowl-fe`가 LiveKit DataChannel `chat.message.sent`로 송수신하며, `meetbowl-stt`는 채팅 내용을 생성하거나 저장하지 않는다.

| Event | 관련 FR | 설명 |
|---|---|---|
| `caption.updated` | FR-041 | 화면 표시용 실시간 자막 |
| `caption.language.changed` | FR-042 | 자막 표시 언어 변경 |
| `stt.status.changed` | — | STT 상태 변경 |
| `feedback.generated` | FR-044 | 화면 표시용 AI 실시간 피드백 |

### Caption Event

```json
{
  "eventId": "uuid",
  "meetingId": "uuid",
  "sessionId": "uuid",
  "speakerId": "speaker-1",
  "speakerName": "홍길동",
  "sourceLanguage": "ko",
  "displayLanguage": "en",
  "text": "The agenda for today's meeting is the deployment schedule.",
  "isFinal": false,
  "startedAtMs": 1000,
  "endedAtMs": 5000
}
```

지원 언어: `ko`, `en`

---

## 6. Redis Stream Producer

`meetbowl-stt`는 서버 내부 실시간성이 필요한 이벤트를 Redis Stream으로 발행한다.

AI 피드백 입력에는 STT Provider가 확정한 Final Transcript만 사용한다. Interim/Partial Transcript는 화면 자막 표시용이며 Redis Stream으로 발행하지 않는다.

| Stream | Event | 설명 |
|---|---|---|
| `meeting:{meetingId}:feedback-source` | `meeting.feedback.requested` | Final Transcript 기반 AI 피드백 분석 요청 |
| `meeting:{meetingId}:status` | `stt.status.changed` | 회의 중 STT 상태 이벤트 |

Redis Stream은 장기 보관 용도로 사용하지 않는다.

### 6.1 Redis Stream Consumer

`meetbowl-stt`는 AI 서버가 생성한 실시간 피드백 결과를 구독하고, LiveKit DataChannel로 회의 참여자에게 전달한다.

| Stream | Event | 처리 |
|---|---|---|
| `meeting:{meetingId}:feedback-result` | `meeting.feedback.generated` | LiveKit DataChannel `feedback.generated`로 전달 |

---

## 7. RabbitMQ Producer

`meetbowl-stt`는 안정적으로 저장되어야 하는 작업을 RabbitMQ로 발행한다. 이것이 Final Transcript 저장과 녹음 메타데이터 저장의 운영 기본 경로다.

| Queue | Event | 관련 FR | 설명 |
|---|---|---|---|
| `api.transcript.final.save` | `transcript.final.created` | FR-048, FR-049 | Final Transcript 저장 요청 |
| `api.recording.save` | `recording.completed` | FR-050 | 녹음 파일 메타데이터 저장 요청 |

### Final Transcript Event Payload

Provider가 확정한 발화 단위로 발행한다. `meetbowl-be`가 수신하여 ERD `stt_transcripts` 기준 회의당 1행(`transcript_text` LONGTEXT)에 누적 반영한다.

```json
{
  "eventId": "uuid",
  "eventType": "transcript.final.created",
  "occurredAt": "2026-06-02T01:00:00Z",
  "producer": "stt-server",
  "version": 1,
  "correlationId": "uuid",
  "payload": {
    "meetingId": "uuid",
    "sessionId": "uuid",
    "speakerId": "speaker-1",
    "speakerName": "홍길동",
    "language": "ko",
    "startedAtMs": 1000,
    "endedAtMs": 5000,
    "text": "오늘 회의 안건은 배포 일정입니다.",
    "provider": "deepgram",
    "idempotencyKey": "uuid"
  }
}
```

`language` 허용값: `ko`, `en`. 한 회의에 여러 언어가 섞이면 `meetbowl-be`가 `stt_transcripts.transcript_language`를 `MIXED`로 저장한다.

### Recording Metadata Payload

```json
{
  "eventType": "recording.completed",
  "payload": {
    "meetingId": "uuid",
    "sessionId": "uuid",
    "fileKey": "recordings/meeting-id/audio.webm",
    "durationSeconds": 3600,
    "mimeType": "audio/webm",
    "sizeBytes": 10485760
  }
}
```

녹음 파일 원본은 S3/MinIO 등 Object Storage에 저장하고, 메타데이터 저장은 `meetbowl-be`를 통해 수행한다.

---

## 8. 저장 원칙

- Interim/Partial Transcript는 DB에 저장하지 않는다. (화면 자막 전용)
- Final Transcript만 저장 대상으로 본다. 저장 단위는 회의당 1행(`stt_transcripts`)이다.
- 사용자 대면 회의 원문 조회는 `meetbowl-be` `GET /meetings/{meetingId}/transcripts` 하나로만 제공한다.
- `meetbowl-stt`는 회의 중 채팅 내용을 저장하지 않는다.
- 회의 녹음 파일 원본은 Object Storage에 저장한다.
- MariaDB 저장은 `meetbowl-be`를 통해 수행한다.

---

## 변경 이력 (이번 수정)

- REST API를 `POST /sessions` 1개로 축소. 세션 start/stop, transcript 조회·flush, caption-language, recording start/stop, health 세부 엔드포인트 제거.
- 세션 시작/종료는 세션 생성 + LiveKit Room 이벤트로, 자막 언어 변경은 DataChannel 이벤트로 대체.
- 점검·장애 대응용 내부 엔드포인트는 운영 단계 추가 사항으로 분리(기본 명세 제외).
- 통신 구조 표를 1번에 추가하여 REST/DataChannel/Redis/RabbitMQ 역할을 명확화.
- FR 번호 정정(FR-048·041·050·042·044), ERD ENUM(`stt_status`, `transcript_language`) 정합.

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
| DELETE | `/shared-workspaces/{spaceId}` | 공유 워크스페이스 삭제 | Owner |
| GET | `/shared-workspaces/{spaceId}` | 공유 워크스페이스 상세 조회 | Member |
| PATCH | `/shared-workspaces/{spaceId}` | 공유 워크스페이스 정보 수정 | Owner |
| POST | `/shared-workspaces/{spaceId}/members` | 공유 워크스페이스 멤버 초대 | Owner |
| GET | `/shared-workspaces/{spaceId}/members` | 멤버 목록 조회 | Member |
| DELETE | `/shared-workspaces/{spaceId}/members/{userId}` | 멤버 추방/탈퇴 | Owner/Self |
| PATCH | `/shared-workspaces/{spaceId}/audience` | 전 직원 공유 대상 설정 | Owner/Admin |
| GET | `/shared-workspaces/{spaceId}/files` | 공유 자료 목록 조회 | Member |
| POST | `/shared-workspaces/{spaceId}/files` | 공유 자료 업로드 | Member |
| GET | `/shared-workspaces/{spaceId}/files/{fileId}` | 공유 자료 다운로드 | Member |
| DELETE | `/shared-workspaces/{spaceId}/files/{fileId}` | 공유 자료 삭제 | Member/Owner |
| POST | `/shared-workspaces/{spaceId}/files/{fileId}/versions` | 새 버전 업로드 | Member |
| GET | `/shared-workspaces/{spaceId}/files/{fileId}/versions` | 파일 버전 목록 조회 | Member |
| PATCH | `/shared-workspaces/{spaceId}/files/{fileId}/versions/{versionId}` | 버전 변경 내용 메모 수정 | Member |
공유 워크스페이스 파일은 새 버전이 업로드되어도 기존 버전과 변경 이력을 보존한다.

---

## 13. AI Chatbot Gateway API

프론트엔드는 `meetbowl-ai`를 직접 호출하지 않는다.

챗봇 요청은 `meetbowl-be`가 인증/권한 검증 후 `meetbowl-ai`로 위임한다.

| Method | Endpoint | 설명 | 권한 |
|---|---|---|---|
| POST | `/ai/chat/sessions` | 새 대화 세션 시작 | User/Admin |
| POST | `/ai/chat/sessions/{sessionId}/messages` | 기존 세션에 질의 | Owner |
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