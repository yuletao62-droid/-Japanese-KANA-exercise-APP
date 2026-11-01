package com.kana.typing;

/** Japanese kana tables: 46 seion, 20 dakuten, and their union. */
public final class KanaData {
    private KanaData() {
    }

    public static final Kana[] SEION = {
            new Kana("あ", "ア", "a"),
            new Kana("い", "イ", "i"),
            new Kana("う", "ウ", "u"),
            new Kana("え", "エ", "e"),
            new Kana("お", "オ", "o"),
            new Kana("か", "カ", "ka"),
            new Kana("き", "キ", "ki"),
            new Kana("く", "ク", "ku"),
            new Kana("け", "ケ", "ke"),
            new Kana("こ", "コ", "ko"),
            new Kana("さ", "サ", "sa"),
            new Kana("し", "シ", "shi", "si"),
            new Kana("す", "ス", "su"),
            new Kana("せ", "セ", "se"),
            new Kana("そ", "ソ", "so"),
            new Kana("た", "タ", "ta"),
            new Kana("ち", "チ", "chi", "ti"),
            new Kana("つ", "ツ", "tsu", "tu"),
            new Kana("て", "テ", "te"),
            new Kana("と", "ト", "to"),
            new Kana("な", "ナ", "na"),
            new Kana("に", "ニ", "ni"),
            new Kana("ぬ", "ヌ", "nu"),
            new Kana("ね", "ネ", "ne"),
            new Kana("の", "ノ", "no"),
            new Kana("は", "ハ", "ha"),
            new Kana("ひ", "ヒ", "hi"),
            new Kana("ふ", "フ", "fu", "hu"),
            new Kana("へ", "ヘ", "he"),
            new Kana("ほ", "ホ", "ho"),
            new Kana("ま", "マ", "ma"),
            new Kana("み", "ミ", "mi"),
            new Kana("む", "ム", "mu"),
            new Kana("め", "メ", "me"),
            new Kana("も", "モ", "mo"),
            new Kana("や", "ヤ", "ya"),
            new Kana("ゆ", "ユ", "yu"),
            new Kana("よ", "ヨ", "yo"),
            new Kana("ら", "ラ", "ra"),
            new Kana("り", "リ", "ri"),
            new Kana("る", "ル", "ru"),
            new Kana("れ", "レ", "re"),
            new Kana("ろ", "ロ", "ro"),
            new Kana("わ", "ワ", "wa"),
            new Kana("を", "ヲ", "wo"),
            new Kana("ん", "ン", "n", "nn")
    };

    public static final Kana[] DAKUON = {
            new Kana("が", "ガ", "ga"),
            new Kana("ぎ", "ギ", "gi"),
            new Kana("ぐ", "グ", "gu"),
            new Kana("げ", "ゲ", "ge"),
            new Kana("ご", "ゴ", "go"),
            new Kana("ざ", "ザ", "za"),
            new Kana("じ", "ジ", "ji", "zi"),
            new Kana("ず", "ズ", "zu"),
            new Kana("ぜ", "ゼ", "ze"),
            new Kana("ぞ", "ゾ", "zo"),
            new Kana("だ", "ダ", "da"),
            new Kana("ぢ", "ヂ", "di"),
            new Kana("づ", "ヅ", "du"),
            new Kana("で", "デ", "de"),
            new Kana("ど", "ド", "do"),
            new Kana("ば", "バ", "ba"),
            new Kana("び", "ビ", "bi"),
            new Kana("ぶ", "ブ", "bu"),
            new Kana("べ", "ベ", "be"),
            new Kana("ぼ", "ボ", "bo")
    };

    public static final Kana[] ALL;

    static {
        ALL = new Kana[SEION.length + DAKUON.length];
        System.arraycopy(SEION, 0, ALL, 0, SEION.length);
        System.arraycopy(DAKUON, 0, ALL, SEION.length, DAKUON.length);
    }

    public static Kana[] forLevel(KanaLevel level) {
        if (level == KanaLevel.DAKUON) {
            return DAKUON;
        }
        if (level == KanaLevel.ALL) {
            return ALL;
        }
        return SEION;
    }
}
