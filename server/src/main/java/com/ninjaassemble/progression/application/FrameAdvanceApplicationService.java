package com.ninjaassemble.progression.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.play.application.ActionRequestService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrameAdvanceApplicationService {
    private static final String ACTION = "FRAME_ADVANCE";
    private final HeroOwnershipService ownership;
    private final WalletService wallet;
    private final ActionRequestService requests;
    private final JdbcTemplate jdbc;

    public FrameAdvanceApplicationService(HeroOwnershipService ownership, WalletService wallet, ActionRequestService requests, JdbcTemplate jdbc) {
        this.ownership = ownership; this.wallet = wallet; this.requests = requests; this.jdbc = jdbc;
    }

    @Transactional
    public FrameAdvanceResult advance(UUID playerId, UUID playerHeroId, UUID requestId) {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        ownership.requireOwned(playerId, playerHeroId);
        FrameState state = jdbc.query("select frame_tier, frame_advance_step from player_heroes where player_id = ? and id = ? for update",
                (rs, row) -> new FrameState(rs.getString(1), rs.getInt(2)), playerId, playerHeroId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("owned hero not found"));

        Optional<String> existing = requests.existing(playerId, requestId, ACTION);
        if (existing.isPresent()) {
            FrameAdvanceResult replay = decode(existing.get());
            if (!replay.playerHeroId().equals(playerHeroId)) throw new IllegalStateException("request id was used for another hero");
            return replay;
        }
        requests.reserve(playerId, requestId, ACTION);

        FrameAdvancePolicy.AdvanceResult policy = FrameAdvancePolicy.advance(state.tier(), state.step());
        wallet.mutate(playerId, Currency.GOLD, -policy.goldCost(), "FRAME_ADVANCE", playerHeroId.toString(), "frame:" + requestId + ":gold");
        jdbc.update("update player_heroes set frame_tier = ?, frame_advance_step = ? where player_id = ? and id = ?",
                policy.tierAfter(), policy.stepAfter(), playerId, playerHeroId);
        jdbc.update("""
                insert into hero_progression_events(player_hero_id, track, before_state, after_state, source)
                values (?, 'FRAME_ADVANCE',
                    jsonb_build_object('tier', ?, 'step', ?::int),
                    jsonb_build_object('tier', ?, 'step', ?::int), ?)
                """, playerHeroId, state.tier(), state.step(), policy.tierAfter(), policy.stepAfter(), requestId.toString());

        FrameAdvanceResult result = new FrameAdvanceResult(playerHeroId, policy.tierAfter(), policy.stepAfter(), policy.goldCost(), policy.profileVersion());
        requests.complete(playerId, requestId, encode(result));
        return result;
    }

    private static String encode(FrameAdvanceResult result) {
        return String.join("\t", result.playerHeroId().toString(), result.frameTier(), Integer.toString(result.frameStep()),
                Long.toString(result.goldCost()), result.profileVersion());
    }

    private static FrameAdvanceResult decode(String stored) {
        String[] p = stored.split("\t", -1);
        if (p.length != 5) throw new IllegalStateException("corrupt stored frame advance response");
        return new FrameAdvanceResult(UUID.fromString(p[0]), p[1], Integer.parseInt(p[2]), Long.parseLong(p[3]), p[4]);
    }

    private record FrameState(String tier, int step) {}
    public record FrameAdvanceResult(UUID playerHeroId, String frameTier, int frameStep, long goldCost, String profileVersion) {}
}
