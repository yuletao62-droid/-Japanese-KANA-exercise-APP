package com.kana.typing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact QWERTY pad drawn as a single canvas. Keeping this custom avoids the
 * system IME entirely, so the trainer always receives plain ROMAJI letters.
 */
public final class KeyboardView extends View {
    public interface Listener {
        void onLetter(char letter);

        void onBackspace();

        void onClear();
    }

    private static final String[] ROWS = {
            "qwertyuiop",
            "asdfghjkl",
            "zxcvbnm"
    };
    private static final float KEY_HEIGHT_DP = 46f;
    private static final float GAP_DP = 5f;
    private static final float PAD_H_DP = 2f;
    private static final float PAD_V_DP = 3f;

    private final List<Key> keys = new ArrayList<>();
    private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint letterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final float scaledDensity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isEnabled() || pressedIndex < 0 || pressedIndex >= keys.size()) {
                return;
            }
            if (!keys.get(pressedIndex).backspace) {
                return;
            }
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            if (listener != null) {
                listener.onClear();
            }
        }
    };

    private Listener listener;
    private int pressedIndex = -1;

    public KeyboardView(Context context) {
        this(context, null);
    }

    public KeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        initPaints();
        setFocusable(true);
        setClickable(true);
    }

    private void initPaints() {
        keyPaint.setStyle(Paint.Style.FILL);
        keyPaint.setColor(0xFFFFFDF8);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(1f));
        borderPaint.setColor(0xFFDED1C1);

        letterPaint.setColor(0xFF2D2823);
        letterPaint.setTextAlign(Paint.Align.CENTER);
        letterPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desiredHeight = Math.round(dp(KEY_HEIGHT_DP * 3f + GAP_DP * 2f + PAD_V_DP * 2f));
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildKeys(w);
    }

    private void buildKeys(int width) {
        keys.clear();
        float padH = dp(PAD_H_DP);
        float padV = dp(PAD_V_DP);
        float gap = dp(GAP_DP);
        float keyH = dp(KEY_HEIGHT_DP);
        float keyW = (width - padH * 2f - gap * 9f) / 10f;
        if (keyW <= 0f) {
            return;
        }

        for (int row = 0; row < ROWS.length; row += 1) {
            String letters = ROWS[row];
            float startX = padH;
            if (row > 0) {
                startX += (keyW + gap) / 2f;
            }
            float y = padV + row * (keyH + gap);
            for (int i = 0; i < letters.length(); i += 1) {
                float x = startX + i * (keyW + gap);
                keys.add(Key.letter(letters.charAt(i), x, y, keyW, keyH));
            }
            if (row == 2) {
                float x = startX + 7f * (keyW + gap);
                keys.add(Key.backspace(x, y, keyW * 2f + gap, keyH));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = dp(10f);

        for (int i = 0; i < keys.size(); i += 1) {
            Key key = keys.get(i);
            boolean pressed = isEnabled() && i == pressedIndex;
            keyPaint.setColor(pressed ? 0xFFF7E5DE : 0xFFFFFDF8);
            borderPaint.setColor(pressed ? 0xFFC95042 : 0xFFDED1C1);
            letterPaint.setColor(pressed ? 0xFFB04437 : 0xFF2D2823);
            canvas.drawRoundRect(key.rect, radius, radius, keyPaint);
            canvas.drawRoundRect(key.rect, radius, radius, borderPaint);

            if (key.backspace) {
                letterPaint.setTextSize(sp(12f));
                letterPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            } else {
                letterPaint.setTextSize(sp(17f));
                letterPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            }
            Paint.FontMetrics metrics = letterPaint.getFontMetrics();
            float baseline = key.rect.centerY() - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(key.label, key.rect.centerX(), baseline, letterPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressedIndex = findKey(x, y);
                if (pressedIndex < 0) {
                    invalidate();
                    return true;
                }
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                Key key = keys.get(pressedIndex);
                if (key.backspace) {
                    if (listener != null) {
                        listener.onBackspace();
                    }
                    handler.postDelayed(longPressRunnable, 500L);
                } else if (listener != null) {
                    listener.onLetter(key.value);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                pressedIndex = -1;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private int findKey(float x, float y) {
        for (int i = 0; i < keys.size(); i += 1) {
            if (keys.get(i).rect.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            handler.removeCallbacks(longPressRunnable);
            pressedIndex = -1;
        }
        setAlpha(enabled ? 1f : 0.48f);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(longPressRunnable);
        super.onDetachedFromWindow();
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * scaledDensity;
    }

    private static final class Key {
        final String label;
        final char value;
        final boolean backspace;
        final RectF rect;

        private Key(String label, char value, boolean backspace, RectF rect) {
            this.label = label;
            this.value = value;
            this.backspace = backspace;
            this.rect = rect;
        }

        static Key letter(char value, float x, float y, float width, float height) {
            return new Key(String.valueOf(Character.toUpperCase(value)), value, false,
                    new RectF(x, y, x + width, y + height));
        }

        static Key backspace(float x, float y, float width, float height) {
            return new Key("删除", '\b', true, new RectF(x, y, x + width, y + height));
        }
    }
}
