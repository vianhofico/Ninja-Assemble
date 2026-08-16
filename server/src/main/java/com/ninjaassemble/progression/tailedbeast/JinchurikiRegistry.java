package com.ninjaassemble.progression.tailedbeast;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class JinchurikiRegistry {
    private final Map<String, JinchurikiDefinition> byCharacter;

    public JinchurikiRegistry(List<JinchurikiDefinition> definitions) {
        if (definitions == null) throw new IllegalArgumentException("definitions required");
        this.byCharacter = definitions.stream().collect(Collectors.toUnmodifiableMap(JinchurikiDefinition::characterId, Function.identity()));
    }

    public JinchurikiDefinition require(String characterId) {
        JinchurikiDefinition definition = byCharacter.get(characterId);
        if (definition == null) throw new IllegalArgumentException("character is not registered as a Jinchuriki: " + characterId);
        return definition;
    }
}
