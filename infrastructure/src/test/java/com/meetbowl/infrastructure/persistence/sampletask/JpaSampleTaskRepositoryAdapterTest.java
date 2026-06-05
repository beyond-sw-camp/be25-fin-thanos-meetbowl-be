package com.meetbowl.infrastructure.persistence.sampletask;

import com.meetbowl.domain.sampletask.SampleTask;
import com.meetbowl.infrastructure.config.InfrastructureConfig;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("sample-jpa")
@SpringBootTest(classes = JpaSampleTaskRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sample-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
})
class JpaSampleTaskRepositoryAdapterTest {

    @Autowired
    private JpaSampleTaskRepositoryAdapter adapter;

    @Test
    void saveConvertsDomainToEntityAndBack() {
        UUID ownerUserId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2099-01-01T01:00:00Z");
        SampleTask sampleTask = SampleTask.create(ownerUserId, "샘플 작업", createdAt);

        SampleTask savedSampleTask = adapter.save(sampleTask);

        assertThat(savedSampleTask.id()).isNotNull();
        assertThat(savedSampleTask.ownerUserId()).isEqualTo(ownerUserId);
        assertThat(savedSampleTask.title()).isEqualTo("샘플 작업");
        assertThat(savedSampleTask.createdAt()).isEqualTo(createdAt);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            InfrastructureConfig.class,
            JpaSampleTaskRepositoryAdapter.class,
            SampleTaskJpaConfig.class
    })
    static class TestApplication {
    }
}
