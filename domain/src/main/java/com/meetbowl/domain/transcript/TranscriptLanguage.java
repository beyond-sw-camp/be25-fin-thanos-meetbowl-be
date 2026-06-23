package com.meetbowl.domain.transcript;

/**
 * 회의 발화와 번역 결과에 사용하는 언어 코드다.
 *
 * <p>원칙적으로는 한국어/영어가 기본이지만, provider가 언어를 아직 확정하지 못한 최종 segment를 넘길 수 있어 UNKNOWN도 보관한다.
 */
public enum TranscriptLanguage {
    KO,
    EN,
    UNKNOWN
}
