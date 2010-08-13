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

import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;

public class MainWindowTheme {
    private float density;
    public Theme theme;

    public MainWindowTheme(String themeName, DisplayMetrics metrics) {
        density = metrics.density;

        if (themeName.equals("battery01")) {
            theme = new Battery01Theme();
        } else if (themeNzame.equals("full-dark")) {
            theme = new FullDarkTheme();
        } else {
            theme = new DefaultTheme();
        }
    }

    public abstract class Theme {
        public LayoutParams mainFrameLayoutParams;
        public LayoutParams mainContentLayoutParams;

        public int mainContentPaddingLeft;
        public int mainContentPaddingTop;
        public int mainContentPaddingRight;
        public int mainContentPaddingBottom;
        public float titleTextSize;
        public float smallTextSize;
    }

    private class DefaultTheme extends Theme {
        public DefaultTheme() {
            mainFrameLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            mainContentLayoutParams = new LayoutParams((int) (180*density), LayoutParams.WRAP_CONTENT);

            mainContentPaddingLeft = (int) (7 * density);
            mainContentPaddingTop = (int) (2 * density);
            mainContentPaddingRight = (int) (7 * density);
            mainContentPaddingBottom = (int) (7 * density);
            titleTextSize = 11 * density;
            smallTextSize = 8 * density;
        }
    }

    private class Battery01Theme extends DefaultTheme {
        public Battery01Theme() {
            mainContentPaddingTop = (int) (10 * density);
        }
    }

    private class FullDarkTheme extends Theme {
        public FullDarkTheme() {
            mainFrameLayoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            mainContentLayoutParams = new LayoutParams((int) (280 * density), LayoutParams.WRAP_CONTENT);

            mainContentPaddingLeft = (int) (7 * density);
            mainContentPaddingTop = (int) (5 * density);
            mainContentPaddingRight = (int) (5 * density);
            mainContentPaddingBottom = (int) (5 * density);
            titleTextSize = 18 * density;
            smallTextSize = 10 * density;
        }
    }
}
