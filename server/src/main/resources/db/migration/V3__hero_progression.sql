alter table player_heroes add column if not exists frame_plus integer not null default 0 check (frame_plus >= 0);
alter table player_heroes add column if not exists current_variant_id varchar(128);
alter table player_heroes add column if not exists awakening_level integer not null default 0 check (awakening_level >= 0);
alter table player_heroes add column if not exists transformation_state jsonb not null default '{}'::jsonb;
alter table player_heroes add column if not exists scroll_state jsonb not null default '{}'::jsonb;

create table hero_variant_unlocks (
    player_hero_id uuid not null references player_heroes(id) on delete cascade,
    variant_id varchar(128) not null,
    unlocked_at timestamptz not null default now(),
    source varchar(96),
    primary key (player_hero_id, variant_id)
);

create table player_scrolls (
    id uuid primary key default gen_random_uuid(),
    player_id uuid not null references players(id) on delete cascade,
    scroll_definition_id varchar(128) not null,
    level integer not null default 1 check (level > 0),
    locked boolean not null default false,
    created_at timestamptz not null default now()
);

create table player_hero_scroll_slots (
    player_hero_id uuid not null references player_heroes(id) on delete cascade,
    element varchar(16) not null check (element in ('YIN_YANG','FIRE','WATER','WIND','EARTH','LIGHTNING')),
    player_scroll_id uuid not null references player_scrolls(id) on delete cascade,
    primary key (player_hero_id, element),
    unique (player_scroll_id)
);

create table hero_progression_events (
    id uuid primary key default gen_random_uuid(),
    player_hero_id uuid not null references player_heroes(id) on delete cascade,
    track varchar(32) not null,
    before_state jsonb not null default '{}'::jsonb,
    after_state jsonb not null default '{}'::jsonb,
    source varchar(128),
    created_at timestamptz not null default now()
);
create index idx_progression_events_hero_created on hero_progression_events(player_hero_id, created_at desc);
