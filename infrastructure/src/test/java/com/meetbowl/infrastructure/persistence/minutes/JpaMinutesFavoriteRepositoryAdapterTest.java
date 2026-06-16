package com.meetbowl.infrastructure.persistence.minutes;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.meetbowl.domain.minutes.Minutes;
import com.meetbowl.domain.minutes.MinutesFavorite;
import com.meetbowl.infrastructure.config.InfrastructureConfig;

@SpringBootTest(classes = JpaMinutesFavoriteRepositoryAdapterTest.TestApplication.class)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:minutes-favorite-jpa-test;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
        })
class JpaMinutesFavoriteRepositoryAdapterTest {

    @Autowired private JpaMinutesRepositoryAdapter minutesAdapter;
    @Autowired private JpaMinutesFavoriteRepositoryAdapter favoriteAdapter;

    @Test
    void findByOrganizationAndSearch() {
        UUID organizationId = UUID.randomUUID();
        Minutes saved = minutesAdapter.save(minutes(UUID.randomUUID(), organizationId));
        minutesAdapter.save(minutes(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(minutesAdapter.findByOrganizationId(organizationId))
                .extracting(Minutes::id)
                .containsExactly(saved.id());
        assertThat(minutesAdapter.searchByOrganizationId(organizationId, "회의록"))
                .extracting(Minutes::id)
                .containsExactly(saved.id());
    }

    @Test
    void saveFindAndDeleteFavorite() {
        UUID userId = UUID.randomUUID();
        Minutes savedMinutes = minutesAdapter.save(minutes(UUID.randomUUID(), UUID.randomUUID()));
        MinutesFavorite savedFavorite =
                favoriteAdapter.save(
                        MinutesFavorite.create(
                                userId,
                                savedMinutes.id(),
                                Instant.parse("2099-01-01T01:00:00Z")));

        assertThat(savedFavorite.id()).isNotNull();
        assertThat(favoriteAdapter.findByUserId(userId))
                .extracting(MinutesFavorite::minutesId)
                .containsExactly(savedMinutes.id());
        assertThat(favoriteAdapter.findByUserIdAndMinutesId(userId, savedMinutes.id()))
                .isPresent();

        favoriteAdapter.deleteByUserIdAndMinutesId(userId, savedMinutes.id());

        assertThat(favoriteAdapter.findByUserIdAndMinutesId(userId, savedMinutes.id())).isEmpty();
    }

    private Minutes minutes(UUID meetingId, UUID organizationId) {
        return Minutes.createDraft(
                meetingId,
                organizationId,
                UUID.randomUUID(),
                "회의 요약",
                "회의록 본문",
                "llm-model-name",
                "minutes-v1");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
        InfrastructureConfig.class,
        JpaMinutesRepositoryAdapter.class,
        JpaMinutesFavoriteRepositoryAdapter.class,
        MinutesJpaConfig.class
    })
    static class TestApplication {}
}
