package com.meetbowl.domain.transcript;

/**
 * 최종 확정된 회의 원문 문장의 인식 언어를 나타낸다.
 *
 * <p>번역 결과 언어가 아니라 STT 원문의 언어다. 공급자가 언어를 확정하지 못했더라도 문장 자체를 버리지 않기 위해 UNKNOWN을 허용한다.
 */
public enum TranscriptLanguage {
    /** 한국어로 인식된 원문이다. */
    KO,

    /** 영어로 인식된 원문이다. */
    EN,

    /** STT 공급자 또는 이벤트에서 언어를 확정하지 못한 원문이다. */
    UNKNOWN
}
