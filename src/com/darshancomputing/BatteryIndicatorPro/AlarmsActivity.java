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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
//import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AlarmsActivity extends Activity /* implements OnItemClickListener */ {
    private AlarmDatabase alarms;
    private Resources res;
    private Context context;
    private Str str;
    private Cursor mCursor;
    private LayoutInflater mInflater;
    private AlarmAdapter mAdapter;
    private ListView mAlarmsList;

    private int curId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        res = getResources();
        str = new Str(res);

        setContentView(R.layout.alarms);
        setWindowSubtitle(res.getString(R.string.alarm_settings));

        alarms = new AlarmDatabase(context);
        mCursor = alarms.getAllAlarms(true);
        startManagingCursor(mCursor);

        alarms.clearAllAlarms();
        alarms.addAlarm(0,  0, true);
        alarms.addAlarm(1, 15, true);
        alarms.addAlarm(2, 95, false);
        alarms.addAlarm(3, 58, false);
        alarms.addAlarm(4,  0, false);

        mInflater = LayoutInflater.from(this);
        mAdapter = new AlarmAdapter(context, mCursor);

        mAlarmsList = (ListView) findViewById(R.id.alarms_list);
        mAlarmsList.setAdapter(mAdapter);
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.setOnItemClickListener(null);
        mAlarmsList.setOnItemLongClickListener(null);
        mAlarmsList.setOnCreateContextMenuListener(null);
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.alarm_item_context, menu);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_alarm:
                System.out.println("..................................... Attempting to delete " + curId);
                alarms.deleteAlarm(curId);
                mCursor.requery();
                return true;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    private class AlarmAdapter extends CursorAdapter {
        public int idIndex, typeIndex, thresholdIndex, enabledIndex;

        public AlarmAdapter(Context context, Cursor cursor) {
            super(context, cursor);

                    idIndex = cursor.getColumnIndexOrThrow(AlarmDatabase.KEY_ID);
                  typeIndex = cursor.getColumnIndexOrThrow(AlarmDatabase.KEY_TYPE);
             thresholdIndex = cursor.getColumnIndexOrThrow(AlarmDatabase.KEY_THRESHOLD);
               enabledIndex = cursor.getColumnIndexOrThrow(AlarmDatabase.KEY_ENABLED);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.alarm_item , parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            final  TextView summary_tv  = (TextView)       view.findViewById(R.id.alarm_summary);
            final      View summary_box =                  view.findViewById(R.id.alarm_summary_box);
            final      View indicator   =                  view.findViewById(R.id.indicator);
            final ImageView barOnOff    = (ImageView) indicator.findViewById(R.id.bar_onoff);
            //final  CheckBox clockOnOff = (CheckBox)  indicator.findViewById(R.id.clock_onoff);

            final int    id = cursor.getInt(idIndex);
            int        type = cursor.getInt(typeIndex);
            int   threshold = cursor.getInt(thresholdIndex);
            Boolean enabled = (cursor.getInt(enabledIndex) == 1);

            barOnOff.setImageResource(enabled ? R.drawable.ic_indicator_on : R.drawable.ic_indicator_off);
            //clockOnOff.setChecked(enabled);

            indicator.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    //clockOnOff.toggle();
                    //Boolean enabled = clockOnOff.isChecked();
                    Boolean enabled = alarms.getEnabledness(id);
                    alarms.setEnabledness(id, ! enabled);

                    barOnOff.setImageResource(! enabled ? R.drawable.ic_indicator_on : R.drawable.ic_indicator_off);
                }
            });

            summary_box.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    System.out.println(".................................. Creating context menu for id = " + id);
                    getMenuInflater().inflate(R.menu.alarm_item_context, menu);
                    curId = id;
                }
            });

            summary_box.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    System.out.println(".................................. Pressed: id = " + id);
                    //alarms.deleteAlarm(id);
                    //mCursor.requery();
                }
            });

            String s = str.alarm_types_display[type];

            if (str.alarm_type_values[type].equals("temp_rises")) {
                s += " " + threshold + str.degree_symbol + "C";
                // TODO: Convert to F if pref is to do so
            }
            if (str.alarm_type_values[type].equals("charge_rises") ||
                str.alarm_type_values[type].equals("charge_drops")) {
                s += " " + threshold + "%";
            }

            summary_tv.setText(s);
        }
    }

    //public void onItemClick(AdapterView parent, View v, int pos, long id) {
        /*Intent intent = new Intent(this, SetAlarm.class);
        intent.putExtra(Alarms.ALARM_ID, (int) id);
        startActivity(intent);*/
    //}
}
