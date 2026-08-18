package com.ninjaassemble.campaign.application;

import com.ninjaassemble.campaign.domain.RewardBundle;
import com.ninjaassemble.campaign.domain.StageDefinition;
import com.ninjaassemble.player.application.EnergyService;
import com.ninjaassemble.player.application.PlayerService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Server-authoritative Campaign sweep with request-id idempotency. */
@Service
public final class CampaignSweepService {
    private final PlayerService players;
    private final CampaignStageFlowService stageFlow;
    private final CampaignProgressService progress;
    private final CampaignRewardService rewards;
    private final EnergyService energy;
    private final JdbcTemplate jdbc;

    public CampaignSweepService(PlayerService players,
                                CampaignStageFlowService stageFlow,
                                CampaignProgressService progress,
                                CampaignRewardService rewards,
                                EnergyService energy,
                                JdbcTemplate jdbc) {
        this.players = players;
        this.stageFlow = stageFlow;
        this.progress = progress;
        this.rewards = rewards;
        this.energy = energy;
        this.jdbc = jdbc;
    }

    @Transactional
    public SweepResult sweep(UUID playerId, String stageId, UUID requestId) {
        if (playerId == null) throw new IllegalArgumentException("playerId is required");
        if (stageId == null || stageId.isBlank()) throw new IllegalArgumentException("stageId is required");
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        players.require(playerId);

        // Serialize duplicate/retry requests before Energy/reward mutation.
        long lockKey = requestId.getMostSignificantBits() ^ requestId.getLeastSignificantBits();
        jdbc.query("select pg_advisory_xact_lock(?)", rs -> { }, lockKey);

        SweepResult replay = load(requestId);
        if (replay != null) {
            if (!replay.playerId().equals(playerId) || !replay.stageId().equals(stageId))
                throw new IllegalStateException("campaign sweep requestId already belongs to another player/stage");
            return replay.withReplayed(true);
        }

        Map<String, CampaignProgressService.StageProgress> progressByStage = progress.load(playerId);
        CampaignProgressService.StageProgress existing = progressByStage.get(stageId);
        if (existing == null || existing.clearCount() <= 0)
            throw new IllegalStateException("campaign sweep requires a previously cleared stage: " + stageId);

        CampaignStageCatalogService.CampaignStageEntry entry = stageFlow.requirePlayable(playerId, stageId);
        StageDefinition stage = entry.stage();
        EnergyService.EnergySnapshot energyAfter = energy.spend(playerId, stage.energyCost());

        RewardBundle repeatReward = stage.repeatReward();
        CampaignRewardService.RewardGrant grant = rewards.grant(
                playerId, stage.id(), repeatReward, requestId, "campaign-sweep");
        progress.recordClear(playerId, stage.id(), Math.max(1, existing.bestStars()));

        List<SweepItemReward> itemRewards = repeatReward.items().entrySet().stream()
                .filter(item -> item.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .map(item -> new SweepItemReward(item.getKey(), item.getValue()))
                .toList();

        SweepResult result = new SweepResult(
                requestId, playerId, stage.id(), CampaignStageCatalogService.VERSION, false,
                stage.energyCost(), energyAfter.current(), grant.playerExp(), grant.gold(), grant.diamond(),
                grant.accountLevelAfter(), itemRewards);

        jdbc.update("""
                insert into campaign_sweeps(
                    request_id, player_id, stage_id, catalog_version, energy_cost, energy_after,
                    player_exp, gold, diamond, account_level_after, item_rewards)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                requestId, playerId, stage.id(), CampaignStageCatalogService.VERSION,
                stage.energyCost(), energyAfter.current(), grant.playerExp(), grant.gold(), grant.diamond(),
                grant.accountLevelAfter(), encodeItems(itemRewards));
        return result;
    }

    private SweepResult load(UUID requestId) {
        List<SweepResult> rows = jdbc.query("""
                select request_id, player_id, stage_id, catalog_version, energy_cost, energy_after,
                       player_exp, gold, diamond, account_level_after, item_rewards
                from campaign_sweeps where request_id = ?
                """, (rs, rowNum) -> new SweepResult(
                rs.getObject("request_id", UUID.class),
                rs.getObject("player_id", UUID.class),
                rs.getString("stage_id"),
                rs.getString("catalog_version"),
                false,
                rs.getInt("energy_cost"),
                rs.getInt("energy_after"),
                rs.getLong("player_exp"),
                rs.getLong("gold"),
                rs.getLong("diamond"),
                rs.getInt("account_level_after"),
                decodeItems(rs.getString("item_rewards"))), requestId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static String encodeItems(List<SweepItemReward> items) {
        return items.stream()
                .sorted(Comparator.comparing(SweepItemReward::itemId))
                .map(item -> item.itemId() + "=" + item.quantity())
                .collect(java.util.stream.Collectors.joining(";"));
    }

    private static List<SweepItemReward> decodeItems(String encoded) {
        if (encoded == null || encoded.isBlank()) return List.of();
        List<SweepItemReward> result = new ArrayList<>();
        for (String token : encoded.split(";")) {
            String[] pieces = token.split("=", 2);
            if (pieces.length != 2 || pieces[0].isBlank())
                throw new IllegalStateException("invalid campaign sweep item payload");
            long quantity = Long.parseLong(pieces[1]);
            if (quantity <= 0) throw new IllegalStateException("invalid campaign sweep item quantity");
            result.add(new SweepItemReward(pieces[0], quantity));
        }
        return List.copyOf(result);
    }

    public record SweepItemReward(String itemId, long quantity) { }

    public record SweepResult(
            UUID requestId,
            UUID playerId,
            String stageId,
            String catalogVersion,
            boolean replayed,
            int energyCost,
            int energyAfter,
            long playerExpReward,
            long goldReward,
            long diamondReward,
            int accountLevelAfter,
            List<SweepItemReward> itemRewards) {
        public SweepResult withReplayed(boolean value) {
            return new SweepResult(requestId, playerId, stageId, catalogVersion, value, energyCost, energyAfter,
                    playerExpReward, goldReward, diamondReward, accountLevelAfter, itemRewards);
        }
    }
}
