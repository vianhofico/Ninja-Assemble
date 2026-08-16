package com.ninjaassemble.play.api;

import com.ninjaassemble.equipment.application.PlayableEquipmentCatalogService;
import com.ninjaassemble.equipment.application.PlayableEquipmentService;
import com.ninjaassemble.progression.scroll.PlayableScrollCatalogService;
import com.ninjaassemble.progression.scroll.PlayableScrollService;
import com.ninjaassemble.progression.tailedbeast.PlayableJinchurikiCatalogService;
import com.ninjaassemble.progression.tailedbeast.PlayableTailedBeastService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/play/{playerId}/loadout")
public class PlayableLoadoutController {
    private final PlayableEquipmentCatalogService equipmentCatalog;
    private final PlayableEquipmentService equipment;
    private final PlayableScrollCatalogService scrollCatalog;
    private final PlayableScrollService scrolls;
    private final PlayableJinchurikiCatalogService jinchuriki;
    private final PlayableTailedBeastService tailedBeast;

    public PlayableLoadoutController(PlayableEquipmentCatalogService equipmentCatalog, PlayableEquipmentService equipment,
                                     PlayableScrollCatalogService scrollCatalog, PlayableScrollService scrolls,
                                     PlayableJinchurikiCatalogService jinchuriki, PlayableTailedBeastService tailedBeast) {
        this.equipmentCatalog = equipmentCatalog; this.equipment = equipment;
        this.scrollCatalog = scrollCatalog; this.scrolls = scrolls;
        this.jinchuriki = jinchuriki; this.tailedBeast = tailedBeast;
    }

    @GetMapping("/equipment/catalog")
    public List<PlayableEquipmentCatalogService.EquipmentView> equipmentCatalog() { return equipmentCatalog.all(); }

    @PostMapping("/equipment/bootstrap")
    public List<PlayableEquipmentService.OwnedEquipmentView> bootstrapEquipment(@PathVariable UUID playerId) {
        return equipment.grantStarterSet(playerId);
    }

    @GetMapping("/equipment")
    public List<PlayableEquipmentService.OwnedEquipmentView> equipment(@PathVariable UUID playerId) {
        return equipment.list(playerId);
    }

    @PutMapping("/equipment/{equipmentId}/equip/{playerHeroId}")
    public PlayableEquipmentService.OwnedEquipmentView equip(@PathVariable UUID playerId, @PathVariable UUID equipmentId,
                                                              @PathVariable UUID playerHeroId) {
        return equipment.equip(playerId, playerHeroId, equipmentId);
    }

    @PostMapping("/equipment/{equipmentId}/enhance")
    public PlayableEquipmentService.EnhanceResult enhance(@PathVariable UUID playerId, @PathVariable UUID equipmentId,
                                                           @RequestBody ActionRequest request) {
        return equipment.enhance(playerId, equipmentId, requireRequestId(request));
    }

    @GetMapping("/scrolls/catalog")
    public List<PlayableScrollCatalogService.ScrollView> scrollCatalog() { return scrollCatalog.all(); }

    @PostMapping("/scrolls/bootstrap")
    public List<PlayableScrollService.OwnedScrollView> bootstrapScrolls(@PathVariable UUID playerId) {
        return scrolls.grantStarterSet(playerId);
    }

    @GetMapping("/scrolls")
    public List<PlayableScrollService.OwnedScrollView> scrolls(@PathVariable UUID playerId) { return scrolls.list(playerId); }

    @PutMapping("/scrolls/{playerScrollId}/inlay/{playerHeroId}")
    public PlayableScrollService.OwnedScrollView inlay(@PathVariable UUID playerId, @PathVariable UUID playerScrollId,
                                                        @PathVariable UUID playerHeroId) {
        return scrolls.inlay(playerId, playerHeroId, playerScrollId);
    }

    @PostMapping("/scrolls/combine")
    public PlayableScrollService.CombineResult combine(@PathVariable UUID playerId, @RequestBody CombineScrollRequest request) {
        if (request == null || request.definitionId() == null || request.definitionId().isBlank())
            throw new IllegalArgumentException("definitionId is required");
        return scrolls.combine(playerId, request.definitionId(), request.level(), requireRequestId(request.requestId()));
    }

    @GetMapping("/tailed-beasts/hosts")
    public List<PlayableJinchurikiCatalogService.HostView> jinchurikiHosts() { return jinchuriki.all(); }

    @PostMapping("/tailed-beasts/materials/bootstrap")
    public PlayableTailedBeastService.MaterialView bootstrapBeastMaterials(@PathVariable UUID playerId) {
        return tailedBeast.bootstrapMaterials(playerId);
    }

    @GetMapping("/tailed-beasts/materials")
    public PlayableTailedBeastService.MaterialView beastMaterials(@PathVariable UUID playerId) { return tailedBeast.materials(playerId); }

    @GetMapping("/tailed-beasts/heroes/{playerHeroId}")
    public PlayableTailedBeastService.BeastProgressView beastProgress(@PathVariable UUID playerId, @PathVariable UUID playerHeroId) {
        return tailedBeast.progress(playerId, playerHeroId);
    }

    @PostMapping("/tailed-beasts/heroes/{playerHeroId}/advance")
    public PlayableTailedBeastService.AdvanceResult advanceBeast(@PathVariable UUID playerId, @PathVariable UUID playerHeroId,
                                                                  @RequestBody ActionRequest request) {
        return tailedBeast.advance(playerId, playerHeroId, requireRequestId(request));
    }

    private static UUID requireRequestId(ActionRequest request) {
        if (request == null || request.requestId() == null) throw new IllegalArgumentException("requestId is required");
        return request.requestId();
    }
    private static UUID requireRequestId(UUID requestId) {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        return requestId;
    }

    public record ActionRequest(UUID requestId) {}
    public record CombineScrollRequest(UUID requestId, String definitionId, int level) {}
}
