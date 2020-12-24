/*
    Copyright (c) 2009-2020 Darshan Computing, LLC

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
    public static final String SETTINGS_FILE = "com.darshancomputing.BatteryIndicatorPro_preferences";
    public static final String SP_SERVICE_FILE = "sp_store";   // Only write from Service process
    public static final String SP_MAIN_FILE = "sp_store_main"; // Only write from main process

    public static final String EXTRA_SCREEN = "com.darshancomputing.BatteryIndicatorPro.PrefScreen";

    public static final String KEY_NOTIFICATION_SETTINGS = "notification_settings";
    public static final String KEY_STATUS_BAR_ICON_SETTINGS = "status_bar_icon_settings";
    public static final String KEY_CURRENT_HACK_SETTINGS = "current_hack_settings";
    public static final String KEY_ALARMS_SETTINGS = "alarms_settings";
    public static final String KEY_ALARM_EDIT_SETTINGS = "alarm_edit_settings";
    public static final String KEY_OTHER_SETTINGS = "other_settings";
    public static final String KEY_ENABLE_LOGGING = "enable_logging";
    public static final String KEY_MAX_LOG_AGE = "max_log_age";
    public static final String KEY_ICON_PLUGIN = "icon_plugin";
    public static final String KEY_ICON_SET = "icon_set";
    public static final String KEY_CONVERT_F = "convert_to_fahrenheit";
    public static final String KEY_NOTIFY_STATUS_DURATION = "notify_status_duration";
    public static final String KEY_AUTOSTART = "autostart";
    public static final String KEY_PREDICTION_TYPE = "prediction_type";
    public static final String KEY_CLASSIC_COLOR_MODE = "classic_color_mode";
    public static final String KEY_STATUS_DUR_EST = "status_dur_est";
    public static final String KEY_CAT_CLASSIC_COLOR_MODE = "category_classic_color_mode";
    public static final String KEY_CAT_COLOR = "category_color";
    public static final String KEY_CAT_CHARGING_INDICATOR = "category_charging_indicator";
    public static final String KEY_CAT_PLUGIN_SETTINGS = "category_plugin_settings";
    public static final String KEY_PLUGIN_SETTINGS = "plugin_settings";
    public static final String KEY_INDICATE_CHARGING = "indicate_charging";
    public static final String KEY_USE_SYSTEM_NOTIFICATION_LAYOUT = "use_system_notification_layout";
    public static final String KEY_ICON_AREA = "icon_area";
    public static final String KEY_TOP_LINE = "top_line";
    public static final String KEY_BOTTOM_LINE = "bottom_line";
    public static final String KEY_TIME_REMAINING_VERBOSITY = "time_remaining_verbosity";
    public static final String KEY_STATUS_DURATION_IN_VITAL_SIGNS = "status_duration_in_vital_signs";
    public static final String KEY_CAT_NOTIFICATION_APPEARANCE = "category_notification_appearance";
    public static final String KEY_NOTIFICATION_PERCENTAGE_TEXT_COLOR = "notification_percentage_text_color";
    public static final String KEY_CUSTOM_PERCENTAGE_TEXT_COLOR = "custom_percentage_text_color";
    public static final String KEY_SHOW_BOX_AROUND_ICON_AREA = "show_box_around_icon_area";
    public static final String KEY_NOTIFICATION_TOP_LINE_COLOR = "notification_top_line_color";
    public static final String KEY_CUSTOM_TOP_LINE_COLOR = "custom_top_line_color";
    public static final String KEY_NOTIFICATION_BOTTOM_LINE_COLOR = "notification_bottom_line_color";
    public static final String KEY_CUSTOM_BOTTOM_LINE_COLOR = "custom_bottom_line_color";
    public static final String KEY_CAT_CURRENT_HACK_MAIN = "category_current_hack_main";
    public static final String KEY_CAT_CURRENT_HACK_UNSUPPORTED = "category_current_hack_unsupported";
    public static final String KEY_ENABLE_CURRENT_HACK = "enable_current_hack";
    public static final String KEY_CURRENT_HACK_PREFER_FS = "current_hack_prefer_fs";
    public static final String KEY_CURRENT_HACK_MULTIPLIER = "current_hack_multiplier";
    public static final String KEY_CAT_CURRENT_HACK_NOTIFICATION = "category_current_hack_notification";
    public static final String KEY_DISPLAY_CURRENT_IN_VITAL_STATS = "display_current_in_vital_stats";
    public static final String KEY_PREFER_CURRENT_AVG_IN_VITAL_STATS = "prefer_current_avg_in_vital_stats";
    public static final String KEY_CAT_CURRENT_HACK_MAIN_WINDOW = "category_current_hack_main_window";
    public static final String KEY_DISPLAY_CURRENT_IN_MAIN_WINDOW = "display_current_in_main_window";
    public static final String KEY_PREFER_CURRENT_AVG_IN_MAIN_WINDOW = "prefer_current_avg_in_main_window";
    public static final String KEY_AUTO_REFRESH_CURRENT_IN_MAIN_WINDOW = "auto_refresh_current_in_main_window";
    public static final String KEY_FIRST_RUN = "first_run";
    public static final String KEY_MIGRATED_SERVICE_DESIRED = "service_desired_migrated_to_sp_main";
    public static final String KEY_ENABLE_NOTIFS_B = "enable_notifications_button";
    public static final String KEY_ENABLE_NOTIFS_SUMMARY = "enable_notifications_summary";

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
        } else if (pref_screen.equals(KEY_STATUS_BAR_ICON_SETTINGS)) {
            frag.setScreen(R.xml.status_bar_icon_pref_screen);
            setWindowSubtitle(res.getString(R.string.status_bar_icon_settings));
        } else if (pref_screen.equals(KEY_NOTIFICATION_SETTINGS)) {
            frag.setScreen(R.xml.notification_pref_screen);
            setWindowSubtitle(res.getString(R.string.notification_settings));
        } else if (pref_screen.equals(KEY_CURRENT_HACK_SETTINGS)) {
            frag.setScreen(R.xml.current_hack_pref_screen);
            setWindowSubtitle(res.getString(R.string.current_hack_settings));
        } else if (pref_screen.equals(KEY_OTHER_SETTINGS)) {
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
