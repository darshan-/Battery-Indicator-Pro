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

    private static final String[] chargeEntries = {
        "5%", "10%", "15%", "20%", "25%", "30%", "35%", "40%", "45%", "50%",
        "55%", "60%", "65%", "70%", "75%", "80%", "85%", "90%", "95%"};
    private static final String[] chargeValues = {
        "5", "10", "15", "20", "25", "30", "35", "40", "45", "50",
        "55", "60", "65", "70", "75", "80", "85", "90", "95"};

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
        typeLP.setValue(mAdapter.type);
        updateSummary(typeLP);
        typeLP.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                if (mAdapter.type.equals((String) newValue)) return false;

                mAdapter.setType((String) newValue);

                ListPreference typeLP = (ListPreference) pref;
                typeLP.setValue((String) newValue);
                updateSummary(typeLP);

                setUpThresholdList(true);

                return false;
            }
        });

        ListPreference threshLP = (ListPreference) mPreferenceScreen.findPreference(KEY_THRESHOLD);
        setUpThresholdList(false);
        threshLP.setValue(mAdapter.threshold);
        updateSummary(threshLP);
        threshLP.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                if (mAdapter.threshold.equals((String) newValue)) return false;

                mAdapter.setThreshold((String) newValue);

                ListPreference threshLP = (ListPreference) pref;
                threshLP.setValue((String) newValue);
                updateSummary(threshLP);

                return false;
            }
        });
    }

    private void updateSummary(ListPreference lp) {
        if (lp.isEnabled())
            lp.setSummary(str.currently_set_to + " " + lp.getEntry());
        else
            lp.setSummary(str.alarm_pref_not_used);
    }

    private void setUpThresholdList(Boolean resetValue) {
        ListPreference lp = (ListPreference) mPreferenceScreen.findPreference(KEY_THRESHOLD);

        if (mAdapter.type.equals("temp_rises")) {
            lp.setEntries(str.temp_alarm_entries);
            lp.setEntryValues(str.temp_alarm_values);
            lp.setEnabled(true);

            //if (str.indexOf(str.temp_alarm_values, mAdapter.threshold) == -1) {
            if (resetValue) {
                mAdapter.setThreshold(str.temp_alarm_values[3]);
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
        public String type, threshold;
        public Boolean enabled;

        public AlarmAdapter() {
            requery();
        }

        public void requery() {
                   id = mCursor.getInt   (AlarmDatabase.INDEX_ID);
                 type = mCursor.getString(AlarmDatabase.INDEX_TYPE);
            threshold = mCursor.getString(AlarmDatabase.INDEX_THRESHOLD);
              enabled = (mCursor.getInt(AlarmDatabase.INDEX_ENABLED) == 1);
        }

        public void setEnabledness(Boolean b) {
            enabled = b;
            alarms.setEnabledness(id, enabled);
        }

        public void setType(String s) {
            type = s;
            alarms.setType(id, type);
        }

        public void setThreshold(String s) {
            threshold = s;
            alarms.setThreshold(id, threshold);
        }
    }
}