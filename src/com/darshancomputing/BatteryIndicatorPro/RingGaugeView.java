/*
    Copyright (c) 2026 Darshan Computing, LLC

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

package com.darshancomputing.BatteryIndicatorPro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

// Circular battery level gauge: a full track ring with an arc for the current level, starting at 12 o'clock
public class RingGaugeView extends View {
    private static final float STROKE_FRACTION = 0.055f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private int level;

    public RingGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        trackPaint.setStyle(Paint.Style.STROKE);
        levelPaint.setStyle(Paint.Style.STROKE);
        levelPaint.setStrokeCap(Paint.Cap.ROUND);

        trackPaint.setColor(context.getResources().getColor(R.color.overview_track));
        levelPaint.setColor(context.getResources().getColor(R.color.overview_green));
    }

    void setLevel(int percent) {
        level = Math.max(0, Math.min(100, percent));
        invalidate();
    }

    void setLevelColor(int color) {
        levelPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Always square, sized by the width we're given
        int w = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(w, w);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float stroke = size * STROKE_FRACTION;
        float inset = stroke / 2;

        trackPaint.setStrokeWidth(stroke);
        levelPaint.setStrokeWidth(stroke);
        oval.set(inset, inset, size - inset, size - inset);

        canvas.drawArc(oval, 0, 360, false, trackPaint);
        if (level > 0)
            canvas.drawArc(oval, -90, 360f * level / 100, false, levelPaint);
    }
}
