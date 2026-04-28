package com.monitor.health.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class HeartRateChartView extends View {

    private Paint linePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Path heartRatePath;
    private Path fillPath;

    private float[] heartRateData = {
            60, 65, 70, 68, 72, 75, 80, 78, 82, 85, 88, 90, 92, 88, 85, 80, 75, 70, 68, 65, 62, 60, 58, 55
    };

    private float chartWidth;
    private float chartHeight;
    private float padding = 40f;

    public HeartRateChartView(Context context) {
        super(context);
        init();
    }

    public HeartRateChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Heart rate line paint
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#FF6B6B")); // Red color
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        // Fill area paint
        fillPaint = new Paint();
        fillPaint.setColor(Color.parseColor("#66FF6B6B")); // Semi-transparent red
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        // Grid lines paint
        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#333333"));
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        heartRatePath = new Path();
        fillPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        chartWidth = w - (padding * 2);
        chartHeight = h - (padding * 2);
        generateHeartRatePath();
    }

    private void generateHeartRatePath() {
        heartRatePath.reset();
        fillPath.reset();

        if (heartRateData.length == 0) return;

        // Calculate scaling factors
        float maxBpm = 200f;
        float minBpm = 0f;
        float bpmRange = maxBpm - minBpm;

        float xStep = chartWidth / (heartRateData.length - 1);

        // Start path
        float startX = padding;
        float startY = padding + chartHeight - ((heartRateData[0] - minBpm) / bpmRange * chartHeight);

        heartRatePath.moveTo(startX, startY);
        fillPath.moveTo(startX, padding + chartHeight); // Bottom of chart
        fillPath.lineTo(startX, startY);

        // Create smooth curve through data points
        for (int i = 0; i < heartRateData.length; i++) {
            float x = padding + (i * xStep);
            float y = padding + chartHeight - ((heartRateData[i] - minBpm) / bpmRange * chartHeight);

            if (i == 0) {
                heartRatePath.moveTo(x, y);
                fillPath.lineTo(x, y);
            } else {
                // Add some randomness to make it look more like a real heart rate
                float randomOffset = (float) (Math.sin(i * 0.5) * 5 + Math.cos(i * 0.3) * 3);
                y += randomOffset;

                // Use quadratic curves for smoother lines
                float prevX = padding + ((i - 1) * xStep);
                float prevY = padding + chartHeight - ((heartRateData[i - 1] - minBpm) / bpmRange * chartHeight);

                float midX = (prevX + x) / 2;
                heartRatePath.quadTo(prevX, prevY, midX, (prevY + y) / 2);
                heartRatePath.quadTo(midX, (prevY + y) / 2, x, y);

                fillPath.lineTo(x, y);
            }
        }

        // Complete the fill path
        float lastX = padding + ((heartRateData.length - 1) * xStep);
        fillPath.lineTo(lastX, padding + chartHeight); // Bottom right
        fillPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw grid lines
        drawGrid(canvas);

        // Draw filled area first
        canvas.drawPath(fillPath, fillPaint);

        // Draw heart rate line on top
        canvas.drawPath(heartRatePath, linePaint);

        // Draw animated pulse effect
        drawPulseEffect(canvas);
    }

    private void drawGrid(Canvas canvas) {
        // Horizontal grid lines (BPM levels)
        int[] bpmLevels = {50, 100, 150, 200};
        float maxBpm = 200f;
        float minBpm = 0f;
        float bpmRange = maxBpm - minBpm;

        for (int bpm : bpmLevels) {
            float y = padding + chartHeight - ((bpm - minBpm) / bpmRange * chartHeight);
            canvas.drawLine(padding, y, padding + chartWidth, y, gridPaint);
        }

        // Vertical grid lines (time)
        float timeStep = chartWidth / 4; // 5 time markers (00:00, 06:00, 12:00, 18:00, 24:00)
        for (int i = 0; i <= 4; i++) {
            float x = padding + (i * timeStep);
            canvas.drawLine(x, padding, x, padding + chartHeight, gridPaint);
        }
    }

    private void drawPulseEffect(Canvas canvas) {
        // Add a subtle pulse animation effect
        long currentTime = System.currentTimeMillis();
        float pulseAlpha = (float) Math.abs(Math.sin(currentTime * 0.003)) * 0.3f + 0.7f;

        Paint pulsePaint = new Paint(linePaint);
        pulsePaint.setAlpha((int) (255 * pulseAlpha));
        canvas.drawPath(heartRatePath, pulsePaint);

        // Invalidate to keep animation running
        invalidate();
    }

    // Method to update heart rate data
    public void updateHeartRateData(float[] newData) {
        this.heartRateData = newData;
        generateHeartRatePath();
        invalidate();
    }

    // Method to add new heart rate reading
    public void addHeartRateReading(float bpm) {
        // Shift array left and add new value
        float[] newData = new float[heartRateData.length];
        System.arraycopy(heartRateData, 1, newData, 0, heartRateData.length - 1);
        newData[newData.length - 1] = bpm;
        updateHeartRateData(newData);
    }
}