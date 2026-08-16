package com.ninjaassemble.campaign.application;

import com.ninjaassemble.campaign.domain.RewardBundle;
import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.inventory.application.InventoryService;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.player.domain.PlayerEntity;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class CampaignRewardService {
    private final PlayerService players;
    private final WalletService wallet;
    private final InventoryService inventory;
    private final Clock clock;
    private final long expPerLevel;

    public CampaignRewardService(PlayerService players, WalletService wallet, InventoryService inventory, Clock clock,
                                 @Value("${game.account.exp-per-level:1000}") long expPerLevel) {
        this.players = players;
        this.wallet = wallet;
        this.inventory = inventory;
        this.clock = clock;
        this.expPerLevel = expPerLevel;
    }

    @Transactional
    public RewardGrant grant(UUID playerId, String stageId, RewardBundle reward, UUID battleId) {
        if (reward == null) throw new IllegalArgumentException("campaign reward is required");
        PlayerEntity player = players.require(playerId);
        if (reward.playerExp() > 0) player.addAccountExp(reward.playerExp(), expPerLevel, clock.instant());

        long gold = 0;
        long diamond = 0;
        for (var entry : reward.currencies().entrySet()) {
            Currency currency = Currency.valueOf(entry.getKey());
            long amount = entry.getValue();
            if (amount <= 0) continue;
            String key = "campaign:" + battleId + ":" + currency.name();
            wallet.mutate(playerId, currency, amount, "CAMPAIGN_STAGE_REWARD", stageId, key);
            if (currency == Currency.GOLD) gold += amount;
            if (currency == Currency.DIAMOND) diamond += amount;
        }

        List<ItemGrant> itemGrants = new ArrayList<>();
        for (var entry : reward.items().entrySet()) {
            long amount = entry.getValue();
            if (amount <= 0) continue;
            var stack = inventory.mutate(
                    playerId,
                    entry.getKey(),
                    amount,
                    "CAMPAIGN_STAGE_REWARD",
                    "campaign:" + battleId + ":item:" + entry.getKey());
            itemGrants.add(new ItemGrant(entry.getKey(), amount, stack.quantity()));
        }
        return new RewardGrant(reward.playerExp(), gold, diamond, player.getAccountLevel(), List.copyOf(itemGrants));
    }

    public record ItemGrant(String itemId, long quantity, long balanceAfter) {}
    public record RewardGrant(long playerExp, long gold, long diamond, int accountLevelAfter, List<ItemGrant> items) {}
}
