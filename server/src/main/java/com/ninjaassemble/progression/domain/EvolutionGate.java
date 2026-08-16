package com.ninjaassemble.progression.domain;

import java.util.ArrayList;
import java.util.List;

public final class EvolutionGate {
    private EvolutionGate() {}

    public static GateResult evaluate(EvolutionRequirement requirement, HeroEvolutionContext context) {
        List<String> missing = new ArrayList<>();
        if (context.level() < requirement.minLevel()) missing.add("level:" + requirement.minLevel());
        if (context.frameTier().ordinal() < requirement.minFrameTier().ordinal()) missing.add("frame:" + requirement.minFrameTier());
        if (context.framePlus() < requirement.minFramePlus()) missing.add("framePlus:" + requirement.minFramePlus());
        for (String flag : requirement.requiredFlags()) if (!context.flags().contains(flag)) missing.add("flag:" + flag);
        requirement.requiredMaterials().forEach((id, amount) -> {
            if (context.materials().getOrDefault(id, 0L) < amount) missing.add("material:" + id + ":" + amount);
        });
        return new GateResult(missing.isEmpty(), List.copyOf(missing));
    }

    public record GateResult(boolean allowed, List<String> missing) {}
}
