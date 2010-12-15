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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class SettingsHelpActivity extends Activity {
    private Resources res;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String pref_screen = intent.getStringExtra(SettingsActivity.EXTRA_SCREEN);
        res = getResources();

        if (pref_screen == null) {
            setContentView(R.layout.main_settings_help);
        } else if (pref_screen.equals(SettingsActivity.KEY_COLOR_SETTINGS)) {
            setContentView(R.layout.color_settings_help);
            setWindowSubtitle(res.getString(R.string.color_settings));
        } else if (pref_screen.equals(SettingsActivity.KEY_TIME_SETTINGS)) {
            setContentView(R.layout.time_settings_help);
            setWindowSubtitle(res.getString(R.string.time_settings));
        } else if (pref_screen.equals(SettingsActivity.KEY_OTHER_SETTINGS)) {
            setContentView(R.layout.other_settings_help);
            setWindowSubtitle(res.getString(R.string.other_settings));
        } else if (pref_screen.equals(SettingsActivity.KEY_ALARM_SETTINGS)) {
            setContentView(R.layout.alarm_settings_help);
            setWindowSubtitle(res.getString(R.string.alarm_settings));
        } else if (pref_screen.equals(SettingsActivity.KEY_ALARM_EDIT_SETTINGS)) {
            setContentView(R.layout.alarm_edit_help);
            setWindowSubtitle(res.getString(R.string.alarm_settings_subtitle));
        } else {
            setContentView(R.layout.main_settings_help);
        }

    }

    private void setWindowSubtitle(String subtitle) {
        setTitle(res.getString(R.string.app_full_name) + " - " + subtitle);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
