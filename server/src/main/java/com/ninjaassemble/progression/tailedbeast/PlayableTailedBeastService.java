package com.ninjaassemble.progression.tailedbeast;

import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.play.application.ActionRequestService;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayableTailedBeastService {
    private static final String ACTION = "TAILED_BEAST_ADVANCE";
    private static final String VERSION = "tailed-beast-expansion-playable-v1";
    private static final int MAX_PLAYABLE_STAGE = 5;

    private final JdbcTemplate jdbc;
    private final HeroOwnershipService ownership;
    private final PlayableJinchurikiCatalogService hosts;
    private final ActionRequestService requests;
    private final Clock clock;

    public PlayableTailedBeastService(JdbcTemplate jdbc, HeroOwnershipService ownership,
                                      PlayableJinchurikiCatalogService hosts, ActionRequestService requests, Clock clock) {
        this.jdbc = jdbc; this.ownership = ownership; this.hosts = hosts; this.requests = requests; this.clock = clock;
    }

    @Transactional
    public MaterialView bootstrapMaterials(UUID playerId) {
        jdbc.update("""
                insert into player_beast_materials(player_id, beast_soul, beast_bone, updated_at)
                values (?, 100, 20, ?)
                on conflict (player_id) do update
                    set beast_soul = greatest(player_beast_materials.beast_soul, 100),
                        beast_bone = greatest(player_beast_materials.beast_bone, 20),
                        updated_at = excluded.updated_at
                """, playerId, clock.instant());
        return materials(playerId);
    }

    @Transactional(readOnly = true)
    public MaterialView materials(UUID playerId) {
        return jdbc.query("select beast_soul, beast_bone from player_beast_materials where player_id = ?",
                (rs, row) -> new MaterialView(rs.getLong(1), rs.getLong(2)), playerId).stream().findFirst()
                .orElse(new MaterialView(0, 0));
    }

    @Transactional(readOnly = true)
    public BeastProgressView progress(UUID playerId, UUID playerHeroId) {
        OwnedHeroView hero = ownership.requireOwned(playerId, playerHeroId);
        PlayableJinchurikiCatalogService.HostView host = hosts.require(hero.characterId());
        return jdbc.query("""
                select beast, stage, soul_spent, beast_bone_spent
                from player_tailed_beast_progress where player_hero_id = ?
                """, (rs, row) -> new BeastProgressView(hero.id(), hero.characterId(), rs.getString(1), rs.getInt(2),
                rs.getLong(3), rs.getLong(4), VERSION), playerHeroId).stream().findFirst()
                .orElse(new BeastProgressView(hero.id(), hero.characterId(), host.beast(), 0, 0, 0, VERSION));
    }

    @Transactional
    public AdvanceResult advance(UUID playerId, UUID playerHeroId, UUID requestId) {
        OwnedHeroView hero = ownership.requireOwned(playerId, playerHeroId);
        PlayableJinchurikiCatalogService.HostView host = hosts.require(hero.characterId());
        Optional<String> existing = requests.existing(playerId, requestId, ACTION);
        if (existing.isPresent()) return decode(playerId, playerHeroId, existing.get());
        requests.reserve(playerId, requestId, ACTION);

        bootstrapMaterials(playerId);
        BeastProgressView before = progress(playerId, playerHeroId);
        if (before.stage() >= MAX_PLAYABLE_STAGE) throw new IllegalStateException("Jinchuriki is at the current playable stage cap");
        int targetStage = before.stage() + 1;
        long soulCost = targetStage * 10L;
        long boneCost = Math.max(0, targetStage - 2L) * 2L;
        MaterialView material = materials(playerId);
        if (material.beastSoul() < soulCost || material.beastBone() < boneCost) throw new IllegalStateException("not enough Tailed Beast materials");

        jdbc.update("""
                update player_beast_materials
                set beast_soul = beast_soul - ?, beast_bone = beast_bone - ?, updated_at = ?
                where player_id = ?
                """, soulCost, boneCost, clock.instant(), playerId);
        jdbc.update("""
                insert into player_tailed_beast_progress(player_hero_id, beast, stage, soul_spent, beast_bone_spent, updated_at)
                values (?, ?, ?, ?, ?, ?)
                on conflict (player_hero_id) do update set beast = excluded.beast, stage = excluded.stage,
                    soul_spent = player_tailed_beast_progress.soul_spent + excluded.soul_spent,
                    beast_bone_spent = player_tailed_beast_progress.beast_bone_spent + excluded.beast_bone_spent,
                    updated_at = excluded.updated_at
                """, playerHeroId, host.beast(), targetStage, soulCost, boneCost, clock.instant());
        jdbc.update("""
                update player_heroes
                set tailed_beast_state = jsonb_build_object('beast', ?, 'stage', ?, 'profileVersion', ?)
                where player_id = ? and id = ?
                """, host.beast(), targetStage, VERSION, playerId, playerHeroId);

        AdvanceResult result = new AdvanceResult(progress(playerId, playerHeroId), materials(playerId), soulCost, boneCost);
        requests.complete(playerId, requestId, targetStage + "\t" + soulCost + "\t" + boneCost);
        return result;
    }

    private AdvanceResult decode(UUID playerId, UUID playerHeroId, String stored) {
        String[] p = stored.split("\t", -1);
        if (p.length != 3) throw new IllegalStateException("corrupt stored Tailed Beast response");
        return new AdvanceResult(progress(playerId, playerHeroId), materials(playerId), Long.parseLong(p[1]), Long.parseLong(p[2]));
    }

    public record MaterialView(long beastSoul, long beastBone) {}
    public record BeastProgressView(UUID playerHeroId, String characterId, String beast, int stage,
                                    long soulSpent, long beastBoneSpent, String profileVersion) {}
    public record AdvanceResult(BeastProgressView progress, MaterialView remainingMaterials, long soulCost, long beastBoneCost) {}
}
