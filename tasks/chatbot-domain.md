# 챗봇 도메인 모델 및 엔티티 설계

## 목적

`meetbowl-be`가 AI 챗봇 게이트웨이로서 사용자 인증/권한 검증 이후 대화 이력과 답변 출처를 저장하기 위한 도메인 모델과 JPA 엔티티를 정의한다.

## 책임 경계

- `meetbowl-be`는 챗봇 세션, 메시지, 답변 출처 이력을 MariaDB에 저장한다.
- `meetbowl-be`는 사용자 권한 context를 구성해 `meetbowl-ai`에 전달한다.
- `meetbowl-ai`는 RAG 검색, LLM 호출, citation 생성 책임을 가진다.
- `meetbowl-be`는 Qdrant, LLM Provider, Embedding Provider에 직접 접근하지 않는다.

## 도메인 모델

### ChatSession

- 사용자별 대화 세션이다.
- `ownerUserId`로 소유자를 식별한다.
- `scopeType`과 `scopeId`로 특정 회의/회의록/워크스페이스 문맥을 선택적으로 제한한다.
- 삭제 API는 이력 보존을 위해 `DELETED` 상태와 `deletedAt`을 기록하는 soft delete로 처리한다.

### ChatMessage

- 세션 내 USER, ASSISTANT, SYSTEM 메시지다.
- `sequenceNumber`로 세션 내 정렬 순서를 보장한다.
- AI 답변 메시지는 `modelName`, `promptVersion`, `aiRequestId`를 선택적으로 저장한다.

### ChatMessageCitation

- ASSISTANT 메시지가 참조한 RAG 출처다.
- source type과 source id를 저장하되 원문 전체는 저장하지 않는다.
- `snippet`은 짧은 근거 발췌만 저장한다.
- 검색 가능한 source type은 다음 다섯 가지다.
  - `BACKUP_MAIL`: 사용자가 수동 또는 자동으로 백업한 메일
  - `MINUTES`: 사용자가 Host 또는 Participant이고 상태가 `APPROVED` 또는 `SHARED`인 회의록
  - `PERSONAL_MEMO`: 현재 사용자가 소유한 개인 메모
  - `PERSONAL_DRIVE_FILE`: 현재 사용자가 소유한 개인 드라이브 파일
  - `SHARED_WORKSPACE_FILE_VERSION`: 현재 사용자가 Owner 또는 Member인 공유 워크스페이스 파일 버전
- 개인 드라이브는 버전을 관리하지 않으므로 `sourceId`에 파일 ID를 저장한다.
- 공유 워크스페이스 자료는 버전 단위로 검색하므로 `sourceId`에 실제 답변 생성에 사용한 파일 버전 ID를 저장한다.
- 사용자가 공유 워크스페이스 권한을 잃으면 기존 citation의 제목, 근거, 링크는 API 응답 변환 단계에서 마스킹한다. 저장된 citation 자체는 감사 이력으로 유지한다.

### ChatSessionContext

- 세션은 사용자가 명시적으로 삭제하기 전까지 유지한다.
- LLM에 전달할 과거 대화 Window Size와 대화 요약 정책은 미확정이다.
- 정책 확정 전에는 별도 `ChatSessionContext` 도메인과 테이블을 추가하지 않는다.
- PydanticAI의 `message_history`에는 `meetbowl-be`가 선택한 대화 이력만 전달한다.

## AI 챗봇 실행 책임

- `meetbowl-be`는 현재 인증 사용자 기준으로 자료 접근 범위를 매 질문마다 계산한다.
- `meetbowl-ai`는 PydanticAI Tool을 통해 허용 범위 안에서만 Qdrant를 검색한다.
- LLM Tool 인자에는 사용자 ID나 허용 리소스 ID를 노출하지 않는다.
- 대화 세션, 메시지, citation의 기준 저장소는 `meetbowl-be`의 MariaDB다.
- `meetbowl-ai`는 챗봇 대화 이력을 자체 DB에 저장하지 않는다.

## 테이블

- `chat_sessions`
- `chat_messages`
- `chat_message_citations`

## 구현 범위

- `domain/src/main/java/com/meetbowl/domain/chatbot`
- `infrastructure/src/main/java/com/meetbowl/infrastructure/persistence/chatbot`
- 도메인 단위 테스트
- JPA Adapter 저장/조회 테스트
