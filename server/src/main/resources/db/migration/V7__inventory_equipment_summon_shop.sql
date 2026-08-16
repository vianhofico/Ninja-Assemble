create table inventory_stacks (
    player_id uuid not null references players(id) on delete cascade,
    item_definition_id varchar(160) not null,
    item_type varchar(32) not null,
    quantity bigint not null default 0 check (quantity >= 0),
    primary key (player_id, item_definition_id)
);

create table inventory_ledger (
    id uuid primary key default gen_random_uuid(),
    player_id uuid not null references players(id) on delete cascade,
    item_definition_id varchar(160) not null,
    delta bigint not null,
    balance_before bigint not null check (balance_before >= 0),
    balance_after bigint not null check (balance_after >= 0),
    reason varchar(96) not null,
    idempotency_key varchar(160),
    created_at timestamptz not null default now()
);
create unique index ux_inventory_ledger_idempotency on inventory_ledger(player_id, idempotency_key) where idempotency_key is not null;

create table equipment_definitions (
    id varchar(160) primary key,
    name_key varchar(160) not null,
    slot varchar(24) not null,
    rarity varchar(16) not null,
    max_enhance_level integer not null default 0,
    set_id varchar(128),
    content jsonb not null default '{}'::jsonb,
    parity_status varchar(24) not null default 'DISCOVERED'
);

create table player_equipment (
    id uuid primary key default gen_random_uuid(),
    player_id uuid not null references players(id) on delete cascade,
    equipment_definition_id varchar(160) not null,
    enhance_level integer not null default 0,
    refine_level integer not null default 0,
    rolled_stats jsonb not null default '{}'::jsonb,
    equipped_player_hero_id uuid references player_heroes(id) on delete set null,
    equipped_slot varchar(24)
);

create table summon_state (
    player_id uuid not null references players(id) on delete cascade,
    banner_id varchar(128) not null,
    banner_version varchar(96) not null,
    pulls_since_pity integer not null default 0 check (pulls_since_pity >= 0),
    total_pulls bigint not null default 0 check (total_pulls >= 0),
    primary key (player_id, banner_id)
);

create table summon_history (
    id uuid primary key default gen_random_uuid(),
    player_id uuid not null references players(id) on delete cascade,
    banner_id varchar(128) not null,
    banner_version varchar(96) not null,
    seed bigint not null,
    hero_variant_id varchar(160) not null,
    rarity varchar(16) not null,
    pity_triggered boolean not null,
    duplicate boolean not null default false,
    created_at timestamptz not null default now()
);

create table shop_purchase_state (
    player_id uuid not null references players(id) on delete cascade,
    shop_id varchar(128) not null,
    offer_id varchar(128) not null,
    reset_key varchar(96) not null,
    purchase_count integer not null default 0 check (purchase_count >= 0),
    primary key (player_id, shop_id, offer_id, reset_key)
);
