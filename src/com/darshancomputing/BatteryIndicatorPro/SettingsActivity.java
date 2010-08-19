/*
    Copyright (c) 2009, 2010 Josiah Barber (aka Darshan)

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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    public static final String KEY_COLOR_SETTINGS = "color_settings";
    public static final String KEY_TIME_SETTINGS = "time_settings";
    public static final String KEY_OTHER_SETTINGS = "other_settings";
    public static final String KEY_DISABLE_LOCKING = "disable_lock_screen";
    public static final String KEY_CONFIRM_DISABLE_LOCKING = "confirm_disable_lock_screen";
    public static final String KEY_FINISH_AFTER_TOGGLE_LOCK = "finish_after_toggle_lock";
    public static final String KEY_AUTO_DISABLE_LOCKING = "auto_disable_lock_screen";
    public static final String KEY_ENABLE_LOGGING = "enable_logging";
    public static final String KEY_LOG_EVERYTHING = "log_everything";
    public static final String KEY_MW_THEME = "main_window_theme";
    public static final String KEY_CONVERT_F = "convert_to_fahrenheit";
    public static final String KEY_AUTOSTART = "autostart";
    public static final String KEY_CHARGE_AS_TEXT = "charge_as_text";
    public static final String KEY_STATUS_DUR_EST = "status_dur_est";
    public static final String KEY_RED = "use_red";
    public static final String KEY_RED_THRESH = "red_threshold";
    public static final String KEY_AMBER = "use_amber";
    public static final String KEY_AMBER_THRESH = "amber_threshold";
    public static final String KEY_GREEN = "use_green";
    public static final String KEY_GREEN_THRESH = "green_threshold";
    public static final String KEY_COLOR_PREVIEW = "color_preview";
    public static final String KEY_USB_CHARGE_TIME = "usb_charge_time";
    public static final String KEY_AC_CHARGE_TIME = "ac_charge_time";
    public static final String KEY_LIGHT_USAGE_TIME = "light_usage_time";
    public static final String KEY_NORMAL_USAGE_TIME = "normal_usage_time";
    public static final String KEY_HEAVY_USAGE_TIME = "heavy_usage_time";
    public static final String KEY_CONSTANT_USAGE_TIME = "constant_usage_time";
    public static final String KEY_SHOW_CHARGE_TIME = "show_charge_time";
    public static final String KEY_SHOW_LIGHT_USAGE = "show_light_usage";
    public static final String KEY_SHOW_NORMAL_USAGE = "show_normal_usage";
    public static final String KEY_SHOW_HEAVY_USAGE = "show_heavy_usage";
    public static final String KEY_SHOW_CONSTANT_USAGE = "show_constant_usage";

    private static final String[] PARENTS = {KEY_SHOW_CHARGE_TIME, KEY_SHOW_CHARGE_TIME, /* Keep this doubled key first! */
                                             KEY_RED, KEY_AMBER, KEY_GREEN,
                                             KEY_SHOW_LIGHT_USAGE, KEY_SHOW_NORMAL_USAGE,
                                             KEY_SHOW_HEAVY_USAGE, KEY_SHOW_CONSTANT_USAGE,
                                             KEY_ENABLE_LOGGING};
    private static final String[] DEPENDENTS = {KEY_USB_CHARGE_TIME, KEY_AC_CHARGE_TIME,
                                                KEY_RED_THRESH, KEY_AMBER_THRESH, KEY_GREEN_THRESH,
                                                KEY_LIGHT_USAGE_TIME, KEY_NORMAL_USAGE_TIME,
                                                KEY_HEAVY_USAGE_TIME, KEY_CONSTANT_USAGE_TIME,
                                                KEY_LOG_EVERYTHING};

    private static final String[] LIST_PREFS = {KEY_AUTOSTART, KEY_MW_THEME, KEY_STATUS_DUR_EST,
                                               KEY_RED_THRESH, KEY_AMBER_THRESH, KEY_GREEN_THRESH,
                                               KEY_USB_CHARGE_TIME, KEY_AC_CHARGE_TIME,
                                               KEY_LIGHT_USAGE_TIME, KEY_NORMAL_USAGE_TIME,
                                               KEY_HEAVY_USAGE_TIME, KEY_CONSTANT_USAGE_TIME};

    private static final String[] RESET_SERVICE = {KEY_CONVERT_F, KEY_CHARGE_AS_TEXT, KEY_STATUS_DUR_EST,
                                                   KEY_AUTO_DISABLE_LOCKING, KEY_RED, KEY_RED_THRESH,
                                                   KEY_AMBER, KEY_AMBER_THRESH, KEY_GREEN, KEY_GREEN_THRESH};

    private static final String EXTRA_SCREEN = "com.darshancomputing.BatteryIndicatorPro.PrefScreen";

    public static final int   RED = 0;
    public static final int AMBER = 1;
    public static final int GREEN = 2;

    /* Red must go down to 0 and green must go up to 100,
       which is why they aren't listed here. */
    public static final int   RED_ICON_MAX = 30;
    public static final int AMBER_ICON_MIN =  0;
    public static final int AMBER_ICON_MAX = 50;
    public static final int GREEN_ICON_MIN = 20;

    public static final int   RED_SETTING_MIN =  5;
    public static final int   RED_SETTING_MAX = 30;
    public static final int AMBER_SETTING_MIN = 10;
    public static final int AMBER_SETTING_MAX = 50;
    public static final int GREEN_SETTING_MIN = 20;
    /* public static final int GREEN_SETTING_MAX = 100; /* TODO: use this, and possibly set it to 95. */

    private Intent biServiceIntent;
    private BIServiceConnection biServiceConnection;

    private Resources res;
    private PreferenceScreen mPreferenceScreen;
    private SharedPreferences mSharedPreferences;

    private ColorPreviewPreference cpbPref;

    private ListPreference   redThresh;
    private ListPreference amberThresh;
    private ListPreference greenThresh;

    private Boolean   redEnabled;
    private Boolean amberEnabled;
    private Boolean greenEnabled;

    private int   iRedThresh;
    private int iAmberThresh;
    private int iGreenThresh;

    private static final String[] fivePercents = {
        "5", "10",
        "15", "20",
        "25", "30",
        "35", "40",
        "45", "50",
        "55", "60",
        "65", "70",
        "75", "80",
        "85", "90",
        "95", "100"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //android.os.Debug.startMethodTracing();

        Intent intent = getIntent();
        String pref_screen = intent.getStringExtra(EXTRA_SCREEN);
        res = getResources();

        if (pref_screen == null) {
            setPrefScreen(R.xml.main_pref_screen);
        } else if (pref_screen.equals(KEY_COLOR_SETTINGS)) {
            setPrefScreen(R.xml.color_pref_screen);
            setWindowSubtitle(res.getString(R.string.color_settings));

            cpbPref     = (ColorPreviewPreference) mPreferenceScreen.findPreference(KEY_COLOR_PREVIEW);

            redThresh   = (ListPreference) mPreferenceScreen.findPreference(KEY_RED_THRESH);
            amberThresh = (ListPreference) mPreferenceScreen.findPreference(KEY_AMBER_THRESH);
            greenThresh = (ListPreference) mPreferenceScreen.findPreference(KEY_GREEN_THRESH);

            redEnabled   = mSharedPreferences.getBoolean(  KEY_RED, false);
            amberEnabled = mSharedPreferences.getBoolean(KEY_AMBER, false);
            greenEnabled = mSharedPreferences.getBoolean(KEY_GREEN, false);

            iRedThresh   = Integer.valueOf(  redThresh.getValue());
            iAmberThresh = Integer.valueOf(amberThresh.getValue());
            iGreenThresh = Integer.valueOf(greenThresh.getValue());
        } else if (pref_screen.equals(KEY_TIME_SETTINGS)) {
            setPrefScreen(R.xml.time_pref_screen);
            setWindowSubtitle(res.getString(R.string.time_settings));
        } else if (pref_screen.equals(KEY_OTHER_SETTINGS)) {
            setPrefScreen(R.xml.other_pref_screen);
            setWindowSubtitle(res.getString(R.string.other_settings));
        } else {
            setPrefScreen(R.xml.main_pref_screen);
        }

        for (int i=0; i < PARENTS.length; i++) setEnablednessOfDeps(i);
        validateColorPrefs(null);
        for (int i=0; i < LIST_PREFS.length; i++) updateListPrefSummary(LIST_PREFS[i]);
        updateConvertFSummary();
        setEnablednessOfMutuallyExclusive(KEY_CONFIRM_DISABLE_LOCKING, KEY_FINISH_AFTER_TOGGLE_LOCK);

        biServiceIntent = new Intent(this, BatteryIndicatorService.class);
        biServiceConnection = new BIServiceConnection();
        bindService(biServiceIntent, biServiceConnection, 0);
    }

    private void setWindowSubtitle(String subtitle) {
        setTitle(res.getString(R.string.app_full_name) + " - " + subtitle);
    }

    private void setPrefScreen(int resource) {
        addPreferencesFromResource(resource);

        mPreferenceScreen  = getPreferenceScreen();
        mSharedPreferences = mPreferenceScreen.getSharedPreferences();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(biServiceConnection);
        //android.os.Debug.stopMethodTracing();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_help:
            ComponentName comp = new ComponentName(getPackageName(), SettingsHelpActivity.class.getName());
            startActivity(new Intent().setComponent(comp));

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (key == null) return false;

        if (key.equals(KEY_COLOR_SETTINGS) || key.equals(KEY_TIME_SETTINGS) || key.equals(KEY_OTHER_SETTINGS)) {
            ComponentName comp = new ComponentName(getPackageName(), SettingsActivity.class.getName());
            startActivity(new Intent().setComponent(comp).putExtra(EXTRA_SCREEN, key));

            return true;
        }

        return false;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

        if (key.equals(KEY_RED)) {
            redEnabled = mSharedPreferences.getBoolean(KEY_RED, false);
        } else if (key.equals(KEY_AMBER)) {
            amberEnabled = mSharedPreferences.getBoolean(KEY_AMBER, false);
        } else if (key.equals(KEY_GREEN)) {
            greenEnabled = mSharedPreferences.getBoolean(KEY_GREEN, false);
        } else if (key.equals(KEY_RED_THRESH)) {
            iRedThresh = Integer.valueOf(redThresh.getValue());
        } else if (key.equals(KEY_AMBER_THRESH)) {
            iAmberThresh = Integer.valueOf(amberThresh.getValue());
        } else if (key.equals(KEY_GREEN_THRESH)) {
            iGreenThresh = Integer.valueOf(greenThresh.getValue());
        }

        if (key.equals(KEY_RED) || key.equals(KEY_RED_THRESH) ||
            key.equals(KEY_AMBER) || key.equals(KEY_AMBER_THRESH) ||
            key.equals(KEY_GREEN) || key.equals(KEY_GREEN_THRESH)) {
            validateColorPrefs(key);
        }

        for (int i=0; i < PARENTS.length; i++) {
            if (key.equals(PARENTS[i])) {
                setEnablednessOfDeps(i);
                if (i == 0) setEnablednessOfDeps(1); /* Doubled charge key */
                break;
            }
        }

        for (int i=0; i < LIST_PREFS.length; i++) {
            if (key.equals(LIST_PREFS[i])) {
                updateListPrefSummary(LIST_PREFS[i]);
                break;
            }
        }

        if (key.equals(KEY_CONFIRM_DISABLE_LOCKING) || key.equals(KEY_FINISH_AFTER_TOGGLE_LOCK))
            setEnablednessOfMutuallyExclusive(KEY_CONFIRM_DISABLE_LOCKING, KEY_FINISH_AFTER_TOGGLE_LOCK);

        if (key.equals(KEY_CONVERT_F)) {
            updateConvertFSummary();
        }

        for (int i=0; i < RESET_SERVICE.length; i++) {
            if (key.equals(RESET_SERVICE[i])) {
                biServiceConnection.biService.reloadSettings();
                break;
            }
        }

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void updateConvertFSummary() {
        Preference pref = (CheckBoxPreference) mPreferenceScreen.findPreference(KEY_CONVERT_F);
        if (pref == null) return;

        pref.setSummary(res.getString(R.string.currently_using) + " " +
                        (mSharedPreferences.getBoolean(KEY_CONVERT_F, false) ?
                         res.getString(R.string.fahrenheit) : res.getString(R.string.celsius)));
    }

    private void setEnablednessOfDeps(int index) {
        Preference dependent = mPreferenceScreen.findPreference(DEPENDENTS[index]);
        if (dependent == null) return;

        if (mSharedPreferences.getBoolean(PARENTS[index], false))
            dependent.setEnabled(true);
        else
            dependent.setEnabled(false);

        updateListPrefSummary(DEPENDENTS[index]);
    }

    private void setEnablednessOfMutuallyExclusive(String key1, String key2) {
        Preference pref1 = mPreferenceScreen.findPreference(key1);
        Preference pref2 = mPreferenceScreen.findPreference(key2);

        if (pref1 == null) return;

        if (mSharedPreferences.getBoolean(key1, false))
            pref2.setEnabled(false);
        else if (mSharedPreferences.getBoolean(key2, false))
            pref1.setEnabled(false);
        else {
            pref1.setEnabled(true);
            pref2.setEnabled(true);
        }
    }

    private void updateListPrefSummary(String key) {
        ListPreference pref;
        try { /* Code is simplest elsewhere if we call this on all dependents, but some aren't ListPreferences. */
            pref = (ListPreference) mPreferenceScreen.findPreference(key);
        } catch (java.lang.ClassCastException e) {
            return;
        }

        if (pref == null) return;

        if (pref.isEnabled()) {
            pref.setSummary(res.getString(R.string.currently_set_to) + " " + pref.getEntry());
        } else {
            pref.setSummary(res.getString(R.string.currently_disabled));
        }
    }

    private void validateColorPrefs(String changedKey) {
        if (cpbPref == null) return;
        String [] a;

        if (changedKey == null) {
            a = xToYBy5(RED_SETTING_MIN, RED_SETTING_MAX);
            redThresh.setEntries(a);
            redThresh.setEntryValues(a);

            /* Older version had a higher max; user's setting could be too high. */
            if (iRedThresh > RED_SETTING_MAX) {
                redThresh.setValue("" + RED_SETTING_MAX);
                iRedThresh = RED_SETTING_MAX;
            }
        }

        if (changedKey == null || changedKey.equals(KEY_RED) || changedKey.equals(KEY_RED_THRESH) ||
            changedKey.equals(KEY_AMBER)) {
            if (amberEnabled) {
                a = xToYBy5(determineMin(AMBER), AMBER_SETTING_MAX);
                amberThresh.setEntries(a);
                amberThresh.setEntryValues(a);

                if (iAmberThresh < Integer.valueOf(a[0])) {
                    amberThresh.setValue(a[0]);
                    iAmberThresh = Integer.valueOf(a[0]);
                    updateListPrefSummary(KEY_AMBER_THRESH);
                }
            }
        }

        if (changedKey == null || !changedKey.equals(KEY_GREEN_THRESH)) {
            if (greenEnabled) {
                a = xToYBy5(determineMin(GREEN), 100);
                greenThresh.setEntries(a);
                greenThresh.setEntryValues(a);

                if (iGreenThresh < Integer.valueOf(a[0])) {
                    greenThresh.setValue(a[0]);
                    iGreenThresh = Integer.valueOf(a[0]);
                    updateListPrefSummary(KEY_GREEN_THRESH);
                }
            }
        }

        updateColorPreviewBar();
    }

    /* Determine the minimum valid threshold setting for a particular color, based on other active settings,
         with red being independent, amber depending on red, and green depending on both others. */
    private int determineMin(int color) {
        switch (color) {
        case RED:
            return RED_SETTING_MIN;
        case AMBER:
            if (redEnabled)
                return java.lang.Math.max(iRedThresh + 5, AMBER_SETTING_MIN);
            else
                return AMBER_SETTING_MIN;
        case GREEN:
            if (amberEnabled)
                return java.lang.Math.max(iAmberThresh, GREEN_SETTING_MIN);
            if (redEnabled)
                return java.lang.Math.max(iRedThresh, GREEN_SETTING_MIN);
            else
                return GREEN_SETTING_MIN;
        default:
                return GREEN_SETTING_MIN;
        }
    }

    private static String[] xToYBy5(int x, int y) {
        int i = (x / 5) - 1;
        int j = (100 - y) / 5;

        String[] a = new String[fivePercents.length - i - j];

        System.arraycopy(fivePercents, i, a, 0, a.length);

        return a;
    }

    private void updateColorPreviewBar() {
        cpbPref.redThresh   =   redEnabled ?   iRedThresh :   0;
        cpbPref.amberThresh = amberEnabled ? iAmberThresh :   0;
        cpbPref.greenThresh = greenEnabled ? iGreenThresh : 100;
    }
}
