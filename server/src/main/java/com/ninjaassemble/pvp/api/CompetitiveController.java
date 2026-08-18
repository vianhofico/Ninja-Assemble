package com.ninjaassemble.pvp.api;

import com.ninjaassemble.pvp.application.CompetitiveSeasonService;
import com.ninjaassemble.pvp.application.ProductionArenaService;
import com.ninjaassemble.pvp.application.ProductionShadowArenaService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/play/{playerId}/competitive")
public final class CompetitiveController {
    private final ProductionArenaService arena;
    private final ProductionShadowArenaService shadow;

    public CompetitiveController(ProductionArenaService arena, ProductionShadowArenaService shadow) {
        this.arena=arena; this.shadow=shadow;
    }

    @GetMapping("/arena") public ProductionArenaService.ArenaState arena(@PathVariable UUID playerId){return arena.state(playerId);}
    @PutMapping("/arena/defense") public ProductionArenaService.DefenseView arenaDefense(@PathVariable UUID playerId,@RequestBody FormationRequest request){return arena.saveDefense(playerId,requireFormation(request,5));}
    @PostMapping("/arena/{opponentPlayerId}/battle") public ProductionArenaService.ArenaBattleView arenaBattle(@PathVariable UUID playerId,@PathVariable UUID opponentPlayerId,@RequestBody ActionRequest request){return arena.fight(playerId,opponentPlayerId,requireRequestId(request));}
    @GetMapping("/arena/history") public List<ProductionArenaService.HistoryItem> arenaHistory(@PathVariable UUID playerId,@RequestParam(defaultValue="20") int limit){return arena.history(playerId,limit);}
    @PostMapping("/arena/season/claim") public CompetitiveSeasonService.SeasonRewardState claimArenaSeason(@PathVariable UUID playerId){return arena.claimPreviousSeason(playerId);}

    @GetMapping("/shadow-arena") public ProductionShadowArenaService.ShadowArenaState shadow(@PathVariable UUID playerId){return shadow.state(playerId);}
    @PutMapping("/shadow-arena/defense") public ProductionShadowArenaService.ShadowDefenseView shadowDefense(@PathVariable UUID playerId,@RequestBody FormationRequest request){return shadow.saveDefense(playerId,requireFormation(request,15));}
    @PostMapping("/shadow-arena/{opponentPlayerId}/battle") public ProductionShadowArenaService.ShadowArenaBattleView shadowBattle(@PathVariable UUID playerId,@PathVariable UUID opponentPlayerId,@RequestBody ActionRequest request){return shadow.fight(playerId,opponentPlayerId,requireRequestId(request));}
    @GetMapping("/shadow-arena/history") public List<ProductionShadowArenaService.HistoryItem> shadowHistory(@PathVariable UUID playerId,@RequestParam(defaultValue="20") int limit){return shadow.history(playerId,limit);}
    @PostMapping("/shadow-arena/season/claim") public CompetitiveSeasonService.SeasonRewardState claimShadowSeason(@PathVariable UUID playerId){return shadow.claimPreviousSeason(playerId);}

    private static UUID requireRequestId(ActionRequest request){if(request==null||request.requestId()==null)throw new IllegalArgumentException("requestId is required");return request.requestId();}
    private static List<UUID> requireFormation(FormationRequest request,int count){if(request==null||request.playerHeroIds()==null||request.playerHeroIds().size()!=count)throw new IllegalArgumentException("formation requires "+count+" ninja");return request.playerHeroIds();}
    public record ActionRequest(UUID requestId){}
    public record FormationRequest(List<UUID> playerHeroIds){}
}
