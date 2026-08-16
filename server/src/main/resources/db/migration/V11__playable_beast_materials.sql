create table player_beast_materials (
    player_id uuid primary key references players(id) on delete cascade,
    beast_soul bigint not null default 0 check (beast_soul >= 0),
    beast_bone bigint not null default 0 check (beast_bone >= 0),
    updated_at timestamptz not null default now()
);
