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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class AlarmEditActivity extends PreferenceActivity {
    private Resources res;
    private Context context;
    private Str str;
    private PreferenceScreen mPreferenceScreen;
    private SharedPreferences settings;
    private AlarmDatabase alarms;
    private Cursor mCursor;
    private AlarmAdapter mAdapter;

    public static final String KEY_ENABLED   = "enabled";
    public static final String KEY_TYPE      = "type";
    public static final String KEY_THRESHOLD = "threshold";
    public static final String KEY_RINGTONE  = "ringtone";
    public static final String KEY_VIBRATE   = "vibrate";
    public static final String KEY_LIGHTS    = "lights";

    public static final String EXTRA_ALARM_ID = "com.darshancomputing.BatteryIndicatorPro.AlarmID";

    private static final String[] chargeEntries = {
        "5%", "10%", "15%", "20%", "25%", "30%", "35%", "40%", "45%", "50%",
        "55%", "60%", "65%", "70%", "75%", "80%", "85%", "90%", "95%", "99%"};
    private static final String[] chargeValues = {
        "5", "10", "15", "20", "25", "30", "35", "40", "45", "50",
        "55", "60", "65", "70", "75", "80", "85", "90", "95", "99"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        res = getResources();
        str = new Str(res);
        alarms = new AlarmDatabase(context);
        settings = PreferenceManager.getDefaultSharedPreferences(context);

        if (res.getBoolean(R.bool.override_list_activity_layout)) {
            setContentView(R.layout.list_activity);
            getListView().setDivider(res.getDrawable(R.drawable.my_divider));
        }

        if (res.getBoolean(R.bool.api_level_14_plus))
            getActionBar().setHomeButtonEnabled(true); // Stranglely disabled by default for API level 14+

        mCursor = alarms.getAlarm(getIntent().getIntExtra(EXTRA_ALARM_ID, -1));
        mAdapter = new AlarmAdapter();

        setWindowSubtitle(res.getString(R.string.alarm_settings_subtitle));

        addPreferencesFromResource(R.xml.alarm_pref_screen);
        mPreferenceScreen = getPreferenceScreen();

        syncValuesAndSetListeners();
    }

    private void setWindowSubtitle(String subtitle) {
        if (res.getBoolean(R.bool.long_activity_names))
            setTitle(res.getString(R.string.app_full_name) + " - " + subtitle);
        else
            setTitle(subtitle);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.alarm_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_delete:
            alarms.deleteAlarm(mAdapter.id);
            finish();
            return true;
        case R.id.menu_help:
            ComponentName comp = new ComponentName(getPackageName(), SettingsHelpActivity.class.getName());
            Intent intent = new Intent().setComponent(comp).putExtra(SettingsActivity.EXTRA_SCREEN, SettingsActivity.KEY_ALARM_EDIT_SETTINGS);
            startActivity(intent);

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
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
                if (mAdapter.type.equals((String) newValue)) return false;

                mAdapter.setType((String) newValue);

                ((ListPreference) pref).setValue((String) newValue);
                updateSummary((ListPreference) pref);

                setUpThresholdList(true);

                return false;
            }
        });

        lp = (ListPreference) mPreferenceScreen.findPreference(KEY_THRESHOLD);
        setUpThresholdList(false);
        lp.setValue(mAdapter.threshold);
        updateSummary(lp);
        lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                if (mAdapter.threshold.equals((String) newValue)) return false;

                mAdapter.setThreshold((String) newValue);

                ((ListPreference) pref).setValue((String) newValue);
                updateSummary((ListPreference) pref);

                return false;
            }
        });

        AlarmRingtonePreference rp = (AlarmRingtonePreference) mPreferenceScreen.findPreference(KEY_RINGTONE);
        rp.setValue(mAdapter.ringtone);
        rp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                if (mAdapter.ringtone.equals(newValue)) return false;

                mAdapter.setRingtone((String) newValue);
                ((AlarmRingtonePreference) pref).setValue((String) newValue);

                return false;
            }
        });

        cb = (CheckBoxPreference) mPreferenceScreen.findPreference(KEY_VIBRATE);
        cb.setChecked(mAdapter.vibrate);
        cb.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                mAdapter.setVibrate((Boolean) newValue);
                return true;
            }
        });

        cb = (CheckBoxPreference) mPreferenceScreen.findPreference(KEY_LIGHTS);
        cb.setChecked(mAdapter.lights);
        cb.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                mAdapter.setLights((Boolean) newValue);
                return true;
            }
        });
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
            lp.setSummary(str.currently_set_to + entry);
        else
            lp.setSummary(str.alarm_pref_not_used);
    }

    private void setUpThresholdList(Boolean resetValue) {
        ListPreference lp = (ListPreference) mPreferenceScreen.findPreference(KEY_THRESHOLD);

        if (mAdapter.type.equals("temp_rises")) {
            lp.setEntries(str.temp_alarm_entries);
            lp.setEntryValues(str.temp_alarm_values);
            lp.setEnabled(true);

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
        public String type, threshold, ringtone;
        public Boolean enabled, vibrate, lights;

        public AlarmAdapter() {
            requery();
        }

        public void requery() {
                   id = mCursor.getInt   (AlarmDatabase.INDEX_ID);
                 type = mCursor.getString(AlarmDatabase.INDEX_TYPE);
            threshold = mCursor.getString(AlarmDatabase.INDEX_THRESHOLD);
             ringtone = mCursor.getString(AlarmDatabase.INDEX_RINGTONE);
              enabled = (mCursor.getInt(AlarmDatabase.INDEX_ENABLED) == 1);
              vibrate = (mCursor.getInt(AlarmDatabase.INDEX_VIBRATE) == 1);
              lights = (mCursor.getInt(AlarmDatabase.INDEX_LIGHTS) == 1);
         }

        public void setEnabled(Boolean b) {
            enabled = b;
            alarms.setEnabled(id, enabled);
        }

        public void setVibrate(Boolean b) {
            vibrate = b;
            alarms.setVibrate(id, vibrate);
        }

        public void setLights(Boolean b) {
            lights = b;
            alarms.setLights(id, lights);
        }

        public void setType(String s) {
            type = s;
            alarms.setType(id, type);
        }

        public void setThreshold(String s) {
            threshold = s;
            alarms.setThreshold(id, threshold);
        }

        public void setRingtone(String s) {
            ringtone = s;
            alarms.setRingtone(id, ringtone);
        }
    }
}
