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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

// Battery level over a fixed time window (24 hours), with plugged-in periods shaded
public class LevelChartView extends View {
    static final long WINDOW_MS = 24L * 60 * 60 * 1000;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chargePaint = new Paint();
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float density;

    private List<long[]> points; // {time, percent, status}, oldest first
    private long now;

    public LevelChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources res = context.getResources();
        density = res.getDisplayMetrics().density;

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2 * density);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(res.getColor(R.color.accent));

        gridPaint.setStrokeWidth(1 * density);
        gridPaint.setColor(res.getColor(R.color.ring_track));

        chargePaint.setColor(res.getColor(R.color.log_charged));
        chargePaint.setAlpha(40);

        labelPaint.setTextSize(10 * res.getDisplayMetrics().scaledDensity);
        labelPaint.setColor(res.getColor(R.color.secondary_text));
    }

    // `points` comes from LogDatabase.getLevelHistory(); the current reading extends the line to "now"
    void setData(List<long[]> points, long now, int percent, int status) {
        this.points = new java.util.ArrayList<long[]>(points);
        this.points.add(new long[]{now, percent, status});
        this.now = now;
        invalidate();
    }

    // Less than an hour of history draws as a sliver at the right edge, which reads as broken
    private static final long MIN_HISTORY_MS = 60L * 60 * 1000;

    boolean hasEnoughData() {
        return points != null && points.size() >= 2 && now - points.get(0)[0] >= MIN_HISTORY_MS;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float labelW = labelPaint.measureText("100%") + 6 * density;
        float left = labelW, top = 6 * density;
        float right = getWidth(), bottom = getHeight() - 6 * density;
        float w = right - left, h = bottom - top;

        // Grid at 0, 50 and 100%, labeled on the left
        for (int pct = 0; pct <= 100; pct += 50) {
            float y = bottom - h * pct / 100;
            canvas.drawLine(left, y, right, y, gridPaint);
            canvas.drawText(pct + "%", 0, y + labelPaint.getTextSize() / 3, labelPaint);
        }

        if (!hasEnoughData()) return;

        long start = now - WINDOW_MS;
        path.reset();
        boolean first = true;

        for (int i = 0; i < points.size(); i++) {
            long[] p = points.get(i);
            float x = left + w * Math.max(0, p[0] - start) / WINDOW_MS;
            float y = bottom - h * p[1] / 100;

            // Shade from this point to the next while plugged in
            if (p[2] != BatteryInfo.STATUS_UNPLUGGED && i + 1 < points.size()) {
                float nextX = left + w * Math.max(0, points.get(i + 1)[0] - start) / WINDOW_MS;
                canvas.drawRect(x, top, nextX, bottom, chargePaint);
            }

            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, linePaint);
    }
}
