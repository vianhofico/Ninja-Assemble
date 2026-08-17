package com.ninjaassemble.battle.sim;

/**
 * Application-facing entry point for the real-time simulator.
 *
 * <p>M47 introduced the new engine through the legacy {@link BattleRequest} container so existing callers could
 * migrate incrementally. This executor isolates that temporary bridge: production application code can now depend
 * only on {@link RealtimeBattleRequest}. When the legacy request/ruleset is removed, only this boundary and the
 * engine overload need to change.</p>
 */
public final class RealtimeBattleExecutor {
    private final RealtimeDeterministicBattleEngine engine;

    public RealtimeBattleExecutor() {
        this(new RealtimeDeterministicBattleEngine());
    }

    RealtimeBattleExecutor(RealtimeDeterministicBattleEngine engine) {
        if (engine == null) throw new IllegalArgumentException("realtime engine required");
        this.engine = engine;
    }

    public RealtimeBattleResult simulate(RealtimeBattleRequest request) {
        if (request == null) throw new IllegalArgumentException("realtime battle request required");
        BattleRequest compatibilityRequest = new BattleRequest(
                request.seed(),
                BattleRuleset.experimentalV1(),
                request.units()
        );
        return engine.simulate(compatibilityRequest, request.ruleset());
    }
}
