package com.ninjaassemble.summon.domain;

import java.util.List;
import java.util.SplittableRandom;

public final class SummonEngine {
    public PullResult pull(SummonBannerDefinition banner, SummonState state, long seed) {
        if (banner == null || state == null) throw new IllegalArgumentException("banner/state required");
        boolean pity = state.pullsSincePity() + 1 >= banner.hardPity();
        List<SummonPoolEntry> candidates = pity
                ? banner.pool().stream().filter(entry -> entry.rarity().ordinal() >= banner.pityRarity().ordinal()).toList()
                : banner.pool();
        SummonPoolEntry selected = weighted(candidates, new SplittableRandom(seed));
        boolean reset = selected.rarity().ordinal() >= banner.pityRarity().ordinal();
        SummonState next = new SummonState(reset ? 0 : state.pullsSincePity() + 1);
        return new PullResult(selected, next, pity, banner.version(), seed);
    }

    private static SummonPoolEntry weighted(List<SummonPoolEntry> entries, SplittableRandom random) {
        long total = entries.stream().mapToLong(SummonPoolEntry::weight).sum();
        long roll = random.nextLong(total);
        long cursor = 0;
        for (SummonPoolEntry entry : entries) {
            cursor += entry.weight();
            if (roll < cursor) return entry;
        }
        throw new IllegalStateException("weighted pool resolution failed");
    }

    public record PullResult(SummonPoolEntry entry, SummonState nextState, boolean pityTriggered, String bannerVersion, long seed) {}
}
