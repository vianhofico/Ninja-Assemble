create table play_action_log (
    player_id uuid not null references players(id) on delete cascade,
    request_key varchar(160) not null,
    action_type varchar(48) not null,
    result_text text,
    created_at timestamptz not null default now(),
    completed_at timestamptz,
    primary key (player_id, request_key)
);

create index idx_play_action_log_player_created on play_action_log(player_id, created_at desc);

create index if not exists idx_player_heroes_player_id on player_heroes(player_id, id);
create index if not exists idx_variant_unlocks_player_hero on hero_variant_unlocks(player_hero_id);
