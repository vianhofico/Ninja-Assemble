package com.ninjaassemble.progression.api;

import com.ninjaassemble.progression.application.FrameAdvanceApplicationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/play/{playerId}/progression")
public class HeroProgressionController {
    private final FrameAdvanceApplicationService frameAdvance;

    public HeroProgressionController(FrameAdvanceApplicationService frameAdvance) {
        this.frameAdvance = frameAdvance;
    }

    @PostMapping("/heroes/{playerHeroId}/frame-advance")
    public FrameAdvanceApplicationService.FrameAdvanceResult frameAdvance(@PathVariable UUID playerId,
                                                                          @PathVariable UUID playerHeroId,
                                                                          @RequestBody ActionRequest request) {
        return frameAdvance.advance(playerId, playerHeroId, requireRequestId(request));
    }

    private static UUID requireRequestId(ActionRequest request) {
        if (request == null || request.requestId() == null) throw new IllegalArgumentException("requestId is required");
        return request.requestId();
    }

    public record ActionRequest(UUID requestId) {}
}
