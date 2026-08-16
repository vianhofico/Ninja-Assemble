package com.ninjaassemble.progression.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.play.application.ActionRequestService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvolutionApplicationService {
    private static final String ACTION = "HERO_EVOLUTION";
    private final EvolutionPathCatalogService paths;
    private final HeroOwnershipService ownership;
    private final WalletService wallet;
    private final ActionRequestService requests;

    public EvolutionApplicationService(EvolutionPathCatalogService paths, HeroOwnershipService ownership,
                                       WalletService wallet, ActionRequestService requests) {
        this.paths = paths; this.ownership = ownership; this.wallet = wallet; this.requests = requests;
    }

    @Transactional
    public EvolutionResult evolve(UUID playerId, UUID playerHeroId, String targetVariant, UUID requestId) {
        OwnedHeroView hero = ownership.requireOwned(playerId, playerHeroId);
        EvolutionPathCatalogService.EvolutionPath path = paths.require(hero.characterId(), targetVariant);
        if (hero.level() < path.minLevel()) throw new IllegalStateException("hero level requirement not met: " + path.minLevel());
        if (FrameAdvancePolicy.rank(hero.frameTier()) < FrameAdvancePolicy.rank(path.minFrame())) {
            throw new IllegalStateException("frame requirement not met: " + path.minFrame());
        }
        if (!path.prerequisiteVariant().equals("BASE") && !ownership.hasVariant(playerId, hero.characterId(), path.prerequisiteVariant())) {
            throw new IllegalStateException("prerequisite variant not unlocked: " + path.prerequisiteVariant());
        }
        if (ownership.hasVariant(playerId, hero.characterId(), targetVariant)) {
            ownership.selectVariant(playerId, playerHeroId, targetVariant);
            return new EvolutionResult(hero.characterId(), targetVariant, 0, true, path.profileVersion());
        }
        Optional<String> existing = requests.existing(playerId, requestId, ACTION);
        if (existing.isPresent()) return decode(existing.get());
        requests.reserve(playerId, requestId, ACTION);
        wallet.mutate(playerId, Currency.GOLD, -path.goldCost(), "HERO_EVOLUTION", targetVariant, "evolution:" + requestId + ":gold");
        boolean unlocked = ownership.unlockVariant(playerId, hero.characterId(), targetVariant);
        if (!unlocked) throw new IllegalStateException("evolution target was already unlocked concurrently");
        ownership.selectVariant(playerId, playerHeroId, targetVariant);
        EvolutionResult result = new EvolutionResult(hero.characterId(), targetVariant, path.goldCost(), false, path.profileVersion());
        requests.complete(playerId, requestId, encode(result));
        return result;
    }

    private static String encode(EvolutionResult result) {
        return String.join("\t", result.characterId(), result.targetVariant(), Long.toString(result.goldCost()),
                Boolean.toString(result.alreadyUnlocked()), result.profileVersion());
    }

    private static EvolutionResult decode(String stored) {
        String[] p = stored.split("\t", -1);
        if (p.length != 5) throw new IllegalStateException("corrupt stored evolution response");
        return new EvolutionResult(p[0], p[1], Long.parseLong(p[2]), Boolean.parseBoolean(p[3]), p[4]);
    }

    public record EvolutionResult(String characterId, String targetVariant, long goldCost, boolean alreadyUnlocked, String profileVersion) {}
}
