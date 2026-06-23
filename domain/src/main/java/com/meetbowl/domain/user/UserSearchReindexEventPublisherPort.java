package com.meetbowl.domain.user;

/** application이 회원 검색 재색인 요청을 비동기 이벤트로 발행할 때 사용하는 Port다. */
public interface UserSearchReindexEventPublisherPort {

    void publish(UserSearchReindexRequestedEvent event);
}
