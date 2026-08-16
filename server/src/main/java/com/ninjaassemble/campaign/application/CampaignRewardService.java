package com.ninjaassemble.campaign.application;

import com.ninjaassemble.campaign.domain.RewardBundle;
import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.player.domain.PlayerEntity;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class CampaignRewardService {
    private final PlayerService players;
    private final WalletService wallet;
    private final Clock clock;
    private final long expPerLevel;

    public CampaignRewardService(PlayerService players, WalletService wallet, Clock clock,
                                 @Value("${game.account.exp-per-level:1000}") long expPerLevel) {
        this.players = players;
        this.wallet = wallet;
        this.clock = clock;
        this.expPerLevel = expPerLevel;
    }

    @Transactional
    public RewardGrant grant(UUID playerId, String stageId, RewardBundle reward, UUID battleId) {
        if (reward == null) throw new IllegalArgumentException("campaign reward is required");
        if (!reward.items().isEmpty()) throw new IllegalStateException("campaign item rewards require the M28 drop-table/inventory bridge");
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
        return new RewardGrant(reward.playerExp(), gold, diamond, player.getAccountLevel());
    }

    public record RewardGrant(long playerExp, long gold, long diamond, int accountLevelAfter) {}
}
