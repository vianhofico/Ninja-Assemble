package com.ninjaassemble.progression.scroll;

public record ScrollCombineProfile(String version, int copiesRequired, int maxLevel) {
    public ScrollCombineProfile {
        if (version == null || version.isBlank() || copiesRequired < 2 || maxLevel < 2) throw new IllegalArgumentException("invalid combine profile");
    }

    public static ScrollCombineProfile experimentalV1() { return new ScrollCombineProfile("experimental-v1-unverified", 3, 10); }
}
