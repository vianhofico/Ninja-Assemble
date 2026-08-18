create table player_progression_tracks (
    player_id uuid not null references players(id) on delete cascade,
    track_id varchar(96) not null,
    level integer not null default 0 check (level >= 0),
    updated_at timestamptz not null default now(),
    primary key (player_id, track_id)
);

create table progression_upgrade_requests (
    request_id uuid primary key,
    player_id uuid not null references players(id) on delete cascade,
    track_id varchar(96) not null,
    result_json text not null,
    created_at timestamptz not null default now()
);
create index idx_progression_upgrade_player_created on progression_upgrade_requests(player_id, created_at desc);
