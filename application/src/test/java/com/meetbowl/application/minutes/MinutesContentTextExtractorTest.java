package com.meetbowl.application.minutes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class MinutesContentTextExtractorTest {

    private final MinutesContentTextExtractor extractor =
            new MinutesContentTextExtractor(new ObjectMapper().findAndRegisterModules());

    @Test
    void keepsHeadingsParagraphsAndBulletsReadable() {
        String content =
                """
                {
                  "type":"doc",
                  "content":[
                    {"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"결정사항"}]},
                    {"type":"bulletList","content":[
                      {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"6월 1일 런칭"}]}]},
                      {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"프로덕트 우선 채용"}]}]}
                    ]},
                    {"type":"paragraph","content":[{"type":"text","text":"후속 조치는 별도 공유한다."}]}
                  ]
                }
                """;

        assertEquals(
                "결정사항\n\n- 6월 1일 런칭\n- 프로덕트 우선 채용\n\n후속 조치는 별도 공유한다.", extractor.extract(content));
    }

    @Test
    void supportsOrderedListBlockquoteAndHardBreak() {
        String content =
                """
                {
                  "type":"doc",
                  "content":[
                    {"type":"orderedList","content":[
                      {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"첫 번째 항목"}]}]},
                      {"type":"listItem","content":[{"type":"paragraph","content":[{"type":"text","text":"둘째 항목"}]}]}
                    ]},
                    {"type":"blockquote","content":[
                      {"type":"paragraph","content":[
                        {"type":"text","text":"검토 메모"},
                        {"type":"hardBreak"},
                        {"type":"text","text":"다음 회의에 재논의"}
                      ]}
                    ]}
                  ]
                }
                """;

        assertEquals("1. 첫 번째 항목\n2. 둘째 항목\n\n> 검토 메모\n> 다음 회의에 재논의", extractor.extract(content));
    }

    @Test
    void fallsBackToOriginalTextWhenContentIsNotTiptapJson() {
        assertEquals("plain text", extractor.extract("plain text"));
    }
}
