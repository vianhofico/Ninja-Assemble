create table competitive_daily_attempts (
    player_id uuid not null references players(id) on delete cascade,
    mode varchar(32) not null check (mode in ('ARENA','SHADOW_ARENA')),
    season_id varchar(64) not null,
    game_date date not null,
    attempts_used integer not null default 0 check (attempts_used >= 0),
    updated_at timestamptz not null default now(),
    primary key (player_id, mode, season_id, game_date)
);
create index idx_competitive_daily_attempts_date on competitive_daily_attempts(game_date, mode, season_id);
