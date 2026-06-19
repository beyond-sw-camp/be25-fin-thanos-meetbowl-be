package com.meetbowl.common.event;

/** 루트 docs/event-contract.md에 정의된 이벤트 이름의 코드 기준이다. 계약 문서 변경 없이 임의 이벤트를 추가하지 않는다. */
public final class EventTypes {

    public static final String MEETING_ENDED = "meeting.ended";
    public static final String MINUTES_GENERATION_REQUESTED = "minutes.generation.requested";
    public static final String MINUTES_GENERATED = "minutes.generated";
    public static final String DOCUMENT_INDEX_REQUESTED = "document.index.requested";
    public static final String DOCUMENT_INDEX_REMOVED = "document.index.removed";
    public static final String USER_SEARCH_REINDEX_REQUESTED = "user.search.reindex.requested";
    public static final String MAIL_SEND_REQUESTED = "mail.send.requested";
    public static final String MAIL_SENT = "mail.sent";
    public static final String MAIL_FAILED = "mail.failed";

    private EventTypes() {}
}
