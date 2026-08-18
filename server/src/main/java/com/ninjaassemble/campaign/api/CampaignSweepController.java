package com.ninjaassemble.campaign.api;

import com.ninjaassemble.campaign.application.CampaignSweepService;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/play/{playerId}/campaign/stages")
public final class CampaignSweepController {
    private final CampaignSweepService sweeps;

    public CampaignSweepController(CampaignSweepService sweeps) {
        this.sweeps = sweeps;
    }

    @PostMapping("/{stageId}/sweep")
    public CampaignSweepService.SweepResult sweep(@PathVariable UUID playerId,
                                                  @PathVariable String stageId,
                                                  @RequestBody SweepRequest request) {
        if (request == null || request.requestId() == null)
            throw new IllegalArgumentException("requestId is required");
        return sweeps.sweep(playerId, stageId, request.requestId());
    }

    public record SweepRequest(UUID requestId) { }
}
