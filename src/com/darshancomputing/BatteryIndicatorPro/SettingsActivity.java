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
    public static final String KEY_DISABLE_LOCKING = "disable_lock_screen";
    public static final String KEY_CONFIRM_DISABLE_LOCKING = "confirm_disable_lock_screen";
    public static final String KEY_AUTO_DISABLE_LOCKING = "auto_disable_lock_screen";
    public static final String KEY_LOG_SCREEN = "log_screen";
    public static final String KEY_CONVERT_F = "convert_to_fahrenheit";
    public static final String KEY_AUTOSTART = "autostart";
    public static final String KEY_STATUS_DUR_EST = "status_dur_est";
    public static final String KEY_RED = "use_red";
    public static final String KEY_RED_THRESH = "red_threshold";
    public static final String KEY_AMBER = "use_amber";
    public static final String KEY_AMBER_THRESH = "amber_threshold";
    public static final String KEY_GREEN = "use_green";
    public static final String KEY_GREEN_THRESH = "green_threshold";
    public static final String KEY_COLOR_PREVIEW = "color_preview";

    public static final int   RED = 0;
    public static final int AMBER = 1;
    public static final int GREEN = 2;

    public static final int   RED_ICON_MAX = 30;
    public static final int AMBER_ICON_MIN =  0;
    public static final int AMBER_ICON_MAX = 50;
    public static final int GREEN_ICON_MIN = 20;

    public static final int   RED_SETTING_MIN =  5;
    public static final int   RED_SETTING_MAX = 30;
    public static final int AMBER_SETTING_MIN = 10;
    public static final int AMBER_SETTING_MAX = 50;
    public static final int GREEN_SETTING_MIN = 20;

    private Intent biServiceIntent;
    private BIServiceConnection biServiceConnection;

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

        addPreferencesFromResource(R.xml.pref_screen);

        biServiceIntent = new Intent(this, BatteryIndicatorService.class);
        biServiceConnection = new BIServiceConnection();
        bindService(biServiceIntent, biServiceConnection, 0);

        mPreferenceScreen  = getPreferenceScreen();
        mSharedPreferences = mPreferenceScreen.getSharedPreferences();

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

        validateColorPrefs(null);

        updateConvertFSummary();

        updateListPrefSummary(KEY_AUTOSTART);
        updateListPrefSummary(KEY_STATUS_DUR_EST);
        updateListPrefSummary(KEY_RED_THRESH);
        updateListPrefSummary(KEY_AMBER_THRESH);
        updateListPrefSummary(KEY_GREEN_THRESH);

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
            ComponentName comp = new ComponentName(getPackageName(),
                                                   SettingsHelpActivity.class.getName());
            startActivity(new Intent().setComponent(comp));

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(KEY_LOG_SCREEN)) {
            ComponentName comp = new ComponentName(getPackageName(),
                                                   LogViewActivity.class.getName());
            startActivity(new Intent().setComponent(comp));

            return false; // I'm guessing that returning false prevents Android from trying to process this...
        }

        return true;
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

        if (key.equals(KEY_CONVERT_F)) {
            updateConvertFSummary();
        } else if (key.equals(KEY_AUTOSTART) || key.equals(KEY_STATUS_DUR_EST) ||
                   key.equals(KEY_RED_THRESH) ||
                   key.equals(KEY_AMBER_THRESH) || key.equals(KEY_GREEN_THRESH)) {
            updateListPrefSummary(key);
        }

        /* Update dependent's summary as well */
        if (key.equals(KEY_RED)) updateListPrefSummary(KEY_RED_THRESH);
        else if (key.equals(KEY_AMBER)) updateListPrefSummary(KEY_AMBER_THRESH);
        else if (key.equals(KEY_GREEN)) updateListPrefSummary(KEY_GREEN_THRESH);

        /* Restart service for those that require it */
        if (key.equals(KEY_CONVERT_F) || key.equals(KEY_RED) || key.equals(KEY_RED_THRESH) ||
                   key.equals(KEY_STATUS_DUR_EST) || key.equals(KEY_AUTO_DISABLE_LOCKING) ||
                   key.equals(KEY_AMBER) || key.equals(KEY_AMBER_THRESH) ||
                   key.equals(KEY_GREEN) || key.equals(KEY_GREEN_THRESH)) {
            biServiceConnection.biService.reloadSettings(); /* New soft reset */
        }

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void updateConvertFSummary() {
        Preference pref = (CheckBoxPreference) mPreferenceScreen.findPreference(KEY_CONVERT_F);
        pref.setSummary(getResources().getString(R.string.currently_using) + " " +
                        (mSharedPreferences.getBoolean(KEY_CONVERT_F, false) ?
                         getResources().getString(R.string.fahrenheit) : getResources().getString(R.string.celsius)));
    }

    private void updateListPrefSummary(String key) {
        ListPreference pref = (ListPreference) mPreferenceScreen.findPreference(key);

        if (pref.isEnabled()) {
            pref.setSummary(getResources().getString(R.string.currently_set_to) + " " + pref.getEntry());
        } else {
            pref.setSummary(getResources().getString(R.string.currently_disabled));
        }
    }

    private void validateColorPrefs(String changedKey) {
        String [] a;

        if (changedKey == null || changedKey.equals(KEY_RED)) {
            if (redEnabled) {
                redThresh.setEnabled(true);

                a = xToYBy5(determineMin(RED), RED_SETTING_MAX);
                redThresh.setEntries(a);
                redThresh.setEntryValues(a);

                /* Older version had a higher max; user's setting could be too high. */
                if (iRedThresh > RED_SETTING_MAX) {
                    redThresh.setValue("" + RED_SETTING_MAX);
                    iRedThresh = RED_SETTING_MAX;
                }
            } else {
                redThresh.setEnabled(false);
            }
        }

        if (changedKey == null || changedKey.equals(KEY_RED) || changedKey.equals(KEY_RED_THRESH) ||
            changedKey.equals(KEY_AMBER)) {
            if (amberEnabled) {
                amberThresh.setEnabled(true);

                a = xToYBy5(determineMin(AMBER), AMBER_SETTING_MAX);
                amberThresh.setEntries(a);
                amberThresh.setEntryValues(a);

                if (iAmberThresh < Integer.valueOf(a[0])) {
                    amberThresh.setValue(a[0]);
                    iAmberThresh = Integer.valueOf(a[0]);
                    updateListPrefSummary(KEY_AMBER_THRESH);
                }
            } else {
                amberThresh.setEnabled(false);
            }
        }

        if (changedKey == null || !changedKey.equals(KEY_GREEN_THRESH)) {
            if (greenEnabled) {
                greenThresh.setEnabled(true);

                a = xToYBy5(determineMin(GREEN), 100);
                greenThresh.setEntries(a);
                greenThresh.setEntryValues(a);

                if (iGreenThresh < Integer.valueOf(a[0])) {
                    greenThresh.setValue(a[0]);
                    iGreenThresh = Integer.valueOf(a[0]);
                    updateListPrefSummary(KEY_GREEN_THRESH);
                }
            } else {
                greenThresh.setEnabled(false);
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
