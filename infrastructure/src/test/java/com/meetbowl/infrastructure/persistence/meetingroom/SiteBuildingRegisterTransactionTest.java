package com.meetbowl.infrastructure.persistence.meetingroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.application.meetingroom.SiteBuildingRegisterUseCase;
import com.meetbowl.application.meetingroom.SiteWithBuildingResult;
import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.meetingroom.BuildingRepositoryPort;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

/**
 * 사이트+건물 동시 생성의 트랜잭션 경계를 실제 DB(H2)로 검증한다. 핵심은 "사이트만 생기고 건물이 누락된 중간 상태"가 남지 않는지다.
 *
 * <p>유닛 테스트({@code SiteBuildingRegisterUseCaseTest})는 fake 저장소라 롤백을 흉내내지 못하므로, 실제 트랜잭션 매니저가 있는 이 통합
 * 테스트에서 부분 저장 방지를 보장한다.
 */
@SpringBootTest(classes = SiteBuildingRegisterTransactionTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:site-building-tx-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class SiteBuildingRegisterTransactionTest {

    @Autowired private SiteBuildingRegisterUseCase siteBuildingRegisterUseCase;
    @Autowired private SiteRepositoryPort siteRepositoryPort;
    @Autowired private BuildingRepositoryPort buildingRepositoryPort;
    @Autowired private SpringDataSiteRepository springDataSiteRepository;
    @Autowired private SpringDataBuildingRepository springDataBuildingRepository;

    @BeforeEach
    void cleanUp() {
        springDataBuildingRepository.deleteAll();
        springDataSiteRepository.deleteAll();
    }

    @Test
    void commitsBothSiteAndBuildingOnSuccess() {
        SiteWithBuildingResult result = siteBuildingRegisterUseCase.execute("판교 사옥", "본관");

        assertThat(result.siteId()).isNotNull();
        assertThat(result.buildingId()).isNotNull();
        assertThat(siteRepositoryPort.findAll()).hasSize(1);
        // 건물이 방금 생성된 사이트로 연결돼 저장된다(site_id 연결).
        assertThat(buildingRepositoryPort.findBySiteId(result.siteId())).hasSize(1);
    }

    @Test
    void rollsBackSiteWhenBuildingCreationFails() {
        // 건물명이 공백이면 사이트 저장 이후 Building.create 단계에서 예외가 난다 → 전체 트랜잭션 롤백.
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> siteBuildingRegisterUseCase.execute("롤백 사옥", " "));
        assertThat(exception.errorCode()).isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);

        // 사이트만 생기는 중간 상태가 없어야 한다(부분 저장 방지) — 사이트·건물 모두 미저장.
        assertThat(siteRepositoryPort.findAll()).isEmpty();
        assertThat(buildingRepositoryPort.findAll()).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        MeetingRoomJpaConfig.class,
        JpaSiteRepositoryAdapter.class,
        JpaBuildingRepositoryAdapter.class,
        SiteBuildingRegisterUseCase.class
    })
    static class TestApplication {}
}
