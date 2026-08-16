package com.ninjaassemble.shop.application;

import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.inventory.application.ItemCatalogService;
import com.ninjaassemble.shop.domain.ShopDefinition;
import com.ninjaassemble.shop.domain.ShopOffer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public final class ShopCatalogService {
    public static final String VERSION = "shop-catalog-design-v1";
    private static final String RESOURCE = "/game-data/shop/shop-offers.csv";
    private final List<ShopEntry> shops;
    private final Map<String, ShopEntry> byId;

    public ShopCatalogService(ItemCatalogService items) {
        Map<String, Builder> builders = new LinkedHashMap<>();
        try (InputStream input = ShopCatalogService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing shop catalog: " + RESOURCE);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                boolean header = true;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] cells = line.split(",", -1);
                    if (cells.length != 11) throw new IllegalStateException("invalid shop row: " + line);
                    items.require(cells[5]);
                    Currency.valueOf(cells[7]);
                    Integer limit = cells[9].isBlank() ? null : Integer.valueOf(cells[9]);
                    ShopOffer offer = new ShopOffer(cells[4], cells[5], Long.parseLong(cells[6]), cells[7], Long.parseLong(cells[8]), limit);
                    Builder builder = builders.computeIfAbsent(cells[0], ignored -> new Builder(cells[0], cells[1], cells[2], cells[3]));
                    if (!builder.refreshProfile.equals(cells[3])) throw new IllegalStateException("mixed refresh profile in shop: " + cells[0]);
                    if (!"DESIGN_BASELINE".equals(cells[10])) throw new IllegalStateException("shop offer must remain DESIGN_BASELINE: " + cells[4]);
                    if (builder.offers.stream().anyMatch(existing -> existing.id().equals(offer.id()))) throw new IllegalStateException("duplicate shop offer: " + offer.id());
                    builder.offers.add(offer);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("cannot load shop catalog", exception);
        }
        Map<String, ShopEntry> index = new LinkedHashMap<>();
        for (Builder builder : builders.values()) {
            ShopDefinition definition = new ShopDefinition(builder.id, "shop." + builder.id, builder.refreshProfile, builder.offers);
            index.put(builder.id, new ShopEntry(definition, builder.nameEn, builder.nameVi));
        }
        shops = List.copyOf(index.values());
        byId = Map.copyOf(index);
    }

    public List<ShopEntry> all() { return shops; }
    public ShopEntry require(String id) {
        ShopEntry value = byId.get(id);
        if (value == null) throw new IllegalArgumentException("unknown shop: " + id);
        return value;
    }
    public OfferEntry requireOffer(String shopId, String offerId) {
        ShopEntry shop = require(shopId);
        ShopOffer offer = shop.definition().offers().stream().filter(it -> it.id().equals(offerId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown shop offer: " + shopId + ":" + offerId));
        return new OfferEntry(shop, offer);
    }

    private static final class Builder {
        private final String id, nameEn, nameVi, refreshProfile;
        private final List<ShopOffer> offers = new ArrayList<>();
        private Builder(String id, String nameEn, String nameVi, String refreshProfile) {
            this.id = id; this.nameEn = nameEn; this.nameVi = nameVi; this.refreshProfile = refreshProfile;
        }
    }

    public record ShopEntry(ShopDefinition definition, String nameEn, String nameVi) {}
    public record OfferEntry(ShopEntry shop, ShopOffer offer) {}
}
