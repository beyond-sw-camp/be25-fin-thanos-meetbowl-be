package com.meetbowl.api.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.meetbowl.infrastructure.search.user.UserSearchIndexInitializer;

/**
 * 사용자 검색 인덱스 준비는 기존 단일 배포(`all`)와 worker 역할에서만 수행한다.
 *
 * <p>API 전용 인스턴스까지 같은 준비 작업을 반복하게 두면 Blue/Green 대기 인스턴스도 기동 시 불필요한 외부 호출을 수행하므로 startup hook만 분리한다.
 */
@Component
@ConditionalOnMeetbowlAppRole({MeetbowlAppRole.ALL, MeetbowlAppRole.WORKER})
public class UserSearchIndexStartupInitializer implements ApplicationRunner {

    private final UserSearchIndexInitializer userSearchIndexInitializer;

    public UserSearchIndexStartupInitializer(UserSearchIndexInitializer userSearchIndexInitializer) {
        this.userSearchIndexInitializer = userSearchIndexInitializer;
    }

    @Override
    public void run(ApplicationArguments args) {
        userSearchIndexInitializer.prepare();
    }
}
