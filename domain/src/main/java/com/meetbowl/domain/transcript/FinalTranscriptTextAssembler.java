package com.meetbowl.domain.transcript;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Final Transcript 세그먼트를 AI 입력과 사용자 조회가 공유하는 하나의 원문 문자열로 조립한다. */
public final class FinalTranscriptTextAssembler {

    private FinalTranscriptTextAssembler() {}

    public static String assemble(List<MeetingTranscriptSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "";
        }
        // 저장소 구현과 무관하게 sequence를 최종 기준으로 삼아 AI 입력과 화면 원문의 순서를 일치시킨다.
        return segments.stream()
                .sorted(Comparator.comparingLong(MeetingTranscriptSegment::sequence))
                .map(MeetingTranscriptSegment::sourceText)
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .collect(Collectors.joining("\n"));
    }
}
