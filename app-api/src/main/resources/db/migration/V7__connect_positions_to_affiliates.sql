alter table positions
    add column affiliate_id BINARY(16) null after id;

create index idx_positions_affiliate_sort
    on positions (affiliate_id, sort_order);

create temporary table tmp_position_primary_affiliate as
select
    u.position_id as old_position_id,
    min(u.affiliate_id) as primary_affiliate_id
from users u
where u.position_id is not null
  and u.affiliate_id is not null
group by u.position_id;

set @fallback_affiliate_id = (
    select a.id
    from affiliates a
    order by a.created_at asc, a.id asc
    limit 1
);

update positions p
left join tmp_position_primary_affiliate t
    on t.old_position_id = p.id
set p.affiliate_id = coalesce(t.primary_affiliate_id, @fallback_affiliate_id)
where p.affiliate_id is null;

create temporary table tmp_position_clone_map as
select
    u.position_id as old_position_id,
    u.affiliate_id as affiliate_id,
    UNHEX(REPLACE(UUID(), '-', '')) as new_position_id
from users u
join tmp_position_primary_affiliate t
    on t.old_position_id = u.position_id
where u.position_id is not null
  and u.affiliate_id is not null
  and u.affiliate_id <> t.primary_affiliate_id
group by u.position_id, u.affiliate_id;

insert into positions (
    id,
    affiliate_id,
    code,
    name,
    status,
    sort_order,
    created_at,
    updated_at
)
select
    c.new_position_id,
    c.affiliate_id,
    p.code,
    p.name,
    p.status,
    p.sort_order,
    p.created_at,
    p.updated_at
from tmp_position_clone_map c
join positions p
    on p.id = c.old_position_id;

update users u
join tmp_position_clone_map c
    on c.old_position_id = u.position_id
   and c.affiliate_id = u.affiliate_id
set u.position_id = c.new_position_id;

drop temporary table if exists tmp_position_clone_map;
drop temporary table if exists tmp_position_primary_affiliate;
