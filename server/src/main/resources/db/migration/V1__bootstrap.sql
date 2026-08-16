create extension if not exists pgcrypto;

create table players (
    id uuid primary key default gen_random_uuid(), display_name varchar(64) not null,
    account_level integer not null default 1 check (account_level > 0), account_exp bigint not null default 0 check (account_exp >= 0),
    energy integer not null default 0 check (energy >= 0), created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table hero_definitions (
    id varchar(96) primary key, display_name varchar(96) not null, variant varchar(96),
    archetype varchar(16) not null check (archetype in ('PHYSICAL','CHAKRA','HYBRID','UNKNOWN')),
    definition_version integer not null default 1 check (definition_version > 0),
    parity_status varchar(24) not null default 'DISCOVERED' check (parity_status in ('DISCOVERED','DOCUMENTED','IMPLEMENTED','VERIFIED','PARITY_PASS')),
    content jsonb not null default '{}'::jsonb
);
create table player_heroes (
    id uuid primary key default gen_random_uuid(), player_id uuid not null references players(id) on delete cascade,
    hero_definition_id varchar(96) not null references hero_definitions(id), level integer not null default 1 check (level > 0),
    exp bigint not null default 0 check (exp >= 0), frame_tier varchar(16) not null default 'GENIN' check (frame_tier in ('GENIN','CHUNIN','JONIN','KAGE','SIX_PATH','AWAKENING')),
    frame_advance_step integer not null default 0 check (frame_advance_step >= 0), tailed_beast_state jsonb not null default '{}'::jsonb,
    skill_state jsonb not null default '{}'::jsonb, created_at timestamptz not null default now(), unique (player_id, hero_definition_id)
);
create table formations (
    id uuid primary key default gen_random_uuid(), player_id uuid not null references players(id) on delete cascade,
    mode varchar(24) not null, squad_index integer not null default 0 check (squad_index >= 0), is_defense boolean not null default false,
    unique (player_id, mode, squad_index, is_defense)
);
create table formation_slots (
    formation_id uuid not null references formations(id) on delete cascade, slot_index integer not null check (slot_index between 0 and 4),
    player_hero_id uuid not null references player_heroes(id) on delete cascade, primary key (formation_id, slot_index), unique (formation_id, player_hero_id)
);
create table wallet_balances (
    player_id uuid not null references players(id) on delete cascade, currency varchar(32) not null,
    amount bigint not null default 0 check (amount >= 0), primary key (player_id, currency)
);
create index idx_player_heroes_player on player_heroes(player_id);
create index idx_formations_player_mode on formations(player_id, mode);
