package com.meetbowl.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.meetbowl.application.auth.InitializeLocalAccountsUseCase;

/** 운영 데이터와 분리된 local 프로필에서만 Swagger 확인용 계정을 생성한다. */
@Profile("local")
@Component
public class LocalDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDataInitializer.class);

    private final InitializeLocalAccountsUseCase initializeLocalAccountsUseCase;

    public LocalDataInitializer(InitializeLocalAccountsUseCase initializeLocalAccountsUseCase) {
        this.initializeLocalAccountsUseCase = initializeLocalAccountsUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            initializeLocalAccountsUseCase.execute();
        } catch (DataAccessException exception) {
            // 로컬 DB를 막 초기화한 직후에는 users 같은 필수 테이블이 아직 없을 수 있다.
            // 이 경우 Swagger 확인용 샘플 계정 생성보다 API 서버 기동 자체가 더 중요하므로
            // 경고만 남기고 계속 실행해 회의 join/STT 연결 점검을 가능하게 한다.
            log.warn("로컬 계정 초기화를 건너뜁니다. 데이터베이스 스키마가 아직 준비되지 않았습니다.", exception);
        }
    }
}
