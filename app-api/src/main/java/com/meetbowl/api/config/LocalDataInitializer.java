package com.meetbowl.api.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.meetbowl.application.auth.InitializeLocalAccountsUseCase;

/** 운영 데이터와 분리된 local 프로필에서만 Swagger 확인용 계정을 생성한다. */
@Profile("local")
@Component
public class LocalDataInitializer implements ApplicationRunner {

    private final InitializeLocalAccountsUseCase initializeLocalAccountsUseCase;

    public LocalDataInitializer(InitializeLocalAccountsUseCase initializeLocalAccountsUseCase) {
        this.initializeLocalAccountsUseCase = initializeLocalAccountsUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        initializeLocalAccountsUseCase.execute();
    }
}
