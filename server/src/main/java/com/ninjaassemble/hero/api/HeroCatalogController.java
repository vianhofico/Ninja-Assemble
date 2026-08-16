package com.ninjaassemble.hero.api;

import com.ninjaassemble.hero.catalog.HeroCatalogEntry;
import com.ninjaassemble.hero.catalog.HeroCatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/heroes")
public class HeroCatalogController {
    private final HeroCatalogService catalog;
    public HeroCatalogController(HeroCatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/catalog")
    public List<HeroCatalogEntry> catalog(@RequestParam(required = false) String group) {
        return catalog.byGroup(group);
    }
}
