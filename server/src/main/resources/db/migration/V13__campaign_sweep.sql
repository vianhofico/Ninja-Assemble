create table campaign_sweeps (
    request_id uuid primary key,
    player_id uuid not null references players(id) on delete cascade,
    stage_id varchar(128) not null,
    catalog_version varchar(96) not null,
    energy_cost integer not null check (energy_cost > 0),
    energy_after integer not null check (energy_after >= 0),
    player_exp bigint not null default 0 check (player_exp >= 0),
    gold bigint not null default 0 check (gold >= 0),
    diamond bigint not null default 0 check (diamond >= 0),
    account_level_after integer not null check (account_level_after > 0),
    item_rewards text not null default '',
    created_at timestamptz not null default now()
);
create index idx_campaign_sweeps_player_created on campaign_sweeps(player_id, created_at desc);
create index idx_campaign_sweeps_player_stage on campaign_sweeps(player_id, stage_id, created_at desc);
