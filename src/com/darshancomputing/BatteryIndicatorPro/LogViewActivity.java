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
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class LogViewActivity extends ListActivity {
    private LogDatabase logs;
    private Resources   res;

    private static final String[] KEYS = {LogDatabase.KEY_STATUS, LogDatabase.KEY_PLUGGED, LogDatabase.KEY_CHARGE};
    private static final int[]     IDS = {R.id.status, R.id.plugged, R.id.percent};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        res = getResources();

        logs = new LogDatabase(context);
        Cursor mCursor = logs.getAllLogs();
        startManagingCursor(mCursor);

        setListAdapter(new LogCursorAdapter(context, R.layout.log_item, mCursor, KEYS, IDS));
    }

    @Override
    protected void onResume() {
        super.onResume();
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

            if (tv.getId() == R.id.status ) {
                tv.setText(statuses[Integer.valueOf(text)]);
            } else if (tv.getId() == R.id.plugged) {
                System.out.println("...................................................... " + text);
                int plugged = Integer.valueOf(text);
                if (plugged < 1) return;

                TextView status = (TextView) ((ViewGroup) tv.getParent()).findViewById(R.id.status);
                status.setText(status.getText() + " " + pluggeds[plugged]);
            } else if (tv.getId() == R.id.percent) {
                tv.setText(text + "%");
            } else {
                tv.setText(text);
            }
        }
    }
}
