package com.monitor.health.ui.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class GaugeView extends View {
    private Paint needlePaint;
    private Paint arcPaint;
    private RectF arcRect;
    private float currentValue = 0;
    private float maxValue = 100;
    private String arcColor = "#EF5350";  // Default red color

    public GaugeView(Context context) {
        super(context);
        init();
    }

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setColor(Color.WHITE);
        needlePaint.setStrokeWidth(4f);
        needlePaint.setStyle(Paint.Style.STROKE);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(30f);

        arcRect = new RectF();
    }

    public void setValue(float value, float max) {
        this.currentValue = value;
        this.maxValue = max;
        invalidate();
    }

    public void setArcColor(String color) {
        this.arcColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        int padding = 40;

        arcRect.set(padding, padding, size - padding, size - padding);

        arcPaint.setColor(Color.parseColor(arcColor));
        canvas.drawArc(arcRect, 180, 180, false, arcPaint);

        float centerX = size / 2f;
        float centerY = size / 2f;
        float angle = 180 + (currentValue / maxValue) * 180;
        float needleLength = (size / 2f) - padding - 20;

        double radians = Math.toRadians(angle);
        float endX = (float) (centerX + needleLength * Math.cos(radians));
        float endY = (float) (centerY + needleLength * Math.sin(radians));

        canvas.drawLine(centerX, centerY, endX, endY, needlePaint);
    }
}
