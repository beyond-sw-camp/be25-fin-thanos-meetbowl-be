create table meeting_ended_outbox (
    id BINARY(16) not null,
    correlation_id BINARY(16) not null,
    meeting_id BINARY(16) not null,
    organization_id BINARY(16) not null,
    host_user_id BINARY(16) not null,
    reviewer_user_id BINARY(16) not null,
    title varchar(200) not null,
    started_at datetime(6) not null,
    ended_at datetime(6) not null,
    occurred_at datetime(6) not null,
    publish_attempts integer not null,
    next_attempt_at datetime(6) not null,
    last_failure_reason varchar(500),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

create index idx_meeting_ended_outbox_ready
    on meeting_ended_outbox (next_attempt_at, created_at);
