package com.meetbowl.application.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.meetbowl.domain.user.UserSearchReindexEventPublisherPort;
import com.meetbowl.domain.user.UserSearchReindexRequestedEvent;

/** 회원 검색 재색인 이벤트를 DB 커밋 이후에만 발행해 consumer가 최신 상태를 읽도록 보장한다. */
@Component
public class UserSearchReindexRequestDispatcher {

    private final UserSearchReindexEventPublisherPort userSearchReindexEventPublisherPort;

    public UserSearchReindexRequestDispatcher(
            UserSearchReindexEventPublisherPort userSearchReindexEventPublisherPort) {
        this.userSearchReindexEventPublisherPort = userSearchReindexEventPublisherPort;
    }

    public void publishAfterCommit(UserSearchReindexRequestedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            userSearchReindexEventPublisherPort.publish(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        userSearchReindexEventPublisherPort.publish(event);
                    }
                });
    }
}
