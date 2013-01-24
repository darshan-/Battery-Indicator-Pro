/*
    Copyright (c) 2013 Josiah Barber (aka Darshan)

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
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;

class BatteryLevelView extends ImageView {
    private int level = 100;
    private Bitmap battery;
    private Paint bitmap_paint;

    private int width, top_h, body_h, bottom_h;
    private Bitmap battery_top, battery_body, battery_bottom;

    public BatteryLevelView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources res = context.getResources();

        BitmapFactory bf = new BitmapFactory();
        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        bfo.inScaled = false;
        bfo.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;

        battery_top    = bf.decodeResource(res, R.drawable.empty_battery_top   , bfo);
        battery_body   = bf.decodeResource(res, R.drawable.empty_battery_body  , bfo);
        battery_bottom = bf.decodeResource(res, R.drawable.empty_battery_bottom, bfo);

           width = battery_top.getWidth();
           top_h = battery_top.getHeight();
          body_h = battery_body.getHeight();
        bottom_h = battery_bottom.getHeight();

        Canvas canvas = new Canvas();
        battery = Bitmap.createBitmap(width, top_h + body_h + bottom_h, Bitmap.Config.ARGB_8888);
        battery.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        canvas.setBitmap(battery);

        System.out.println("..........create... canvas: " + canvas.getWidth() + "x" + canvas.getHeight());
        System.out.println("..........create... battery: " + battery.getWidth() + "x" + battery.getHeight());

        Paint fill_paint = new Paint();
        fill_paint.setColor(0xaa33b5e5);
        fill_paint.setAntiAlias(true);
        fill_paint.setStrokeCap(Paint.Cap.ROUND);
        fill_paint.setStrokeJoin(Paint.Join.ROUND);
        fill_paint.setStyle(Paint.Style.FILL);
        fill_paint.setDither(true);

        RectF body_rect = new RectF(0, top_h*3, width, top_h + body_h);

        canvas.drawRoundRect(body_rect, 7.5f, 7.5f, fill_paint);

        bitmap_paint = new Paint();
        bitmap_paint.setAntiAlias(true);
        bitmap_paint.setDither(true);

        System.out.println("..........battery_top: " + battery_top.getWidth() + "x" + battery_top.getHeight());
        System.out.println("..........battery_body: " + battery_body.getWidth() + "x" + battery_body.getHeight());
        System.out.println("..........battery_bottom: " + battery_bottom.getWidth() + "x" + battery_bottom.getHeight());

        System.out.println("..........battery_top.getDensity(): " + battery_top.getDensity());
        System.out.println("..........battery.getDensity(): " + battery.getDensity());

        canvas.drawBitmap(battery_top   , 0, 0             , bitmap_paint);
        canvas.drawBitmap(battery_body  , 0, top_h         , bitmap_paint);
        canvas.drawBitmap(battery_bottom, 0, top_h + body_h, bitmap_paint);

        setImageBitmap(battery);
    }

    public void setLevel(int l) {
        level = l;
    }

    /*@Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        System.out.println("................. canvas: " + canvas.getWidth() + "x" + canvas.getHeight());
        System.out.println("................. battery: " + battery.getWidth() + "x" + battery.getHeight());
        // Draw rounded rect for fullness of battery (solid, height and color determined by charge level)
        //canvas.drawRect(...);
        canvas.drawBitmap(battery, 0, 0, bitmap_paint);
    }*/

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        System.out.println("................. onSizeChanged(" + w + ", " + h + ")");
    }
}
