create table meeting_external_invitee (
    id binary(16) not null,
    meeting_id binary(16) not null,
    name varchar(120) not null,
    email varchar(255) not null,
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    primary key (id)
);

create index idx_meeting_external_invitee_meeting
    on meeting_external_invitee (meeting_id);

alter table if exists meeting_external_invitee
    add constraint fk_meeting_external_invitee_meeting
    foreign key (meeting_id)
    references meeting (id);

create table mail_external_recipient (
    mail_id binary(16) not null,
    recipient_order integer not null,
    recipient_name varchar(120) not null,
    recipient_email varchar(255) not null,
    primary key (mail_id, recipient_order)
);

create index idx_mail_external_recipient_email
    on mail_external_recipient (recipient_email);

alter table if exists mail_external_recipient
    add constraint fk_mail_external_recipient_mail
    foreign key (mail_id)
    references mail (id);
