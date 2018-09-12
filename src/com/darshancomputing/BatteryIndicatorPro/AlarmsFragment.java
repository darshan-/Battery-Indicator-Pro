/*
    Copyright (c) 2010-2018 Darshan-Josiah Barber

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
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.app.Fragment;

public class AlarmsFragment extends Fragment {
    private static PersistentFragment pfrag;
    private AlarmDatabase alarms;
    //private Resources res;
    //private Str str;
    private Cursor mCursor;
    private LayoutInflater mInflater;
    private LinearLayout mAlarmsList;
    private Boolean convertF;

    private int curId; /* The alarm id for the View that was just long-pressed */
    private int curIndex; /* The ViewGroup index of the currently focused item (to set focus after deletion) */

    private NotificationManager mNotificationManager;
    private NotificationChannel alarmChan;
    private boolean appNotifsEnabled;
    private boolean alarmNotifsEnabled;

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        alarmChan = mNotificationManager.getNotificationChannel(BatteryInfoService.ALARM_CHAN_ID);

        appNotifsEnabled = mNotificationManager.areNotificationsEnabled();
        alarmNotifsEnabled = alarmChan == null || alarmChan.getImportance() > 0; // null okay, just not created yet, so not blocked

        if (!appNotifsEnabled || !alarmNotifsEnabled) {
            mInflater = inflater;
            View view = mInflater.inflate(R.layout.alarms_no_notifs, container, false);

            Button b = view.findViewById(R.id.enable_notifs_button);
            TextView tv = view.findViewById(R.id.enable_notifs_summary);

            if (!appNotifsEnabled) {
                b.setText(R.string.app_notifs_disabled_b);
                tv.setText(R.string.app_notifs_alarms_disabled_summary);
            } else {
                b.setText(R.string.alarm_notifs_disabled_b);
                tv.setText(R.string.alarm_notifs_disabled_summary);
            }

            b.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Intent intent;

                    if (!appNotifsEnabled) {
                        intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    } else {
                        intent = new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                        intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, alarmChan.getId());
                    }

                    intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    startActivity(intent);
                }
            });

            return view;
        }

        mInflater = inflater;
        View view = mInflater.inflate(R.layout.alarms, container, false);

        mAlarmsList = (LinearLayout) view.findViewById(R.id.alarms_list);

        if (mCursor == null) {
            TextView addAlarmTv = (TextView) view.findViewById(R.id.add_alarm_tv);
            addAlarmTv.setText("Database error!");
            return view;
        }

        view.findViewById(R.id.add_alarm).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                int id = alarms.addAlarm();
                if (id < 0) {
                    Toast.makeText(getActivity(), "Error!", Toast.LENGTH_SHORT).show();
                }
                ComponentName comp = new ComponentName(getActivity().getPackageName(), AlarmEditActivity.class.getName());
                startActivity(new Intent().setComponent(comp).putExtra(AlarmEditActivity.EXTRA_ALARM_ID, id));
            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        //setRetainInstance(true);

        alarms = new AlarmDatabase(getActivity().getApplicationContext());
        mCursor = alarms.getAllAlarms(true);

        if (mCursor != null)
            mCursor.registerDataSetObserver(new AlarmsObserver());

        pfrag = PersistentFragment.getInstance(getFragmentManager());
        convertF = pfrag.settings.getBoolean(SettingsActivity.KEY_CONVERT_F,
                                             pfrag.res.getBoolean(R.bool.default_convert_to_fahrenheit));
    }

    private void populateList() {
        if (!appNotifsEnabled || !alarmNotifsEnabled) return;

        mAlarmsList.removeAllViews();

        if (mCursor != null && mCursor.moveToFirst()) {
            while (! mCursor.isAfterLast()) {
                View v = mInflater.inflate(R.layout.alarm_item, mAlarmsList, false);
                bindView(v);
                mAlarmsList.addView(v, mAlarmsList.getChildCount());
                mCursor.moveToNext();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCursor != null) mCursor.close();
        alarms.close();
        if (mAlarmsList != null) // onCreateView may not have been called yet (e.g. if pager is at position 0)
            mAlarmsList.removeAllViews(); // Don't want any instance state saved
    }

    @Override
    public void onResume() {
        super.onResume();

        // Turns out alarmChan is unchangeable, so getImportance() just returns the importance at the time getNotificationChannel was called
        alarmChan = mNotificationManager.getNotificationChannel(BatteryInfoService.ALARM_CHAN_ID);

        if (appNotifsEnabled != mNotificationManager.areNotificationsEnabled() ||
            alarmNotifsEnabled != (alarmChan == null || alarmChan.getImportance() > 0)) {
            Intent intent = new Intent(getActivity(), BatteryInfoActivity.class).putExtra(BatteryInfoService.EXTRA_EDIT_ALARMS, true);
            startActivity(intent);
            getActivity().finish();
            return;
        }

        convertF = pfrag.settings.getBoolean(SettingsActivity.KEY_CONVERT_F,
                                             pfrag.res.getBoolean(R.bool.default_convert_to_fahrenheit));

        if (mCursor != null) mCursor.requery();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCursor != null) mCursor.deactivate();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.alarms, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.menu_help:
            ComponentName comp = new ComponentName(getActivity().getPackageName(), SettingsHelpActivity.class.getName());
            intent = new Intent().setComponent(comp).putExtra(SettingsActivity.EXTRA_SCREEN, SettingsActivity.KEY_ALARMS_SETTINGS);
            startActivity(intent);

            return true;
        case R.id.menu_alarm_channel:
            intent = new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, alarmChan.getId());
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);

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
                if (mCursor != null) mCursor.requery();

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
        AlarmsObserver(){
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

        final int     id = mCursor.getInt   (mCursor.getColumnIndex(AlarmDatabase.KEY_ID));
        String      type = mCursor.getString(mCursor.getColumnIndex(AlarmDatabase.KEY_TYPE));
        String threshold = mCursor.getString(mCursor.getColumnIndex(AlarmDatabase.KEY_THRESHOLD));
        Boolean  enabled = (mCursor.getInt(mCursor.getColumnIndex(AlarmDatabase.KEY_ENABLED)) == 1);

        String s = Str.alarm_types_display[Str.indexOf(Str.alarm_type_values, type)];
        if (type.equals("temp_drops") || type.equals("temp_rises")) {
            s += " " + Str.formatTemp(Integer.valueOf(threshold), convertF, false);
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

                getActivity().getMenuInflater().inflate(R.menu.alarm_item_context, menu);
                menu.setHeaderTitle(summary);
            }
        });

        summary_box.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, android.view.KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction() == KeyEvent.ACTION_DOWN)
                    v.setPressed(true);

                return false;
            }
        });

        summary_box.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ComponentName comp = new ComponentName(getActivity().getPackageName(), AlarmEditActivity.class.getName());
                startActivity(new Intent().setComponent(comp).putExtra(AlarmEditActivity.EXTRA_ALARM_ID, id));
            }
        });
    }
}
