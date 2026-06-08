package com.meetbowl.infrastructure.persistence.organization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.meetbowl.domain.organization.Job;
import com.meetbowl.domain.organization.JobRepositoryPort;

@Repository
public class JpaJobRepositoryAdapter implements JobRepositoryPort {
    private final SpringDataJobRepository repository;

    public JpaJobRepositoryAdapter(SpringDataJobRepository repository) {
        this.repository = repository;
    }

    @Override
    public Job save(Job job) {
        return repository.save(JobEntity.from(job)).toDomain();
    }

    @Override
    public Optional<Job> findById(UUID jobId) {
        return repository.findById(jobId).map(JobEntity::toDomain);
    }
}
