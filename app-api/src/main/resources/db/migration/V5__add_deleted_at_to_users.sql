alter table users
    add column deleted_at datetime(6) null after active_until;
