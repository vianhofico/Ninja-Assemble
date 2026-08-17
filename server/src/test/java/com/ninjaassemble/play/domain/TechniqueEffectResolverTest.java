package com.ninjaassemble.play.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import com.ninjaassemble.hero.domain.EffectType;
import com.ninjaassemble.hero.domain.TargetSelector;
import java.util.List;
import org.junit.jupiter.api.Test;

class TechniqueEffectResolverTest {
    private final HeroContentCatalogService catalog = new HeroContentCatalogService();
    private final TechniqueEffectResolver resolver = new TechniqueEffectResolver();

    @Test
    void allTechniquesHaveAnExplicitRuntimeOrDeferredMappingState() {
        int runtime = 0;
        int deferredPassive = 0;
        for (HeroContentCatalogService.TechniqueView technique : catalog.allTechniques()) {
            TechniqueEffectResolver.Resolution resolution = resolver.resolve(technique);
            assertEquals(technique.id(), resolution.techniqueId());
            if ("PASSIVE".equals(technique.kind())) {
                assertEquals(TechniqueEffectResolver.MappingStatus.DEFERRED_PASSIVE, resolution.status());
                assertTrue(resolution.effects().isEmpty());
                deferredPassive++;
            } else {
                assertEquals(TechniqueEffectResolver.MappingStatus.RUNTIME, resolution.status());
                assertFalse(resolution.effects().isEmpty());
                runtime++;
            }
        }
        assertEquals(catalog.techniqueCount(), runtime + deferredPassive);
        assertTrue(runtime > 0);
        assertTrue(deferredPassive > 0);
    }

    @Test
    void curatedMedicalPoisonControlAndShieldTechniquesUseMillisecondSemantics() {
        var healing = resolver.resolve(catalog.technique("mass-healing")).effects();
        assertEquals(List.of(EffectType.HEAL, EffectType.CLEANSE), healing.stream().map(it -> it.type()).toList());
        assertEquals(TargetSelector.ALL_ALLIES, healing.get(0).target());

        var poison = resolver.resolve(catalog.technique("poison-cloud")).effects();
        assertEquals(List.of(EffectType.DAMAGE, EffectType.STATUS), poison.stream().map(it -> it.type()).toList());
        assertEquals("POISON", poison.get(1).status());
        assertEquals(6_000L, poison.get(1).durationMs());
        assertEquals(1_000L, poison.get(1).tickIntervalMs());

        var shield = resolver.resolve(catalog.technique("sand-shield")).effects();
        assertEquals(EffectType.SHIELD, shield.get(0).type());

        var control = resolver.resolve(catalog.technique("tsukuyomi")).effects();
        assertEquals("STUN", control.get(1).status());
        assertEquals(3_000L, control.get(1).durationMs());
        assertEquals(0L, control.get(1).tickIntervalMs());
    }

    @Test
    void genericFallbackUsesOnlyMachineReadableKindChannelAndTags() {
        var basic = resolver.resolve(catalog.technique("basic-kunai"));
        assertEquals("KIND_BASIC", basic.basis());
        assertEquals(TargetSelector.FRONTMOST_ENEMY, basic.effects().get(0).target());

        var ultimate = resolver.resolve(catalog.technique("kirin"));
        assertEquals("TAG_BURST_ULTIMATE", ultimate.basis());
        assertEquals(TargetSelector.ALL_ENEMIES, ultimate.effects().get(0).target());

        var active = resolver.resolve(catalog.technique("rasengan"));
        assertEquals("CURATED_ID", active.basis());
        assertEquals(TargetSelector.FRONTMOST_ENEMY, active.effects().get(0).target());
    }
}
