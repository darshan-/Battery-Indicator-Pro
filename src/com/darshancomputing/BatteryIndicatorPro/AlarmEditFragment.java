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
import android.database.Cursor;
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
import androidx.preference.Preference.OnPreferenceChangeListener;
//import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

public class AlarmEditFragment extends PreferenceFragmentCompat {
    private Resources res;

    private PreferenceScreen mPreferenceScreen;
    private AlarmDatabase alarms;
    private Cursor mCursor;
    private AlarmAdapter mAdapter;
    private NotificationManager mNotificationManager;

    public static final String KEY_ENABLED      = "enabled";
    public static final String KEY_TYPE         = "type";
    public static final String KEY_THRESHOLD    = "threshold";

    public static final String KEY_CHAN_SETTINGS_B = "channel_settings_button";

    public static final String EXTRA_ALARM_ID = "com.darshancomputing.BatteryIndicatorPro.AlarmID";

    private static final String[] chargeEntries = {
        "5%", "10%", "15%", "20%", "25%", "30%", "35%", "40%", "45%", "50%",
        "55%", "60%", "65%", "70%", "75%", "80%", "85%", "90%", "95%", "99%"};
    private static final String[] chargeValues = {
        "5", "10", "15", "20", "25", "30", "35", "40", "45", "50",
        "55", "60", "65", "70", "75", "80", "85", "90", "95", "99"};

    private boolean chanDisabled;

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
        alarms = new AlarmDatabase(getActivity());

        mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        mCursor = alarms.getAlarm(getActivity().getIntent().getIntExtra(EXTRA_ALARM_ID, -1));
        mAdapter = new AlarmAdapter();

