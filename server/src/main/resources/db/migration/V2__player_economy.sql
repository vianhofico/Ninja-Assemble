alter table players add column if not exists guest_key varchar(96);
alter table players add column if not exists energy_cap integer not null default 120 check (energy_cap > 0);
alter table players add column if not exists energy_updated_at timestamptz not null default now();
create unique index if not exists ux_players_guest_key on players(guest_key) where guest_key is not null;

create table wallet_ledger (
    id uuid primary key,
    player_id uuid not null references players(id) on delete cascade,
    currency varchar(32) not null,
    delta bigint not null,
    balance_before bigint not null check (balance_before >= 0),
    balance_after bigint not null check (balance_after >= 0),
    reason varchar(64) not null,
    source varchar(128),
    idempotency_key varchar(160),
    created_at timestamptz not null default now()
);
create unique index ux_wallet_ledger_idempotency on wallet_ledger(player_id, idempotency_key) where idempotency_key is not null;
create index idx_wallet_ledger_player_created on wallet_ledger(player_id, created_at desc);

create table reward_grants (
    id uuid primary key,
    player_id uuid not null references players(id) on delete cascade,
    grant_key varchar(160) not null,
    payload jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    unique(player_id, grant_key)
);
