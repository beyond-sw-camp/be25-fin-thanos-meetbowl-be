package com.meetbowl.application.meeting;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

/**
 * 회의별 게스트 표시명 순번을 관리한다.
 *
 * <p>같은 회의 안에서는 입장 순서대로 `게스트 1`, `게스트 2`처럼 증가시키고, 회의 종료 시에는 카운터를 초기화한다.
 */
@Service
public class MeetingGuestNameAllocator {

    private final ConcurrentMap<UUID, AtomicInteger> counters = new ConcurrentHashMap<>();

    public int nextGuestSequence(UUID meetingId) {
        if (meetingId == null) {
            return 1;
        }
        // 회의별로 순번을 따로 관리해야 다른 회의의 입장 순서가 섞이지 않는다.
        return counters.computeIfAbsent(meetingId, ignored -> new AtomicInteger(0)).incrementAndGet();
    }

    public void reset(UUID meetingId) {
        if (meetingId == null) return;
        counters.remove(meetingId);
    }
}
