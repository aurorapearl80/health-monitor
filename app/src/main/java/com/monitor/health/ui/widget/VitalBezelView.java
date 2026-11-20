package com.monitor.health.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.monitor.health.R;

/**
 * Decorative backdrop for a watch screen: a soft glow in the screen's accent colour behind the
 * top of the content and, on round displays, a thin luminous arc that hugs the bezel.
 *
 * It is purely visual (it does not represent a value), sits behind the scrolling content and
 * never takes touches. The colour defaults to {@code ?attr/vitalAccent}, so a screen picks its
 * colour just by applying a {@code ThemeOverlay.Vital.*} to its layout root.
 */
public class VitalBezelView extends View {

    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final Matrix shaderMatrix = new Matrix();

    @ColorInt private int color;
    private float sweepDegrees = 110f;
    private float strokeWidth;
    private boolean showGlow = true;
    private boolean round;

    public VitalBezelView(Context context) {
        this(context, null);
    }

    public VitalBezelView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VitalBezelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        strokeWidth = dp(3f);
        color = resolveThemeColor(context, R.attr.vitalAccent, Color.WHITE);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VitalBezelView, defStyleAttr, 0);
            try {
                color = a.getColor(R.styleable.VitalBezelView_bezelColor, color);
                sweepDegrees = a.getFloat(R.styleable.VitalBezelView_bezelSweep, sweepDegrees);
                strokeWidth = a.getDimension(R.styleable.VitalBezelView_bezelStrokeWidth, strokeWidth);
                showGlow = a.getBoolean(R.styleable.VitalBezelView_bezelShowGlow, showGlow);
            } finally {
                a.recycle();
            }
        }

        round = context.getResources().getConfiguration().isScreenRound();

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(0x14FFFFFF);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Change the accent at runtime (e.g. to reflect an out-of-range reading). */
    public void setBezelColor(@ColorInt int newColor) {
        if (newColor == color) return;
        color = newColor;
        rebuildShaders(getWidth(), getHeight());
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rebuildShaders(w, h);
    }

    private void rebuildShaders(int w, int h) {
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f;
        float cy = h / 2f;

        // Glow: centred just above the top edge so it reads as light falling into the screen.
        float glowRadius = Math.max(w, h) * 0.72f;
        glowPaint.setShader(new RadialGradient(
                cx, -h * 0.12f, glowRadius,
                new int[]{withAlpha(color, 0x52), withAlpha(color, 0x1C), withAlpha(color, 0x00)},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));

        // Arc: bright in the middle, fading to nothing at both ends.
        float inset = strokeWidth / 2f + dp(3f);
        float radius = Math.min(w, h) / 2f - inset;
        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

        float half = (sweepDegrees / 2f) / 360f;
        SweepGradient sweep = new SweepGradient(cx, cy,
                new int[]{withAlpha(color, 0x00), color, withAlpha(color, 0x00), withAlpha(color, 0x00)},
                new float[]{0f, half, half * 2f, 1f});
        // Rotate so the gradient starts at the left end of the arc (arc is centred on 12 o'clock).
        shaderMatrix.reset();
        shaderMatrix.setRotate(270f - sweepDegrees / 2f, cx, cy);
        sweep.setLocalMatrix(shaderMatrix);
        arcPaint.setShader(sweep);
        arcPaint.setStrokeWidth(strokeWidth);
        trackPaint.setStrokeWidth(dp(1f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (showGlow) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), glowPaint);
        }
        if (round) {
            canvas.drawOval(arcBounds, trackPaint);
            // Stop 1 degree short at each end so the round caps stay inside the faded region.
            canvas.drawArc(arcBounds, 270f - sweepDegrees / 2f + 1f, sweepDegrees - 2f, false, arcPaint);
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    @ColorInt
    private static int withAlpha(@ColorInt int c, int alpha) {
        return (c & 0x00FFFFFF) | (alpha << 24);
    }

    @ColorInt
    private static int resolveThemeColor(Context context, int attr, @ColorInt int fallback) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) {
                return context.getColor(tv.resourceId);
            }
            return tv.data;
        }
        return fallback;
    }
}
