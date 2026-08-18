package com.ninjaassemble.pve.api;

import com.ninjaassemble.pve.application.ResourcePveApplicationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/play/{playerId}/resource-pve")
public final class ResourcePveController {
    private final ResourcePveApplicationService resourcePve;
    public ResourcePveController(ResourcePveApplicationService resourcePve) { this.resourcePve = resourcePve; }

    @GetMapping
    public ResourcePveApplicationService.ResourcePveBoard board(@PathVariable UUID playerId) {
        return resourcePve.board(playerId);
    }

    @PostMapping("/{modeId}/battle")
    public ResourcePveApplicationService.ResourcePveBattleView battle(@PathVariable UUID playerId,
                                                                       @PathVariable String modeId,
                                                                       @RequestBody BattleRequest request) {
        if (request == null || request.requestId() == null) throw new IllegalArgumentException("requestId is required");
        return resourcePve.play(playerId, modeId, request.requestId());
    }

    public record BattleRequest(UUID requestId) { }
}
