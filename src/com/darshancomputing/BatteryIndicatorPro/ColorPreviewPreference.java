/*
    Copyright (c) 2010-2013 Darshan-Josiah Barber

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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ColorPreviewPreference extends Preference {
    public int redThresh;
    public int amberThresh;
    public int greenThresh;

    public ColorPreviewPreference(Context context) {
        super(context);
    }

    public ColorPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPreviewPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateView(ViewGroup parent){
        return super.onCreateView(parent);
    }

    @Override
    protected void onBindView (View view) {
        super.onBindView(view);

        ImageView iv = (ImageView) view.findViewById(R.id.color_preview_bar_v);
        if (iv == null) return;
        LayerDrawable ld = (LayerDrawable) iv.getDrawable();

        for (int i = 0; i < 20; i++) {
            if      (redThresh    > i*5) ld.getDrawable(i).setLevel(1);
            else if (amberThresh  > i*5) ld.getDrawable(i).setLevel(2);
            else if (greenThresh <= i*5) ld.getDrawable(i).setLevel(3);
        }
    }
}
