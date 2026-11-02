package com.kana.typing;

/** The kana set used by a practice session. */
public enum KanaLevel {
    SEION("清音"),
    DAKUON("浊音"),
    HANDAKUON("半浊音"),
    ALL("全部");

    public final String label;

    KanaLevel(String label) {
        this.label = label;
    }
}
