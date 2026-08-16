package com.ninjaassemble.hero.awakening;

import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/play/{playerId}/heroes/{playerHeroId}/awakening")
public final class HeroAwakeningController {
    private final HeroOwnershipService ownership;
    private final AwakeningPresentationCatalogService presentation;

    public HeroAwakeningController(HeroOwnershipService ownership, AwakeningPresentationCatalogService presentation) {
        this.ownership = ownership;
        this.presentation = presentation;
    }

    @GetMapping
    public AwakeningView preview(@PathVariable UUID playerId, @PathVariable UUID playerHeroId) {
        return view(ownership.requireOwned(playerId, playerHeroId), false);
    }

    @PostMapping
    public AwakeningView awaken(@PathVariable UUID playerId, @PathVariable UUID playerHeroId) {
        boolean changed = ownership.awaken(playerId, playerHeroId);
        return view(ownership.requireOwned(playerId, playerHeroId), changed);
    }

    private AwakeningView view(OwnedHeroView hero, boolean changed) {
        boolean available = hero.awakeningId() != null && !hero.awakeningId().isBlank();
        AwakeningPresentationCatalogService.AwakeningPresentation visual = available
                ? presentation.require(hero.awakeningId())
                : null;
        return new AwakeningView(
                hero,
                available,
                hero.awakened(),
                changed,
                available ? hero.awakeningId() : "",
                available ? hero.awakeningName() : "",
                visual);
    }

    public record AwakeningView(
            OwnedHeroView hero,
            boolean available,
            boolean awakened,
            boolean changed,
            String awakeningId,
            String awakeningName,
            AwakeningPresentationCatalogService.AwakeningPresentation visual
    ) {}
}
