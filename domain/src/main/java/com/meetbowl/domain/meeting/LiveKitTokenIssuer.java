package com.meetbowl.domain.meeting;

/**
 * LiveKit join token 발급 Port다.
 *
 * <p>Application은 "누가 어떤 room에 어떤 이름으로 들어갈지"만 결정하고, 실제 JWT 서명과 claim 포맷은 Infrastructure 구현체에 위임한다.
 */
public interface LiveKitTokenIssuer {

    LiveKitTokenIssueResult issue(LiveKitTokenIssueCommand command);
}
