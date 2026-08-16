package com.ninjaassemble.hero.api;

import com.ninjaassemble.hero.catalog.HeroCatalogEntry;
import com.ninjaassemble.hero.catalog.HeroCatalogService;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import com.ninjaassemble.hero.catalog.HeroVariantEntry;
import com.ninjaassemble.hero.catalog.VariantCatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/heroes")
public class HeroCatalogController {
    private final HeroCatalogService catalog;
    private final VariantCatalogService variants;
    private final HeroContentCatalogService content;

    public HeroCatalogController(HeroCatalogService catalog, VariantCatalogService variants, HeroContentCatalogService content) {
        this.catalog = catalog;
        this.variants = variants;
        this.content = content;
    }

    @GetMapping("/catalog")
    public List<HeroCatalogEntry> catalog(@RequestParam(required = false) String group) {
        return catalog.byGroup(group);
    }

    /** Legacy discovery surface retained until M45/M46 client migration. */
    @Deprecated(forRemoval = true)
    @GetMapping("/{id}/variants")
    public List<HeroVariantEntry> variants(@PathVariable String id) {
        catalog.require(id);
        return variants.forCharacter(id);
    }

    /** Production Hero Version kit endpoint: exactly 5 normal slots or 6 awakened slots. */
    @GetMapping("/versions/{heroId}/kit")
    public HeroContentCatalogService.HeroKitView heroVersionKit(@PathVariable String heroId,
                                                                @RequestParam(defaultValue = "false") boolean awakened) {
        return content.resolveHero(heroId, awakened);
    }

    /** Legacy compatibility endpoint. Resolution is bridge-only; generic character/variant fallback was removed. */
    @Deprecated(forRemoval = true)
    @GetMapping("/{id}/kit")
    public HeroContentCatalogService.HeroKitView legacyKit(@PathVariable String id,
                                                           @RequestParam(required = false) String variant) {
        catalog.require(id);
        return content.resolve(id, variant);
    }
}
