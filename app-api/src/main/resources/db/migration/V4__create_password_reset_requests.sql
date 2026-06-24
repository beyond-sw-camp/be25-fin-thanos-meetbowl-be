create table password_reset_requests (
    created_at datetime(6) not null,
    requested_at datetime(6) not null,
    updated_at datetime(6) not null,
    user_id BINARY(16) not null,
    id BINARY(16) not null,
    processed_by_admin_id BINARY(16),
    requester_name varchar(100) not null,
    login_id varchar(100) not null,
    email varchar(255) not null,
    status enum ('APPROVED','PENDING','REJECTED') not null,
    processed_at datetime(6),
    primary key (id)
) engine=InnoDB;

create index idx_password_reset_requests_status_requested_at
    on password_reset_requests (status, requested_at);
