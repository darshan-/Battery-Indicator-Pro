/*
    Copyright (c) 2010-2015 Darshan-Josiah Barber

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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AlarmsActivity extends Activity {
    private AlarmDatabase alarms;
    private Resources res;
    private Context context;
    private Str str;
    private Cursor mCursor;
    private LayoutInflater mInflater;
    private LinearLayout mAlarmsList;

    private int curId; /* The alarm id for the View that was just long-pressed */
    private int curIndex; /* The ViewGroup index of the currently focused item (to set focus after deletion) */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        res = getResources();
        str = new Str(res);

        // Stranglely disabled by default for API level 14+
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.alarms);
        setWindowSubtitle(res.getString(R.string.alarm_settings));

        alarms = new AlarmDatabase(context);
        mCursor = alarms.getAllAlarms(true);

        mInflater = LayoutInflater.from(context);
        mAlarmsList = (LinearLayout) findViewById(R.id.alarms_list);
        mCursor.registerDataSetObserver(new AlarmsObserver());

        View addAlarm = findViewById(R.id.add_alarm);
        addAlarm.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                int id = alarms.addAlarm();
                if (id < 0) {
                    Toast.makeText(context, "Error!", Toast.LENGTH_SHORT).show();
                }
                ComponentName comp = new ComponentName(getPackageName(), AlarmEditActivity.class.getName());
                startActivity(new Intent().setComponent(comp).putExtra(AlarmEditActivity.EXTRA_ALARM_ID, id));
            }
        });
    }

    private void setWindowSubtitle(String subtitle) {
        if (res.getBoolean(R.bool.long_activity_names))
            setTitle(res.getString(R.string.app_full_name) + " - " + subtitle);
        else
            setTitle(subtitle);
    }

    private void populateList() {
        mAlarmsList.removeAllViews();

        if (mCursor.moveToFirst()) {
            while (! mCursor.isAfterLast()) {
                View v = mInflater.inflate(R.layout.alarm_item, mAlarmsList, false);
                bindView(v);
                mAlarmsList.addView(v, mAlarmsList.getChildCount());
                mCursor.moveToNext();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCursor.close();
        alarms.close();
        mAlarmsList.removeAllViews(); // Don't want any instance state saved
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCursor.requery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCursor.deactivate();
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
            Intent intent = new Intent().setComponent(comp).putExtra(SettingsActivity.EXTRA_SCREEN, SettingsActivity.KEY_ALARMS_SETTINGS);
            startActivity(intent);

            return true;
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_alarm:
                alarms.deleteAlarm(curId);
                mCursor.requery();

                int childCount = mAlarmsList.getChildCount();
                if (curIndex < childCount)
                    mAlarmsList.getChildAt(curIndex).findViewById(R.id.alarm_summary_box).requestFocus();
                else if (childCount > 0)
                    mAlarmsList.getChildAt(curIndex - 1).findViewById(R.id.alarm_summary_box).requestFocus();

                return true;
            default:
                break;
        }

        return super.onContextItemSelected(item);
    }

    private class AlarmsObserver extends DataSetObserver {
        public AlarmsObserver(){
            super();
        }

        @Override
        public void onChanged() {
            super.onChanged();
            populateList();
        }
    }

    private void bindView(View view) {
        final  TextView summary_tv  = (TextView)       view.findViewById(R.id.alarm_summary);
        final      View summary_box =                  view.findViewById(R.id.alarm_summary_box);
        final CompoundButton toggle = (CompoundButton) view.findViewById(R.id.toggle);

        final int    id  = mCursor.getInt   (AlarmDatabase.INDEX_ID);
        String     type  = mCursor.getString(AlarmDatabase.INDEX_TYPE);
        String threshold = mCursor.getString(AlarmDatabase.INDEX_THRESHOLD);
        Boolean enabled  = (mCursor.getInt(AlarmDatabase.INDEX_ENABLED) == 1);

        String s = str.alarm_types_display[str.indexOf(str.alarm_type_values, type)];
        if (type.equals("temp_rises")) {
            Boolean convertF = android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(SettingsActivity.KEY_CONVERT_F, false);
            s += " " + str.formatTemp(Integer.valueOf(threshold), convertF, false);
        } else if (type.equals("charge_drops") || type.equals("charge_rises")) {
            s += " " + threshold + "%";
        }
        final String summary = s;

        summary_tv.setText(summary);

        toggle.setChecked(enabled);

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alarms.setEnabled(id, isChecked);
            }
        });

        summary_box.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                curId = id;
                curIndex = mAlarmsList.indexOfChild((View) v.getParent().getParent());

                getMenuInflater().inflate(R.menu.alarm_item_context, menu);
                menu.setHeaderTitle(summary);
            }
        });

        summary_box.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, android.view.KeyEvent event) {
                if (keyCode == event.KEYCODE_DPAD_CENTER && event.getAction() == event.ACTION_DOWN)
                    v.setPressed(true);

                return false;
            }
        });

        summary_box.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ComponentName comp = new ComponentName(getPackageName(), AlarmEditActivity.class.getName());
                startActivity(new Intent().setComponent(comp).putExtra(AlarmEditActivity.EXTRA_ALARM_ID, id));
            }
        });
    }
}
