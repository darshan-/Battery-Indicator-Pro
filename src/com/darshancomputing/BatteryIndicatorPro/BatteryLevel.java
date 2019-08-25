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
    private int width, top_h, body_h;
    private Canvas canvas;
    private Paint fill_paint, bitmap_paint;
    private static Bitmap battery_top, battery_body, battery;

    static final int SIZE_LARGE = 1;
    static final int SIZE_NOTIFICATION = 4;

    private static BatteryLevel[] instances = new BatteryLevel[]{null, null, null, null, null}; // So that [1], [2], and [4] exist

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

        //BitmapFactory bf = new BitmapFactory();
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        bfo.inScaled = false;
        bfo.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
        bfo.inSampleSize = inSampleSize;

        battery_top    = BitmapFactory.decodeResource(res, R.drawable.empty_battery_top   , bfo);
        battery_body   = BitmapFactory.decodeResource(res, R.drawable.empty_battery_body  , bfo);

           width = battery_top.getWidth();
           top_h = battery_top.getHeight();
          body_h = battery_body.getHeight();

        canvas = new Canvas();

        battery = Bitmap.createBitmap(width, top_h + body_h, Bitmap.Config.ARGB_8888);
        battery.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        canvas.setBitmap(battery);

        fill_paint = new Paint();
        fill_paint.setColor(0xaa33b5e5);
        fill_paint.setAntiAlias(true);
        fill_paint.setStrokeCap(Paint.Cap.ROUND);
        fill_paint.setStrokeJoin(Paint.Join.ROUND);
        fill_paint.setStyle(Paint.Style.FILL);
        fill_paint.setDither(true);

        bitmap_paint = new Paint();
        bitmap_paint.setAntiAlias(true);
        bitmap_paint.setDither(true);
    }

    public void setLevel(int level) {
        if (level < 0) level = 0; // I suspect we might get called with -1 in certain circumstances

        int rect_top = top_h + (body_h * (100 - level) / 100);
        int rect_bottom = top_h + body_h;

        RectF body_rect = new RectF(0, rect_top, width, rect_bottom);

        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

        canvas.drawRoundRect(body_rect, 7.5f, 7.5f, fill_paint);

        canvas.drawBitmap(battery_top   , 0, 0             , bitmap_paint);
        canvas.drawBitmap(battery_body  , 0, top_h         , bitmap_paint);
    }

    Bitmap getBitmap() {
        return battery;
    }
}
