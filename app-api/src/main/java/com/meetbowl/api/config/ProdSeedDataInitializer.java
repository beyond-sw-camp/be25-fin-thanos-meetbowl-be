package com.meetbowl.api.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.meetbowl.application.auth.InitializeLocalAccountsUseCase;

/** 운영 `prod` 프로필에서만 기본 관리자 2개와 검증용 계정을 한 번 채운다. */
@Profile("prod")
@Component
public class ProdSeedDataInitializer implements ApplicationRunner {

    private final InitializeLocalAccountsUseCase initializeLocalAccountsUseCase;

    public ProdSeedDataInitializer(InitializeLocalAccountsUseCase initializeLocalAccountsUseCase) {
        this.initializeLocalAccountsUseCase = initializeLocalAccountsUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        // prod는 운영 스키마를 대상으로 하므로 예외를 숨기지 않는다.
        initializeLocalAccountsUseCase.execute();
    }
}
