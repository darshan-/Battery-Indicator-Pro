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
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

public class AlarmEditActivity extends PreferenceActivity implements OnPreferenceChangeListener {
    private Resources res;
    private Context context;
    private Str str;
    private PreferenceScreen mPreferenceScreen;
    private AlarmDatabase alarms;
    private Cursor mCursor;
    private AlarmAdapter mAdapter;

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

        System.out.println("........................ id=" + mAdapter.id + " enabled=" + mAdapter.enabled);

        addPreferencesFromResource(R.xml.alarm_pref_screen);

        mPreferenceScreen = getPreferenceScreen();
        setOnPreferenceChangeListeners(mPreferenceScreen);
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

    private void setOnPreferenceChangeListeners(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pg = (PreferenceGroup) p;

            for (int i=0, count = pg.getPreferenceCount(); i < count; i++)
                setOnPreferenceChangeListeners(pg.getPreference(i));
        } else {
            p.setOnPreferenceChangeListener(this);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
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
    }
}