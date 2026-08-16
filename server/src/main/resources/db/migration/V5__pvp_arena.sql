create table arena_profiles (
    player_id uuid primary key references players(id) on delete cascade,
    season_id varchar(64) not null,
    rating bigint not null default 0 check (rating >= 0),
    defense_formation_id uuid references formations(id) on delete set null,
    updated_at timestamptz not null default now()
);

create table arena_opponent_snapshots (
    id uuid primary key default gen_random_uuid(),
    player_id uuid not null references players(id) on delete cascade,
    opponent_player_id uuid not null references players(id) on delete cascade,
    season_id varchar(64) not null,
    opponent_rating bigint not null check (opponent_rating >= 0),
    formation_snapshot jsonb not null,
    captured_at timestamptz not null default now()
);
create index idx_arena_snapshot_player_captured on arena_opponent_snapshots(player_id, captured_at desc);

create table arena_battles (
    id uuid primary key default gen_random_uuid(),
    challenger_id uuid not null references players(id) on delete cascade,
    opponent_id uuid not null references players(id) on delete cascade,
    opponent_snapshot_id uuid references arena_opponent_snapshots(id) on delete set null,
    battle_seed bigint not null,
    ruleset_version varchar(96) not null,
    rating_profile_version varchar(96) not null,
    result varchar(16),
    rating_before bigint,
    rating_after bigint,
    reward_grant_key varchar(160),
    created_at timestamptz not null default now()
);

create table shadow_arena_profiles (
    player_id uuid not null references players(id) on delete cascade,
    season_id varchar(64) not null,
    rating bigint not null default 0 check (rating >= 0),
    roster_snapshot jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now(),
    primary key (player_id, season_id)
);

create table shadow_arena_battles (
    id uuid primary key default gen_random_uuid(),
    challenger_id uuid not null references players(id) on delete cascade,
    opponent_id uuid not null references players(id) on delete cascade,
    season_id varchar(64) not null,
    squad_results jsonb not null default '[]'::jsonb,
    winner varchar(16),
    reward_grant_key varchar(160),
    created_at timestamptz not null default now()
);
