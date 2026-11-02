package com.kana.typing;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Independent game state for a practice session. This class uses no Android
 * APIs, which keeps the rules testable on a normal JVM.
 */
public final class GameSession {
    public static final String EMPTY = "empty";
    public static final String PARTIAL = "partial";
    public static final String CORRECT = "correct";
    public static final String WRONG = "wrong";

    private final Random random;
    private Kana[] pool;
    private KanaLevel level;
    private int currentIndex = -1;
    private Script script = Script.HIRAGANA;

    public Kana current;
    public int roundNo;
    public int correct;
    public int wrong;
    public int streak;
    public int bestStreak;

    public GameSession() {
        this(new Random(), KanaLevel.SEION);
    }

    public GameSession(Random random, KanaLevel level) {
        this.random = random == null ? new Random() : random;
        this.level = level == null ? KanaLevel.SEION : level;
        this.pool = KanaData.forLevel(this.level);
        newQuestion();
    }

    public Kana newQuestion() {
        if (pool.length == 1) {
            currentIndex = 0;
        } else {
            int next;
            do {
                next = random.nextInt(pool.length);
            } while (next == currentIndex);
            currentIndex = next;
        }
        current = pool[currentIndex];
        roundNo += 1;
        return current;
    }

    public boolean switchScript(Script newScript) {
        if (newScript == null || newScript == script) {
            return false;
        }
        script = newScript;
        newQuestion();
        return true;
    }

    public boolean switchLevel(KanaLevel newLevel) {
        if (newLevel == null || newLevel == level) {
            return false;
        }
        level = newLevel;
        pool = KanaData.forLevel(newLevel);
        currentIndex = -1;
        newQuestion();
        return true;
    }

    public Script getScript() {
        return script;
    }

    public KanaLevel getLevel() {
        return level;
    }

    public int getPoolSize() {
        return pool.length;
    }

    public String getPrompt() {
        return current.glyph(script);
    }

    public String getPrimaryAnswer() {
        return current.primaryRomaji();
    }

    public String[] getAlternatives() {
        return current.alternatives();
    }

    /**
     * Builds a small unique answer set for ROMAJI -> kana practice, always
     * containing the current kana.
     */
    public List<Kana> buildChoices(int count) {
        Kana[] source = pool.length >= count ? pool : KanaData.ALL;
        int desired = Math.min(Math.max(1, count), source.length);
        List<Kana> distractors = new ArrayList<>();
        for (Kana kana : source) {
            if (kana != current) {
                distractors.add(kana);
            }
        }
        Collections.shuffle(distractors, random);
        List<Kana> result = new ArrayList<>();
        result.add(current);
        for (int i = 0; i < distractors.size() && result.size() < desired; i += 1) {
            result.add(distractors.get(i));
        }
        Collections.shuffle(result, random);
        return result;
    }

    public String classify(String typed) {
        String normalized = normalize(typed);
        if (normalized.length() == 0) {
            return EMPTY;
        }
        for (String answer : current.romaji) {
            if (normalized.equals(answer)) {
                return CORRECT;
            }
        }
        for (String answer : current.romaji) {
            if (answer.startsWith(normalized)) {
                return PARTIAL;
            }
        }
        return WRONG;
    }

    public static String normalize(String typed) {
        if (typed == null) {
            return "";
        }
        return Normalizer.normalize(typed, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public void recordCorrect() {
        correct += 1;
        streak += 1;
        if (streak > bestStreak) {
            bestStreak = streak;
        }
    }

    public void recordWrong() {
        wrong += 1;
        streak = 0;
    }

    public int answered() {
        return correct + wrong;
    }

    public double accuracy() {
        if (answered() == 0) {
            return 0.0;
        }
        return (double) correct / (double) answered() * 100.0;
    }

    public Kana reset() {
        roundNo = 0;
        correct = 0;
        wrong = 0;
        streak = 0;
        bestStreak = 0;
        return newQuestion();
    }
}
