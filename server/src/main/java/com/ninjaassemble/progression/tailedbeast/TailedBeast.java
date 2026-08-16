package com.ninjaassemble.progression.tailedbeast;

public enum TailedBeast {
    SHUKAKU(1), MATATABI(2), ISOBU(3), SON_GOKU(4), KOKUO(5), SAIKEN(6), CHOMEI(7), GYUKI(8), KURAMA(9), TEN_TAILS(10);

    private final int tails;
    TailedBeast(int tails) { this.tails = tails; }
    public int tails() { return tails; }
}
