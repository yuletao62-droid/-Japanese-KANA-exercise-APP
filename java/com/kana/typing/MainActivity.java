package com.kana.typing;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single-screen kana trainer with both directions:
 * show kana -> type ROMAJI, or show ROMAJI -> choose the kana.
 */
public final class MainActivity extends Activity implements KeyboardView.Listener {
    private enum PracticeMode {
        KANA_TO_ROMAJI,
        ROMAJI_TO_KANA
    }

    private static final String TAG = "KanaTyping";
    private static final int STATE_TYPING = 0;
    private static final int STATE_CORRECT = 1;
    private static final int STATE_ERROR = 2;
    private static final int CHOICE_COUNT = 8;
    private static final long AUTO_ADVANCE_MS = 1600L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable advanceRunnable = new Runnable() {
        @Override
        public void run() {
            advance();
        }
    };
    private final List<TextView> choiceViews = new ArrayList<>();
    private final List<String> choiceGlyphs = new ArrayList<>();
    private final Map<String, Integer> audioResources = new HashMap<>();

    private GameSession session;
    private PracticeMode practiceMode = PracticeMode.KANA_TO_ROMAJI;
    private int state = STATE_TYPING;
    private String typed = "";

    private KanaView kanaView;
    private TextView roundLabel;
    private TextView scriptLabel;
    private TextView promptLabel;
    private TextView inputView;
    private TextView feedbackMain;
    private TextView feedbackSub;
    private TextView nextButton;
    private TextView toggleHiragana;
    private TextView toggleKatakana;
    private TextView modeForward;
    private TextView modeReverse;
    private TextView levelSeion;
    private TextView levelDakuon;
    private TextView levelAll;
    private TextView statCorrect;
    private TextView statWrong;
    private TextView statAccuracy;
    private TextView statStreak;
    private TextView keyboardHint;
    private KeyboardView keyboard;
    private LinearLayout kanaChoices;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        setContentView(R.layout.activity_main);
        bindViews();
        buildChoiceGrid();
        loadAudioResources();
        session = new GameSession();
        renderQuestion();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        window.setStatusBarColor(color(R.color.bg));

