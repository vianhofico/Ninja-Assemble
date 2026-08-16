create unique index if not exists ux_player_equipment_hero_slot
    on player_equipment(equipped_player_hero_id, equipped_slot)
    where equipped_player_hero_id is not null and equipped_slot is not null;

create table player_college_scrolls (
    id uuid primary key default gen_random_uuid(),
    player_id uuid not null references players(id) on delete cascade,
    scroll_definition_id varchar(128) not null,
    level integer not null default 1 check (level between 1 and 10),
    created_at timestamptz not null default now()
);
create index idx_player_college_scrolls_player on player_college_scrolls(player_id, scroll_definition_id);

create table player_college_scroll_slots (
    player_hero_id uuid not null references player_heroes(id) on delete cascade,
    element varchar(16) not null,
    player_scroll_id uuid not null references player_college_scrolls(id) on delete cascade,
    primary key (player_hero_id, element),
    unique (player_scroll_id)
);

create table player_tailed_beast_progress (
    player_hero_id uuid primary key references player_heroes(id) on delete cascade,
    beast varchar(32) not null,
    stage integer not null default 0 check (stage between 0 and 10),
    soul_spent bigint not null default 0 check (soul_spent >= 0),
    beast_bone_spent bigint not null default 0 check (beast_bone_spent >= 0),
    updated_at timestamptz not null default now()
);
