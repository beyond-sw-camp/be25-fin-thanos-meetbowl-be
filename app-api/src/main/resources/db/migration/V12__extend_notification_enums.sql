alter table notification
    modify column resource_type enum ('COMMUNITY_POST','MAIL','MEETING','MEETING_MINUTES');

alter table notification
    modify column type enum (
        'COMMUNITY_POST_COMMENTED',
        'COMMUNITY_POST_LIKED',
        'MAIL_RECEIVED',
        'MEETING_CANCELLED',
        'MEETING_REMINDER',
        'MEETING_UPDATED',
        'MINUTES_REVIEW_REMINDER',
        'MINUTES_REVIEW_REQUEST'
    ) not null;
