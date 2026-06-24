alter table admin_audit_logs
    add column target_login_id varchar(100) null after target_id;
