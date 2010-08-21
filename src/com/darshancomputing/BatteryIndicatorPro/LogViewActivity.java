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
//import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class LogViewActivity extends ListActivity {
    private LogDatabase logs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();

        logs = new LogDatabase(context);
        Cursor mCursor = logs.getAllLogs();
        startManagingCursor(mCursor);

        setListAdapter(new SimpleCursorAdapter(context, R.layout.log_item, mCursor,
                                               new String[] {LogDatabase.KEY_STATUS, LogDatabase.KEY_CHARGE},
                                               new int[]    {R.id.status, R.id.percent}));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
