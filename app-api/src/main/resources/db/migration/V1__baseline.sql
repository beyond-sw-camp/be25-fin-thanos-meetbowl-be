    create table admin_audit_logs (
        created_at datetime(6) not null,
        occurred_at datetime(6) not null,
        updated_at datetime(6) not null,
        actor_id BINARY(16) not null,
        id BINARY(16) not null,
        target_id BINARY(16),
        action_area varchar(100) not null,
        action_name varchar(100) not null,
        actor_name varchar(100) not null,
        ip_address varchar(100),
        target_type varchar(100) not null,
        user_agent varchar(500),
        after_value TEXT,
        before_value TEXT,
        result enum ('FAILURE','SUCCESS') not null,
        primary key (id)
    ) engine=InnoDB;

    create table affiliates (
        sort_order integer,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        code varchar(50),
        name varchar(100) not null,
        status enum ('ACTIVE','INACTIVE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table building (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        site_id BINARY(16) not null,
        name varchar(100) not null,
        primary key (id)
    ) engine=InnoDB;

    create table community_alias (
        alias_no integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        user_id BINARY(16) not null,
        primary key (id)
    ) engine=InnoDB;

    create table community_comment (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        author_user_id BINARY(16) not null,
        id BINARY(16) not null,
        post_id BINARY(16) not null,
        content TEXT not null,
        primary key (id)
    ) engine=InnoDB;

    create table community_comment_like (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        comment_id BINARY(16) not null,
        id BINARY(16) not null,
        user_id BINARY(16) not null,
        primary key (id)
    ) engine=InnoDB;

    create table community_post (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        view_count bigint not null,
        author_user_id BINARY(16) not null,
        id BINARY(16) not null,
        title varchar(200) not null,
        content TEXT not null,
        category enum ('COMPANY_LIFE','FREE','HOBBY','RESTAURANT') not null,
        primary key (id)
    ) engine=InnoDB;

    create table community_post_like (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        post_id BINARY(16) not null,
        user_id BINARY(16) not null,
        primary key (id)
    ) engine=InnoDB;

    create table departments (
        sort_order integer,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        affiliate_id BINARY(16),
        id BINARY(16) not null,
        parent_department_id BINARY(16),
        code varchar(50),
        name varchar(100) not null,
        status enum ('ACTIVE','INACTIVE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table mail (
        retry_count integer not null,
        created_at datetime(6) not null,
        failed_at datetime(6),
        requested_at datetime(6),
        sent_at datetime(6),
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        idempotency_key BINARY(16) not null,
        organization_id BINARY(16) not null,
        related_resource_id BINARY(16),
        sender_user_id BINARY(16) not null,
        failure_code varchar(100),
        subject varchar(200) not null,
        body MEDIUMTEXT not null,
        body_type enum ('MINUTES_SHARE','TEXT') not null,
        delivery_status enum ('DRAFT','FAILED','REQUESTED','RETRYING','SENT') not null,
        mail_type enum ('ANNOUNCEMENT','NORMAL','SYSTEM') not null,
        related_resource_type enum ('MEETING','MEETING_MINUTES','WORKSPACE'),
        primary key (id)
    ) engine=InnoDB;

    create table mail_attachment (
        created_at datetime(6) not null,
        size_bytes bigint not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        mail_id BINARY(16) not null,
        uploader_user_id BINARY(16) not null,
        mime_type varchar(150) not null,
        object_key varchar(500) not null,
        original_file_name varchar(255) not null,
        stored_file_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table mail_recipient (
        recipient_order integer not null,
        mail_id BINARY(16) not null,
        recipient_user_id BINARY(16) not null,
        primary key (recipient_order, mail_id)
    ) engine=InnoDB;

    create table mail_retention_policies (
        auto_delete_enabled bit not null,
        inbox_retention_days integer not null,
        sent_retention_days integer not null,
        trash_retention_days integer not null,
        created_at datetime(6) not null,
        policy_updated_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        updated_by BINARY(16) not null,
        primary key (id)
    ) engine=InnoDB;

    create table mailbox_entry (
        created_at datetime(6) not null,
        permanently_deleted_at datetime(6),
        read_at datetime(6),
        trashed_at datetime(6),
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        mail_id BINARY(16) not null,
        owner_user_id BINARY(16) not null,
        mailbox_type enum ('INBOX','SENT') not null,
        primary key (id)
    ) engine=InnoDB;

    create table meeting (
        created_at datetime(6) not null,
        ended_at datetime(6),
        scheduled_at datetime(6) not null,
        scheduled_end_at datetime(6) not null,
        started_at datetime(6),
        updated_at datetime(6) not null,
        host_user_id BINARY(16) not null,
        id BINARY(16) not null,
        meeting_room_id BINARY(16),
        provider varchar(50),
        provider_room_id varchar(200),
        title varchar(200) not null,
        description TEXT,
        status enum ('CANCELLED','ENDED','IN_PROGRESS','SCHEDULED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table meeting_attendee (
        reviewer boolean default false not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        meeting_id BINARY(16) not null,
        user_id BINARY(16) not null,
        attendance_status enum ('ACCEPTED','DECLINED','INVITED') not null,
        role enum ('HOST','PARTICIPANT') not null,
        primary key (id)
    ) engine=InnoDB;

    create table meeting_room (
        available bit not null,
        capacity integer not null,
        floor integer,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        building_id BINARY(16) not null,
        id BINARY(16) not null,
        name varchar(100) not null,
        location varchar(200),
        primary key (id)
    ) engine=InnoDB;

    create table meeting_transcript_segments (
        created_at datetime(6) not null,
        ended_at_ms bigint,
        sequence_no bigint not null,
        started_at_ms bigint,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        meeting_id BINARY(16) not null,
        source_event_id BINARY(16) not null,
        segment_id varchar(100) not null,
        en_text TEXT not null,
        ko_text TEXT not null,
        source_text TEXT not null,
        source_language enum ('EN','KO','UNKNOWN') not null,
        primary key (id)
    ) engine=InnoDB;

    create table minutes (
        approved_at datetime(6),
        created_at datetime(6) not null,
        deletion_scheduled_at datetime(6),
        shared_at datetime(6),
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        meeting_id BINARY(16) not null,
        organization_id BINARY(16) not null,
        reviewer_user_id BINARY(16) not null,
        model varchar(100) not null,
        prompt_version varchar(100) not null,
        content TEXT not null,
        summary TEXT not null,
        status enum ('APPROVED','DELETION_SCHEDULED','DRAFT','IN_REVIEW','SHARED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table minutes_favorites (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        minutes_id BINARY(16) not null,
        user_id BINARY(16) not null,
        primary key (id)
    ) engine=InnoDB;

    create table minutes_generated_event_inbox (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        event_id BINARY(16) not null,
        id BINARY(16) not null,
        meeting_id BINARY(16) not null,
        primary key (id)
    ) engine=InnoDB;

    create table notification (
        created_at datetime(6) not null,
        read_at datetime(6),
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        recipient_user_id BINARY(16) not null,
        resource_id BINARY(16),
        title varchar(200) not null,
        content TEXT not null,
        resource_type enum ('MEETING','MEETING_MINUTES'),
        type enum ('MEETING_CANCELLED','MEETING_REMINDER','MEETING_UPDATED','MINUTES_REVIEW_REMINDER','MINUTES_REVIEW_REQUEST') not null,
        primary key (id)
    ) engine=InnoDB;

    create table personal_workspace_backup_attachments (
        created_at datetime(6) not null,
        size_bytes bigint not null,
        updated_at datetime(6) not null,
        backup_id BINARY(16) not null,
        id BINARY(16) not null,
        mime_type varchar(150) not null,
        object_key varchar(500) not null,
        original_file_name varchar(255) not null,
        stored_file_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table personal_workspace_backup_bookmarks (
        bookmarked_at datetime(6) not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        backup_id BINARY(16) not null,
        id BINARY(16) not null,
        owner_user_id BINARY(16) not null,
        primary key (id)
    ) engine=InnoDB;

    create table personal_workspace_backups (
        backed_up_at datetime(6) not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        owner_user_id BINARY(16) not null,
        source_id BINARY(16) not null,
        title varchar(150) not null,
        summary varchar(1000),
        body MEDIUMTEXT,
        source_type enum ('MAIL') not null,
        primary key (id)
    ) engine=InnoDB;

    create table personal_workspace_calendar_events (
        all_day bit not null,
        created_at datetime(6) not null,
        ended_at datetime(6) not null,
        started_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        owner_user_id BINARY(16) not null,
        source_id BINARY(16),
        title varchar(100) not null,
        description varchar(1000),
        source enum ('MEETING','PERSONAL') not null,
        primary key (id)
    ) engine=InnoDB;

    create table personal_workspace_calendar_subscriptions (
        created_at datetime(6) not null,
        subscribed_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        subscriber_user_id BINARY(16) not null,
        target_user_id BINARY(16) not null,
        primary key (id)
    ) engine=InnoDB;

    create table personal_workspace_drive_files (
        created_at datetime(6) not null,
        deleted_at datetime(6),
        size_bytes bigint not null,
        updated_at datetime(6) not null,
        uploaded_at datetime(6) not null,
        id BINARY(16) not null,
        owner_user_id BINARY(16) not null,
        content_type varchar(100),
        storage_key varchar(500) not null,
        original_file_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table personal_workspace_memos (
        created_at datetime(6) not null,
        memo_created_at datetime(6) not null,
        memo_updated_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        owner_user_id BINARY(16) not null,
        title varchar(100) not null,
        content varchar(10000) not null,
        primary key (id)
    ) engine=InnoDB;

    create table positions (
        sort_order integer,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        code varchar(50),
        name varchar(100) not null,
        status enum ('ACTIVE','INACTIVE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table shared_workspace_file_versions (
        version_major integer not null,
        version_minor integer not null,
        version_patch integer not null,
        created_at datetime(6) not null,
        size_bytes bigint not null,
        updated_at datetime(6) not null,
        uploaded_at datetime(6) not null,
        file_id BINARY(16) not null,
        id BINARY(16) not null,
        uploader_user_id BINARY(16) not null,
        content_type varchar(100),
        storage_key varchar(500) not null,
        change_memo varchar(1000),
        original_file_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table shared_workspace_files (
        current_version_major integer not null,
        current_version_minor integer not null,
        current_version_patch integer not null,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        size_bytes bigint not null,
        updated_at datetime(6) not null,
        uploaded_at datetime(6) not null,
        id BINARY(16) not null,
        uploader_user_id BINARY(16) not null,
        workspace_id BINARY(16) not null,
        content_type varchar(100),
        storage_key varchar(500) not null,
        original_file_name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table shared_workspace_members (
        created_at datetime(6) not null,
        joined_at datetime(6) not null,
        removed_at datetime(6),
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        invited_by_user_id BINARY(16) not null,
        user_id BINARY(16) not null,
        workspace_id BINARY(16) not null,
        role enum ('MEMBER','OWNER') not null,
        status enum ('ACTIVE','REMOVED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table shared_workspaces (
        created_at datetime(6) not null,
        deleted_at datetime(6),
        updated_at datetime(6) not null,
        workspace_created_at datetime(6) not null,
        id BINARY(16) not null,
        organization_id BINARY(16) not null,
        owner_user_id BINARY(16) not null,
        name varchar(100) not null,
        description varchar(1000),
        visibility enum ('MEMBERS_ONLY','ORGANIZATION') not null,
        primary key (id)
    ) engine=InnoDB;

    create table site (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        name varchar(100) not null,
        address varchar(300),
        primary key (id)
    ) engine=InnoDB;

    create table teams (
        sort_order integer,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        department_id BINARY(16),
        id BINARY(16) not null,
        code varchar(50),
        name varchar(100) not null,
        status enum ('ACTIVE','INACTIVE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table user_settings (
        meeting_reminder_minutes_before integer not null,
        minutes_review_reminder_minutes integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        user_id BINARY(16) not null,
        primary key (id)
    ) engine=InnoDB;

    create table users (
        initial_password_change_required bit not null,
        active_from datetime(6),
        active_until datetime(6),
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        affiliate_id BINARY(16),
        department_id BINARY(16),
        id BINARY(16) not null,
        position_id BINARY(16),
        team_id BINARY(16),
        login_id varchar(100) not null,
        name varchar(100) not null,
        email varchar(255) not null,
        password_hash varchar(255) not null,
        role enum ('ADMIN','SYSTEM','USER') not null,
        status enum ('ACTIVE','INACTIVE','LOCKED') not null,
        primary key (id)
    ) engine=InnoDB;

    create index idx_building_site 
       on building (site_id);

    alter table if exists community_alias 
       add constraint uk_community_alias_user unique (user_id);

    alter table if exists community_alias 
       add constraint uk_community_alias_no unique (alias_no);

    create index idx_community_comment_post 
       on community_comment (post_id);

    alter table if exists community_comment_like 
       add constraint uk_community_comment_like_comment_user unique (comment_id, user_id);

    create index idx_community_post_category 
       on community_post (category);

    alter table if exists community_post_like 
       add constraint uk_community_post_like_post_user unique (post_id, user_id);

    create index idx_mail_organization_requested_at 
       on mail (organization_id, requested_at);

    create index idx_mail_sender_requested_at 
       on mail (sender_user_id, requested_at);

    create index idx_mail_delivery_status 
       on mail (delivery_status);

    alter table if exists mail 
       add constraint uk_mail_idempotency_key unique (idempotency_key);

    create index idx_mail_attachment_mail 
       on mail_attachment (mail_id);

    alter table if exists mail_attachment 
       add constraint uk_mail_attachment_object_key unique (object_key);

    create index idx_mail_recipient_user 
       on mail_recipient (recipient_user_id);

    alter table if exists mail_recipient 
       add constraint uk_mail_recipient_mail_user unique (mail_id, recipient_user_id);

    create index idx_mailbox_entry_owner_type 
       on mailbox_entry (owner_user_id, mailbox_type);

    create index idx_mailbox_entry_owner_trashed 
       on mailbox_entry (owner_user_id, trashed_at);

    alter table if exists mailbox_entry 
       add constraint uk_mailbox_entry_mail_owner_type unique (mail_id, owner_user_id, mailbox_type);

    create index idx_meeting_host 
       on meeting (host_user_id);

    create index idx_meeting_room_scheduled 
       on meeting (meeting_room_id, scheduled_at);

    create index idx_meeting_attendee_user 
       on meeting_attendee (user_id, meeting_id);

    alter table if exists meeting_attendee 
       add constraint uk_meeting_attendee_meeting_user unique (meeting_id, user_id);

    create index idx_meeting_room_building 
       on meeting_room (building_id);

    create index idx_transcript_segments_meeting_sequence 
       on meeting_transcript_segments (meeting_id, sequence_no);

    alter table if exists meeting_transcript_segments 
       add constraint uk_transcript_segments_meeting_segment unique (meeting_id, segment_id);

    alter table if exists meeting_transcript_segments 
       add constraint uk_transcript_segments_meeting_sequence unique (meeting_id, sequence_no);

    alter table if exists meeting_transcript_segments 
       add constraint uk_transcript_segments_source_event unique (source_event_id);

    alter table if exists minutes 
       add constraint uk_minutes_meeting_id unique (meeting_id);

    alter table if exists minutes_favorites 
       add constraint uk_minutes_favorites_user_minutes unique (user_id, minutes_id);

    alter table if exists minutes_generated_event_inbox 
       add constraint uk_minutes_generated_event_id unique (event_id);

    create index idx_notification_recipient_created 
       on notification (recipient_user_id, created_at);

    create index idx_pw_backup_attachment_backup 
       on personal_workspace_backup_attachments (backup_id);

    alter table if exists personal_workspace_backup_bookmarks 
       add constraint uk_personal_workspace_backup_bookmark unique (owner_user_id, backup_id);

    alter table if exists personal_workspace_backups 
       add constraint uk_personal_workspace_backup_source unique (owner_user_id, source_type, source_id);

    alter table if exists personal_workspace_calendar_events 
       add constraint uk_calendar_event_owner_source unique (owner_user_id, source, source_id);

    alter table if exists personal_workspace_calendar_subscriptions 
       add constraint uk_personal_workspace_calendar_subscription_pair unique (subscriber_user_id, target_user_id);

    create index idx_shared_workspace_file_version_file 
       on shared_workspace_file_versions (file_id);

    alter table if exists shared_workspace_file_versions 
       add constraint uk_shared_workspace_file_version unique (file_id, version_major, version_minor, version_patch);

    create index idx_shared_workspace_file_workspace 
       on shared_workspace_files (workspace_id);

    create index idx_shared_workspace_member_user 
       on shared_workspace_members (user_id);

    create index idx_shared_workspace_member_workspace 
       on shared_workspace_members (workspace_id);

    alter table if exists shared_workspace_members 
       add constraint uk_shared_workspace_member unique (workspace_id, user_id);

    create index idx_shared_workspace_owner 
       on shared_workspaces (owner_user_id);

    create index idx_shared_workspace_org_visibility 
       on shared_workspaces (organization_id, visibility);

    alter table if exists user_settings 
       add constraint UK4bos7satl9xeqd18frfeqg6tt unique (user_id);

    alter table if exists users 
       add constraint UKi3xs7wmfu2i3jt079uuetycit unique (login_id);

    alter table if exists users 
       add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);

    alter table if exists mail_attachment 
       add constraint fk_mail_attachment_mail 
       foreign key (mail_id) 
       references mail (id);

    alter table if exists mail_recipient 
       add constraint FKe4lsrti3p5m0v5q3xobtmut6p 
       foreign key (mail_id) 
       references mail (id);

    alter table if exists personal_workspace_backup_attachments 
       add constraint fk_pw_backup_attachment_backup 
       foreign key (backup_id) 
       references personal_workspace_backups (id);
