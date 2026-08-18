create table competitive_battle_requests (
    request_id uuid primary key,
    mode varchar(32) not null check (mode in ('ARENA','SHADOW_ARENA')),
    player_id uuid not null references players(id) on delete cascade,
    opponent_player_id uuid not null references players(id) on delete cascade,
    season_id varchar(64) not null,
    result_json text not null,
    created_at timestamptz not null default now()
);
create index idx_competitive_requests_player_created on competitive_battle_requests(player_id, created_at desc);

create table competitive_season_results (
    player_id uuid not null references players(id) on delete cascade,
    mode varchar(32) not null check (mode in ('ARENA','SHADOW_ARENA')),
    season_id varchar(64) not null,
    final_rating bigint not null check (final_rating >= 0),
    reward_amount bigint not null default 0 check (reward_amount >= 0),
    claimed boolean not null default false,
    settled_at timestamptz not null default now(),
    claimed_at timestamptz,
    primary key (player_id, mode, season_id)
);

create table shadow_defense_formations (
    player_id uuid not null references players(id) on delete cascade,
    season_id varchar(64) not null,
    squad_index integer not null check (squad_index between 0 and 2),
    slot_index integer not null check (slot_index between 0 and 4),
    player_hero_id uuid not null references player_heroes(id) on delete cascade,
    updated_at timestamptz not null default now(),
    primary key (player_id, season_id, squad_index, slot_index),
    unique (player_id, season_id, player_hero_id)
);
create index idx_shadow_defense_player_season on shadow_defense_formations(player_id, season_id, squad_index, slot_index);
