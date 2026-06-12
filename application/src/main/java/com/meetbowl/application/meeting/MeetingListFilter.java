package com.meetbowl.application.meeting;

/**
 * 내 회의 목록 조회 필터다. FE의 "전체 / 내가 주최한 회의 / 초대된 회의" 탭에 대응한다.
 *
 * <ul>
 *   <li>{@code ALL} — 내가 주최했거나 초대된 모든 회의(합집합)
 *   <li>{@code HOST} — 내가 주최한 회의
 *   <li>{@code INVITED} — 내가 참석자로 초대된 회의(주최 제외)
 * </ul>
 */
public enum MeetingListFilter {
    ALL,
    HOST,
    INVITED
}