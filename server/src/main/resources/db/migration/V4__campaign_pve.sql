create table campaign_stage_progress (
    player_id uuid not null references players(id) on delete cascade,
    stage_id varchar(128) not null,
    clear_count integer not null default 0 check (clear_count >= 0),
    best_stars integer not null default 0 check (best_stars between 0 and 3),
    first_cleared_at timestamptz,
    last_cleared_at timestamptz,
    primary key (player_id, stage_id)
);

create table campaign_runs (
    id uuid primary key default gen_random_uuid(),
    player_id uuid not null references players(id) on delete cascade,
    stage_id varchar(128) not null,
    battle_seed bigint not null,
    ruleset_version varchar(96) not null,
    result varchar(16),
    reward_grant_key varchar(160),
    started_at timestamptz not null default now(),
    completed_at timestamptz
);
create index idx_campaign_runs_player_started on campaign_runs(player_id, started_at desc);

create table pve_mode_progress (
    player_id uuid not null references players(id) on delete cascade,
    mode_id varchar(128) not null,
    game_date date not null,
    attempts integer not null default 0 check (attempts >= 0),
    clears integer not null default 0 check (clears >= 0),
    best_score bigint,
    primary key (player_id, mode_id, game_date)
);
