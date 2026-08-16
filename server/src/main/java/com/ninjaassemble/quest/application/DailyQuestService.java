package com.ninjaassemble.quest.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.inventory.application.InventoryService;
import java.time.Clock;
import java.time.Instant;
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
public final class DailyQuestService {
    private final QuestCatalogService catalog;
    private final WalletService wallet;
    private final InventoryService inventory;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ZoneId zone;
    private final int resetHour;

    public DailyQuestService(QuestCatalogService catalog, WalletService wallet, InventoryService inventory,
                             JdbcTemplate jdbc, Clock clock,
                             @Value("${game.clock.zone:Asia/Bangkok}") String zone,
                             @Value("${game.clock.reset-hour:5}") int resetHour) {
        this.catalog = catalog;
        this.wallet = wallet;
        this.inventory = inventory;
        this.jdbc = jdbc;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
        this.resetHour = resetHour;
    }

    @Transactional
    public QuestBoard view(UUID playerId) {
        ResetWindow window = resetWindow();
        List<QuestView> quests = new ArrayList<>();
        for (QuestCatalogService.QuestDefinition quest : catalog.all()) {
            long observed = observed(playerId, quest.metric(), window.start());
            upsert(playerId, quest.id(), window.key(), observed);
            Progress progress = progress(playerId, quest.id(), window.key(), false);
            quests.add(toView(quest, progress));
        }
        return new QuestBoard(QuestCatalogService.VERSION, window.key(), window.nextReset(), List.copyOf(quests));
    }

    @Transactional
    public ClaimResult claim(UUID playerId, String questId) {
        QuestCatalogService.QuestDefinition quest = catalog.require(questId);
        ResetWindow window = resetWindow();
        jdbc.queryForObject("select id from players where id = ? for update", UUID.class, playerId);
        long observed = observed(playerId, quest.metric(), window.start());
        upsert(playerId, quest.id(), window.key(), observed);
        Progress progress = progress(playerId, quest.id(), window.key(), true);
        if (progress.claimed()) return new ClaimResult(questId, window.key(), true, 0, 0, quest.rewardItemId(), 0, progress.currentValue());
        if (progress.currentValue() < quest.target()) throw new IllegalStateException("quest is not complete");

        String prefix = "quest:" + window.key() + ":" + quest.id();
        if (quest.rewardGold() > 0) wallet.mutate(playerId, Currency.GOLD, quest.rewardGold(), "QUEST_REWARD", quest.id(), prefix + ":gold");
        if (quest.rewardDiamond() > 0) wallet.mutate(playerId, Currency.DIAMOND, quest.rewardDiamond(), "QUEST_REWARD", quest.id(), prefix + ":diamond");
        if (quest.rewardItemId() != null && quest.rewardItemQuantity() > 0)
            inventory.mutate(playerId, quest.rewardItemId(), quest.rewardItemQuantity(), "QUEST_REWARD", prefix + ":item:" + quest.rewardItemId());
        jdbc.update("""
                update player_quest_progress set claimed = true, updated_at = ?
                where player_id = ? and quest_id = ? and reset_key = ?
                """, clock.instant(), playerId, quest.id(), window.key());
        return new ClaimResult(questId, window.key(), false, quest.rewardGold(), quest.rewardDiamond(),
                quest.rewardItemId(), quest.rewardItemQuantity(), progress.currentValue());
    }

    private QuestView toView(QuestCatalogService.QuestDefinition quest, Progress progress) {
        return new QuestView(quest.id(), quest.nameEn(), quest.nameVi(), quest.metric().name(), quest.target(),
                Math.min(progress.currentValue(), quest.target()), progress.claimed(),
                progress.currentValue() >= quest.target() && !progress.claimed(),
                quest.rewardGold(), quest.rewardDiamond(), quest.rewardItemId(), quest.rewardItemQuantity());
    }

    private long observed(UUID playerId, QuestCatalogService.Metric metric, Instant start) {
        Long value = switch (metric) {
            case CAMPAIGN_CLEAR -> jdbc.queryForObject("select count(*) from campaign_runs where player_id = ? and completed_at >= ? and result = 'TEAM_A'", Long.class, playerId, start);
            case ARENA_BATTLE -> jdbc.queryForObject("select count(*) from arena_battles where challenger_id = ? and created_at >= ?", Long.class, playerId, start);
            case SUMMON -> jdbc.queryForObject("select count(*) from summon_history where player_id = ? and created_at >= ?", Long.class, playerId, start);
            case HERO_LEVEL_UP -> jdbc.queryForObject("select count(*) from wallet_ledger where player_id = ? and created_at >= ? and reason = 'HERO_LEVEL_UP'", Long.class, playerId, start);
        };
        return value == null ? 0 : value;
    }

    private void upsert(UUID playerId, String questId, String resetKey, long observed) {
        jdbc.update("""
                insert into player_quest_progress(player_id, quest_id, reset_key, current_value, claimed, updated_at)
                values (?, ?, ?, ?, false, ?)
                on conflict (player_id, quest_id, reset_key)
                do update set current_value = greatest(player_quest_progress.current_value, excluded.current_value), updated_at = excluded.updated_at
                """, playerId, questId, resetKey, observed, clock.instant());
    }

    private Progress progress(UUID playerId, String questId, String resetKey, boolean lock) {
        String suffix = lock ? " for update" : "";
        return jdbc.queryForObject("select current_value, claimed from player_quest_progress where player_id = ? and quest_id = ? and reset_key = ?" + suffix,
                (rs, row) -> new Progress(rs.getLong(1), rs.getBoolean(2)), playerId, questId, resetKey);
    }

    private ResetWindow resetWindow() {
        ZonedDateTime now = clock.instant().atZone(zone);
        LocalDate date = now.getHour() < resetHour ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        ZonedDateTime start = date.atTime(resetHour, 0).atZone(zone);
        return new ResetWindow(date.toString(), start.toInstant(), start.plusDays(1).toInstant());
    }

    private record Progress(long currentValue, boolean claimed) {}
    private record ResetWindow(String key, Instant start, Instant nextReset) {}
    public record QuestBoard(String catalogVersion, String resetKey, Instant nextResetAt, List<QuestView> quests) {}
    public record QuestView(String questId, String nameEn, String nameVi, String metric, long target, long currentValue,
                            boolean claimed, boolean claimable, long rewardGold, long rewardDiamond,
                            String rewardItemId, long rewardItemQuantity) {}
    public record ClaimResult(String questId, String resetKey, boolean replayed, long gold, long diamond,
                              String itemId, long itemQuantity, long finalValue) {}
}
