package com.ninjaassemble.play.api;

import com.ninjaassemble.campaign.application.CampaignStageFlowService;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.inventory.application.InventoryService;
import com.ninjaassemble.mail.application.MailApplicationService;
import com.ninjaassemble.play.application.FormationService;
import com.ninjaassemble.play.application.HeroUpgradeService;
import com.ninjaassemble.play.application.PlayableBattleService;
import com.ninjaassemble.play.application.StarterRosterService;
import com.ninjaassemble.play.application.SummonApplicationService;
import com.ninjaassemble.pvp.application.ArenaApplicationService;
import com.ninjaassemble.pvp.application.ShadowArenaApplicationService;
import com.ninjaassemble.quest.application.DailyQuestService;
import com.ninjaassemble.shop.application.ShopApplicationService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/play/{playerId}")
public class PlayableGameController {
    private final StarterRosterService bootstrap;
    private final HeroOwnershipService ownership;
    private final FormationService formations;
    private final PlayableBattleService battles;
    private final CampaignStageFlowService campaign;
    private final InventoryService inventory;
    private final ArenaApplicationService arena;
    private final ShadowArenaApplicationService shadowArena;
    private final ShopApplicationService shop;
    private final DailyQuestService quests;
    private final MailApplicationService mail;
    private final SummonApplicationService summons;
    private final HeroUpgradeService upgrades;

    public PlayableGameController(StarterRosterService bootstrap, HeroOwnershipService ownership, FormationService formations,
                                  PlayableBattleService battles, CampaignStageFlowService campaign, InventoryService inventory,
                                  ArenaApplicationService arena, ShadowArenaApplicationService shadowArena,
                                  ShopApplicationService shop, DailyQuestService quests,
                                  MailApplicationService mail, SummonApplicationService summons, HeroUpgradeService upgrades) {
        this.bootstrap = bootstrap; this.ownership = ownership; this.formations = formations; this.battles = battles;
        this.campaign = campaign; this.inventory = inventory; this.arena = arena; this.shadowArena = shadowArena;
        this.shop = shop; this.quests = quests; this.mail = mail; this.summons = summons; this.upgrades = upgrades;
    }

    @PostMapping("/bootstrap") public StarterRosterService.BootstrapResult bootstrap(@PathVariable UUID playerId) { return bootstrap.bootstrap(playerId); }
    @GetMapping("/heroes") public List<OwnedHeroView> heroes(@PathVariable UUID playerId) { return ownership.list(playerId); }
    @PutMapping("/formation") public FormationService.FormationView formation(@PathVariable UUID playerId, @RequestBody FormationRequest request) { return formations.save(playerId, request.playerHeroIds()); }
    @GetMapping("/formation") public FormationService.FormationView formation(@PathVariable UUID playerId) { return formations.load(playerId); }
    @GetMapping("/campaign/stages") public CampaignStageFlowService.CampaignStageList campaignStages(@PathVariable UUID playerId) { return campaign.list(playerId); }
    @PostMapping("/campaign/stages/{stageId}/battle") public PlayableBattleService.PlayBattleResult campaignBattle(@PathVariable UUID playerId, @PathVariable String stageId) { return battles.play(playerId, stageId); }
    @GetMapping("/inventory") public InventoryService.InventoryView inventory(@PathVariable UUID playerId) { return inventory.view(playerId); }
    @GetMapping("/arena") public ArenaApplicationService.ArenaState arena(@PathVariable UUID playerId) { return arena.state(playerId); }
    @PostMapping("/arena/{opponentPlayerId}/battle") public ArenaApplicationService.ArenaBattleView arenaBattle(@PathVariable UUID playerId, @PathVariable UUID opponentPlayerId) { return arena.fight(playerId, opponentPlayerId); }
    @GetMapping("/shadow-arena") public ShadowArenaApplicationService.ShadowArenaState shadowArena(@PathVariable UUID playerId) { return shadowArena.state(playerId); }
    @PostMapping("/shadow-arena/{opponentPlayerId}/battle") public ShadowArenaApplicationService.ShadowArenaBattleView shadowArenaBattle(@PathVariable UUID playerId, @PathVariable UUID opponentPlayerId) { return shadowArena.fight(playerId, opponentPlayerId); }
    @GetMapping("/shop") public ShopApplicationService.ShopView shop(@PathVariable UUID playerId) { return shop.view(playerId); }
    @PostMapping("/shop/{shopId}/{offerId}/purchase") public ShopApplicationService.PurchaseResult purchase(@PathVariable UUID playerId, @PathVariable String shopId, @PathVariable String offerId, @RequestBody ActionRequest request) { return shop.purchase(playerId, shopId, offerId, requireRequestId(request)); }
    @GetMapping("/quests") public DailyQuestService.QuestBoard quests(@PathVariable UUID playerId) { return quests.view(playerId); }
    @PostMapping("/quests/{questId}/claim") public DailyQuestService.ClaimResult claimQuest(@PathVariable UUID playerId, @PathVariable String questId) { return quests.claim(playerId, questId); }
    @GetMapping("/mail") public MailApplicationService.Mailbox mail(@PathVariable UUID playerId) { return mail.view(playerId); }
    @PostMapping("/mail/{mailId}/read") public void readMail(@PathVariable UUID playerId, @PathVariable UUID mailId) { mail.markRead(playerId, mailId); }
    @PostMapping("/mail/{mailId}/claim") public MailApplicationService.ClaimResult claimMail(@PathVariable UUID playerId, @PathVariable UUID mailId) { return mail.claim(playerId, mailId); }
    @PostMapping("/battle") public PlayableBattleService.PlayBattleResult battle(@PathVariable UUID playerId) { return battles.play(playerId); }
    @PostMapping("/summon") public SummonApplicationService.SummonResult summon(@PathVariable UUID playerId, @RequestBody ActionRequest request) { return summons.summon(playerId, requireRequestId(request)); }
    @PostMapping("/heroes/{playerHeroId}/level-up") public HeroUpgradeService.UpgradeResult levelUp(@PathVariable UUID playerId, @PathVariable UUID playerHeroId, @RequestBody ActionRequest request) { return upgrades.levelUp(playerId, playerHeroId, requireRequestId(request)); }
    @PutMapping("/heroes/{playerHeroId}/variant") public OwnedHeroView selectVariant(@PathVariable UUID playerId, @PathVariable UUID playerHeroId, @RequestBody VariantRequest request) { return ownership.selectVariant(playerId, playerHeroId, request.variant()); }

    private static UUID requireRequestId(ActionRequest request) { if (request == null || request.requestId() == null) throw new IllegalArgumentException("requestId is required"); return request.requestId(); }
    public record FormationRequest(List<UUID> playerHeroIds) {}
    public record ActionRequest(UUID requestId) {}
    public record VariantRequest(String variant) {}
}
