package com.meetbowl.domain.chatbot;

import java.util.Set;
import java.util.UUID;

/**
 * 챗봇 질문 시점에 사용자가 접근 가능한 공유 워크스페이스 ID를 다시 계산하는 출력 포트다.
 *
 * <p>공유 자료 검색 권한은 세션에 저장하지 않고 질문마다 새로 계산해야 한다. 그래야 사용자가 워크스페이스 멤버십을 잃었을 때 다음 질문부터 즉시 검색 대상에서 제외되어,
 * 권한이 사라진 자료가 답변 근거로 새어 나가지 않는다.
 *
 * <p>개인 자료(메일, 메모, 드라이브, 본인이 Host/Participant인 회의록)는 인증 사용자 ID만으로 AI 서버가 필터링하므로 이 포트는 공유 범위만 책임진다.
 */
public interface ChatSharedWorkspaceAccessPort {

    Set<UUID> findAccessibleSharedWorkspaceIds(UUID userId, UUID organizationId);
}
