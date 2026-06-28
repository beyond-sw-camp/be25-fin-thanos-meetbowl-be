alter table site
    add column affiliate_id BINARY(16) null after id;

create index idx_site_affiliate
    on site (affiliate_id);

set @fallback_affiliate_id = (
    select a.id
    from affiliates a
    order by a.sort_order asc, a.created_at asc, a.id asc
    limit 1
);

update site s
set s.affiliate_id = @fallback_affiliate_id
where s.affiliate_id is null;
