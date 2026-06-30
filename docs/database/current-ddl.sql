-- Meetbowl ERDCloud DDL (final)
-- Generated from the current meetbowl-be schema as of 2026-06-26.
-- Uses CREATE TABLE blocks with inline PK, unique constraints, and logical FK constraints for ERDCloud relationship rendering.
-- Polymorphic/reference-key columns such as target_id, related_resource_id, resource_id, source_id, source_event_id, idempotency_key, and correlation_id are intentionally not linked.
-- Standalone indexes are omitted because they do not affect ERD relationship rendering.

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
        constraint fk_departments_affiliate foreign key (affiliate_id) references affiliates (id),
        constraint fk_departments_parent_department foreign key (parent_department_id) references departments (id),
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

    create table site (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        name varchar(100) not null,
        address varchar(300),
        primary key (id)
    ) engine=InnoDB;

    create table building (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        site_id BINARY(16) not null,
        name varchar(100) not null,
        constraint fk_building_site foreign key (site_id) references site (id),
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
        constraint fk_meeting_room_building foreign key (building_id) references building (id),
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
        constraint fk_teams_department foreign key (department_id) references departments (id),
        primary key (id)
    ) engine=InnoDB;

    create table users (
        initial_password_change_required bit not null,
        active_from datetime(6),
        active_until datetime(6),
        created_at datetime(6) not null,
        deleted_at datetime(6),
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
        constraint fk_users_affiliate foreign key (affiliate_id) references affiliates (id),
        constraint fk_users_department foreign key (department_id) references departments (id),
        constraint fk_users_position foreign key (position_id) references positions (id),
        constraint fk_users_team foreign key (team_id) references teams (id),
        constraint uk_users_login_id unique (login_id),
        constraint uk_users_email unique (email),
        primary key (id)
    ) engine=InnoDB;

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
        target_login_id varchar(100),
        target_name varchar(100),
        target_type varchar(100) not null,
        user_agent varchar(500),
        after_value TEXT,
        before_value TEXT,
        result enum ('FAILURE','SUCCESS') not null,
        constraint fk_admin_audit_logs_actor_user foreign key (actor_id) references users (id),
        primary key (id)
    ) engine=InnoDB;

    create table community_alias (
        alias_no integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        user_id BINARY(16) not null,
        constraint fk_community_alias_user foreign key (user_id) references users (id),
        constraint uk_community_alias_user unique (user_id),
        constraint uk_community_alias_no unique (alias_no),
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
        constraint fk_community_post_author_user foreign key (author_user_id) references users (id),
        primary key (id)
    ) engine=InnoDB;

    create table community_comment (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        author_user_id BINARY(16) not null,
        id BINARY(16) not null,
        post_id BINARY(16) not null,
        content TEXT not null,
        constraint fk_community_comment_author_user foreign key (author_user_id) references users (id),
        constraint fk_community_comment_post foreign key (post_id) references community_post (id),
        primary key (id)
    ) engine=InnoDB;

    create table community_comment_like (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        comment_id BINARY(16) not null,
        id BINARY(16) not null,
        user_id BINARY(16) not null,
        constraint fk_community_comment_like_comment foreign key (comment_id) references community_comment (id),
        constraint fk_community_comment_like_user foreign key (user_id) references users (id),
        constraint uk_community_comment_like_comment_user unique (comment_id, user_id),
        primary key (id)
    ) engine=InnoDB;

    create table community_post_like (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        post_id BINARY(16) not null,
        user_id BINARY(16) not null,
        constraint fk_community_post_like_post foreign key (post_id) references community_post (id),
        constraint fk_community_post_like_user foreign key (user_id) references users (id),
        constraint uk_community_post_like_post_user unique (post_id, user_id),
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
        constraint fk_mail_organization_affiliate foreign key (organization_id) references affiliates (id),
        constraint fk_mail_sender_user foreign key (sender_user_id) references users (id),
        constraint uk_mail_idempotency_key unique (idempotency_key),
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
        constraint fk_mail_attachment_mail foreign key (mail_id) references mail (id),
        constraint fk_mail_attachment_uploader_user foreign key (uploader_user_id) references users (id),
        constraint uk_mail_attachment_object_key unique (object_key),
        primary key (id)
    ) engine=InnoDB;

    create table mail_recipient (
        recipient_order integer not null,
        mail_id BINARY(16) not null,
        recipient_user_id BINARY(16) not null,
        constraint fk_mail_recipient_mail foreign key (mail_id) references mail (id),
        constraint fk_mail_recipient_user foreign key (recipient_user_id) references users (id),
        constraint uk_mail_recipient_mail_user unique (mail_id, recipient_user_id),
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
        constraint fk_mail_retention_policies_updated_by foreign key (updated_by) references users (id),
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
        constraint fk_mailbox_entry_mail foreign key (mail_id) references mail (id),
        constraint fk_mailbox_entry_owner_user foreign key (owner_user_id) references users (id),
        constraint uk_mailbox_entry_mail_owner_type unique (mail_id, owner_user_id, mailbox_type),
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
        constraint fk_meeting_host_user foreign key (host_user_id) references users (id),
        constraint fk_meeting_room foreign key (meeting_room_id) references meeting_room (id),
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
        role enum ('HOST','PARTICIPANT','REVIEWER') not null,
        constraint fk_meeting_attendee_meeting foreign key (meeting_id) references meeting (id),
        constraint fk_meeting_attendee_user foreign key (user_id) references users (id),
        constraint uk_meeting_attendee_meeting_user unique (meeting_id, user_id),
        primary key (id)
    ) engine=InnoDB;

    create table meeting_ended_outbox (
        publish_attempts integer not null,
        created_at datetime(6) not null,
        ended_at datetime(6) not null,
        next_attempt_at datetime(6) not null,
        occurred_at datetime(6) not null,
        started_at datetime(6) not null,
        updated_at datetime(6) not null,
        correlation_id BINARY(16) not null,
        host_user_id BINARY(16) not null,
        id BINARY(16) not null,
        meeting_id BINARY(16) not null,
        organization_id BINARY(16) not null,
        reviewer_user_id BINARY(16) not null,
        title varchar(200) not null,
        last_failure_reason varchar(500),
        constraint fk_meeting_ended_outbox_meeting foreign key (meeting_id) references meeting (id),
        constraint fk_meeting_ended_outbox_organization foreign key (organization_id) references affiliates (id),
        constraint fk_meeting_ended_outbox_host_user foreign key (host_user_id) references users (id),
        constraint fk_meeting_ended_outbox_reviewer_user foreign key (reviewer_user_id) references users (id),
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
        constraint fk_meeting_transcript_segments_meeting foreign key (meeting_id) references meeting (id),
        constraint uk_transcript_segments_meeting_segment unique (meeting_id, segment_id),
        constraint uk_transcript_segments_meeting_sequence unique (meeting_id, sequence_no),
        constraint uk_transcript_segments_source_event unique (source_event_id),
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
        constraint fk_minutes_meeting foreign key (meeting_id) references meeting (id),
        constraint fk_minutes_organization_affiliate foreign key (organization_id) references affiliates (id),
        constraint fk_minutes_reviewer_user foreign key (reviewer_user_id) references users (id),
        constraint uk_minutes_meeting_id unique (meeting_id),
        primary key (id)
    ) engine=InnoDB;

    create table minutes_favorites (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        minutes_id BINARY(16) not null,
        user_id BINARY(16) not null,
        constraint fk_minutes_favorites_minutes foreign key (minutes_id) references minutes (id),
        constraint fk_minutes_favorites_user foreign key (user_id) references users (id),
        constraint uk_minutes_favorites_user_minutes unique (user_id, minutes_id),
        primary key (id)
    ) engine=InnoDB;

    create table minutes_generated_event_inbox (
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        event_id BINARY(16) not null,
        id BINARY(16) not null,
        meeting_id BINARY(16) not null,
        constraint fk_minutes_generated_event_inbox_meeting foreign key (meeting_id) references meeting (id),
        constraint uk_minutes_generated_event_id unique (event_id),
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
        constraint fk_notification_recipient_user foreign key (recipient_user_id) references users (id),
        primary key (id)
    ) engine=InnoDB;

    create table password_reset_requests (
        created_at datetime(6) not null,
        processed_at datetime(6),
        requested_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        processed_by_admin_id BINARY(16),
        user_id BINARY(16) not null,
        login_id varchar(100) not null,
        requester_name varchar(100) not null,
        email varchar(255) not null,
        status enum ('APPROVED','PENDING','REJECTED') not null,
        constraint fk_password_reset_requests_user foreign key (user_id) references users (id),
        constraint fk_password_reset_requests_processed_by foreign key (processed_by_admin_id) references users (id),
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
        constraint fk_personal_workspace_backups_owner_user foreign key (owner_user_id) references users (id),
        constraint uk_personal_workspace_backup_source unique (owner_user_id, source_type, source_id),
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
        constraint fk_pw_backup_attachment_backup foreign key (backup_id) references personal_workspace_backups (id),
        primary key (id)
    ) engine=InnoDB;

    create table personal_workspace_backup_bookmarks (
        bookmarked_at datetime(6) not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        backup_id BINARY(16) not null,
        id BINARY(16) not null,
        owner_user_id BINARY(16) not null,
        constraint fk_pw_backup_bookmarks_backup foreign key (backup_id) references personal_workspace_backups (id),
        constraint fk_pw_backup_bookmarks_owner_user foreign key (owner_user_id) references users (id),
        constraint uk_personal_workspace_backup_bookmark unique (owner_user_id, backup_id),
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
        constraint fk_pw_calendar_events_owner_user foreign key (owner_user_id) references users (id),
        constraint uk_calendar_event_owner_source unique (owner_user_id, source, source_id),
        primary key (id)
    ) engine=InnoDB;

    create table personal_workspace_calendar_subscriptions (
        created_at datetime(6) not null,
        subscribed_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        subscriber_user_id BINARY(16) not null,
        target_user_id BINARY(16) not null,
        constraint fk_pw_calendar_subscriptions_subscriber foreign key (subscriber_user_id) references users (id),
        constraint fk_pw_calendar_subscriptions_target foreign key (target_user_id) references users (id),
        constraint uk_personal_workspace_calendar_subscription_pair unique (subscriber_user_id, target_user_id),
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
        constraint fk_personal_workspace_drive_files_owner foreign key (owner_user_id) references users (id),
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
        constraint fk_personal_workspace_memos_owner foreign key (owner_user_id) references users (id),
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
        constraint fk_shared_workspaces_organization foreign key (organization_id) references affiliates (id),
        constraint fk_shared_workspaces_owner_user foreign key (owner_user_id) references users (id),
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
        constraint fk_shared_workspace_files_workspace foreign key (workspace_id) references shared_workspaces (id),
        constraint fk_shared_workspace_files_uploader foreign key (uploader_user_id) references users (id),
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
        constraint fk_shared_workspace_file_versions_file foreign key (file_id) references shared_workspace_files (id),
        constraint fk_shared_workspace_file_versions_uploader foreign key (uploader_user_id) references users (id),
        constraint uk_shared_workspace_file_version unique (file_id, version_major, version_minor, version_patch),
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
        constraint fk_shared_workspace_members_invited_by foreign key (invited_by_user_id) references users (id),
        constraint fk_shared_workspace_members_user foreign key (user_id) references users (id),
        constraint fk_shared_workspace_members_workspace foreign key (workspace_id) references shared_workspaces (id),
        constraint uk_shared_workspace_member unique (workspace_id, user_id),
        primary key (id)
    ) engine=InnoDB;

    create table user_settings (
        meeting_reminder_minutes_before integer not null,
        minutes_review_reminder_minutes integer not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        id BINARY(16) not null,
        user_id BINARY(16) not null,
        constraint fk_user_settings_user foreign key (user_id) references users (id),
        constraint uk_user_settings_user unique (user_id),
        primary key (id)
    ) engine=InnoDB;
