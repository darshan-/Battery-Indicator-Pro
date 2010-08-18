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

import android.content.res.Resources;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;

public class MainWindowTheme {
    private float density;
    private Resources res;

    public Theme theme;

    public MainWindowTheme(String themeName, DisplayMetrics metrics, Resources r) {
        density = metrics.density;
        res = r;

        if (themeName.equals("colorful")) {
            theme = new ColorfulTheme();
        } else if (themeName.equals("full-dark")) {
            theme = new FullDarkTheme();
        } else {
            theme = new DefaultTheme();
        }
    }

    public abstract class Theme {
        public int mainFrameLayout;

        public int mainLayoutPaddingLeft;
        public int mainLayoutPaddingTop;
        public int mainLayoutPaddingRight;
        public int mainLayoutPaddingBottom;

        public int[] timeRemainingIds = {R.id.time_til_charged, R.id.light_usage, R.id.normal_usage,
                                         R.id.heavy_usage, R.id.constant_usage};
        public int[] timeRemainingStrings = {R.string.fully_charged_in, R.string.light_usage, R.string.normal_usage,
                                             R.string.heavy_usage, R.string.constant_usage};
        public int[] timeRemainingColors = {R.color.time_til_charged, R.color.light_usage, R.color.normal_usage,
                                             R.color.heavy_usage, R.color.constant_usage};
        public String[] timeRemainingKeys = {"", SettingsActivity.KEY_LIGHT_USAGE_TIME, SettingsActivity.KEY_NORMAL_USAGE_TIME,
                                             SettingsActivity.KEY_HEAVY_USAGE_TIME, SettingsActivity.KEY_CONSTANT_USAGE_TIME};
        public int[] timeRemainingDefaults = {0, R.string.default_light_usage_time, R.string.default_normal_usage_time,
                                              R.string.default_heavy_usage_time, R.string.default_constant_usage_time};

        public String timeRemaining(int index, SharedPreferences settings, int percent) {
            String s;
            int t;

            if (percent == -1) return "...";

            if (index == 0) {
                int last_plugged = settings.getInt("last_plugged", 2);
                if (last_plugged == 1) {
                    s = settings.getString(SettingsActivity.KEY_AC_CHARGE_TIME,
                                           res.getString(R.string.default_ac_charge_time));
                } else {
                    s = settings.getString(SettingsActivity.KEY_USB_CHARGE_TIME,
                                           res.getString(R.string.default_usb_charge_time));
                }
                t = 100 - percent;
            } else {
                s = settings.getString(timeRemainingKeys[index],
                                       res.getString(timeRemainingDefaults[index]));
                t = percent;
            }

            return formatTimeRemaining(Math.round(t * Integer.valueOf(s) / (float) 100.0));
        }

        /* Takes time as a whole number of minutes */
        protected String formatTimeRemaining(int t) {
            return  "" + (t / 60) + ":" + String.format("%02d", t % 60) + "h"; /* TODO: Make the h optional? translatable! */
        }

        public boolean timeRemainingVisible(int index, SharedPreferences settings) {
            int last_status = settings.getInt("last_status", 0); /* TODO this should be a contant in Service */

            switch (index) {
            case 0:
                if (last_status == 2 && settings.getBoolean(SettingsActivity.KEY_SHOW_CHARGE_TIME, true)) return true; /* TODO: Service constant */
                break;
            case 1:
                if (settings.getBoolean(SettingsActivity.KEY_SHOW_LIGHT_USAGE, true)) return true;
                break;
            case 2:
                if (settings.getBoolean(SettingsActivity.KEY_SHOW_NORMAL_USAGE, true)) return true;
                break;
            case 3:
                if (settings.getBoolean(SettingsActivity.KEY_SHOW_HEAVY_USAGE, true)) return true;
                break;
            case 4:
                if (settings.getBoolean(SettingsActivity.KEY_SHOW_CONSTANT_USAGE, true)) return true;
                break;
            default:
                return true;
            }

            return false;
        }
    }

    private class DefaultTheme extends Theme {
        public DefaultTheme() {
            mainFrameLayout = R.layout.main_frame_default;

            int[] mainLayoutPadding = res.getIntArray(R.array.theme_default_main_layout_padding);

            mainLayoutPaddingLeft = (int) (mainLayoutPadding[0] * density);
            mainLayoutPaddingTop = (int) (mainLayoutPadding[1] * density);
            mainLayoutPaddingRight = (int) (mainLayoutPadding[2] * density);
            mainLayoutPaddingBottom = (int) (mainLayoutPadding[3] * density);
        }
    }

    private class ColorfulTheme extends DefaultTheme {
        public ColorfulTheme() {
            mainFrameLayout = R.layout.main_frame_colorful;
        }
    }

    private class FullDarkTheme extends Theme {
        public FullDarkTheme() {
            mainFrameLayout = R.layout.main_frame_full_dark;

            mainLayoutPaddingLeft = 0;
            mainLayoutPaddingTop = 0;
            mainLayoutPaddingRight = 0;
            mainLayoutPaddingBottom = 0;
        }
    }
}
