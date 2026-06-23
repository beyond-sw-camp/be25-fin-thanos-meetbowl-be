package com.meetbowl.infrastructure.persistence.minutes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** JPA 쿼리 메서드만 제공하는 내부 repository다. domain/application 계층에서는 직접 사용하지 않는다. */
interface SpringDataMinutesRepository extends JpaRepository<MinutesEntity, UUID> {

    Optional<MinutesEntity> findByMeetingId(UUID meetingId);

    List<MinutesEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    @Query(
            """
            select minutes
            from MinutesEntity minutes
            where minutes.organizationId = :organizationId
              and (
                lower(minutes.summary) like lower(concat('%', :keyword, '%'))
                or lower(minutes.content) like lower(concat('%', :keyword, '%'))
              )
            order by minutes.createdAt desc
            """)
    List<MinutesEntity> searchByOrganizationId(
            @Param("organizationId") UUID organizationId, @Param("keyword") String keyword);

    boolean existsByMeetingId(UUID meetingId);
}
