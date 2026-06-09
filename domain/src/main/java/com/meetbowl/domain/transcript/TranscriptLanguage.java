package com.meetbowl.domain.transcript;

/**
 * 회의 발화와 번역 결과에 사용하는 언어 코드다.
 *
 * <p>현재 실시간 번역 세션은 한국어→영어, 영어→한국어 두 방향만 지원하므로 KO와 EN만 허용한다.
 */
public enum TranscriptLanguage {
    KO,
    EN
}
