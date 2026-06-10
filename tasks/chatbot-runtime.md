# 챗봇 휘발성 대화 처리

## 목적

챗봇 대화를 업무 데이터로 보관하지 않고 현재 화면의 실행 문맥으로만 사용한다. 이 정책은 사용자가 챗봇 화면을 나간 뒤 질문, 답변, 출처가 서버에 남지 않아야 한다는 요구사항을 보장하기 위해 존재한다.

## 책임 경계

- `meetbowl-fe`는 현재 화면의 메모리에서만 대화 내용을 유지한다.
- `meetbowl-fe`는 후속 질문에 필요한 대화 범위만 `messageHistory`로 전송한다.
- `meetbowl-be`는 인증과 현재 자료 접근 권한을 검증한 뒤 요청을 `meetbowl-ai`에 위임한다.
- `meetbowl-ai`는 전달받은 `messageHistory`를 한 번의 Agent 실행 문맥으로만 사용한다.
- 어느 서버도 챗봇 질문, 답변, citation을 MariaDB, Qdrant, Redis 또는 로그에 저장하지 않는다.

## 종료 규칙

- 화면 이탈, 새로고침, 탭 종료 시 프론트엔드 메모리의 대화를 폐기한다.
- 세션 목록, 대화 상세, 대화 복구, 대화 삭제 API를 만들지 않는다.
- 서버는 대화 식별자나 soft delete 상태를 관리하지 않는다.
- 장애 분석에는 `requestId`, `correlationId`, latency, 오류 코드만 사용하고 질문과 답변 본문은 남기지 않는다.

## 구현 범위

- `domain/chatbot`의 비영속 대화 문맥, 메시지, 검색 범위, citation 모델
- `POST /api/v1/ai/chat/messages` Gateway API
- 요청별 권한 context 계산
- 요청 DTO의 제한된 `messageHistory`
- AI 내부 API의 stateless 처리
- 화면 이탈 시 프론트엔드 상태 초기화
- 서버 저장소에 챗봇 테이블과 Repository가 없음을 검증하는 아키텍처 테스트

비영속 도메인 모델은 요청 검증과 BE-AI 계약을 표현하기 위해 유지한다. 다만 DB 식별자, 세션 상태, 삭제 시각처럼 영속 수명주기를 전제하는 필드는 두지 않는다.
