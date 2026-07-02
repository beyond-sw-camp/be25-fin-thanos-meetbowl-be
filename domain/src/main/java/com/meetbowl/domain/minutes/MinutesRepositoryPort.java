package com.meetbowl.domain.minutes;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** domain/application 계층이 회의록 저장소에 기대하는 계약이다. 구현은 infrastructure adapter가 담당한다. */
public interface MinutesRepositoryPort {

    Minutes save(Minutes minutes);

    Optional<Minutes> findById(UUID minutesId);

    Optional<Minutes> findByMeetingId(UUID meetingId);

    List<Minutes> findByOrganizationId(UUID organizationId);

    List<Minutes> findByOrganizationIdAndMeetingIds(UUID organizationId, Set<UUID> meetingIds);

    List<Minutes> searchByOrganizationId(UUID organizationId, String keyword);

    List<Minutes> searchByOrganizationIdAndMeetingIds(
            UUID organizationId, Set<UUID> meetingIds, String keyword);

    boolean existsByMeetingId(UUID meetingId);
}
