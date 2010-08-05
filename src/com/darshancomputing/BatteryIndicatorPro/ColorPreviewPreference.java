/*
    Copyright (c) 2010 Josiah Barber (aka Darshan)

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
    private int redThresh;
    private int amberThresh;
    private int greenThresh;

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
        setLayoutResource(R.layout.color_preview_pref);
        return super.onCreateView(parent);
    }

    @Override
    protected void onBindView (View view) {
        super.onBindView(view);

        ImageView iv = (ImageView) view.findViewById(R.id.color_preview_bar_v);

        LayerDrawable ld = (LayerDrawable) iv.getDrawable();
        /* ld.getDrawable(int index) is indexed from 0-19 */

        if (greenThresh <= 20) ld.getDrawable( 4).setLevel(3);
        if (greenThresh <= 25) ld.getDrawable( 5).setLevel(3);
        if (greenThresh <= 30) ld.getDrawable( 6).setLevel(3);
        if (greenThresh <= 35) ld.getDrawable( 7).setLevel(3);
        if (greenThresh <= 40) ld.getDrawable( 8).setLevel(3);
        if (greenThresh <= 45) ld.getDrawable( 9).setLevel(3);
        if (greenThresh <= 50) ld.getDrawable(10).setLevel(3);
        if (greenThresh <= 55) ld.getDrawable(11).setLevel(3);
        if (greenThresh <= 60) ld.getDrawable(12).setLevel(3);
        if (greenThresh <= 65) ld.getDrawable(13).setLevel(3);
        if (greenThresh <= 70) ld.getDrawable(14).setLevel(3);
        if (greenThresh <= 75) ld.getDrawable(15).setLevel(3);
        if (greenThresh <= 80) ld.getDrawable(16).setLevel(3);
        if (greenThresh <= 85) ld.getDrawable(17).setLevel(3);
        if (greenThresh <= 90) ld.getDrawable(18).setLevel(3);
        if (greenThresh <= 95) ld.getDrawable(19).setLevel(3);
        if (amberThresh >   0) ld.getDrawable( 0).setLevel(2);
        if (amberThresh >   5) ld.getDrawable( 1).setLevel(2);
        if (amberThresh >  10) ld.getDrawable( 2).setLevel(2);
        if (amberThresh >  15) ld.getDrawable( 3).setLevel(2);
        if (amberThresh >  20) ld.getDrawable( 4).setLevel(2);
        if (amberThresh >  25) ld.getDrawable( 5).setLevel(2);
        if (amberThresh >  30) ld.getDrawable( 6).setLevel(2);
        if (amberThresh >  35) ld.getDrawable( 7).setLevel(2);
        if (amberThresh >  40) ld.getDrawable( 8).setLevel(2);
        if (amberThresh >  45) ld.getDrawable( 9).setLevel(2);
        if (redThresh   >   0) ld.getDrawable( 0).setLevel(1);
        if (redThresh   >   5) ld.getDrawable( 1).setLevel(1);
        if (redThresh   >  10) ld.getDrawable( 2).setLevel(1);
        if (redThresh   >  15) ld.getDrawable( 3).setLevel(1);
        if (redThresh   >  20) ld.getDrawable( 4).setLevel(1);
        if (redThresh   >  25) ld.getDrawable( 5).setLevel(1);
    }

    public void updateView(int red, int amber, int green) {
        redThresh = red;
        amberThresh = amber;
        greenThresh = green;
    }

}
