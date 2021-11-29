/*
    Copyright (c) 2010-2021 Darshan Computing, LLC

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

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsHelpActivity extends AppCompatActivity {
    private Resources res;
    private int[] has_links = {};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String pref_screen = intent.getStringExtra(SettingsActivity.EXTRA_SCREEN);
        res = getResources();

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setElevation(0);
        }

        if (pref_screen == null) {
            setContentView(R.layout.main_settings_help);
            setWindowSubtitle(res.getString(R.string.settings_activity_subtitle));
        } else if (pref_screen.equals(SettingsFragment.KEY_NOTIFICATION_SETTINGS)) {
            setContentView(R.layout.notification_settings_help);
            setWindowSubtitle(res.getString(R.string.notification_settings));
        } else if (pref_screen.equals(SettingsFragment.KEY_STATUS_BAR_ICON_SETTINGS)) {
            setContentView(R.layout.status_bar_icon_settings_help);
            setWindowSubtitle(res.getString(R.string.status_bar_icon_settings));
        } else if (pref_screen.equals(SettingsFragment.KEY_CURRENT_HACK_SETTINGS)) {
            setContentView(R.layout.current_hack_settings_help);
            setWindowSubtitle(res.getString(R.string.current_hack_settings));
        } else if (pref_screen.equals(SettingsFragment.KEY_OTHER_SETTINGS)) {
            setContentView(R.layout.other_settings_help);
            setWindowSubtitle(res.getString(R.string.other_settings));

            has_links = new int[] {};
        } else if (pref_screen.equals(SettingsFragment.KEY_ALARMS_SETTINGS)) {
            setContentView(R.layout.alarm_settings_help);
            setWindowSubtitle(res.getString(R.string.alarm_settings));
        } else if (pref_screen.equals(SettingsFragment.KEY_ALARM_EDIT_SETTINGS)) {
            setContentView(R.layout.alarm_edit_help);
            setWindowSubtitle(res.getString(R.string.alarm_settings_subtitle));
        } else {
            setContentView(R.layout.main_settings_help);
        }

        TextView tv;
        MovementMethod linkMovement = LinkMovementMethod.getInstance();

        for (int i=0; i < has_links.length; i++) {
            tv = (TextView) findViewById(has_links[i]);
            tv.setMovementMethod(linkMovement);
            tv.setAutoLinkMask(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        }
    }

    private void setWindowSubtitle(String subtitle) {
        if (res.getBoolean(R.bool.long_activity_names))
            setTitle(res.getString(R.string.app_full_name) + " - " + subtitle);
        else
            setTitle(subtitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
