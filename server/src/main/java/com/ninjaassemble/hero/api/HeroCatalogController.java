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

    @GetMapping("/{id}/variants")
    public List<HeroVariantEntry> variants(@PathVariable String id) {
        catalog.require(id);
        return variants.forCharacter(id);
    }

    @GetMapping("/{id}/kit")
    public HeroContentCatalogService.HeroKitView kit(@PathVariable String id, @RequestParam(required = false) String variant) {
        catalog.require(id);
        return content.resolve(id, variant);
    }
}
