/*
    Copyright (c) 2013-2020 Darshan Computing, LLC

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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;

class BatteryLevel {
    private Canvas canvas;
    private Paint paint;
    private static Bitmap battery;
    private int mLevel, mColor;

    static final int SIZE_LARGE = 1;
    static final int SIZE_NOTIFICATION = 4;


    private static final float FACTOR_LARGE = 33.25f;
    private static final float FACTOR_SMALL = 13.33f;

    private final float factor_width = 12.0f;
    private final float factor_bod_h = 21.0f;
    private final float factor_top_w = 5.0f;
    private final float factor_top_h = 1.5f;
    private final float factor_stroke = 1.0f;

    private final int width;
    private final int bod_h;
    private final int top_w;
    private final int top_h;
    private final int total_h;
    private final int stroke_w;

    private static BatteryLevel largeInstance; // For main activity
    private static BatteryLevel smallInstance; // For widgets and notifications

    public static BatteryLevel getLargeInstance(Context context) {
        if (largeInstance != null)
            return largeInstance;

        largeInstance = new BatteryLevel(context, FACTOR_LARGE);

        return largeInstance;
    }

    public static BatteryLevel getSmallInstance(Context context) {
        if (smallInstance != null)
            return smallInstance;

        smallInstance = new BatteryLevel(context, FACTOR_SMALL);

        return smallInstance;
    }

    private BatteryLevel(Context context, float size_factor) {
        Resources res = context.getResources();

        width = (int) (size_factor * factor_width);
        bod_h = (int) (size_factor * factor_bod_h);
        top_w = (int) (size_factor * factor_top_w);
        top_h = (int) (size_factor * factor_top_h);
        total_h = (int) (bod_h + top_h);
        stroke_w = (int) (size_factor * factor_stroke);

        canvas = new Canvas();

        battery = Bitmap.createBitmap(width, total_h, Bitmap.Config.ARGB_8888);
        battery.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        canvas.setBitmap(battery);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setDither(true);
        paint.setStrokeWidth(stroke_w);
    }

    public void setColor(int color) {
        mColor = color;
        setLevel(mLevel);
    }

    public void setLevel(int level) {
        if (level < 0) level = 0; // I suspect we might get called with -1 in certain circumstances

        mLevel = level;

        float hsw = stroke_w*0.5f;
        int isw = (int) stroke_w;
        float radius = hsw;

        RectF outline_rect = new RectF(hsw, top_h+hsw, width-hsw, total_h-hsw);
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

        paint.setColor(mColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke_w);
        canvas.drawRoundRect(outline_rect, radius, radius, paint);

        int top = top_h + isw + ((bod_h-2*isw) * (100-level) / 100);
        RectF fill_rect = new RectF(stroke_w, top, width-stroke_w, total_h-stroke_w);

        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(fill_rect, 0, 0, paint);

        int top_left = (width - top_w) / 2;
        RectF top_rect = new RectF(top_left, 0, top_left+top_w, top_h+stroke_w);
        canvas.drawRoundRect(top_rect, radius, radius, paint);
    }

    Bitmap getBitmap() {
        return battery;
    }
}
