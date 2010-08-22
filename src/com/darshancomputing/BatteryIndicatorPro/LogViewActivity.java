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
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import java.util.Date;

public class LogViewActivity extends ListActivity {
    private LogDatabase logs;
    private Resources res;
    private Context context;
    private Cursor mCursor;
    private SimpleCursorAdapter mAdapter;
    private Boolean reversed = false;

    private static final String[] KEYS = {LogDatabase.KEY_STATUS, LogDatabase.KEY_PLUGGED,
                                          LogDatabase.KEY_CHARGE, LogDatabase.KEY_TIME};
    private static final int[]     IDS = {R.id.status, R.id.plugged, R.id.percent, R.id.time};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        res = getResources();

        logs = new LogDatabase(context);
        mCursor = logs.getAllLogs(false);
        startManagingCursor(mCursor);

        mAdapter = new LogCursorAdapter(context, R.layout.log_item, mCursor, KEYS, IDS);
        setListAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_clear:
            //logs.clear();
            //reloadList();

            return true;
        case R.id.menu_reverse:
            reversed = (reversed) ? false : true;
            reloadList();

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void reloadList(){
        stopManagingCursor(mCursor);
        mCursor = logs.getAllLogs(reversed);
        startManagingCursor(mCursor);

        mAdapter.changeCursor(mCursor);
    }

    /* Extending SimpleCursorAdapter and overriding setViewText is the simplest way I could think
       of to post-process (interpret and format) the data from the cursor.  I have no idea if
       there's a better way that I'm missing or if this is the obvious and expected way to do so. */
    private class LogCursorAdapter extends SimpleCursorAdapter {
        public LogCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to){
            super(context, layout, c, from, to);
        }

        //@Override
        public void setViewText(TextView tv, String text){
            String[] statuses = res.getStringArray(R.array.log_statuses);
            String[] pluggeds = res.getStringArray(R.array.pluggeds);

            switch (tv.getId()) {
            case R.id.status:
                int status = Integer.valueOf(text);
                tv.setText(statuses[status]);
                TextView percent_tv = getSibling(tv, R.id.percent);

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

                break;
            case R.id.plugged:
                int plugged = Integer.valueOf(text);
                if (plugged < 1) return;

                TextView status_tv = getSibling(tv, R.id.status);
                status_tv.setText(status_tv.getText() + " " + pluggeds[plugged]);

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
            return android.text.format.DateFormat.getDateFormat(context).format(d) + " "
                 + android.text.format.DateFormat.getTimeFormat(context).format(d);
        }
    }
}
