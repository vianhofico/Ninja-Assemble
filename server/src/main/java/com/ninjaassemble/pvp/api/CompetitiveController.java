package com.ninjaassemble.pvp.api;

import com.ninjaassemble.pvp.application.CompetitiveApplicationService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/play/{playerId}/competitive")
public final class CompetitiveController {
    private final CompetitiveApplicationService competitive;

    public CompetitiveController(CompetitiveApplicationService competitive) {
        this.competitive = competitive;
    }

    @GetMapping
    public CompetitiveApplicationService.CompetitiveBoard board(@PathVariable UUID playerId) {
        return competitive.board(playerId);
    }

    @PostMapping("/arena/{opponentPlayerId}/battle")
    public CompetitiveApplicationService.CompetitiveBattleResult arenaBattle(
            @PathVariable UUID playerId,
            @PathVariable UUID opponentPlayerId,
            @RequestBody ActionRequest request) {
        return competitive.fightArena(playerId, opponentPlayerId, requireRequestId(request));
    }

    @PostMapping("/shadow-arena/{opponentPlayerId}/battle")
    public CompetitiveApplicationService.CompetitiveBattleResult shadowArenaBattle(
            @PathVariable UUID playerId,
            @PathVariable UUID opponentPlayerId,
            @RequestBody ActionRequest request) {
        return competitive.fightShadowArena(playerId, opponentPlayerId, requireRequestId(request));
    }

    @GetMapping("/history")
    public CompetitiveApplicationService.CompetitiveHistory history(
            @PathVariable UUID playerId,
            @RequestParam CompetitiveApplicationService.Mode mode) {
        return competitive.history(playerId, mode);
    }

    @GetMapping("/leaderboard")
    public List<CompetitiveApplicationService.LeaderboardEntry> leaderboard(
            @PathVariable UUID playerId,
            @RequestParam CompetitiveApplicationService.Mode mode) {
        return competitive.leaderboard(mode);
    }

    private static UUID requireRequestId(ActionRequest request) {
        if (request == null || request.requestId() == null) throw new IllegalArgumentException("requestId is required");
        return request.requestId();
    }

    public record ActionRequest(UUID requestId) {}
}
