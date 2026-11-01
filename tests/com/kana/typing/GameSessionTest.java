package com.kana.typing;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Plain-JVM tests for the Android app's underlying game rules. */
public final class GameSessionTest {
    private static int checks;

    private GameSessionTest() {
    }

    private static void check(boolean condition, String message) {
        checks += 1;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        testKanaTable();
        testNormalizationAndClassification();
        testQuestionFlow();
        testScriptSwitch();
        testStatsAndReset();
        testLevelsAndChoices();
        System.out.println("GameSessionTest: " + checks + " checks passed");
    }

    private static void testKanaTable() {
        check(KanaData.SEION.length == 46, "expected 46 seion kana");
        check(KanaData.DAKUON.length == 20, "expected 20 dakuten kana");
        check(KanaData.ALL.length == 66, "expected 66 total kana");

        HashSet<String> hiragana = new HashSet<>();
        HashSet<String> katakana = new HashSet<>();
        HashSet<String> spellings = new HashSet<>();
        for (Kana kana : KanaData.ALL) {
            hiragana.add(kana.hiragana);
            katakana.add(kana.katakana);
            check(kana.romaji.length > 0, "missing romaji for " + kana.hiragana);
            for (String spelling : kana.romaji) {
                check(spelling.equals(spelling.toLowerCase()), "romaji must be lower-case");
                check(spelling.matches("[a-z]+"), "romaji must be ASCII letters");
                spellings.add(kana.hiragana + ":" + spelling);
            }
            check(kana.katakana.codePointAt(0) - kana.hiragana.codePointAt(0) == 0x60,
                    "katakana offset for " + kana.hiragana);
        }
        check(hiragana.size() == 66, "duplicate hiragana");
        check(katakana.size() == 66, "duplicate katakana");
        check(spellings.size() == 72, "unexpected number of accepted spellings");

        Kana ji = find("じ");
        check(java.util.Arrays.asList(ji.romaji).contains("ji"), "ji for じ");
        check(java.util.Arrays.asList(ji.romaji).contains("zi"), "zi for じ");
        Kana di = find("ぢ");
        check(java.util.Arrays.asList(di.romaji).contains("di"), "di for ぢ");
        check(di.primaryRomaji().equals("di"), "ぢ primary romaji must be di");
        Kana du = find("づ");
        check(du.primaryRomaji().equals("du"), "づ primary romaji must be du");
    }

    private static void testNormalizationAndClassification() {
        check(GameSession.normalize("  SHI ").equals("shi"), "normalize case/space");
        check(GameSession.normalize("ｓｈｉ").equals("shi"), "normalize full-width");
        Kana shi = find("し");
        GameSession session = new GameSession(new Random(1), KanaLevel.SEION);
        session.current = shi;
        check(session.classify("").equals(GameSession.EMPTY), "empty");
        check(session.classify("s").equals(GameSession.PARTIAL), "partial");
        check(session.classify("shi").equals(GameSession.CORRECT), "correct shi");
        check(session.classify("SI").equals(GameSession.CORRECT), "correct alternative");
        check(session.classify("ｓｉ").equals(GameSession.CORRECT), "correct full-width");
        check(session.classify("sha").equals(GameSession.WRONG), "wrong");
        check(shi.primaryRomaji().equals("shi"), "primary answer");
        check(shi.alternatives().length == 1 && shi.alternatives()[0].equals("si"),
                "si accepted as an alternative");
        Kana n = find("ん");
        check(java.util.Arrays.asList(n.romaji).contains("n"), "n accepted");
        check(java.util.Arrays.asList(n.romaji).contains("nn"), "nn accepted");
        Kana ji = find("じ");
        session.current = ji;
        check(session.classify("ji").equals(GameSession.CORRECT), "ji correct");
        check(session.classify("zi").equals(GameSession.CORRECT), "zi correct");
        session.current = find("ぢ");
        check(session.classify("di").equals(GameSession.CORRECT), "di correct");
        check(session.classify("ji").equals(GameSession.WRONG), "ambiguous ji is rejected for ぢ");
    }

