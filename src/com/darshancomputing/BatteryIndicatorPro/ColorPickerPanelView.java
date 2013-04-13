/*
  Modified from original source which was:
  * Copyright (C) 2010 Daniel Nilsson
  * Licensed under the Apache License, Version 2.0
  * At http://github.com/attenzione/android-ColorPickerPreference

  This file, or at least the changes from the original are

  Copyright (c) 2013 Darshan-Josiah Barber


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

public class ColorPickerPanelView extends View {
    private final static float BORDER_WIDTH_PX = 1;
    private float mDensity = 1f;
    private int   mBorderColor = 0xff6E6E6E;
    private int   mColor = 0xff000000;
    private Paint mBorderPaint;
    private Paint mColorPaint;
    private RectF mDrawingRect;
    private RectF mColorRect;

    public ColorPickerPanelView(Context context){
        this(context, null);
    }

    public ColorPickerPanelView(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }

    public ColorPickerPanelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        mBorderPaint = new Paint();
        mColorPaint = new Paint();
        mDensity = getContext().getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final RectF rect = mColorRect;

        if(BORDER_WIDTH_PX > 0){
            mBorderPaint.setColor(mBorderColor);
            canvas.drawRect(mDrawingRect, mBorderPaint);
        }

        mColorPaint.setColor(mColor);
        canvas.drawRect(rect, mColorPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mDrawingRect = new RectF();
        mDrawingRect.left =  getPaddingLeft();
        mDrawingRect.right  = w - getPaddingRight();
        mDrawingRect.top = getPaddingTop();
        mDrawingRect.bottom = h - getPaddingBottom();

        setUpColorRect();
    }

    private void setUpColorRect(){
        final RectF dRect = mDrawingRect;

        float left = dRect.left + BORDER_WIDTH_PX;
        float top = dRect.top + BORDER_WIDTH_PX;
        float bottom = dRect.bottom - BORDER_WIDTH_PX;
        float right = dRect.right - BORDER_WIDTH_PX;

        mColorRect = new RectF(left,top, right, bottom);
    }

    public void setColor(int color){
        mColor = color;
        invalidate();
    }

    public int getColor(){
        return mColor;
    }

    public void setBorderColor(int color){
        mBorderColor = color;
        invalidate();
    }

    public int getBorderColor(){
        return mBorderColor;
    }
}
