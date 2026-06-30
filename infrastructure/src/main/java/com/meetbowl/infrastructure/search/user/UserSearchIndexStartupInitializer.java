package com.meetbowl.infrastructure.search.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 사용자 검색 인덱스 준비는 기존 단일 배포(all)와 worker 역할에서만 수행한다.
 *
 * <p>API 전용 인스턴스까지 같은 준비 작업을 반복하게 두면 Blue/Green 대기 인스턴스도 기동 시
 * 불필요한 외부 호출을 수행하므로 startup hook에서 역할을 한 번 더 확인한다.
 */
@Component
public class UserSearchIndexStartupInitializer implements ApplicationRunner {

    private static final String ROLE_PROPERTY = "meetbowl.app.role";

    private final Environment environment;
    private final UserSearchIndexInitializer userSearchIndexInitializer;

    public UserSearchIndexStartupInitializer(
            Environment environment, UserSearchIndexInitializer userSearchIndexInitializer) {
        this.environment = environment;
        this.userSearchIndexInitializer = userSearchIndexInitializer;
    }

    @Override
    public void run(ApplicationArguments args) {
        String rawRole = environment.getProperty(ROLE_PROPERTY, "all");
        if (!"all".equalsIgnoreCase(rawRole) && !"worker".equalsIgnoreCase(rawRole)) {
            return;
        }
        userSearchIndexInitializer.prepare();
    }
}
