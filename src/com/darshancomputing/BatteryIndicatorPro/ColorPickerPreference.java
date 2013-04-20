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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ColorPickerPreference extends Preference implements Preference.OnPreferenceClickListener,
                                                                 ColorPickerDialog.OnColorChangedListener {
    View mView;
    ColorPickerDialog mDialog;
    private int mValue = Color.BLACK;
    private float mDensity = 0;

    public ColorPickerPreference(Context context) {
        super(context);
        init(context, null);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getColor(index, Color.BLACK);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        onColorChanged(restoreValue ? getPersistedInt(mValue) : (Integer) defaultValue);
    }

    private void init(Context context, AttributeSet attrs) {
        mDensity = getContext().getResources().getDisplayMetrics().density;
        setOnPreferenceClickListener(this);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mView = view;
        setPreviewColor();
    }

    private void setPreviewColor() {
        if (! isEnabled()) return;
        if (mView == null) return;
        ImageView iView = new ImageView(getContext());
        LinearLayout widgetFrameView = ((LinearLayout) mView.findViewById(android.R.id.widget_frame));
        if (widgetFrameView == null) return;
        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.setPadding(widgetFrameView.getPaddingLeft(),
                                   widgetFrameView.getPaddingTop(),
                                   (int) (mDensity * 8),
                                   widgetFrameView.getPaddingBottom());
        // remove already created preview image
        int count = widgetFrameView.getChildCount();
        if (count > 0)
            widgetFrameView.removeViews(0, count);

        widgetFrameView.addView(iView);
        widgetFrameView.setMinimumWidth(0);
        iView.setImageBitmap(getPreviewBitmap());
    }

    private void setPreviewVisible(boolean visible) {
        if (mView == null) return;
        LinearLayout widgetFrameView = ((LinearLayout) mView.findViewById(android.R.id.widget_frame));
        if (widgetFrameView == null) return;
        widgetFrameView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private Bitmap getPreviewBitmap() {
        int d = (int) (mDensity * 31); //30dip
        int color = mValue;
        Bitmap bm = Bitmap.createBitmap(d, d, Config.ARGB_8888);
        int w = bm.getWidth();
        int h = bm.getHeight();
        int c = color;
        for (int i = 0; i < w; i++) {
            for (int j = i; j < h; j++) {
                c = (i <= 1 || j <= 1 || i >= w-2 || j >= h-2) ? Color.GRAY : color;
                bm.setPixel(i, j, c);
                if (i != j) {
                    bm.setPixel(j, i, c);
                }
            }
        }

        return bm;
    }

    @Override
    public void onColorChanged(int color) {
        if (isPersistent()) {
            persistInt(color);
        }
        mValue = color;
        setPreviewColor();
        try {
            getOnPreferenceChangeListener().onPreferenceChange(this, color);
        } catch (NullPointerException e) {

        }
    }

    public boolean onPreferenceClick(Preference preference) {
        showDialog(null);
        return false;
    }

    protected void showDialog(Bundle state) {
        mDialog = new ColorPickerDialog(getContext(), mValue);
        mDialog.setOnColorChangedListener(this);
        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.show();
    }

    @Override
    public void setEnabled(boolean enabled) {
        setPreviewVisible(enabled);
        super.setEnabled(enabled);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.dialogBundle = mDialog.onSaveInstanceState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof SavedState)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        showDialog(myState.dialogBundle);
    }

    private static class SavedState extends BaseSavedState {
        Bundle dialogBundle;

        public SavedState(Parcel source) {
            super(source);
            dialogBundle = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeBundle(dialogBundle);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
            new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
