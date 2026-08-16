package com.ninjaassemble.meta.application;

import com.ninjaassemble.campaign.domain.RewardBundle;
import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.inventory.application.InventoryService;
import com.ninjaassemble.meta.domain.EventDefinition;
import com.ninjaassemble.meta.domain.ObjectiveDefinition;
import com.ninjaassemble.meta.domain.ObjectiveType;
import com.ninjaassemble.meta.domain.QuestDefinition;
import com.ninjaassemble.meta.domain.ResetCadence;
import com.ninjaassemble.player.application.PlayerService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class WeeklyEventService {
    public static final String PROFILE_VERSION = "weekly-event-design-v1";
    private final PlayerService players;
    private final WalletService wallet;
    private final InventoryService inventory;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ZoneId zone;
    private final int resetHour;

    public WeeklyEventService(PlayerService players, WalletService wallet, InventoryService inventory, JdbcTemplate jdbc,
                              Clock clock, @Value("${game.clock.zone:Asia/Bangkok}") String zone,
                              @Value("${game.clock.reset-hour:5}") int resetHour) {
        this.players = players;
        this.wallet = wallet;
        this.inventory = inventory;
        this.jdbc = jdbc;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
        this.resetHour = resetHour;
    }

    @Transactional
    public EventBoard view(UUID playerId) {
        players.require(playerId);
        Window window = window();
        EventDefinition event = definition(window);
        if (!event.activeAt(clock.instant())) throw new IllegalStateException("weekly event is not active");
        ensureProgress(playerId, event.id());
        int claimMask = claimMask(playerId, event.id(), false);
        List<ObjectiveView> views = new ArrayList<>();
        for (int index = 0; index < event.objectives().size(); index++) {
            QuestDefinition quest = event.objectives().get(index);
            long current = observed(playerId, quest.objective().type(), window.start());
            boolean claimed = (claimMask & (1 << index)) != 0;
            views.add(view(index, quest, current, claimed));
        }
        persistSnapshot(playerId, event.id(), claimMask, views);
        return new EventBoard(PROFILE_VERSION, event.id(), "Shinobi Weekly", "Tuần lễ Shinobi",
                event.startsAt(), event.endsAt(), List.copyOf(views));
    }

    @Transactional
    public ClaimResult claim(UUID playerId, String objectiveId) {
        players.require(playerId);
        Window window = window();
        EventDefinition event = definition(window);
        if (!event.activeAt(clock.instant())) throw new IllegalStateException("weekly event is not active");
        int index = objectiveIndex(event, objectiveId);
        QuestDefinition quest = event.objectives().get(index);
        ensureProgress(playerId, event.id());
        int claimMask = claimMask(playerId, event.id(), true);
        int bit = 1 << index;
        long current = observed(playerId, quest.objective().type(), window.start());
        if ((claimMask & bit) != 0) return result(event.id(), quest, current, true);
        if (current < quest.objective().target()) throw new IllegalStateException("event objective is not complete");

        RewardBundle reward = quest.reward();
        long gold = reward.currencies().getOrDefault("GOLD", 0L);
        long diamond = reward.currencies().getOrDefault("DIAMOND", 0L);
        String itemId = reward.items().keySet().stream().findFirst().orElse(null);
        long itemQuantity = itemId == null ? 0 : reward.items().getOrDefault(itemId, 0L);
        String prefix = "event:" + event.id() + ":" + quest.id();
        if (gold > 0) wallet.mutate(playerId, Currency.GOLD, gold, "EVENT_REWARD", quest.id(), prefix + ":gold");
        if (diamond > 0) wallet.mutate(playerId, Currency.DIAMOND, diamond, "EVENT_REWARD", quest.id(), prefix + ":diamond");
        if (itemId != null && itemQuantity > 0)
            inventory.mutate(playerId, itemId, itemQuantity, "EVENT_REWARD", prefix + ":item:" + itemId);
        int updatedMask = claimMask | bit;
        jdbc.update("update player_event_progress set objective_state = jsonb_set(objective_state, '{claimMask}', to_jsonb(?::int), true), updated_at = ? where player_id = ? and event_id = ?",
                updatedMask, clock.instant(), playerId, event.id());
        return result(event.id(), quest, current, false);
    }

    private EventDefinition definition(Window window) {
        List<QuestDefinition> objectives = List.of(
                quest("weekly-campaign-5", "event.weekly.campaign", ObjectiveType.CLEAR_STAGE, 5,
                        new RewardBundle(0, Map.of("GOLD", 12_000L), Map.of("upgrade-scroll", 2L))),
                quest("weekly-arena-3", "event.weekly.arena", ObjectiveType.WIN_ARENA, 3,
                        new RewardBundle(0, Map.of("DIAMOND", 80L), Map.of())),
                quest("weekly-summon-2", "event.weekly.summon", ObjectiveType.SUMMON, 2,
                        new RewardBundle(0, Map.of("GOLD", 20_000L, "DIAMOND", 120L), Map.of("summon-ticket", 1L)))
        );
        return new EventDefinition("shinobi-weekly-" + window.key(), "event.shinobi_weekly", window.start(), window.end(), objectives);
    }

    private static QuestDefinition quest(String id, String nameKey, ObjectiveType type, long target, RewardBundle reward) {
        return new QuestDefinition(id, nameKey, new ObjectiveDefinition(type, null, target), reward, ResetCadence.EVENT);
    }

    private long observed(UUID playerId, ObjectiveType type, Instant start) {
        Long value = switch (type) {
            case CLEAR_STAGE -> jdbc.queryForObject("select count(*) from campaign_runs where player_id = ? and completed_at >= ? and result = 'TEAM_A'", Long.class, playerId, start);
            case WIN_ARENA -> jdbc.queryForObject("select count(*) from arena_battles where challenger_id = ? and created_at >= ? and result = 'TEAM_A'", Long.class, playerId, start);
            case SUMMON -> jdbc.queryForObject("select count(*) from summon_history where player_id = ? and created_at >= ?", Long.class, playerId, start);
            default -> throw new IllegalArgumentException("unsupported weekly event objective: " + type);
        };
        return value == null ? 0 : value;
    }

    private void ensureProgress(UUID playerId, String eventId) {
        jdbc.update("""
                insert into player_event_progress(player_id, event_id, objective_state, updated_at)
                values (?, ?, jsonb_build_object('claimMask', 0), ?)
                on conflict (player_id, event_id) do nothing
                """, playerId, eventId, clock.instant());
    }

    private int claimMask(UUID playerId, String eventId, boolean lock) {
        String suffix = lock ? " for update" : "";
        Integer value = jdbc.queryForObject("select coalesce((objective_state->>'claimMask')::int, 0) from player_event_progress where player_id = ? and event_id = ?" + suffix,
                Integer.class, playerId, eventId);
        return value == null ? 0 : value;
    }

    private void persistSnapshot(UUID playerId, String eventId, int claimMask, List<ObjectiveView> views) {
        long campaign = current(views, "weekly-campaign-5");
        long arena = current(views, "weekly-arena-3");
        long summon = current(views, "weekly-summon-2");
        jdbc.update("""
                update player_event_progress
                set objective_state = jsonb_build_object('claimMask', ?::int, 'campaignClear', ?::bigint, 'arenaWins', ?::bigint, 'summons', ?::bigint), updated_at = ?
                where player_id = ? and event_id = ?
                """, claimMask, campaign, arena, summon, clock.instant(), playerId, eventId);
    }

    private static long current(List<ObjectiveView> views, String id) {
        return views.stream().filter(view -> view.objectiveId().equals(id)).findFirst().map(ObjectiveView::currentValue).orElse(0L);
    }

    private static ObjectiveView view(int index, QuestDefinition quest, long current, boolean claimed) {
        RewardBundle reward = quest.reward();
        String itemId = reward.items().keySet().stream().findFirst().orElse(null);
        long itemQuantity = itemId == null ? 0 : reward.items().getOrDefault(itemId, 0L);
        return new ObjectiveView(index, quest.id(), quest.objective().type().name(), quest.objective().target(),
                Math.min(current, quest.objective().target()), claimed, current >= quest.objective().target() && !claimed,
                reward.currencies().getOrDefault("GOLD", 0L), reward.currencies().getOrDefault("DIAMOND", 0L), itemId, itemQuantity);
    }

    private static int objectiveIndex(EventDefinition event, String objectiveId) {
        if (objectiveId == null || objectiveId.isBlank()) throw new IllegalArgumentException("objectiveId is required");
        for (int i = 0; i < event.objectives().size(); i++) if (event.objectives().get(i).id().equals(objectiveId)) return i;
        throw new IllegalArgumentException("unknown event objective: " + objectiveId);
    }

    private ClaimResult result(String eventId, QuestDefinition quest, long current, boolean replayed) {
        RewardBundle reward = quest.reward();
        String itemId = reward.items().keySet().stream().findFirst().orElse(null);
        long itemQuantity = itemId == null ? 0 : reward.items().getOrDefault(itemId, 0L);
        return new ClaimResult(eventId, quest.id(), replayed, reward.currencies().getOrDefault("GOLD", 0L),
                reward.currencies().getOrDefault("DIAMOND", 0L), itemId, itemQuantity, current);
    }

    private Window window() {
        ZonedDateTime now = clock.instant().atZone(zone);
        LocalDate effectiveDate = now.getHour() < resetHour ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        LocalDate monday = effectiveDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        ZonedDateTime start = monday.atTime(resetHour, 0).atZone(zone);
        return new Window(monday.toString(), start.toInstant(), start.plusDays(7).toInstant());
    }

    private record Window(String key, Instant start, Instant end) {}
    public record EventBoard(String profileVersion, String eventId, String nameEn, String nameVi, Instant startsAt, Instant endsAt,
                             List<ObjectiveView> objectives) {}
    public record ObjectiveView(int index, String objectiveId, String metric, long target, long currentValue, boolean claimed,
                                boolean claimable, long rewardGold, long rewardDiamond, String rewardItemId, long rewardItemQuantity) {}
    public record ClaimResult(String eventId, String objectiveId, boolean replayed, long gold, long diamond,
                              String itemId, long itemQuantity, long finalValue) {}
}
