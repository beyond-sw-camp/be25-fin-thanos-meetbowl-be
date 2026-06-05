package com.meetbowl.infrastructure.persistence.sampletask;

import com.meetbowl.domain.sampletask.SampleTask;
import com.meetbowl.domain.sampletask.SampleTaskRepositoryPort;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * 샘플 계층 연결을 위한 임시 adapter다.
 * sample 프로필 전용이며 실제 업무 저장소로 사용하지 않는다.
 * JPA adapter 패턴을 확인하려면 sample-jpa 프로필을 사용한다.
 */
@Profile("sample")
@Repository
public class InMemorySampleTaskRepositoryAdapter implements SampleTaskRepositoryPort {

    private final Map<UUID, SampleTask> sampleTasks = new ConcurrentHashMap<>();

    @Override
    public SampleTask save(SampleTask sampleTask) {
        sampleTasks.put(sampleTask.id(), sampleTask);
        return sampleTask;
    }
}
