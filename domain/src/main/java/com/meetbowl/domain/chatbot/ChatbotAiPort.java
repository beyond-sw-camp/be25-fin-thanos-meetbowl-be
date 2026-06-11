package com.meetbowl.domain.chatbot;

/**
 * 챗봇 답변 생성을 외부 AI 서버에 위임하는 출력 포트다.
 *
 * <p>meetbowl-be는 LLM 호출과 Qdrant 검색을 직접 수행하지 않는다. 대신 검증된 요청 문맥만 전달하고, 검색 권한 필터와 모델 실행은 meetbowl-ai가
 * 담당한다. 도메인 타입만 주고받아 외부 클라이언트 DTO가 상위 계층으로 새지 않도록 한다.
 *
 * <p>구현체(infrastructure adapter)는 응답을 {@link ChatAnswer} 스키마로 검증하고, 검증에 실패하거나 AI 서버를 사용할 수 없을 때는 업무
 * 예외로 변환한다. 질문과 답변 본문은 어느 구현체도 저장하거나 로그에 남기지 않는다.
 */
public interface ChatbotAiPort {

    ChatAnswer ask(ChatRequestContext requestContext);
}