        View decor = window.getDecorView();
        int flags = decor.getSystemUiVisibility();
        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) {
            window.setNavigationBarColor(color(R.color.bg));
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            window.setNavigationBarColor(color(R.color.ink));
        }
        decor.setSystemUiVisibility(flags);
    }

    private void bindViews() {
        kanaView = findViewById(R.id.kana_view);
        roundLabel = findViewById(R.id.round_label);
        scriptLabel = findViewById(R.id.script_label);
        promptLabel = findViewById(R.id.prompt_label);
        inputView = findViewById(R.id.input_view);
        feedbackMain = findViewById(R.id.feedback_main);
        feedbackSub = findViewById(R.id.feedback_sub);
        nextButton = findViewById(R.id.next_button);
        toggleHiragana = findViewById(R.id.toggle_hiragana);
        toggleKatakana = findViewById(R.id.toggle_katakana);
        modeForward = findViewById(R.id.mode_forward);
        modeReverse = findViewById(R.id.mode_reverse);
        levelSeion = findViewById(R.id.level_seion);
        levelDakuon = findViewById(R.id.level_dakuon);
        levelAll = findViewById(R.id.level_all);
        statCorrect = findViewById(R.id.stat_correct);
        statWrong = findViewById(R.id.stat_wrong);
        statAccuracy = findViewById(R.id.stat_accuracy);
        statStreak = findViewById(R.id.stat_streak);
        keyboardHint = findViewById(R.id.keyboard_hint);
        keyboard = findViewById(R.id.keyboard);
        kanaChoices = findViewById(R.id.kana_choices);

        keyboard.setListener(this);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == STATE_ERROR || state == STATE_CORRECT) {
                    handler.removeCallbacks(advanceRunnable);
                    advance();
                }
            }
        });
        toggleHiragana.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectScript(Script.HIRAGANA);
            }
        });
        toggleKatakana.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectScript(Script.KATAKANA);
            }
        });
        modeForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPracticeMode(PracticeMode.KANA_TO_ROMAJI);
            }
        });
        modeReverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPracticeMode(PracticeMode.ROMAJI_TO_KANA);
            }
        });
        levelSeion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLevel(KanaLevel.SEION);
            }
        });
        levelDakuon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLevel(KanaLevel.DAKUON);
            }
        });
        levelAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLevel(KanaLevel.ALL);
            }
        });
    }

    private void buildChoiceGrid() {
        kanaChoices.removeAllViews();
        choiceViews.clear();
        int gap = dp(3);
        int rowHeight = dp(56);
        for (int row = 0; row < 2; row += 1) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, rowHeight);
            rowParams.bottomMargin = row == 0 ? dp(5) : 0;
            kanaChoices.addView(rowLayout, rowParams);

            for (int col = 0; col < 4; col += 1) {
                final int index = row * 4 + col;
                TextView tile = new TextView(this);
                tile.setGravity(Gravity.CENTER);
                tile.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
                tile.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                tile.setTextColor(color(R.color.ink));
                tile.setBackgroundResource(R.drawable.choice_bg);
                tile.setClickable(true);
                tile.setFocusable(true);
                tile.setIncludeFontPadding(false);
                tile.setContentDescription(getString(R.string.choice_description, ""));
                tile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onChoiceSelected(index);
                    }
                });
                LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                tileParams.setMargins(gap, 0, gap, 0);
                rowLayout.addView(tile, tileParams);
                choiceViews.add(tile);
            }
        }
    }

    private void renderQuestion() {
        handler.removeCallbacks(advanceRunnable);
        state = STATE_TYPING;
        typed = "";

        updateControls();
        roundLabel.setText(String.format(Locale.CHINA, "第 %02d 题", session.roundNo));
        String levelText = session.getLevel().label + " " + session.getPoolSize();
        scriptLabel.setText(session.getScript().labelEn + " / "
                + session.getScript().labelZh + " · " + levelText);

        if (practiceMode == PracticeMode.KANA_TO_ROMAJI) {
            promptLabel.setText(R.string.prompt_forward);
            kanaView.setKana(session.getPrompt());
            keyboardHint.setText(R.string.keyboard_hint);
            keyboard.setVisibility(View.VISIBLE);
            keyboard.setEnabled(true);
            kanaChoices.setVisibility(View.GONE);
        } else {
            promptLabel.setText(R.string.prompt_reverse);
            kanaView.setRomaji(session.getPrimaryAnswer());
            keyboardHint.setText(R.string.choices_hint);
            keyboard.setVisibility(View.GONE);
            keyboard.setEnabled(false);
            kanaChoices.setVisibility(View.VISIBLE);
            populateChoices();
        }

        inputView.setBackgroundResource(R.drawable.input_bg);
        showEmptyInput();
        showNeutralFeedback();
        nextButton.setVisibility(View.GONE);
        updateStats();
    }

    private void updateControls() {
        styleSegment(toggleHiragana, session.getScript() == Script.HIRAGANA);
        styleSegment(toggleKatakana, session.getScript() == Script.KATAKANA);
        styleSegment(modeForward, practiceMode == PracticeMode.KANA_TO_ROMAJI);
        styleSegment(modeReverse, practiceMode == PracticeMode.ROMAJI_TO_KANA);
        styleSegment(levelSeion, session.getLevel() == KanaLevel.SEION);
        styleSegment(levelDakuon, session.getLevel() == KanaLevel.DAKUON);
        styleSegment(levelAll, session.getLevel() == KanaLevel.ALL);
    }

    private void styleSegment(TextView view, boolean active) {
        if (active) {
            view.setBackgroundResource(R.drawable.toggle_active);
            view.setTextColor(color(R.color.white));
            view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
            view.setTextColor(color(R.color.muted));
            view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
    }

    private void showNeutralFeedback() {
        boolean forward = practiceMode == PracticeMode.KANA_TO_ROMAJI;
        feedbackMain.setTextColor(color(R.color.muted));
        feedbackMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        feedbackMain.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        feedbackMain.setText(forward ? R.string.neutral_forward_main : R.string.neutral_reverse_main);
        feedbackSub.setTextColor(color(R.color.muted_light));
        feedbackSub.setText(forward ? R.string.neutral_forward_sub : R.string.neutral_reverse_sub);
    }

    private void showEmptyInput() {
        boolean forward = practiceMode == PracticeMode.KANA_TO_ROMAJI;
        inputView.setText(forward ? R.string.input_hint_forward : R.string.input_hint_reverse);
        inputView.setTextColor(color(R.color.muted_light));
        inputView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        inputView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        inputView.setLetterSpacing(0.02f);
        inputView.setContentDescription(getString(R.string.input_description));
    }

    private void renderTypedInput() {
        if (typed.length() == 0) {
            showEmptyInput();
            return;
        }
        inputView.setText(typed);
        inputView.setTextColor(color(R.color.ink));
        inputView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f);
        inputView.setTypeface(Typeface.MONOSPACE);
        inputView.setLetterSpacing(0.08f);
        inputView.setContentDescription("已输入 " + typed);
    }

    private void showSelectedKana(String glyph, int textColor) {
        inputView.setText(glyph);
        inputView.setTextColor(textColor);
        inputView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
        inputView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        inputView.setLetterSpacing(0f);
        inputView.setContentDescription("已选择 " + glyph);
    }

    private void populateChoices() {
        List<Kana> choices = session.buildChoices(CHOICE_COUNT);
        choiceGlyphs.clear();
        for (int i = 0; i < choiceViews.size(); i += 1) {
            TextView tile = choiceViews.get(i);
            if (i < choices.size()) {
                String glyph = choices.get(i).glyph(session.getScript());
                choiceGlyphs.add(glyph);
                tile.setVisibility(View.VISIBLE);
                tile.setText(glyph);
                tile.setContentDescription(getString(R.string.choice_description, glyph));
                tile.setBackgroundResource(R.drawable.choice_bg);
                tile.setTextColor(color(R.color.ink));
                tile.setAlpha(1f);
                tile.setEnabled(true);
                tile.setClickable(true);
                tile.setFocusable(true);
            } else {
                choiceGlyphs.add("");
                tile.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onLetter(char letter) {
        if (practiceMode != PracticeMode.KANA_TO_ROMAJI || state != STATE_TYPING) {
            return;
        }
        typed = typed + Character.toLowerCase(letter);
        renderTypedInput();
        String result = session.classify(typed);
        if (GameSession.CORRECT.equals(result)) {
            handleCorrect();
        } else if (GameSession.WRONG.equals(result)) {
            handleWrong();
        }
    }

    @Override
    public void onBackspace() {
        if (practiceMode != PracticeMode.KANA_TO_ROMAJI || state != STATE_TYPING
                || typed.length() == 0) {
            return;
        }
        typed = typed.substring(0, typed.length() - 1);
        inputView.setBackgroundResource(R.drawable.input_bg);
        renderTypedInput();
        if (typed.length() == 0) {
            showNeutralFeedback();
        }
    }

    @Override
    public void onClear() {
        if (practiceMode != PracticeMode.KANA_TO_ROMAJI || state != STATE_TYPING) {
            return;
        }
        typed = "";
        inputView.setBackgroundResource(R.drawable.input_bg);
        renderTypedInput();
        showNeutralFeedback();
    }

    private void onChoiceSelected(int index) {
        if (practiceMode != PracticeMode.ROMAJI_TO_KANA || state != STATE_TYPING
                || index < 0 || index >= choiceGlyphs.size()) {
            return;
        }
        String chosen = choiceGlyphs.get(index);
        if (chosen.length() == 0) {
            return;
        }
        TextView tile = choiceViews.get(index);
        if (chosen.equals(session.getPrompt())) {
            handleReverseCorrect(tile);
        } else {
            handleReverseWrong(chosen, tile);
        }
    }

    private void handleCorrect() {
        state = STATE_CORRECT;
        session.recordCorrect();
        keyboard.setEnabled(false);
        inputView.setBackgroundResource(R.drawable.input_success);
        inputView.setTextColor(color(R.color.green_dark));

        feedbackMain.setTextColor(color(R.color.green_dark));
        feedbackMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        feedbackMain.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        feedbackMain.setText("✓ 答对了 · " + session.getPrimaryAnswer());

        feedbackSub.setTextColor(color(R.color.green));
        if (session.streak >= 2) {
            feedbackSub.setText("连续答对 " + session.streak + " 题 · 即将进入下一题");
        } else {
            feedbackSub.setText("即将进入下一题");
        }
        nextButton.setVisibility(View.GONE);
        updateStats();
        playCorrectPronunciation();
        handler.postDelayed(advanceRunnable, AUTO_ADVANCE_MS);
    }

    private void handleWrong() {
        state = STATE_ERROR;
        session.recordWrong();
        keyboard.setEnabled(false);
        inputView.setBackgroundResource(R.drawable.input_error);
        inputView.setTextColor(color(R.color.accent_dark));

        feedbackMain.setTextColor(color(R.color.accent_dark));
        feedbackMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        feedbackMain.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        feedbackMain.setText("「" + session.getPrompt() + "」的罗马音是 "
                + session.getPrimaryAnswer());

        StringBuilder sub = new StringBuilder("你输入了 “").append(typed).append("”");
        String[] alternatives = session.getAlternatives();
        if (alternatives.length > 0) {
            sub.append(" · 也可输入 ");
            for (int i = 0; i < alternatives.length; i += 1) {
                if (i > 0) {
                    sub.append(" / ");
                }
                sub.append(alternatives[i]);
            }
        }
        sub.append(" · 点击「下一题」继续");
        feedbackSub.setTextColor(color(R.color.accent_dark));
        feedbackSub.setText(sub.toString());

        nextButton.setVisibility(View.VISIBLE);
        updateStats();
        playCorrectPronunciation();
    }

    private void handleReverseCorrect(TextView correctTile) {
        state = STATE_CORRECT;
        session.recordCorrect();
        TextView correct = findChoiceView(session.getPrompt());
        lockChoices(correct, null);
        inputView.setBackgroundResource(R.drawable.input_success);
        showSelectedKana(session.getPrompt(), color(R.color.green_dark));

        feedbackMain.setTextColor(color(R.color.green_dark));
        feedbackMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        feedbackMain.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        feedbackMain.setText("✓ 答对了 · " + session.getPrompt() + " = "
                + session.getPrimaryAnswer());
        feedbackSub.setTextColor(color(R.color.green));
        if (session.streak >= 2) {
            feedbackSub.setText("连续答对 " + session.streak + " 题 · 即将进入下一题");
        } else {
            feedbackSub.setText("即将进入下一题");
        }
        nextButton.setVisibility(View.GONE);
        updateStats();
        playCorrectPronunciation();
        handler.postDelayed(advanceRunnable, AUTO_ADVANCE_MS);
    }

    private void handleReverseWrong(String chosen, TextView wrongTile) {
        state = STATE_ERROR;
        session.recordWrong();
        TextView correct = findChoiceView(session.getPrompt());
        lockChoices(correct, wrongTile);
        inputView.setBackgroundResource(R.drawable.input_error);
        showSelectedKana(chosen, color(R.color.accent_dark));

        feedbackMain.setTextColor(color(R.color.accent_dark));
        feedbackMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        feedbackMain.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        feedbackMain.setText("“" + session.getPrimaryAnswer() + "” 对应的是「"
                + session.getPrompt() + "」");
        feedbackSub.setTextColor(color(R.color.accent_dark));
        feedbackSub.setText("你选了「" + chosen + "」 · 点击「下一题」继续");

        nextButton.setVisibility(View.VISIBLE);
        updateStats();
        playCorrectPronunciation();
    }

    private void lockChoices(TextView correct, TextView wrong) {
        for (TextView tile : choiceViews) {
            tile.setClickable(false);
            tile.setFocusable(false);
            tile.setEnabled(false);
            if (tile == correct) {
                tile.setBackgroundResource(R.drawable.choice_correct);
                tile.setTextColor(color(R.color.green_dark));
                tile.setAlpha(1f);
            } else if (tile == wrong) {
                tile.setBackgroundResource(R.drawable.choice_wrong);
                tile.setTextColor(color(R.color.accent_dark));
                tile.setAlpha(1f);
            } else {
                tile.setAlpha(0.42f);
            }
        }
    }

    private TextView findChoiceView(String glyph) {
        for (int i = 0; i < choiceGlyphs.size() && i < choiceViews.size(); i += 1) {
            if (choiceGlyphs.get(i).equals(glyph)) {
                return choiceViews.get(i);
            }
        }
        return null;
    }

    private void selectPracticeMode(PracticeMode mode) {
        if (mode == practiceMode) {
            return;
        }
        handler.removeCallbacks(advanceRunnable);
        practiceMode = mode;
        session.newQuestion();
        renderQuestion();
    }

    private void selectLevel(KanaLevel level) {
        if (level == session.getLevel()) {
            return;
        }
        handler.removeCallbacks(advanceRunnable);
        session.switchLevel(level);
        renderQuestion();
    }

    private void selectScript(Script script) {
        if (session.getScript() == script) {
            return;
        }
        handler.removeCallbacks(advanceRunnable);
        session.switchScript(script);
        renderQuestion();
    }

    private void advance() {
        if (state != STATE_CORRECT && state != STATE_ERROR) {
            return;
        }
        handler.removeCallbacks(advanceRunnable);
        session.newQuestion();
        renderQuestion();
    }

    private void updateStats() {
        statCorrect.setText(String.valueOf(session.correct));
        statWrong.setText(String.valueOf(session.wrong));
        if (session.answered() == 0) {
            statAccuracy.setText("—");
        } else {
            statAccuracy.setText(String.format(Locale.CHINA, "%.0f%%", session.accuracy()));
        }
        statStreak.setText(String.valueOf(session.streak));
    }

    private void loadAudioResources() {
        for (Kana kana : KanaData.ALL) {
            int resourceId = getResources().getIdentifier(
                    "kana_" + kana.primaryRomaji(), "raw", getPackageName());
            if (resourceId != 0) {
                audioResources.put(kana.primaryRomaji(), resourceId);
            }
        }
        Log.i(TAG, "bundled audio clips loaded: " + audioResources.size());
        if (audioResources.size() != 66) {
            Log.w(TAG, "expected 66 audio clips, found " + audioResources.size());
        }
    }

    private void playCorrectPronunciation() {
        if (session == null || session.current == null) {
            return;
        }
        Integer resourceId = audioResources.get(session.current.primaryRomaji());
        if (resourceId == null) {
            Log.w(TAG, "missing audio for " + session.current.hiragana);
            return;
        }
        releaseAudio();
        try {
            mediaPlayer = MediaPlayer.create(this, resourceId);
            if (mediaPlayer == null) {
                Log.w(TAG, "MediaPlayer.create returned null");
                return;
            }
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer player) {
                    player.release();
                    if (mediaPlayer == player) {
                        mediaPlayer = null;
                    }
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer player, int what, int extra) {
                    Log.w(TAG, "audio playback error " + what + "/" + extra);
                    player.release();
                    if (mediaPlayer == player) {
                        mediaPlayer = null;
                    }
                    return true;
                }
            });
            mediaPlayer.start();
            Log.i(TAG, "playing pronunciation for " + session.current.hiragana);
        } catch (Exception exception) {
            Log.w(TAG, "unable to play pronunciation", exception);
            releaseAudio();
        }
    }

    private void releaseAudio() {
        if (mediaPlayer == null) {
            return;
        }
        try {
            mediaPlayer.stop();
        } catch (IllegalStateException ignored) {
            // Already completed or not prepared; release below is enough.
        }
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }
        int code = event.getKeyCode();
        if (practiceMode == PracticeMode.ROMAJI_TO_KANA) {
            if (code >= KeyEvent.KEYCODE_1 && code < KeyEvent.KEYCODE_1 + CHOICE_COUNT) {
                if (state == STATE_TYPING) {
                    int index = code - KeyEvent.KEYCODE_1;
                    if (index >= 0 && index < choiceViews.size()
                            && choiceViews.get(index).getVisibility() == View.VISIBLE) {
                        choiceViews.get(index).performClick();
                    }
                }
                return true;
            }
            if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (state != STATE_TYPING) {
                    handler.removeCallbacks(advanceRunnable);
                    advance();
                }
                return true;
            }
            return super.dispatchKeyEvent(event);
        }

        if (code >= KeyEvent.KEYCODE_A && code <= KeyEvent.KEYCODE_Z) {
            onLetter((char) ('a' + code - KeyEvent.KEYCODE_A));
            return true;
        }
        if (code == KeyEvent.KEYCODE_DEL) {
            onBackspace();
            return true;
        }
        if (code == KeyEvent.KEYCODE_FORWARD_DEL) {
            onClear();
            return true;
        }
        if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (state == STATE_TYPING && typed.length() > 0) {
                handleWrong();
            } else if (state != STATE_TYPING) {
                handler.removeCallbacks(advanceRunnable);
                advance();
            }
            return true;
        }
        if (code == KeyEvent.KEYCODE_ESCAPE) {
            onClear();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(advanceRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (state == STATE_CORRECT) {
            handler.postDelayed(advanceRunnable, 800L);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseAudio();
        super.onDestroy();
    }

    private int color(int id) {
        return getResources().getColor(id);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