    private static Kana find(String hiragana) {
        for (Kana kana : KanaData.ALL) {
            if (kana.hiragana.equals(hiragana)) {
                return kana;
            }
        }
        throw new AssertionError("kana not found: " + hiragana);
    }

    private static void testQuestionFlow() {
        GameSession session = new GameSession(new Random(12), KanaLevel.ALL);
        Kana previous = session.current;
        check(session.roundNo == 1, "initial round number");
        for (int i = 0; i < 350; i += 1) {
            Kana next = session.newQuestion();
            check(next != previous, "question repeated immediately");
            previous = next;
        }
        check(session.roundNo == 351, "round counter increments");
        HashSet<String> seen = new HashSet<>();
        for (int i = 0; i < 2500; i += 1) {
            seen.add(session.newQuestion().hiragana);
        }
        check(seen.size() == 66, "all kana should be reachable");
    }

    private static void testScriptSwitch() {
        GameSession session = new GameSession(new Random(3), KanaLevel.SEION);
        check(session.getScript() == Script.HIRAGANA, "starts in hiragana");
        check(session.getPrompt().equals(session.current.hiragana), "hiragana glyph");
        check(session.switchScript(Script.KATAKANA), "switch succeeds");
        check(session.getScript() == Script.KATAKANA, "script changed");
        check(session.getPrompt().equals(session.current.katakana), "katakana glyph");
        check(!session.switchScript(Script.KATAKANA), "same script is a no-op");
    }

    private static void testStatsAndReset() {
        GameSession session = new GameSession(new Random(4), KanaLevel.SEION);
        session.recordCorrect();
        session.recordCorrect();
        session.recordWrong();
        session.recordCorrect();
        check(session.correct == 3, "correct count");
        check(session.wrong == 1, "wrong count");
        check(session.answered() == 4, "answered count");
        check(session.streak == 1, "current streak");
        check(session.bestStreak == 2, "best streak");
        check(Math.abs(session.accuracy() - 75.0) < 0.0001, "accuracy");
        session.reset();
        check(session.correct == 0 && session.wrong == 0, "stats reset");
        check(session.roundNo == 1, "round reset and new question");
        check(session.streak == 0 && session.bestStreak == 0, "streak reset");
    }

    private static void testLevelsAndChoices() {
        GameSession session = new GameSession(new Random(8), KanaLevel.SEION);
        check(session.getLevel() == KanaLevel.SEION, "initial level");
        check(session.getPoolSize() == 46, "seion pool size");
        check(session.switchLevel(KanaLevel.DAKUON), "switch to dakuten");
        check(session.getLevel() == KanaLevel.DAKUON, "level set to dakuten");
        check(session.getPoolSize() == 20, "dakuten pool size");
        check(containsKana(KanaData.DAKUON, session.current.hiragana), "dakuten question");
        check(!session.switchLevel(KanaLevel.DAKUON), "same level is a no-op");
        check(session.switchLevel(KanaLevel.ALL), "switch to all");
        check(session.getPoolSize() == 66, "all pool size");

        for (int i = 0; i < 100; i += 1) {
            List<Kana> choices = session.buildChoices(8);
            check(choices.size() == 8, "expected 8 choices");
            check(choices.contains(session.current), "choices contain the answer");
            HashSet<String> glyphs = new HashSet<>();
            for (Kana choice : choices) {
                glyphs.add(choice.hiragana);
                check(containsKana(KanaData.ALL, choice.hiragana), "choice outside pool");
            }
            check(glyphs.size() == 8, "choices must be unique");
            session.newQuestion();
        }

        session.switchLevel(KanaLevel.DAKUON);
        for (int i = 0; i < 100; i += 1) {
            List<Kana> choices = session.buildChoices(8);
            for (Kana choice : choices) {
                check(containsKana(KanaData.DAKUON, choice.hiragana), "choice outside dakuten pool");
            }
            session.newQuestion();
        }
    }

    private static boolean containsKana(Kana[] table, String hiragana) {
        for (Kana kana : table) {
            if (kana.hiragana.equals(hiragana)) {
                return true;
            }
        }
        return false;
    }
}
