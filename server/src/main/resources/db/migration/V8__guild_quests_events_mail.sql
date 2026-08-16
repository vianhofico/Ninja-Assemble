create table guilds (
    id uuid primary key default gen_random_uuid(),
    name varchar(64) not null unique,
    level integer not null default 1 check (level > 0),
    exp bigint not null default 0 check (exp >= 0),
    notice text,
    created_at timestamptz not null default now()
);

create table guild_members (
    guild_id uuid not null references guilds(id) on delete cascade,
    player_id uuid not null references players(id) on delete cascade,
    role varchar(16) not null,
    contribution bigint not null default 0 check (contribution >= 0),
    joined_at timestamptz not null default now(),
    primary key (guild_id, player_id),
    unique (player_id)
);

create table guild_contribution_ledger (
    id uuid primary key default gen_random_uuid(),
    guild_id uuid not null references guilds(id) on delete cascade,
    player_id uuid not null references players(id) on delete cascade,
    delta bigint not null check (delta > 0),
    source varchar(128) not null,
    created_at timestamptz not null default now()
);

create table guild_boss_runs (
    id uuid primary key default gen_random_uuid(),
    guild_id uuid not null references guilds(id) on delete cascade,
    boss_definition_id varchar(128) not null,
    reset_key varchar(96) not null,
    max_hp bigint not null check (max_hp > 0),
    current_hp bigint not null check (current_hp >= 0),
    state jsonb not null default '{}'::jsonb,
    unique (guild_id, boss_definition_id, reset_key)
);

create table player_quest_progress (
    player_id uuid not null references players(id) on delete cascade,
    quest_id varchar(128) not null,
    reset_key varchar(96) not null,
    current_value bigint not null default 0 check (current_value >= 0),
    claimed boolean not null default false,
    updated_at timestamptz not null default now(),
    primary key (player_id, quest_id, reset_key)
);

create table player_event_progress (
    player_id uuid not null references players(id) on delete cascade,
    event_id varchar(128) not null,
    objective_state jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now(),
    primary key (player_id, event_id)
);

create table player_mail (
    id uuid primary key default gen_random_uuid(),
    player_id uuid not null references players(id) on delete cascade,
    subject_key varchar(160) not null,
    body_key varchar(160) not null,
    attachments jsonb not null default '[]'::jsonb,
    read boolean not null default false,
    claimed boolean not null default false,
    created_at timestamptz not null default now(),
    expires_at timestamptz
);
create index idx_player_mail_player_created on player_mail(player_id, created_at desc);
