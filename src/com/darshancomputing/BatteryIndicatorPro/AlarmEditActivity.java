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

import android.content.Context;
//import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

public class AlarmEditActivity extends PreferenceActivity {
    private Resources res;
    private Context context;
    private Str str;
    private PreferenceScreen mPreferenceScreen;
    private AlarmDatabase alarms;
    private Cursor mCursor;
    private AlarmAdapter mAdapter;

    public static final String KEY_ENABLED   = "enabled";
    public static final String KEY_TYPE      = "type";
    public static final String KEY_THRESHOLD = "threshold";

    public static final String EXTRA_ALARM_ID = "com.darshancomputing.BatteryIndicatorPro.AlarmID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        res = getResources();
        str = new Str(res);
        alarms = new AlarmDatabase(context);

        mCursor = alarms.getAlarm(getIntent().getIntExtra(EXTRA_ALARM_ID, -1));
        mAdapter = new AlarmAdapter();

        setWindowSubtitle(res.getString(R.string.alarm_settings_subtitle));

        addPreferencesFromResource(R.xml.alarm_pref_screen);
        mPreferenceScreen = getPreferenceScreen();

        syncValuesAndSetListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCursor.close();
        alarms.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCursor.requery();
        mCursor.moveToFirst();
        mAdapter.requery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCursor.deactivate();
    }

    private void setWindowSubtitle(String subtitle) {
        setTitle(res.getString(R.string.app_full_name) + " - " + subtitle);
    }

    private void syncValuesAndSetListeners() {
        CheckBoxPreference enabledCB = (CheckBoxPreference) mPreferenceScreen.findPreference(KEY_ENABLED);
        enabledCB.setChecked(mAdapter.enabled);
        enabledCB.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                mAdapter.setEnabledness((Boolean) newValue);
                return true;
            }
        });

        ListPreference typeLP = (ListPreference) mPreferenceScreen.findPreference(KEY_TYPE);
        typeLP.setValue(str.alarm_type_values[mAdapter.type]);
        updateSummary(typeLP);
        typeLP.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                mAdapter.setType((String) newValue);

                ListPreference typeLP = (ListPreference) pref;
                typeLP.setValue((String) newValue);
                updateSummary(typeLP);

                return false;
            }
        });

        /*
        ListPreference threshLP = (ListPreference) mPreferenceScreen.findPreference(KEY_THRESHOLD);
        typeLP.setValue(str.alarm_type_values[mAdapter.type]);
        updateSummary(typeLP);
        typeLP.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                mAdapter.setType((String) newValue);

                ListPreference typeLP = (ListPreference) pref;
                typeLP.setValue((String) newValue);
                updateSummary(typeLP);

                return false;
            }
        });
        */
    }

    private void updateSummary(ListPreference lp) {
        lp.setSummary(str.currently_set_to + " " + lp.getEntry());
    }

    private void setUpThresholdList() {
        ListPreference lp = (ListPreference) mPreferenceScreen.findPreference(KEY_THRESHOLD);

        switch (mAdapter.type) {
        case 1:
        case 2:
            // Charge
            lp.setEnabled(true);
            break;
        case 3:
            lp.setEntries(str.temp_alarm_entries);
            lp.setEntryValues(str.temp_alarm_values);
            lp.setEnabled(true);
            break;
        default:
            lp.setEnabled(false);
        }
    }

    private class AlarmAdapter {
        public int id, type, threshold;
        public Boolean enabled;
        private int idIndex, typeIndex, thresholdIndex, enabledIndex;

        public AlarmAdapter() {
                   idIndex = mCursor.getColumnIndexOrThrow(AlarmDatabase.KEY_ID);
                 typeIndex = mCursor.getColumnIndexOrThrow(AlarmDatabase.KEY_TYPE);
            thresholdIndex = mCursor.getColumnIndexOrThrow(AlarmDatabase.KEY_THRESHOLD);
              enabledIndex = mCursor.getColumnIndexOrThrow(AlarmDatabase.KEY_ENABLED);

            requery();
        }

        public void requery() {
                   id = mCursor.getInt(idIndex);
                 type = mCursor.getInt(typeIndex);
            threshold = mCursor.getInt(thresholdIndex);
              enabled = (mCursor.getInt(enabledIndex) == 1);
        }

        public void setEnabledness(Boolean b) {
            enabled = b;
            alarms.setEnabledness(id, enabled);
        }

        public void setType(String s) {
            type = indexOf(str.alarm_type_values, s);
            alarms.setType(id, type);
        }

        public void setThreshold(int i) {
            threshold = i;
            //alarms.setThreshold(id, threshold);
        }

        private int indexOf(String[] a, String key) {
            for (int i=0, size=a.length; i < size; i++)
                if (key.equals(a[i])) return i;

            return -1;
        }
    }
}