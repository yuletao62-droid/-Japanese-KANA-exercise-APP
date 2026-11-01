package com.kana.typing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** Draws the pale sun disc and either a large kana glyph or a ROMAJI prompt. */
public final class KanaView extends View {
    private final Paint discPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface kanaTypeface = Typeface.create("sans-serif", Typeface.BOLD);
    private final Typeface latinTypeface = Typeface.create("sans-serif", Typeface.BOLD);
    private final float density;
    private String displayText = "あ";
    private boolean latin;

    public KanaView(Context context) {
        this(context, null);
    }

    public KanaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KanaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        initPaints();
        setWillNotDraw(false);
        updateContentDescription();
    }

    private void initPaints() {
        discPaint.setStyle(Paint.Style.FILL);
        discPaint.setColor(0xFFF8EEE3);
        discPaint.setDither(true);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(1.2f));
        ringPaint.setColor(0xFFE7D1BF);

        innerRingPaint.setStyle(Paint.Style.STROKE);
        innerRingPaint.setStrokeWidth(dp(0.8f));
        innerRingPaint.setColor(0xFFF2E2D4);

        textPaint.setColor(getResources().getColor(R.color.ink));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(kanaTypeface);
        textPaint.setDither(true);
    }

    public void setKana(String value) {
        displayText = value == null ? "" : value;
        latin = false;
        updateContentDescription();
        invalidate();
    }

    public void setRomaji(String value) {
        displayText = value == null ? "" : value;
        latin = true;
        updateContentDescription();
        invalidate();
    }

    private void updateContentDescription() {
        setContentDescription((latin ? "当前罗马音：" : "当前假名：") + displayText);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 1f || height <= 1f) {
            return;
        }

        float diameter = Math.min(width * 0.84f, height * 0.94f);
        diameter = Math.min(diameter, dp(236f));
        if (diameter < dp(60f)) {
            return;
        }
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = diameter / 2f;

        canvas.drawCircle(cx, cy, radius, discPaint);
        canvas.drawCircle(cx, cy, radius, ringPaint);
        canvas.drawCircle(cx, cy, radius * 0.86f, innerRingPaint);

        textPaint.setTypeface(latin ? latinTypeface : kanaTypeface);
        if (latin) {
            float textSize = diameter * 0.52f;
            textPaint.setTextSize(textSize);
            float textWidth = textPaint.measureText(displayText);
            float maxWidth = diameter * 0.86f;
            if (textWidth > maxWidth && textWidth > 0f) {
                textPaint.setTextSize(textSize * maxWidth / textWidth);
            }
        } else {
            textPaint.setTextSize(diameter * 0.60f);
        }
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = cy - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(displayText, cx, baseline, textPaint);
    }

    private float dp(float value) {
        return value * density;
    }
}
