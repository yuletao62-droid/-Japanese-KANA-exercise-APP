package com.kana.typing;

/** Writing system currently used for the prompt. */
public enum Script {
    HIRAGANA("HIRAGANA", "平假名"),
    KATAKANA("KATAKANA", "片假名");

    public final String labelEn;
    public final String labelZh;

    Script(String labelEn, String labelZh) {
        this.labelEn = labelEn;
        this.labelZh = labelZh;
    }

    public Script toggle() {
        return this == HIRAGANA ? KATAKANA : HIRAGANA;
    }
}
