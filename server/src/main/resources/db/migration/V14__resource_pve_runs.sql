create table resource_pve_runs (
    request_id uuid primary key,
    player_id uuid not null references players(id) on delete cascade,
    mode_id varchar(128) not null,
    game_date date not null,
    result_json text not null,
    created_at timestamptz not null default now()
);
create index idx_resource_pve_runs_player_date on resource_pve_runs(player_id, game_date desc, created_at desc);
create index idx_resource_pve_runs_mode_date on resource_pve_runs(mode_id, game_date desc);
