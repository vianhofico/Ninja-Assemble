create table competitive_daily_attempts (
    player_id uuid not null references players(id) on delete cascade,
    mode varchar(24) not null check (mode in ('ARENA', 'SHADOW_ARENA')),
    season_id varchar(64) not null,
    game_date date not null,
    attempts_used integer not null default 0 check (attempts_used >= 0),
    updated_at timestamptz not null default now(),
    primary key (player_id, mode, season_id, game_date)
);

create table competitive_action_runs (
    player_id uuid not null references players(id) on delete cascade,
    mode varchar(24) not null check (mode in ('ARENA', 'SHADOW_ARENA')),
    request_id uuid not null,
    opponent_player_id uuid not null references players(id) on delete cascade,
    season_id varchar(64) not null,
    game_date date not null,
    result_json jsonb not null,
    created_at timestamptz not null default now(),
    primary key (player_id, mode, request_id)
);

create index idx_competitive_runs_player_mode_created
    on competitive_action_runs(player_id, mode, created_at desc);
