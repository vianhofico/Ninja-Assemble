package com.ninjaassemble.guild.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.guild.domain.GuildBossState;
import com.ninjaassemble.guild.domain.GuildMemberState;
import com.ninjaassemble.guild.domain.GuildRole;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.play.application.FormationService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class GuildApplicationService {
    public static final String PROFILE_VERSION = "guild-loop-design-v1";
    public static final String BOSS_DAMAGE_PROFILE_VERSION = "guild-boss-power-damage-v1";
    private static final int MEMBER_CAP = 30;
    private static final String BOSS_ID = "training-beast-v1";
    private static final long BOSS_MAX_HP = 1_000_000L;
    private static final Set<Long> DONATION_AMOUNTS = Set.of(1_000L, 5_000L, 10_000L);
    private static final long BOSS_CONTRIBUTION = 50;
    private static final long BOSS_COIN_REWARD = 50;
    private static final long BOSS_KILL_BONUS = 200;

    private final PlayerService players;
    private final FormationService formations;
    private final WalletService wallet;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ZoneId zone;
    private final int resetHour;

    public GuildApplicationService(PlayerService players, FormationService formations, WalletService wallet,
                                   JdbcTemplate jdbc, Clock clock,
                                   @Value("${game.clock.zone:Asia/Bangkok}") String zone,
                                   @Value("${game.clock.reset-hour:5}") int resetHour) {
        this.players = players;
        this.formations = formations;
        this.wallet = wallet;
        this.jdbc = jdbc;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
        this.resetHour = resetHour;
    }

    @Transactional
    public GuildState state(UUID playerId) {
        players.require(playerId);
        Membership membership = membership(playerId, false);
        List<GuildDiscoverView> discover = discover();
        if (membership == null) {
            return new GuildState(PROFILE_VERSION, null, discover, null, List.of(), 0, null);
        }
        GuildView guild = guild(membership.guildId());
        List<MemberView> members = members(membership.guildId());
        long todayContribution = todayContribution(membership.guildId(), playerId);
        BossView boss = boss(membership.guildId(), playerId, false);
        return new GuildState(PROFILE_VERSION, membership.role().name(), discover, guild, members, todayContribution, boss);
    }

    @Transactional
    public GuildState create(UUID playerId, String name) {
        players.require(playerId);
        jdbc.queryForObject("select id from players where id = ? for update", UUID.class, playerId);
        if (membership(playerId, false) != null) throw new IllegalStateException("player already belongs to a guild");
        String safeName = name == null ? "" : name.trim();
        if (safeName.length() < 3 || safeName.length() > 32) throw new IllegalArgumentException("guild name must contain 3-32 characters");
        UUID guildId = UUID.randomUUID();
        jdbc.update("insert into guilds(id, name, level, exp, notice, created_at) values (?, ?, 1, 0, '', ?)", guildId, safeName, clock.instant());
        jdbc.update("insert into guild_members(guild_id, player_id, role, contribution, joined_at) values (?, ?, 'LEADER', 0, ?)", guildId, playerId, clock.instant());
        return state(playerId);
    }

    @Transactional
    public GuildState join(UUID playerId, UUID guildId) {
        players.require(playerId);
        jdbc.queryForObject("select id from players where id = ? for update", UUID.class, playerId);
        if (membership(playerId, false) != null) throw new IllegalStateException("player already belongs to a guild");
        UUID found = jdbc.query("select id from guilds where id = ? for update", rs -> rs.next() ? rs.getObject(1, UUID.class) : null, guildId);
        if (found == null) throw new IllegalArgumentException("guild not found");
        Integer members = jdbc.queryForObject("select count(*) from guild_members where guild_id = ?", Integer.class, guildId);
        if (members != null && members >= MEMBER_CAP) throw new IllegalStateException("guild member cap reached");
        jdbc.update("insert into guild_members(guild_id, player_id, role, contribution, joined_at) values (?, ?, 'MEMBER', 0, ?)", guildId, playerId, clock.instant());
        return state(playerId);
    }

    @Transactional
    public GuildState leave(UUID playerId) {
        Membership membership = membership(playerId, true);
        if (membership == null) throw new IllegalStateException("player is not in a guild");
        Integer count = jdbc.queryForObject("select count(*) from guild_members where guild_id = ?", Integer.class, membership.guildId());
        if (membership.role() == GuildRole.LEADER) {
            if (count != null && count > 1) throw new IllegalStateException("leader must transfer leadership before leaving");
            jdbc.update("delete from guilds where id = ?", membership.guildId());
        } else {
            jdbc.update("delete from guild_members where guild_id = ? and player_id = ?", membership.guildId(), playerId);
        }
        return state(playerId);
    }

    @Transactional
    public DonationResult contribute(UUID playerId, long goldAmount, UUID requestId) {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        if (!DONATION_AMOUNTS.contains(goldAmount)) throw new IllegalArgumentException("allowed donations: 1000, 5000, 10000 Gold");
        String paymentKey = "guild:" + requestId + ":gold";
        Boolean replay = jdbc.queryForObject("select exists(select 1 from wallet_ledger where player_id = ? and idempotency_key = ?)", Boolean.class, playerId, paymentKey);
        Membership membership = membership(playerId, true);
        if (membership == null) throw new IllegalStateException("join a guild before contributing");
        if (Boolean.TRUE.equals(replay)) {
            return new DonationResult(true, goldAmount, 0, memberContribution(membership.guildId(), playerId), wallet.getBalance(playerId, Currency.GUILD_COIN));
        }
        if (wallet.getBalance(playerId, Currency.GOLD) < goldAmount) throw new IllegalStateException("not enough Gold");
        long points = goldAmount / 100;
        GuildMemberState member = new GuildMemberState(playerId, membership.role(), memberContribution(membership.guildId(), playerId)).contribute(points);
        wallet.mutate(playerId, Currency.GOLD, -goldAmount, "GUILD_DONATION", membership.guildId().toString(), paymentKey);
        long coinAfter = wallet.mutate(playerId, Currency.GUILD_COIN, points, "GUILD_DONATION_REWARD", membership.guildId().toString(), "guild:" + requestId + ":coin");
        jdbc.update("update guild_members set contribution = ? where guild_id = ? and player_id = ?", member.contribution(), membership.guildId(), playerId);
        addGuildExp(membership.guildId(), points);
        jdbc.update("insert into guild_contribution_ledger(guild_id, player_id, delta, source, created_at) values (?, ?, ?, ?, ?)", membership.guildId(), playerId, points, "DONATION:" + requestId, clock.instant());
        return new DonationResult(false, goldAmount, points, member.contribution(), coinAfter);
    }

    @Transactional
    public BossHitResult hitBoss(UUID playerId, UUID requestId) {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        Membership membership = membership(playerId, true);
        if (membership == null) throw new IllegalStateException("join a guild before attacking the boss");
        FormationService.FormationView formation = formations.load(playerId);
        if (formation.heroes().size() != 5) throw new IllegalStateException("save a five-ninja formation before guild boss");
        String resetKey = resetKey();
        String hitSource = "BOSS:" + resetKey;
        Boolean alreadyHit = jdbc.queryForObject("select exists(select 1 from guild_contribution_ledger where guild_id = ? and player_id = ? and source = ?)", Boolean.class, membership.guildId(), playerId, hitSource);
        BossView before = boss(membership.guildId(), playerId, true);
        if (Boolean.TRUE.equals(alreadyHit)) return new BossHitResult(true, resetKey, 0, before.currentHp(), before.defeated(), 0, wallet.getBalance(playerId, Currency.GUILD_COIN));
        if (before.defeated()) return new BossHitResult(false, resetKey, 0, 0, true, 0, wallet.getBalance(playerId, Currency.GUILD_COIN));

        long teamPower = formation.heroes().stream().mapToLong(GuildApplicationService::heroPower).sum();
        long requestedDamage = Math.max(1, teamPower * 3);
        GuildBossState state = new GuildBossState(BOSS_ID, before.maxHp(), before.currentHp(), before.maxHp() - before.currentHp());
        GuildBossState.DamageResult damage = state.apply(playerId, requestedDamage);
        jdbc.update("update guild_boss_runs set current_hp = ?, state = cast(? as jsonb) where guild_id = ? and boss_definition_id = ? and reset_key = ?",
                damage.state().currentHp(), bossStateJson(damage.state()), membership.guildId(), BOSS_ID, resetKey);
        jdbc.update("update guild_members set contribution = contribution + ? where guild_id = ? and player_id = ?", BOSS_CONTRIBUTION, membership.guildId(), playerId);
        addGuildExp(membership.guildId(), BOSS_CONTRIBUTION);
        jdbc.update("insert into guild_contribution_ledger(guild_id, player_id, delta, source, created_at) values (?, ?, ?, ?, ?)", membership.guildId(), playerId, BOSS_CONTRIBUTION, hitSource, clock.instant());
        boolean defeated = damage.state().currentHp() == 0;
        long coinReward = BOSS_COIN_REWARD + (defeated ? BOSS_KILL_BONUS : 0);
        long coinAfter = wallet.mutate(playerId, Currency.GUILD_COIN, coinReward, "GUILD_BOSS_REWARD", membership.guildId().toString(), "guildboss:" + resetKey + ":" + playerId);
        return new BossHitResult(false, resetKey, damage.appliedDamage(), damage.state().currentHp(), defeated, coinReward, coinAfter);
    }

    private Membership membership(UUID playerId, boolean lock) {
        return jdbc.query("""
                select guild_id, role from guild_members where player_id = ?
                """ + (lock ? " for update" : ""), rs -> rs.next() ? new Membership(rs.getObject(1, UUID.class), GuildRole.valueOf(rs.getString(2))) : null, playerId);
    }

    private GuildView guild(UUID guildId) {
        return jdbc.queryForObject("select id, name, level, exp, coalesce(notice, '') notice from guilds where id = ?",
                (rs, row) -> new GuildView(rs.getObject("id", UUID.class), rs.getString("name"), rs.getInt("level"), rs.getLong("exp"), rs.getString("notice"), memberCount(guildId)), guildId);
    }

    private List<GuildDiscoverView> discover() {
        return jdbc.query("""
                select g.id, g.name, g.level, count(gm.player_id) members
                from guilds g left join guild_members gm on gm.guild_id = g.id
                group by g.id, g.name, g.level
                order by g.level desc, count(gm.player_id) desc, g.created_at, g.id
                limit 10
                """, (rs, row) -> new GuildDiscoverView(rs.getObject("id", UUID.class), rs.getString("name"), rs.getInt("level"), rs.getInt("members"), MEMBER_CAP));
    }

    private List<MemberView> members(UUID guildId) {
        return jdbc.query("""
                select gm.player_id, p.display_name, gm.role, gm.contribution
                from guild_members gm join players p on p.id = gm.player_id
                where gm.guild_id = ?
                order by case gm.role when 'LEADER' then 0 when 'OFFICER' then 1 else 2 end, gm.contribution desc, gm.joined_at
                """, (rs, row) -> new MemberView(rs.getObject("player_id", UUID.class), rs.getString("display_name"), rs.getString("role"), rs.getLong("contribution")), guildId);
    }

    private BossView boss(UUID guildId, UUID playerId, boolean lock) {
        String resetKey = resetKey();
        jdbc.update("""
                insert into guild_boss_runs(id, guild_id, boss_definition_id, reset_key, max_hp, current_hp, state)
                values (?, ?, ?, ?, ?, ?, cast(? as jsonb))
                on conflict (guild_id, boss_definition_id, reset_key) do nothing
                """, UUID.randomUUID(), guildId, BOSS_ID, resetKey, BOSS_MAX_HP, BOSS_MAX_HP,
                bossStateJson(new GuildBossState(BOSS_ID, BOSS_MAX_HP, BOSS_MAX_HP, 0)));
        String sql = "select max_hp, current_hp from guild_boss_runs where guild_id = ? and boss_definition_id = ? and reset_key = ?" + (lock ? " for update" : "");
        BossCore core = jdbc.queryForObject(sql, (rs, row) -> new BossCore(rs.getLong("max_hp"), rs.getLong("current_hp")), guildId, BOSS_ID, resetKey);
        Boolean hit = playerId == null ? false : jdbc.queryForObject("select exists(select 1 from guild_contribution_ledger where guild_id = ? and player_id = ? and source = ?)", Boolean.class, guildId, playerId, "BOSS:" + resetKey);
        return new BossView(BOSS_ID, resetKey, core.maxHp(), core.currentHp(), core.currentHp() == 0, Boolean.TRUE.equals(hit), BOSS_DAMAGE_PROFILE_VERSION);
    }

    private long todayContribution(UUID guildId, UUID playerId) {
        Long value = jdbc.queryForObject("select coalesce(sum(delta), 0) from guild_contribution_ledger where guild_id = ? and player_id = ? and created_at >= ?",
                Long.class, guildId, playerId, resetStart().toInstant());
        return value == null ? 0 : value;
    }

    private long memberContribution(UUID guildId, UUID playerId) {
        Long value = jdbc.queryForObject("select contribution from guild_members where guild_id = ? and player_id = ?", Long.class, guildId, playerId);
        return value == null ? 0 : value;
    }
    private int memberCount(UUID guildId) { Integer value = jdbc.queryForObject("select count(*) from guild_members where guild_id = ?", Integer.class, guildId); return value == null ? 0 : value; }
    private void addGuildExp(UUID guildId, long amount) { jdbc.update("update guilds set exp = exp + ? where id = ?", amount, guildId); jdbc.update("update guilds set level = 1 + (exp / 1000)::int where id = ?", guildId); }
    private ZonedDateTime resetStart() { ZonedDateTime now = clock.instant().atZone(zone); LocalDate date = now.getHour() < resetHour ? now.toLocalDate().minusDays(1) : now.toLocalDate(); return date.atTime(resetHour, 0).atZone(zone); }
    private String resetKey() { return resetStart().toLocalDate().toString(); }
    private static long heroPower(OwnedHeroView hero) { return hero.level() * 1_000L + hero.awakeningLevel() * 250L + 500L; }
    private static String bossStateJson(GuildBossState state) { return "{\"bossDefinitionId\":\"" + state.bossDefinitionId() + "\",\"maxHp\":" + state.maxHp() + ",\"currentHp\":" + state.currentHp() + ",\"totalDamage\":" + state.totalDamage() + "}"; }

    private record Membership(UUID guildId, GuildRole role) {}
    private record BossCore(long maxHp, long currentHp) {}
    public record GuildState(String profileVersion, String role, List<GuildDiscoverView> discover, GuildView guild, List<MemberView> members, long todayContribution, BossView boss) {}
    public record GuildDiscoverView(UUID guildId, String name, int level, int members, int memberCap) {}
    public record GuildView(UUID guildId, String name, int level, long exp, String notice, int memberCount) {}
    public record MemberView(UUID playerId, String displayName, String role, long contribution) {}
    public record BossView(String bossId, String resetKey, long maxHp, long currentHp, boolean defeated, boolean playerHitToday, String damageProfileVersion) {}
    public record DonationResult(boolean replayed, long goldSpent, long contributionPoints, long memberContribution, long guildCoinBalance) {}
    public record BossHitResult(boolean replayed, String resetKey, long damage, long bossHpAfter, boolean defeated, long guildCoinReward, long guildCoinBalance) {}
}
