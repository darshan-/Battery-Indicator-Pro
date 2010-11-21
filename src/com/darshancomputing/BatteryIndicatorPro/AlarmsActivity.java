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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AlarmsActivity extends Activity implements OnItemClickListener {
    private AlarmDatabase alarms;
    private Resources res;
    private Context context;
    private Cursor mCursor;
    private LayoutInflater mInflater;
    private AlarmAdapter mAdapter;
    private ListView mAlarmsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        res = getResources();

        setContentView(R.layout.alarms);
        setWindowSubtitle(res.getString(R.string.alarm_settings));

        alarms = new AlarmDatabase(context);
        mCursor = alarms.getAllAlarms(false);
        startManagingCursor(mCursor);

        alarms.clearAllAlarms();
        alarms.addAlarm(1, 15, true);
        alarms.addAlarm(2, 45, false);

        mInflater = LayoutInflater.from(this);
        mAdapter = new AlarmAdapter(context, mCursor);

        mAlarmsList = (ListView) findViewById(R.id.alarms_list);
        mAlarmsList.setAdapter(mAdapter);
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.setOnItemClickListener(this);
        mAlarmsList.setOnCreateContextMenuListener(this);
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
    }

    private void setWindowSubtitle(String subtitle) {
        setTitle(res.getString(R.string.app_full_name) + " - " + subtitle);
    }

    private class AlarmAdapter extends CursorAdapter {
        public int typeIndex, thresholdIndex, enabledIndex;

        public AlarmAdapter(Context context, Cursor cursor) {
            super(context, cursor);

                  typeIndex = cursor.getColumnIndexOrThrow(AlarmDatabase.KEY_TYPE);
             thresholdIndex = cursor.getColumnIndexOrThrow(AlarmDatabase.KEY_THRESHOLD);
               enabledIndex = cursor.getColumnIndexOrThrow(AlarmDatabase.KEY_ENABLED);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.alarm_item , parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            TextView summary_tv = (TextView)       view.findViewById(R.id.alarm_summary);
                View  indicator =                  view.findViewById(R.id.indicator);
            ImageView barOnOff  = (ImageView) indicator.findViewById(R.id.bar_onoff);
            CheckBox clockOnOff = (CheckBox)  indicator.findViewById(R.id.clock_onoff);

            int      type = cursor.getInt(typeIndex);
            int threshold = cursor.getInt(thresholdIndex);
            Boolean enabled = (cursor.getInt(enabledIndex) == 1);

            barOnOff.setImageResource(enabled ? R.drawable.ic_indicator_on : R.drawable.ic_indicator_off);
            clockOnOff.setChecked(enabled);

            /*indicator.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        clockOnOff.toggle();
                        updateIndicatorAndAlarm(clockOnOff.isChecked(),
                                barOnOff, alarm);
                    }
            });*/

            String s;
        }
    }

    public void onItemClick(AdapterView parent, View v, int pos, long id) {
        /*Intent intent = new Intent(this, SetAlarm.class);
        intent.putExtra(Alarms.ALARM_ID, (int) id);
        startActivity(intent);*/
    }
}
