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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class LogViewActivity extends ListActivity {
    private LogDatabase logs;
    private Resources res;
    private Context context;
    private SharedPreferences settings;
    private Str str;
    private Col col;
    private Cursor completeCursor;
    private Cursor filteredCursor;
    private LayoutInflater mInflater;
    private LogAdapter mAdapter;
    private TextView header_text;
    private Boolean reversed = false;
    private Boolean convertF;

    private static final int DIALOG_CONFIRM_CLEAR_LOGS   = 0;
    private static final int DIALOG_CONFIGURE_LOG_FILTER = 1;

    private static final String[] CSV_ORDER = {LogDatabase.KEY_TIME, LogDatabase.KEY_STATUS_CODE, LogDatabase.KEY_CHARGE, 
                                               LogDatabase.KEY_TEMPERATURE, LogDatabase.KEY_VOLTAGE};

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

        // Stranglely disabled by default for API level 14+
        if (res.getBoolean(R.bool.api_level_14_plus))
            getActionBar().setHomeButtonEnabled(true);

        setWindowSubtitle(res.getString(R.string.log_view_activity_subtitle));

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        convertF = settings.getBoolean(SettingsActivity.KEY_CONVERT_F, false);
        str = new Str(res);
        col = new Col();

        if (res.getBoolean(R.bool.override_list_activity_layout)) {
            setContentView(R.layout.list_activity);
            getListView().setDivider(res.getDrawable(R.drawable.my_divider));
        }

        View logs_header = View.inflate(context, R.layout.logs_header, null);
        getListView().addHeaderView(logs_header, null, false);
        header_text = (TextView) logs_header.findViewById(R.id.header_text);

        logs = new LogDatabase(context);
        completeCursor = logs.getAllLogs(false);
        startManagingCursor(completeCursor);
        filteredCursor = new FilteredCursor(completeCursor);

        mInflater = LayoutInflater.from(this);

        mAdapter = new LogAdapter(context, filteredCursor);
        setListAdapter(mAdapter);

        setHeaderText();
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
        completeCursor.close();
        logs.close();
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
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
        case DIALOG_CONFIRM_CLEAR_LOGS:
            builder.setTitle(str.confirm_clear_logs)
                .setPositiveButton(str.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            logs.clearAllLogs();
                            reloadList(false);

                            di.cancel();
                        }
                    })
                .setNegativeButton(str.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            di.cancel();
                        }
                    });

            dialog = builder.create();
            break;
        case DIALOG_CONFIGURE_LOG_FILTER:
            final String[] log_filter_pref_keys = res.getStringArray(R.array.log_filter_pref_keys);
            final boolean[] checked_items = new boolean[log_filter_pref_keys.length];

            for (int i = 0; i < checked_items.length; i++) {
                checked_items[i] = settings.getBoolean(log_filter_pref_keys[i], true);
            }

            builder.setTitle(str.configure_log_filter)
                .setMultiChoiceItems(R.array.log_filters, checked_items,
                                     new DialogInterface.OnMultiChoiceClickListener() {
                                         @Override
                                         public void onClick(DialogInterface di, int id, boolean isChecked) {
                                             SharedPreferences.Editor editor = settings.edit();

                                             if (isChecked) {
                                                 editor.putBoolean(log_filter_pref_keys[id], true);
                                             } else {
                                                 editor.putBoolean(log_filter_pref_keys[id], false);
                                             }

                                             editor.commit();
                                         }
                                     })
                .setPositiveButton(str.okay, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            reloadList(false);

                            di.cancel();
                        }
                    })
                .setNegativeButton(str.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            di.cancel();
                        }
                    });

            dialog = builder.create();
            break;
        default:
            dialog = null;
        }

        return dialog;
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

        switch (filteredCursor.getCount()) {
        case 0:
            menu.findItem(R.id.menu_clear).setEnabled(false);
            menu.findItem(R.id.menu_export).setEnabled(false);
            menu.findItem(R.id.menu_reverse).setEnabled(false);
            break;
        case 1:
            menu.findItem(R.id.menu_clear).setEnabled(true);
            menu.findItem(R.id.menu_export).setEnabled(true);
            menu.findItem(R.id.menu_reverse).setEnabled(false);
            break;
        default:
            menu.findItem(R.id.menu_clear).setEnabled(true);
            menu.findItem(R.id.menu_export).setEnabled(true);
            menu.findItem(R.id.menu_reverse).setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_clear:
            showDialog(DIALOG_CONFIRM_CLEAR_LOGS);

            return true;
        case R.id.menu_log_filter:
            showDialog(DIALOG_CONFIGURE_LOG_FILTER);

            return true;
        case R.id.menu_export:
            exportCSV();

            return true;
        case R.id.menu_reverse:
            reversed = (reversed) ? false : true;
            reloadList(true);

            return true;
        case android.R.id.home:
            startActivity(new Intent(this, BatteryIndicator.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void reloadList(Boolean newQuery){
        if (newQuery) {
            stopManagingCursor(completeCursor);
            completeCursor = logs.getAllLogs(reversed);
            startManagingCursor(completeCursor);
            filteredCursor = new FilteredCursor(completeCursor);

            mAdapter.changeCursor(filteredCursor);
        } else {
            filteredCursor.requery();
        }

        setHeaderText();
    }

    private void setHeaderText() {
        int count = filteredCursor.getCount();

        if (count == 0)
            header_text.setText(str.logs_empty);
        else
            header_text.setText(str.n_log_items(count));
    }

    private void exportCSV() {
        String state = Environment.getExternalStorageState();

        if (state != null && state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            Toast.makeText(context, str.read_only_storage, Toast.LENGTH_SHORT).show();
            return;
        } else if (state == null || !state.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(context, str.inaccessible_w_reason + state, Toast.LENGTH_SHORT).show();
            return;
        }

        Date d = new Date();
        String csvFileName = "BatteryIndicatorPro-Logs-" + d.getTime() + ".csv";

        File root    = Environment.getExternalStorageDirectory();
        File csvFile = new File(root, csvFileName);

        String[] csvFields = {str.date, str.time, str.status, str.charge, str.temperature, str.voltage};

        try {
            if (!csvFile.createNewFile() || !csvFile.canWrite()) {
                Toast.makeText(context, str.inaccessible_storage, Toast.LENGTH_SHORT).show();
                return;
            }

            BufferedWriter buf = new BufferedWriter(new FileWriter(csvFile));

            int cols = csvFields.length;
            int i;
            for (i = 0; i < cols; i++) {
                buf.write(csvFields[i]);
                if (i != cols - 1) buf.write(",");
            }
            buf.write("\r\n");

            int statusCode;
            int[] statusCodes;
            int status, plugged, status_age;
            String s;

            for (completeCursor.moveToFirst(); !completeCursor.isAfterLast(); completeCursor.moveToNext()) {
                cols = CSV_ORDER.length;
                for (i = 0; i < cols; i++) {
                    if (CSV_ORDER[i].equals(LogDatabase.KEY_TIME)) {
                        d.setTime(completeCursor.getLong(mAdapter.timeIndex));
                        buf.write(mAdapter.dateFormat.format(d) + "," + mAdapter.timeFormat.format(d) + ",");
                    } else if (CSV_ORDER[i].equals(LogDatabase.KEY_STATUS_CODE)) {
                        statusCode  = completeCursor.getInt(mAdapter.statusCodeIndex);
                        statusCodes = LogDatabase.decodeStatus(statusCode);
                        status      = statusCodes[0];
                        plugged     = statusCodes[1];
                        status_age  = statusCodes[2];

                        if (status_age == LogDatabase.STATUS_OLD)
                            s = str.log_statuses_old[status];
                        else
                            s = str.log_statuses[status];
                        if (plugged > 0)
                            s += " " + str.pluggeds[plugged];

                        buf.write(s + ",");
                    } else if (CSV_ORDER[i].equals(LogDatabase.KEY_CHARGE)) {
                        buf.write(String.valueOf(completeCursor.getInt(mAdapter.chargeIndex)) + ",");
                    } else if (CSV_ORDER[i].equals(LogDatabase.KEY_TEMPERATURE)) {
                        buf.write(String.valueOf(completeCursor.getInt(mAdapter.temperatureIndex) / 10.0) + ",");
                    } else if (CSV_ORDER[i].equals(LogDatabase.KEY_VOLTAGE)) {
                        buf.write(String.valueOf(completeCursor.getInt(mAdapter.voltageIndex) / 1000.0));
                    }
                }
                buf.write("\r\n");
            }
            buf.close();
        } catch (Exception e) {
            Toast.makeText(context, str.inaccessible_storage, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, str.file_written, Toast.LENGTH_SHORT).show();
    }

    // Based on http://stackoverflow.com/a/7343721/1427098
    private class FilteredCursor extends CursorWrapper {
        private Cursor wrappedCursor;

        private ArrayList<Integer> shownIDs;
        private int len;
        private int pos;

        public FilteredCursor(Cursor cursor) {
            super(cursor);

            shownIDs = new ArrayList<Integer>();
            wrappedCursor = cursor;

            refilter();
        }

        public void refilter() {
            if (wrappedCursor.isClosed()) return;

            shownIDs.clear();

            int wrappedCursorPos = wrappedCursor.getPosition();
            int statusCodeIndex = wrappedCursor.getColumnIndexOrThrow(LogDatabase.KEY_STATUS_CODE);

            boolean show_plugged_in    = settings.getBoolean("plugged_in",    true);
            boolean show_unplugged     = settings.getBoolean("unplugged",     true);
            boolean show_charging      = settings.getBoolean("charging",      true);
            boolean show_discharging   = settings.getBoolean("discharging",   true);
            boolean show_fully_charged = settings.getBoolean("fully_charged", true);
            boolean show_unknown       = settings.getBoolean("unknown",       true);

            for (wrappedCursor.moveToFirst(); !wrappedCursor.isAfterLast(); wrappedCursor.moveToNext()) {
                int statusCode    = wrappedCursor.getInt(statusCodeIndex);
                int[] statusCodes = LogDatabase.decodeStatus(statusCode);
                int status        = statusCodes[0];
                int plugged       = statusCodes[1];
                int status_age    = statusCodes[2];

                if (status == BatteryIndicatorService.STATUS_FULLY_CHARGED && show_fully_charged) {
                    shownIDs.add(wrappedCursor.getPosition());
                } else if ((status == BatteryIndicatorService.STATUS_UNKNOWN ||
                            status == BatteryIndicatorService.STATUS_DISCHARGING ||
                            status == BatteryIndicatorService.STATUS_NOT_CHARGING ||
                            status > BatteryIndicatorService.STATUS_MAX) &&
                           show_unknown) {
                    shownIDs.add(wrappedCursor.getPosition());
                } else if (status_age == LogDatabase.STATUS_OLD) {
                    if ((status == BatteryIndicatorService.STATUS_UNPLUGGED && show_discharging) ||
                        (status == BatteryIndicatorService.STATUS_CHARGING  && show_charging))
                        shownIDs.add(wrappedCursor.getPosition());
                } else if (status_age == LogDatabase.STATUS_NEW) {
                    if ((status == BatteryIndicatorService.STATUS_UNPLUGGED && show_unplugged) ||
                        (status == BatteryIndicatorService.STATUS_CHARGING  && show_plugged_in))
                        shownIDs.add(wrappedCursor.getPosition());
                }
            }

            wrappedCursor.moveToPosition(wrappedCursorPos);

            len = shownIDs.size();
            pos = -1;
        }

        @Override
        public boolean requery() {
            boolean ret = super.requery();
            refilter();
            return ret;
        }

        @Override
        public int getCount() {
            return len;
        }

        @Override
        public boolean moveToPosition(int newPos) {
            boolean moved = super.moveToPosition(shownIDs.get(newPos));

            if (moved) pos = newPos;

            return moved;
        }

        @Override
        public final boolean move(int offset) {
            return moveToPosition(pos + offset);
        }

        @Override
        public final boolean moveToFirst() {
            return moveToPosition(0);
        }

        @Override
        public final boolean moveToLast() {
            return moveToPosition(len - 1);
        }

        @Override
        public final boolean moveToNext() {
            return moveToPosition(pos + 1);
        }

        @Override
        public final boolean moveToPrevious() {
            return moveToPosition(pos - 1);
        }

        @Override
        public final boolean isFirst() {
            return len != 0 && pos == 0;
        }

        @Override
        public final boolean isLast() {
            return len != 0 && pos == len - 1;
        }

        @Override
        public final boolean isBeforeFirst() {
            return len == 0 || pos == -1;
        }

        @Override
        public final boolean isAfterLast() {
            return len == 0 || pos == len;
        }

        @Override
        public int getPosition() {
            return pos;
        }
    }

    private class LogAdapter extends CursorAdapter {
        public int statusCodeIndex, chargeIndex, timeIndex, temperatureIndex, voltageIndex;
        public DateFormat dateFormat, timeFormat;

        private Date d = new Date();

        public LogAdapter(Context context, Cursor cursor) {
            super(context, cursor);

            dateFormat = android.text.format.DateFormat.getDateFormat(context);
            timeFormat = android.text.format.DateFormat.getTimeFormat(context);

             statusCodeIndex = cursor.getColumnIndexOrThrow(LogDatabase.KEY_STATUS_CODE);
                 chargeIndex = cursor.getColumnIndexOrThrow(LogDatabase.KEY_CHARGE);
                   timeIndex = cursor.getColumnIndexOrThrow(LogDatabase.KEY_TIME);
            temperatureIndex = cursor.getColumnIndexOrThrow(LogDatabase.KEY_TEMPERATURE);
                voltageIndex = cursor.getColumnIndexOrThrow(LogDatabase.KEY_VOLTAGE);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.log_item , parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            TextView    status_tv = (TextView) view.findViewById(R.id.status);
            TextView   percent_tv = (TextView) view.findViewById(R.id.percent);
            TextView      time_tv = (TextView) view.findViewById(R.id.time);
            TextView temp_volt_tv = (TextView) view.findViewById(R.id.temp_volt);

            int statusCode = cursor.getInt(statusCodeIndex);
            int[] statusCodes = LogDatabase.decodeStatus(statusCode);
            int status     = statusCodes[0];
            int plugged    = statusCodes[1];
            int status_age = statusCodes[2];

            String s;

            if (status_age == LogDatabase.STATUS_OLD) {
                 status_tv.setTextColor(col.old_status);
                percent_tv.setTextColor(col.old_status);
                s = str.log_statuses_old[status];
            } else {
                switch (status) {
                case 5:
                     status_tv.setTextColor(col.charged);
                    percent_tv.setTextColor(col.charged);
                    break;
                case 0:
                     status_tv.setTextColor(col.unplugged);
                    percent_tv.setTextColor(col.unplugged);
                    break;
                case 2:
                default:
                     status_tv.setTextColor(col.plugged);
                    percent_tv.setTextColor(col.plugged);
                }

                s = str.log_statuses[status];
            }

            if (plugged > 0)
                s += " " + str.pluggeds[plugged];

            status_tv.setText(s);

            percent_tv.setText("" + cursor.getInt(chargeIndex) + "%");

            d.setTime(cursor.getLong(timeIndex));
            time_tv.setText(dateFormat.format(d) + "  " + timeFormat.format(d));

            int temperature = cursor.getInt(temperatureIndex);
            if (temperature != 0) temp_volt_tv.setText("" + str.formatTemp(temperature, convertF));
            else temp_volt_tv.setText(""); /* TextViews are reused */

            int voltage = cursor.getInt(voltageIndex);
            if (voltage != 0) temp_volt_tv.setText(((String) temp_volt_tv.getText()) + " / " + str.formatVoltage(voltage));
        }
    }

    private class Col {
        public int old_status;
        public int charged;
        public int plugged;
        public int unplugged;

        public Col() {
            old_status = res.getColor(R.color.old_status);
            charged    = res.getColor(R.color.charged);
            plugged    = res.getColor(R.color.plugged);
            unplugged  = res.getColor(R.color.unplugged);
        }
    }
}
