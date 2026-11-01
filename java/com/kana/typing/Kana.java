package com.kana.typing;

/** One kana and the Romanisations accepted for it. */
public final class Kana {
    public final String hiragana;
    public final String katakana;
    public final String[] romaji;

    public Kana(String hiragana, String katakana, String... romaji) {
        this.hiragana = hiragana;
        this.katakana = katakana;
        this.romaji = romaji;
    }

    public String glyph(Script script) {
        return script == Script.KATAKANA ? katakana : hiragana;
    }

    public String primaryRomaji() {
        return romaji[0];
    }

    public String[] alternatives() {
        if (romaji.length <= 1) {
            return new String[0];
        }
        String[] result = new String[romaji.length - 1];
        System.arraycopy(romaji, 1, result, 0, result.length);
        return result;
    }
}
