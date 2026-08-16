package com.ninjaassemble.campaign.application;

import com.ninjaassemble.campaign.domain.CampaignGate;
import com.ninjaassemble.campaign.domain.RewardBundle;
import com.ninjaassemble.campaign.domain.StageDefinition;
import com.ninjaassemble.player.application.EnergyService;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.player.domain.PlayerEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class CampaignStageFlowService {
    private final CampaignStageCatalogService catalog;
    private final CampaignProgressService progress;
    private final PlayerService players;
    private final EnergyService energy;

    public CampaignStageFlowService(CampaignStageCatalogService catalog, CampaignProgressService progress,
                                    PlayerService players, EnergyService energy) {
        this.catalog = catalog;
        this.progress = progress;
        this.players = players;
        this.energy = energy;
    }

    @Transactional
    public CampaignStageList list(UUID playerId) {
        PlayerEntity player = players.require(playerId);
        EnergyService.EnergySnapshot energyState = energy.refresh(playerId);
        Map<String, CampaignProgressService.StageProgress> progressByStage = progress.load(playerId);
        Set<String> completed = progressByStage.values().stream().filter(it -> it.clearCount() > 0)
                .map(CampaignProgressService.StageProgress::stageId).collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<CampaignStageView> result = new ArrayList<>();
        for (CampaignStageCatalogService.CampaignStageEntry entry : catalog.all()) {
            StageDefinition stage = entry.stage();
            CampaignGate.GateResult gate = CampaignGate.evaluate(stage, player.getAccountLevel(), energyState.current(), completed);
            CampaignProgressService.StageProgress state = progressByStage.get(stage.id());
            result.add(new CampaignStageView(
                    stage.id(), stage.chapter(), stage.index(), stage.difficulty().name(), entry.nameEn(), entry.nameVi(),
                    stage.energyCost(), stage.minPlayerLevel(), List.copyOf(stage.prerequisiteStageIds()),
                    gate.allowed(), gate.missing(),
                    state == null ? 0 : state.clearCount(), state == null ? 0 : state.bestStars(),
                    reward(stage.firstClearReward()), reward(stage.repeatReward())));
        }
        return new CampaignStageList(CampaignStageCatalogService.VERSION, player.getAccountLevel(), energyState.current(), energyState.cap(), List.copyOf(result));
    }

    @Transactional
    public CampaignStageCatalogService.CampaignStageEntry requirePlayable(UUID playerId, String stageId) {
        CampaignStageCatalogService.CampaignStageEntry entry = catalog.require(stageId);
        PlayerEntity player = players.require(playerId);
        EnergyService.EnergySnapshot energyState = energy.refresh(playerId);
        Set<String> completed = progress.completedStageIds(playerId);
        CampaignGate.GateResult gate = CampaignGate.evaluate(entry.stage(), player.getAccountLevel(), energyState.current(), completed);
        if (!gate.allowed()) throw new IllegalStateException("campaign stage locked: " + stageId + " :: " + String.join(",", gate.missing()));
        return entry;
    }

    private static CampaignRewardView reward(RewardBundle bundle) {
        return new CampaignRewardView(
                bundle.playerExp(), bundle.currencies().getOrDefault("GOLD", 0L), bundle.currencies().getOrDefault("DIAMOND", 0L));
    }

    public record CampaignStageList(String catalogVersion, int playerLevel, int energy, int energyCap, List<CampaignStageView> stages) {}
    public record CampaignStageView(
            String stageId, int chapter, int stageIndex, String difficulty, String nameEn, String nameVi,
            int energyCost, int minPlayerLevel, List<String> prerequisiteStageIds,
            boolean unlocked, List<String> gateMissing, int clearCount, int bestStars,
            CampaignRewardView firstClearReward, CampaignRewardView repeatReward) {}
    public record CampaignRewardView(long playerExp, long gold, long diamond) {}
}
