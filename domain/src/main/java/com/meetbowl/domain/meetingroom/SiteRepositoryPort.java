package com.meetbowl.domain.meetingroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 사이트 기준정보 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface SiteRepositoryPort {

    Site save(Site site);

    Optional<Site> findById(UUID id);

    List<Site> findAll();

    void deleteById(UUID id);
}
