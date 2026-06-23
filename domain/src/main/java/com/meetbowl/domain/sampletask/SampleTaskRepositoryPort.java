package com.meetbowl.domain.sampletask;

/** 샘플 도메인이 필요로 하는 저장소 계약이다. 실제 구현은 infrastructure adapter가 담당한다. */
public interface SampleTaskRepositoryPort {

    SampleTask save(SampleTask sampleTask);
}
