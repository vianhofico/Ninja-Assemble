create table team_synergy_definitions (
    id varchar(128) primary key,
    name_key varchar(160) not null,
    minimum_members integer not null check (minimum_members > 0),
    required_character_ids jsonb not null default '[]'::jsonb,
    bonuses jsonb not null default '[]'::jsonb,
    parity_status varchar(24) not null default 'DISCOVERED'
);

create table player_team_synergy_state (
    player_id uuid not null references players(id) on delete cascade,
    synergy_id varchar(128) not null,
    discovered boolean not null default false,
    first_activated_at timestamptz,
    primary key (player_id, synergy_id)
);

create table jinchuriki_definitions (
    character_id varchar(128) primary key,
    beast varchar(32) not null,
    progression_track_id varchar(128) not null,
    compatible_variants jsonb not null default '[]'::jsonb,
    parity_status varchar(24) not null default 'DISCOVERED'
);

create table scroll_definitions (
    id varchar(128) primary key,
    name_key varchar(160) not null,
    element varchar(16) not null,
    max_level integer not null check (max_level > 0),
    stats jsonb not null default '{}'::jsonb,
    parity_status varchar(24) not null default 'DISCOVERED'
);
