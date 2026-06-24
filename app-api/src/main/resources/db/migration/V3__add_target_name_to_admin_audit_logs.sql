alter table admin_audit_logs
    add column target_name varchar(100) null after target_login_id;
