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

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import java.util.Date;

public class LogViewActivity extends ListActivity {
    private LogDatabase logs;
    private Resources res;
    private Context context;
    private Str str;
    private Cursor mCursor;
    private LogCursorAdapter mAdapter;
    private TextView logs_header;
    private Boolean reversed = false;

    private static final String[] KEYS = {LogDatabase.KEY_STATUS_CODE, LogDatabase.KEY_CHARGE, LogDatabase.KEY_TIME};
    private static final int[]     IDS = {R.id.status, R.id.percent, R.id.time};

    private static final IntentFilter batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private final Handler mHandler = new Handler();
    private final Runnable mUpdateStatus = new Runnable() {
        public void run() {
            reloadList(false);
        }
    };

    private final BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (! Intent.ACTION_BATTERY_CHANGED.equals(action)) return;

            /* Give the service a couple seconds to process the update */
            mHandler.postDelayed(mUpdateStatus, 2 * 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        res = getResources();
        str = new Str();

        logs_header = (TextView) View.inflate(context, R.layout.logs_header, null);
        getListView().addHeaderView(logs_header);

        logs = new LogDatabase(context);
        mCursor = logs.getAllLogs(false);
        startManagingCursor(mCursor);

        mAdapter = new LogCursorAdapter(context, R.layout.log_item, mCursor, KEYS, IDS);
        setListAdapter(mAdapter);

        setHeaderText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBatteryInfoReceiver, batteryChangedFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBatteryInfoReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logs, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        switch (mCursor.getCount()) {
        case 0:
            menu.findItem(R.id.menu_clear).setEnabled(false);
            menu.findItem(R.id.menu_reverse).setEnabled(false);
            break;
        case 1:
            menu.findItem(R.id.menu_clear).setEnabled(true);
            menu.findItem(R.id.menu_reverse).setEnabled(false);
            break;
        default:
            menu.findItem(R.id.menu_clear).setEnabled(true);
            menu.findItem(R.id.menu_reverse).setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_clear:
            logs.clearAllLogs();
            reloadList(false);

            return true;
        case R.id.menu_reverse:
            reversed = (reversed) ? false : true;
            reloadList(true);

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void reloadList(Boolean newQuery){
        if (newQuery) {
            stopManagingCursor(mCursor);
            mCursor = logs.getAllLogs(reversed);
            startManagingCursor(mCursor);

            mAdapter.changeCursor(mCursor);
        } else {
            mCursor.requery();
        }

        setHeaderText();
    }

    private void setHeaderText() {
        int count = mCursor.getCount();

        if (count == 0)
            logs_header.setText(str.logs_empty);
        else
            logs_header.setText(str.n_log_items(count));
    }

    private class LogCursorAdapter extends SimpleCursorAdapter {
        public LogCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to){
            super(context, layout, c, from, to);
        }

        @Override
        public void setViewText(TextView tv, String text){
            switch (tv.getId()) {
            case R.id.status:
                int[] statusCodes = LogDatabase.decodeStatus(Integer.valueOf(text));
                int status     = statusCodes[0];
                int plugged    = statusCodes[1];
                int status_age = statusCodes[2];

                TextView percent_tv = getSibling(tv, R.id.percent);
                String s;

                if (status_age == LogDatabase.STATUS_OLD) {
                            tv.setTextColor(res.getColor(R.color.old_status));
                    percent_tv.setTextColor(res.getColor(R.color.old_status));
                    s = str.statuses_old[status];
                } else {
                    switch (status) {
                    case 5:
                                tv.setTextColor(res.getColor(R.color.charged));
                        percent_tv.setTextColor(res.getColor(R.color.charged));
                        break;
                    case 0:
                                tv.setTextColor(res.getColor(R.color.unplugged));
                        percent_tv.setTextColor(res.getColor(R.color.unplugged));
                        break;
                    case 2:
                    default:
                        tv.setTextColor(res.getColor(R.color.plugged));
                        percent_tv.setTextColor(res.getColor(R.color.plugged));
                    }

                    s = str.statuses[status];
                }

                if (plugged > 0)
                    s += " " + str.pluggeds[plugged];

                tv.setText(s);
                break;
            case R.id.percent:
                tv.setText(text + "%");
                break;
            case R.id.time:
                tv.setText(formatTime(new Date(Long.valueOf(text))));
                break;
            default:
                tv.setText(text);
            }
        }

        private TextView getSibling(TextView myself, int siblingId) {
            return (TextView) ((ViewGroup) myself.getParent()).findViewById(siblingId);
        }

        private String formatTime(Date d) {
            return android.text.format.DateFormat.getDateFormat(context).format(d) + "  "
                 + android.text.format.DateFormat.getTimeFormat(context).format(d);
        }
    }

    private class Str {
        public String logs_empty;

        private String[] pluggeds;
        private String[] statuses;
        private String[] statuses_old;

        public Str() {
            logs_empty = res.getString(R.string.logs_empty);

            pluggeds     = res.getStringArray(R.array.pluggeds);
            statuses     = res.getStringArray(R.array.log_statuses);
            statuses_old = res.getStringArray(R.array.log_statuses_old);
        }

        public String n_log_items(int n) {
            return String.format(res.getQuantityString(R.plurals.n_log_items, n), n);
        }
    }
}
