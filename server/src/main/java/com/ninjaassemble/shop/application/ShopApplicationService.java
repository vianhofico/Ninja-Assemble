package com.ninjaassemble.shop.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.inventory.application.InventoryService;
import com.ninjaassemble.inventory.application.ItemCatalogService;
import com.ninjaassemble.shop.domain.ShopOffer;
import com.ninjaassemble.shop.domain.ShopPurchaseGate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class ShopApplicationService {
    private final ShopCatalogService catalog;
    private final ItemCatalogService items;
    private final InventoryService inventory;
    private final WalletService wallet;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ZoneId zone;
    private final int resetHour;

    public ShopApplicationService(ShopCatalogService catalog, ItemCatalogService items, InventoryService inventory,
                                  WalletService wallet, JdbcTemplate jdbc, Clock clock,
                                  @Value("${game.clock.zone:Asia/Bangkok}") String zone,
                                  @Value("${game.clock.reset-hour:5}") int resetHour) {
        this.catalog = catalog;
        this.items = items;
        this.inventory = inventory;
        this.wallet = wallet;
        this.jdbc = jdbc;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
        this.resetHour = resetHour;
    }

    @Transactional(readOnly = true)
    public ShopView view(UUID playerId) {
        String resetKey = resetKey();
        List<ShopViewEntry> shops = new ArrayList<>();
        for (ShopCatalogService.ShopEntry entry : catalog.all()) {
            List<OfferView> offers = entry.definition().offers().stream().map(offer -> offerView(playerId, entry, offer, resetKey)).toList();
            shops.add(new ShopViewEntry(entry.definition().id(), entry.nameEn(), entry.nameVi(), entry.definition().refreshProfile(), offers));
        }
        return new ShopView(ShopCatalogService.VERSION, resetKey, List.copyOf(shops));
    }

    @Transactional
    public PurchaseResult purchase(UUID playerId, String shopId, String offerId, UUID requestId) {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        ShopCatalogService.OfferEntry entry = catalog.requireOffer(shopId, offerId);
        ShopOffer offer = entry.offer();
        Currency currency = Currency.valueOf(offer.currency());
        String resetKey = resetKey();
        String paymentKey = "shop:" + requestId + ":pay";
        String itemKey = "shop:" + requestId + ":item";

        Boolean replay = jdbc.queryForObject("select exists(select 1 from wallet_ledger where player_id = ? and idempotency_key = ?)", Boolean.class, playerId, paymentKey);
        if (Boolean.TRUE.equals(replay)) {
            long itemBalance = inventory.list(playerId).stream().filter(it -> it.itemDefinitionId().equals(offer.itemDefinitionId()))
                    .mapToLong(it -> it.quantity()).findFirst().orElse(0L);
            return new PurchaseResult(shopId, offerId, resetKey, true, currency.name(), 0, itemBalance, purchasedCount(playerId, shopId, offerId, resetKey));
        }

        jdbc.queryForObject("select id from players where id = ? for update", UUID.class, playerId);
        int purchased = purchasedCount(playerId, shopId, offerId, resetKey);
        long balance = wallet.getBalance(playerId, currency);
        ShopPurchaseGate.Result gate = ShopPurchaseGate.evaluate(offer, balance, purchased);
        if (!gate.allowed()) throw new IllegalStateException("shop purchase blocked: " + gate.reason());

        wallet.mutate(playerId, currency, -offer.price(), "SHOP_PURCHASE", shopId + ":" + offerId, paymentKey);
        var stack = inventory.mutate(playerId, offer.itemDefinitionId(), offer.quantity(), "SHOP_PURCHASE", itemKey);
        jdbc.update("""
                insert into shop_purchase_state(player_id, shop_id, offer_id, reset_key, purchase_count)
                values (?, ?, ?, ?, 1)
                on conflict (player_id, shop_id, offer_id, reset_key)
                do update set purchase_count = shop_purchase_state.purchase_count + 1
                """, playerId, shopId, offerId, resetKey);
        return new PurchaseResult(shopId, offerId, resetKey, false, currency.name(), offer.price(), stack.quantity(), purchased + 1);
    }

    private OfferView offerView(UUID playerId, ShopCatalogService.ShopEntry shop, ShopOffer offer, String resetKey) {
        ItemCatalogService.ItemDefinition item = items.require(offer.itemDefinitionId());
        int purchased = purchasedCount(playerId, shop.definition().id(), offer.id(), resetKey);
        long balance = wallet.getBalance(playerId, Currency.valueOf(offer.currency()));
        ShopPurchaseGate.Result gate = ShopPurchaseGate.evaluate(offer, balance, purchased);
        Integer remaining = offer.purchaseLimit() == null ? null : Math.max(0, offer.purchaseLimit() - purchased);
        return new OfferView(offer.id(), item.id(), item.nameEn(), item.nameVi(), offer.quantity(), offer.currency(), offer.price(),
                offer.purchaseLimit(), purchased, remaining, gate.allowed(), gate.reason());
    }

    private int purchasedCount(UUID playerId, String shopId, String offerId, String resetKey) {
        Integer count = jdbc.query("""
                select purchase_count from shop_purchase_state
                where player_id = ? and shop_id = ? and offer_id = ? and reset_key = ?
                """, rs -> rs.next() ? rs.getInt(1) : null, playerId, shopId, offerId, resetKey);
        return count == null ? 0 : count;
    }

    private String resetKey() {
        ZonedDateTime now = clock.instant().atZone(zone);
        LocalDate gameDate = now.getHour() < resetHour ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        return gameDate.toString();
    }

    public record ShopView(String catalogVersion, String resetKey, List<ShopViewEntry> shops) {}
    public record ShopViewEntry(String shopId, String nameEn, String nameVi, String refreshProfile, List<OfferView> offers) {}
    public record OfferView(String offerId, String itemId, String itemNameEn, String itemNameVi, long quantity,
                            String currency, long price, Integer purchaseLimit, int purchasedCount, Integer remaining,
                            boolean purchasable, String blockedReason) {}
    public record PurchaseResult(String shopId, String offerId, String resetKey, boolean replayed, String currency,
                                 long charged, long itemBalanceAfter, int purchaseCount) {}
}
