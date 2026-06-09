package com.meetbowl.infrastructure.persistence.meetingroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.meetingroom.Site;
import com.meetbowl.domain.meetingroom.SiteRepositoryPort;

/** Site domain port를 JPA로 구현하는 adapter다. Entity ↔ Domain 변환은 이 경계에서만 수행한다. */
@Repository
public class JpaSiteRepositoryAdapter implements SiteRepositoryPort {

    private final SpringDataSiteRepository springDataSiteRepository;

    public JpaSiteRepositoryAdapter(SpringDataSiteRepository springDataSiteRepository) {
        this.springDataSiteRepository = springDataSiteRepository;
    }

    @Override
    public Site save(Site site) {
        return springDataSiteRepository.save(SiteEntity.from(site)).toDomain();
    }

    @Override
    public Optional<Site> findById(UUID id) {
        return springDataSiteRepository.findById(id).map(SiteEntity::toDomain);
    }

    @Override
    public List<Site> findAll() {
        return springDataSiteRepository.findAll().stream().map(SiteEntity::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataSiteRepository.deleteById(id);
    }
}
