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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import java.util.Locale;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
//import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

public class AlarmEditFragment extends PreferenceFragmentCompat {
    private Resources res;

    public void setScreen() {
        if (res != null)
            setPreferences();
    }

    private void setPreferences() {
        setPreferencesFromResource(R.xml.alarm_pref_screen, null);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        res = getResources();

        setPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // See old syncValuesAndSetListeners()

    // Do I need/want to set this up to go to the paricular alarm channel, if app channel is unblocked
    //   but alarm channel is?  Seem like it?
    public void enableNotifsButtonClick() {
        // Intent intent;
        // if (!appNotifsEnabled) {
        //     intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        // } else {
        //     intent = new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
        //     intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, mainChan.getId());
        // }

        // intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        // startActivity(intent);
    }
}
