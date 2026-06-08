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

## 테이블

- `chat_sessions`
- `chat_messages`
- `chat_message_citations`

## 구현 범위

- `domain/src/main/java/com/meetbowl/domain/chatbot`
- `infrastructure/src/main/java/com/meetbowl/infrastructure/persistence/chatbot`
- 도메인 단위 테스트
- JPA Adapter 저장/조회 테스트
