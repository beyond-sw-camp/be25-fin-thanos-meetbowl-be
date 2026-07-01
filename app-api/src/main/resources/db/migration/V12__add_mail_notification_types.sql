alter table notification
    modify resource_type enum ('MAIL','MEETING','MEETING_MINUTES'),
    modify type enum (
        'MAIL_RECEIVED',
        'MAIL_SHARED',
        'MEETING_CANCELLED',
        'MEETING_REMINDER',
        'MEETING_UPDATED',
        'MINUTES_REVIEW_REMINDER',
        'MINUTES_REVIEW_REQUEST'
    ) not null;
