package com.meetbowl.infrastructure.persistence.sampletask;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.meetbowl.domain.sampletask.SampleTask;
import com.meetbowl.domain.sampletask.SampleTaskRepositoryPort;

/** domain port를 JPA로 구현하는 adapter 예시다. Entity 변환은 adapter 경계에서만 수행한다. */
@Profile("sample-jpa")
@Repository
public class JpaSampleTaskRepositoryAdapter implements SampleTaskRepositoryPort {

    private final SpringDataSampleTaskRepository springDataSampleTaskRepository;

    public JpaSampleTaskRepositoryAdapter(
            SpringDataSampleTaskRepository springDataSampleTaskRepository) {
        this.springDataSampleTaskRepository = springDataSampleTaskRepository;
    }

    @Override
    public SampleTask save(SampleTask sampleTask) {
        SampleTaskEntity savedEntity =
                springDataSampleTaskRepository.save(SampleTaskEntity.from(sampleTask));
        return savedEntity.toDomain();
    }
}
