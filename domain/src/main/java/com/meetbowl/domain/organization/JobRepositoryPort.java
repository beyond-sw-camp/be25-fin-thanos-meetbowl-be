package com.meetbowl.domain.organization;

import java.util.Optional;
import java.util.UUID;

public interface JobRepositoryPort {

    Job save(Job job);

    Optional<Job> findById(UUID jobId);
}
