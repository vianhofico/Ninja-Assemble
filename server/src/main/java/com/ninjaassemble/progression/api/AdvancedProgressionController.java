package com.ninjaassemble.progression.api;

import com.ninjaassemble.progression.application.AdvancedProgressionApplicationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/play/{playerId}/progression/advanced")
public final class AdvancedProgressionController {
    private final AdvancedProgressionApplicationService progression;
    public AdvancedProgressionController(AdvancedProgressionApplicationService progression){this.progression=progression;}
    @GetMapping public AdvancedProgressionApplicationService.ProgressionBoard board(@PathVariable UUID playerId){return progression.board(playerId);}
    @PostMapping("/{trackId}/upgrade") public AdvancedProgressionApplicationService.UpgradeResult upgrade(@PathVariable UUID playerId,@PathVariable String trackId,@RequestBody UpgradeRequest request){if(request==null||request.requestId()==null)throw new IllegalArgumentException("requestId is required");return progression.upgrade(playerId,trackId,request.requestId());}
    public record UpgradeRequest(UUID requestId){}
}
