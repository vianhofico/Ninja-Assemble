package com.ninjaassemble.progression.api;

import com.ninjaassemble.progression.application.EvolutionApplicationService;
import com.ninjaassemble.progression.application.EvolutionPathCatalogService;
import com.ninjaassemble.progression.application.FrameAdvanceApplicationService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/play/{playerId}/progression")
public class HeroProgressionController {
    private final EvolutionPathCatalogService paths;
    private final FrameAdvanceApplicationService frameAdvance;
    private final EvolutionApplicationService evolution;

    public HeroProgressionController(EvolutionPathCatalogService paths, FrameAdvanceApplicationService frameAdvance,
                                     EvolutionApplicationService evolution) {
        this.paths = paths; this.frameAdvance = frameAdvance; this.evolution = evolution;
    }

    @GetMapping("/evolution-paths/{characterId}")
    public List<EvolutionPathCatalogService.EvolutionPath> evolutionPaths(@PathVariable String characterId) {
        return paths.forCharacter(characterId);
    }

    @PostMapping("/heroes/{playerHeroId}/frame-advance")
    public FrameAdvanceApplicationService.FrameAdvanceResult frameAdvance(@PathVariable UUID playerId,
                                                                          @PathVariable UUID playerHeroId,
                                                                          @RequestBody ActionRequest request) {
        return frameAdvance.advance(playerId, playerHeroId, requireRequestId(request));
    }

    @PostMapping("/heroes/{playerHeroId}/evolve")
    public EvolutionApplicationService.EvolutionResult evolve(@PathVariable UUID playerId,
                                                               @PathVariable UUID playerHeroId,
                                                               @RequestBody EvolutionRequest request) {
        if (request == null || request.targetVariant() == null || request.targetVariant().isBlank()) {
            throw new IllegalArgumentException("targetVariant is required");
        }
        return evolution.evolve(playerId, playerHeroId, request.targetVariant(), requireRequestId(request));
    }

    private static UUID requireRequestId(ActionRequest request) {
        if (request == null || request.requestId() == null) throw new IllegalArgumentException("requestId is required");
        return request.requestId();
    }

    public record ActionRequest(UUID requestId) {}
    public record EvolutionRequest(UUID requestId, String targetVariant) implements RequestWithId {}
    private interface RequestWithId {}

    private static UUID requireRequestId(EvolutionRequest request) {
        if (request == null || request.requestId() == null) throw new IllegalArgumentException("requestId is required");
        return request.requestId();
    }
}
