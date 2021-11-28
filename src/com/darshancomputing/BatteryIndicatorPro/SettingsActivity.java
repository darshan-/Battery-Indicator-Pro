/*
    Copyright (c) 2009-2021 Darshan Computing, LLC

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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    public static final String EXTRA_SCREEN = "com.darshancomputing.BatteryIndicatorPro.PrefScreen";

    private Resources res;
    private String pref_screen;
    private int menu_res = R.menu.settings;
    private SettingsFragment frag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        pref_screen = intent.getStringExtra(EXTRA_SCREEN);
        res = getResources();

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setElevation(0);
        }

        int c = getResources().getColor(R.color.windowBackground);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        w.setStatusBarColor(c);

        setContentView(R.layout.prefs);

        if (savedInstanceState == null) {
            frag = new SettingsFragment();

            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, frag, "")
                .commit();
        } else {
            frag = (SettingsFragment) getSupportFragmentManager().findFragmentByTag("");
        }

        if (pref_screen == null) {
            frag.setScreen(R.xml.main_pref_screen);
            setWindowSubtitle(res.getString(R.string.settings_activity_subtitle));
        } else if (pref_screen.equals(SettingsFragment.KEY_STATUS_BAR_ICON_SETTINGS)) {
            frag.setScreen(R.xml.status_bar_icon_pref_screen);
            setWindowSubtitle(res.getString(R.string.status_bar_icon_settings));
        } else if (pref_screen.equals(SettingsFragment.KEY_NOTIFICATION_SETTINGS)) {
            frag.setScreen(R.xml.notification_pref_screen);
            setWindowSubtitle(res.getString(R.string.notification_settings));
        } else if (pref_screen.equals(SettingsFragment.KEY_CURRENT_HACK_SETTINGS)) {
            frag.setScreen(R.xml.current_hack_pref_screen);
            setWindowSubtitle(res.getString(R.string.current_hack_settings));
        } else if (pref_screen.equals(SettingsFragment.KEY_OTHER_SETTINGS)) {
            frag.setScreen(R.xml.other_pref_screen);
            setWindowSubtitle(res.getString(R.string.other_settings));
        } else {
            frag.setScreen(R.xml.main_pref_screen);
            setWindowSubtitle(res.getString(R.string.settings_activity_subtitle));
        }
    }

    private void setWindowSubtitle(String subtitle) {
        if (res.getBoolean(R.bool.long_activity_names))
            setTitle(res.getString(R.string.app_full_name) + " - " + subtitle);
        else
            setTitle(subtitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menu_res, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_help:
            ComponentName comp = new ComponentName(getPackageName(), SettingsHelpActivity.class.getName());
            Intent intent = new Intent().setComponent(comp);

            if (pref_screen != null) intent.putExtra(EXTRA_SCREEN, pref_screen);

            startActivity(intent);

            return true;
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void enableNotifsButtonClick(android.view.View v) {
        frag.enableNotifsButtonClick();
    }
}
