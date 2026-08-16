-- M45: acquisition is Hero-Version based. Preserve pre-M45 summon identity only as an audit column.

alter table summon_history rename column hero_variant_id to legacy_hero_variant_id;
alter table summon_history alter column legacy_hero_variant_id drop not null;

alter table summon_history
    add column hero_version_id varchar(160) references hero_versions(hero_id);

with parsed as (
    select id,
           split_part(legacy_hero_variant_id, '::', 1) as legacy_character_id,
           case
               when split_part(legacy_hero_variant_id, '::', 2) in ('', 'BASE') then '__BASE__'
               else split_part(legacy_hero_variant_id, '::', 2)
           end as legacy_variant_id
    from summon_history
    where legacy_hero_variant_id is not null
), resolved as (
    select parsed.id, map.hero_version_id
    from parsed
    join legacy_variant_hero_version_map map
      on map.legacy_character_id = parsed.legacy_character_id
     and map.legacy_variant_id = parsed.legacy_variant_id
    where map.hero_version_id is not null
)
update summon_history history
set hero_version_id = resolved.hero_version_id
from resolved
where history.id = resolved.id;

create index ix_summon_history_hero_version on summon_history(player_id, hero_version_id, created_at desc)
    where hero_version_id is not null;

alter table summon_history
    add constraint ck_summon_history_identity
    check (hero_version_id is not null or legacy_hero_variant_id is not null);

comment on column summon_history.hero_version_id is
    'M45+ collectible Hero Version identity. New summon writes must use this column.';
comment on column summon_history.legacy_hero_variant_id is
    'Pre-M45 character::variant identity retained only for migration/audit; new summon writes leave it null.';
