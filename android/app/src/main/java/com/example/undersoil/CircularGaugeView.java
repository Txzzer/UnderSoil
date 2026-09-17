package com.example.undersoil;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public class CircularGaugeView extends View {

    private Paint trackBgPaint;
    private Paint trackZonePaint;
    private Paint progressPaint;
    private Paint valueTextPaint;
    private Paint unitTextPaint;
    private Paint titleTextPaint;

    private final RectF arcBounds = new RectF();

    private float minValue = 0f;
    private float maxValue = 100f;
    private float animatedValue = 0f;

    private String title = "";
    private String unit = "";

    // Thresholds: Values below greenMax are Green, up to orangeMax are Orange, above are Red
    private float greenMax = 30f;
    private float orangeMax = 70f;

    private final int greenColor = Color.parseColor("#4CAF50");  // Green
    private final int orangeColor = Color.parseColor("#FF9800"); // Orange
    private final int redColor = Color.parseColor("#F44336");    // Red

    private final int faintGreen = Color.parseColor("#C8E6C9");
    private final int faintOrange = Color.parseColor("#FFE0B2");
    private final int faintRed = Color.parseColor("#FFCDD2");
    private final int trackColor = Color.parseColor("#ECEFF1");

    private ValueAnimator animator;

    // Angle configuration: Start at 135 degrees (bottom left), sweep 270 degrees to 405 (bottom right)
    private static final float START_ANGLE = 135f;
    private static final float SWEEP_ANGLE = 270f;

    public CircularGaugeView(Context context) {
        super(context);
        init();
    }

    public CircularGaugeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trackBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackBgPaint.setStyle(Paint.Style.STROKE);
        trackBgPaint.setStrokeCap(Paint.Cap.ROUND);
        trackBgPaint.setColor(trackColor);

        trackZonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackZonePaint.setStyle(Paint.Style.STROKE);
        trackZonePaint.setStrokeCap(Paint.Cap.BUTT);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(greenColor);

        valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valueTextPaint.setColor(Color.parseColor("#212121"));
        valueTextPaint.setFakeBoldText(true);

        unitTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        unitTextPaint.setColor(Color.parseColor("#757575"));
        unitTextPaint.setFakeBoldText(true);

        titleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titleTextPaint.setTextAlign(Paint.Align.CENTER);
        titleTextPaint.setColor(Color.parseColor("#757575"));
        titleTextPaint.setFakeBoldText(true);
    }

    public void setRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        invalidate();
    }

    public void setThresholds(float greenMax, float orangeMax) {
        this.greenMax = greenMax;
        this.orangeMax = orangeMax;
        invalidate();
    }

    public void setTitle(String title) {
        this.title = title;
        invalidate();
    }

    public void setUnit(String unit) {
        this.unit = unit;
        invalidate();
    }

    public void setValue(float newValue, boolean animate) {
        float clampedTarget = Math.max(minValue, Math.min(maxValue, newValue));

        if (animate) {
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            animator = ValueAnimator.ofFloat(animatedValue, clampedTarget);
            animator.setDuration(800);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                animatedValue = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            animatedValue = clampedTarget;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int dimension = Math.min(width, height);
        if (dimension == 0) {
            dimension = 300;
        }
        setMeasuredDimension(dimension, dimension);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float strokeWidth = width * 0.09f;

        trackBgPaint.setStrokeWidth(strokeWidth);
        trackZonePaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeWidth(strokeWidth);

        float padding = strokeWidth / 2f + 12f;
        arcBounds.set(padding, padding, width - padding, height - padding);

        // 1. Draw base track background
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, trackBgPaint);

        // 2. Draw colored zones on track (faint Green, Orange, Red)
        float range = maxValue - minValue;
        if (range > 0) {
            float gFrac = Math.max(0f, Math.min(1f, (greenMax - minValue) / range));
            float oFrac = Math.max(0f, Math.min(1f, (orangeMax - minValue) / range));

            float gSweep = SWEEP_ANGLE * gFrac;
            float oSweep = SWEEP_ANGLE * (oFrac - gFrac);
            float rSweep = SWEEP_ANGLE * (1f - oFrac);

            if (gSweep > 0) {
                trackZonePaint.setColor(faintGreen);
                canvas.drawArc(arcBounds, START_ANGLE, gSweep, false, trackZonePaint);
            }
            if (oSweep > 0) {
                trackZonePaint.setColor(faintOrange);
                canvas.drawArc(arcBounds, START_ANGLE + gSweep, oSweep, false, trackZonePaint);
            }
            if (rSweep > 0) {
                trackZonePaint.setColor(faintRed);
                canvas.drawArc(arcBounds, START_ANGLE + gSweep + oSweep, rSweep, false, trackZonePaint);
            }
        }

        // 3. Determine active progress color based on current animated value
        int activeColor;
        if (animatedValue <= greenMax) {
            activeColor = greenColor;
        } else if (animatedValue <= orangeMax) {
            activeColor = orangeColor;
        } else {
            activeColor = redColor;
        }
        progressPaint.setColor(activeColor);

        // 4. Draw active progress arc
        float fraction = (animatedValue - minValue) / (maxValue - minValue);
        fraction = Math.max(0f, Math.min(1f, fraction));
        float currentSweep = SWEEP_ANGLE * fraction;

        if (currentSweep > 0) {
            canvas.drawArc(arcBounds, START_ANGLE, currentSweep, false, progressPaint);
        }

        // 5. Draw center text (Value, Unit, Title)
        float cx = width / 2f;
        float cy = height / 2f;

        float valueTextSize = width * 0.18f;
        valueTextPaint.setTextSize(valueTextSize);

        String valueStr = String.format(Locale.US, "%.1f", animatedValue);
        float valueWidth = valueTextPaint.measureText(valueStr);

        unitTextPaint.setTextSize(width * 0.08f);
        float unitWidth = unit.isEmpty() ? 0 : unitTextPaint.measureText(unit);

        float spacing = 6f;
        float totalWidth = valueWidth + (unit.isEmpty() ? 0 : unitWidth + spacing);
        float startX = cx - (totalWidth / 2f);

        // Draw Value
        valueTextPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(valueStr, startX, cy + (valueTextSize * 0.22f), valueTextPaint);

        // Draw Unit
        if (!unit.isEmpty()) {
            canvas.drawText(unit, startX + valueWidth + spacing, cy + (valueTextSize * 0.22f), unitTextPaint);
        }

        // Draw Title Label
        if (!title.isEmpty()) {
            titleTextPaint.setTextSize(width * 0.075f);
            canvas.drawText(title.toUpperCase(Locale.US), cx, cy + (valueTextSize * 0.8f), titleTextPaint);
        }
    }
}