        setPreferences();
        mPreferenceScreen = getPreferenceScreen();
        syncValuesAndSetListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCursor.close();
        alarms.close();
    }

    @Override
    public void onResume() {
        super.onResume();

        mCursor.requery();
        mCursor.moveToFirst();
        mAdapter.requery();

        matchEnabled();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void syncValuesAndSetListeners() {
        CheckBoxPreference cb = (CheckBoxPreference) mPreferenceScreen.findPreference(KEY_ENABLED);
        cb.setChecked(mAdapter.enabled);
        cb.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                mAdapter.setEnabled((Boolean) newValue);
                return true;
            }
        });

        ListPreference lp = (ListPreference) mPreferenceScreen.findPreference(KEY_TYPE);
        lp.setValue(mAdapter.type);
        updateSummary(lp);
        lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                if (mAdapter.type.equals(newValue)) return false;

                mAdapter.setType((String) newValue);

                ((ListPreference) pref).setValue((String) newValue);
                updateSummary((ListPreference) pref);

                setUpThresholdList(true);

                matchEnabled(); // Call after setUpThresholdList, to make sure to disable if necessary

                return false;
            }
        });

        lp = (ListPreference) mPreferenceScreen.findPreference(KEY_THRESHOLD);
        setUpThresholdList(false);
        lp.setValue(mAdapter.threshold);
        updateSummary(lp);
        lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                if (mAdapter.threshold.equals(newValue)) return false;

                mAdapter.setThreshold((String) newValue);

                ((ListPreference) pref).setValue((String) newValue);
                updateSummary((ListPreference) pref);

                return false;
            }
        });
    }

    private void matchEnabled() {
        Preference prefb = mPreferenceScreen.findPreference(KEY_CHAN_SETTINGS_B);

        if (chanDisabled) {
            Preference p = mPreferenceScreen.findPreference(KEY_ENABLED);
            p.setEnabled(false);
            ListPreference lp = (ListPreference) mPreferenceScreen.findPreference(KEY_THRESHOLD);
            //p = mPreferenceScreen.findPreference(KEY_THRESHOLD);
            lp.setEnabled(false);

            //android.text.Spannable txt = (android.text.Spannable) sum;
            android.text.Spannable txt = new android.text.SpannableString(getString(R.string.alarm_chan_disabled_b));
            /*prefb.setSummary(R.string.alarm_chan_settings_b);
            android.text.Spannable old = new android.text.SpannableString(prefb.getSummary());
            Object[] spans = old.getSpans(0, old.length(), Object.class);
            for (Object o : spans) {
                txt.setSpan(o, 0, txt.length(), 0);
            }*/
            //txt.setSpan(new android.text.style.ForegroundColorSpan(0xffee4444), 0, txt.length(), 0);
            txt.setSpan(new android.text.style.ForegroundColorSpan(0xffff2222), 0, txt.length(), 0);
            //txt.setSpan(new android.text.style.ForegroundColorSpan(0xffffffff), 0, txt.length(), 0);
            //txt.setSpan(new android.text.style.ForegroundColorSpan(0xffff0000), 0, txt.length(), 0);
            //txt.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, txt.length(), 0);
            prefb.setSummary(txt);
        } else {
            Preference p = mPreferenceScreen.findPreference(KEY_ENABLED);
            p.setEnabled(true);

            setUpThresholdList(false);

            prefb.setSummary(R.string.alarm_chan_settings_b);
        }
    }

    public void deleteAlarm() {
        alarms.deleteAlarm(mAdapter.id);
    }

    private void updateSummary(ListPreference lp) {
        Boolean formatterUsed;

        lp.setSummary("%%");
        if (lp.getSummary().length() == 2)
            formatterUsed = false;
        else
            formatterUsed = true;

        String entry = (String) lp.getEntry();
        if (entry == null) entry = "";
        if (formatterUsed)
            entry = entry.replace("%", "%%");

        if (lp.isEnabled())
            lp.setSummary(Str.currently_set_to + entry);
        else
            lp.setSummary(Str.alarm_pref_not_used);
    }

    private void setUpThresholdList(Boolean resetValue) {
        ListPreference lp = (ListPreference) mPreferenceScreen.findPreference(KEY_THRESHOLD);

        if (mAdapter.type.equals("temp_drops") || mAdapter.type.equals("temp_rises")) {
            lp.setEntries(Str.temp_alarm_entries);
            lp.setEntryValues(Str.temp_alarm_values);
            lp.setEnabled(true);

            if (resetValue) {
                if (mAdapter.type.equals("temp_drops"))
                    mAdapter.setThreshold("60");
                else
                    mAdapter.setThreshold("460");
                lp.setValue(mAdapter.threshold);
            }
        } else if (mAdapter.type.equals("charge_drops") || mAdapter.type.equals("charge_rises")) {
            lp.setEntries(chargeEntries);
            lp.setEntryValues(chargeValues);
            lp.setEnabled(true);

            if (resetValue) {
                if (mAdapter.type.equals("charge_drops"))
                    mAdapter.setThreshold("20");
                else
                    mAdapter.setThreshold("90");
                        
                lp.setValue(mAdapter.threshold);
            }
        } else {
            lp.setEnabled(false);
        }

        updateSummary(lp);
    }

    private class AlarmAdapter {
        public int id;
        String type, threshold;
        Boolean enabled;

        AlarmAdapter() {
            requery();
        }

        void requery() {
                      id = mCursor.getInt   (mCursor.getColumnIndex(AlarmDatabase.KEY_ID));
                    type = mCursor.getString(mCursor.getColumnIndex(AlarmDatabase.KEY_TYPE));
               threshold = mCursor.getString(mCursor.getColumnIndex(AlarmDatabase.KEY_THRESHOLD));
                 enabled = (mCursor.getInt(mCursor.getColumnIndex(AlarmDatabase.KEY_ENABLED)) == 1);

            chanDisabled = mNotificationManager.getNotificationChannel(type).getImportance() == 0;
         }

        public void setEnabled(Boolean b) {
            enabled = b;
            alarms.setEnabled(id, enabled);
        }

        public void setType(String s) {
            type = s;
            chanDisabled = mNotificationManager.getNotificationChannel(type).getImportance() == 0;
            alarms.setType(id, type);
        }

        void setThreshold(String s) {
            threshold = s;
            alarms.setThreshold(id, threshold);
        }
    }

    public void enableNotifsButtonClick() {
        if (mAdapter.type == null)
            return;

        Intent intent;
        intent = new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
        intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, mAdapter.type);
        intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);

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
