/*
    Copyright (c) 2013-2017 Darshan Computing, LLC

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


    private final int WIDTH = 240;
    private final int BODY_HEIGHT = 420;
    private final int TOP_WIDTH = 100;
    private final int TOP_HEIGHT = 30;
    private final int TOTAL_HEIGHT = BODY_HEIGHT + TOP_HEIGHT;
    private final float STROKE_WIDTH = 20f;

    private static BatteryLevel[] instances = new BatteryLevel[]{null, null, null, null, null}; // So that [1], [2], and [4] exist

    // Get rid of inSampleSize, I think!
    public static BatteryLevel getInstance(Context context, int inSampleSize) {
        if (inSampleSize < 0 || inSampleSize >= instances.length)
            return null;

        if (instances[inSampleSize] == null)
            instances[inSampleSize] = new BatteryLevel(context, inSampleSize);

        return instances[inSampleSize];
    }

    public static BatteryLevel getInstance(Context context) {
        return getInstance(context, SIZE_LARGE);
    }

    private BatteryLevel(Context context, int inSampleSize) {
        Resources res = context.getResources();

        canvas = new Canvas();

        battery = Bitmap.createBitmap(WIDTH, TOTAL_HEIGHT, Bitmap.Config.ARGB_8888);
        battery.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        canvas.setBitmap(battery);

        paint = new Paint();
        mColor = 0xff586fb8;
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setDither(true);
        paint.setStrokeWidth(STROKE_WIDTH);
    }

    public void setColor(int color) {
        mColor = color;
        setLevel(mLevel);
    }

    public void setLevel(int level) {
        if (level < 0) level = 0; // I suspect we might get called with -1 in certain circumstances

        mLevel = level;

        int sw = (int) STROKE_WIDTH;
        RectF outline_rect = new RectF(sw/2, TOP_HEIGHT + sw/2, WIDTH-sw/2, TOTAL_HEIGHT-sw/2);
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

        paint.setColor(mColor);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(outline_rect, sw, sw, paint);

        int top = TOP_HEIGHT + sw/2 + (BODY_HEIGHT * (100-level) / 100);
        RectF fill_rect = new RectF(sw/2, top, WIDTH-sw/2, TOTAL_HEIGHT-sw/2);
        // Draw line across top to level out rounding?

        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(fill_rect, sw, sw, paint);

        int top_left = (WIDTH - TOP_WIDTH) / 2;
        RectF top_rect = new RectF(top_left, 0, top_left+TOP_WIDTH, TOP_HEIGHT+sw);
        canvas.drawRoundRect(top_rect, sw, sw, paint);
    }

    Bitmap getBitmap() {
        return battery;
    }
}
