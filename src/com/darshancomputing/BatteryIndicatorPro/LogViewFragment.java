/*
    Copyright (c) 2010-2021 Darshan Computing, LLC

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.ListFragment;

public class LogViewFragment extends ListFragment {
    private static PersistentFragment pfrag;
    public LogDatabase logs;
    private Col col;
    private Cursor completeCursor;
    private Cursor filteredCursor;
    private Cursor timeDeltaCursor;
    private LayoutInflater mInflater;
    private LogAdapter mAdapter;
    private TextView header_text;
    private Boolean convertF;

    private boolean reversed;
    private boolean noDB;

    //private static final String LOG_TAG = "BatteryBot";

    private static final String[] CSV_ORDER = {LogDatabase.KEY_TIME, LogDatabase.KEY_STATUS_CODE, LogDatabase.KEY_CHARGE, 
                                               LogDatabase.KEY_TEMPERATURE, LogDatabase.KEY_VOLTAGE};

    private static final String KEY_SHOW_SECONDS = "show_seconds";

    private static final int CREATE_CSV_FILE = 1;

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mInflater = inflater;

        View logs_header = View.inflate(getActivity(), R.layout.logs_header, null);
        header_text = (TextView) logs_header.findViewById(R.id.header_text);
        ListView lv = (ListView) view.findViewById(android.R.id.list);
        lv.addHeaderView(logs_header, null, false);
        lv.setFastScrollEnabled(true);
        if (noDB)
            return view;
        setHeaderText();
        setListAdapter(mAdapter);

        return view;
    }

    @Override
    public void onAttach(android.app.Activity a) {
        super.onAttach(a);

        pfrag = PersistentFragment.getInstance(getFragmentManager());

        logs = new LogDatabase(getActivity().getApplicationContext());
        completeCursor = logs.getAllLogs(false);

        if (completeCursor == null) {
            noDB = true;
            return;
        }

        timeDeltaCursor = new TimeDeltaCursor(completeCursor);
        filteredCursor = new FilteredCursor(timeDeltaCursor);

        mAdapter = new LogAdapter(a, filteredCursor);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        convertF = pfrag.settings.getBoolean(SettingsFragment.KEY_CONVERT_F,
                                                pfrag.res.getBoolean(R.bool.default_convert_to_fahrenheit));
        col = new Col();

        if (! pfrag.sp_main.getBoolean("log_filters_migrated_to_sp_main", false))
            migrateFiltersToSpMain();
    }

    private void migrateFiltersToSpMain() {
        SharedPreferences.Editor spm_editor = pfrag.sp_main.edit();

        for (int i = 0; i < Str.log_filter_pref_keys.length; i++) {
            spm_editor.putBoolean(Str.log_filter_pref_keys[i],
                                  pfrag.settings.getBoolean(Str.log_filter_pref_keys[i], true));
        }

        spm_editor.putBoolean("log_filters_migrated_to_sp_main", true).apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (completeCursor != null)
            completeCursor.close();

        logs.close();
    }

    @Override
    public void onResume() {
        super.onResume();

        pfrag.setLVF(this);

        convertF = pfrag.settings.getBoolean(SettingsFragment.KEY_CONVERT_F,
                                             pfrag.res.getBoolean(R.bool.default_convert_to_fahrenheit));
    }

    @Override
    public void onPause() {
        super.onPause();

        pfrag.setLVF(null);
    }

    public static class ConfirmClearLogsDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                .setTitle(pfrag.res.getString(R.string.confirm_clear_logs))
                .setPositiveButton(pfrag.res.getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            LogViewFragment lvf = (LogViewFragment) getTargetFragment();
                            lvf.logs.clearAllLogs();
                            lvf.reloadList(false);
                            di.cancel();
                        }
                    })
                .setNegativeButton(pfrag.res.getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            di.cancel();
                        }
                    })
                .create();
        }
    }

    public static class ConfigureLogFilterDialogFragment extends DialogFragment {
        final boolean[] checked_items = new boolean[Str.log_filter_pref_keys.length];

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            for (int i = 0; i < checked_items.length; i++) {
                checked_items[i] = pfrag.sp_main.getBoolean(Str.log_filter_pref_keys[i], true);
            }

            return new AlertDialog.Builder(getActivity())
                .setTitle(pfrag.res.getString(R.string.configure_log_filter))
                .setMultiChoiceItems(R.array.log_filters, checked_items,
                                     new DialogInterface.OnMultiChoiceClickListener() {
                                         @Override
                                         public void onClick(DialogInterface di, int id, boolean isChecked) {
                                             checked_items[id] = isChecked;
                                             LogViewFragment lvf = (LogViewFragment) getTargetFragment();
                                             lvf.setFilters(checked_items);
                                         }
                                     })
                .setPositiveButton(pfrag.res.getString(R.string.okay),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            di.cancel();
                        }
                    })
                .create();
        }
    }

    private void setFilters(boolean[] checked_items) {
        SharedPreferences.Editor spm_editor = pfrag.sp_main.edit();

        for (int i = 0; i < checked_items.length; i++) {
            spm_editor.putBoolean(Str.log_filter_pref_keys[i], checked_items[i]);
        }

        spm_editor.apply();

        reloadList(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.logs, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // According to Play Developer Console, filteredCursor can be null here, even though it shouldn't be...
        //  I guess onPrepareOptionsMenu() can be called before onCreate()?
        if (filteredCursor == null) {
            menu.findItem(R.id.menu_clear).setEnabled(false);
            menu.findItem(R.id.menu_export).setEnabled(false);
            menu.findItem(R.id.menu_reverse).setEnabled(false);
            return;
        }

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

        if (pfrag.sp_main.getBoolean(KEY_SHOW_SECONDS, false)) {
            menu.findItem(R.id.menu_show_seconds).setVisible(false);
            menu.findItem(R.id.menu_hide_seconds).setVisible(true);
        } else {
            menu.findItem(R.id.menu_show_seconds).setVisible(true);
            menu.findItem(R.id.menu_hide_seconds).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DialogFragment df;
        SharedPreferences.Editor spm_editor;

        if (item.getItemId() == R.id.menu_clear) {
            df = new ConfirmClearLogsDialogFragment();
            df.setTargetFragment(this, 0);
            df.show(getFragmentManager(), "TODO: What is this string for?");

            return true;
        }

        if (item.getItemId() == R.id.menu_log_filter) {
            df = new ConfigureLogFilterDialogFragment();
            df.setTargetFragment(this, 0);
            df.show(getFragmentManager(), "TODO: What is this string for?2");

            return true;
        }

        if (item.getItemId() == R.id.menu_export) {
            exportCSV();

            return true;
        }

        if (item.getItemId() == R.id.menu_reverse) {
            reversed = !reversed;
            reloadList(true);

            return true;
        }

        if (item.getItemId() == R.id.menu_show_seconds) {
            spm_editor = pfrag.sp_main.edit();
            spm_editor.putBoolean(KEY_SHOW_SECONDS, true);
            spm_editor.apply();

            reloadList(false);

            return true;
        }

        if (item.getItemId() == R.id.menu_hide_seconds) {
            spm_editor = pfrag.sp_main.edit();
            spm_editor.putBoolean(KEY_SHOW_SECONDS, false);
            spm_editor.apply();

            reloadList(false);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void reloadList(Boolean newQuery){
        if (newQuery) {
            completeCursor.close();
            completeCursor = logs.getAllLogs(reversed);
            timeDeltaCursor = new TimeDeltaCursor(completeCursor);
            filteredCursor = new FilteredCursor(timeDeltaCursor);

            mAdapter.changeCursor(filteredCursor);
        } else {
            filteredCursor.requery();
            mAdapter.notifyDataSetChanged();
        }

        setHeaderText();
    }

    private void setHeaderText() {
        int count = filteredCursor.getCount();

        if (count == 0)
            header_text.setText(Str.logs_empty);
        else
            header_text.setText(Str.n_log_items(count));
    }

    public void batteryInfoUpdated() {
        reloadList(false);
    }

    private void createNewCSVFile() {
        String ts = new java.text.SimpleDateFormat("yyyy-MM-dd-HHmmss-SSS").format(new Date());
        String csvFileName = "BatteryBot_Pro-Logs-" + ts + ".csv";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, csvFileName);

        startActivityForResult(intent, CREATE_CSV_FILE);
    }


    private void exportCSV() {
        String state = Environment.getExternalStorageState();

        if (state != null && state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            Toast.makeText(getActivity(), Str.read_only_storage, Toast.LENGTH_SHORT).show();
            return;
        } else if (state == null || !state.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getActivity(), Str.inaccessible_w_reason + state, Toast.LENGTH_SHORT).show();
            return;
        }

        createNewCSVFile();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode != CREATE_CSV_FILE)
            return;

        if (resultData == null) {
            // User cancelled / backed out, rather than selecting a location.  Doing nothing is most user-friendly / expected thing.
            return;
        }

        Date d = new Date();
        Uri uri = resultData.getData();

        String[] csvFields = {Str.date, Str.time, Str.status, Str.charge,
                              Str.temperature, Str.temperature_f, Str.voltage};

        try {
            ParcelFileDescriptor pfd = getActivity().getContentResolver().openFileDescriptor(uri, "w");
            FileWriter fileWriter = new FileWriter(pfd.getFileDescriptor());
            BufferedWriter buf = new BufferedWriter(fileWriter);

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

                        if (status == LogDatabase.STATUS_BOOT_COMPLETED)
                            s = Str.status_boot_completed;
                        else if (status_age == LogDatabase.STATUS_OLD)
                            s = Str.log_statuses_old[status];
                        else
                            s = Str.log_statuses[status];

                        if (plugged > 0)
                            s += " " + Str.pluggeds[plugged];

                        buf.write(s + ",");
                    } else if (CSV_ORDER[i].equals(LogDatabase.KEY_CHARGE)) {
                        buf.write(String.valueOf(completeCursor.getInt(mAdapter.chargeIndex)) + ",");
                    } else if (CSV_ORDER[i].equals(LogDatabase.KEY_TEMPERATURE)) {
                        int temperature = completeCursor.getInt(mAdapter.temperatureIndex);
                        buf.write(String.valueOf(temperature / 10.0) + ",");
                        buf.write(String.valueOf(java.lang.Math.round(temperature * 9 / 5.0) / 10.0 + 32.0) + ",");
                    } else if (CSV_ORDER[i].equals(LogDatabase.KEY_VOLTAGE)) {
                        buf.write(String.valueOf(completeCursor.getInt(mAdapter.voltageIndex) / 1000.0));
                    }
                }
                buf.write("\r\n");
            }

            buf.close();
            fileWriter.close();
            pfd.close();
        } catch (Exception e) {
            Toast.makeText(getActivity(), Str.inaccessible_storage, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getActivity(), Str.file_written, Toast.LENGTH_SHORT).show();
    }

    // Based on http://stackoverflow.com/a/7343721/1427098
    private class FilteredCursor extends CursorWrapper {
        private Cursor wrappedCursor;

        private ArrayList<Integer> shownIDs;
        private int len;
        private int pos;

        FilteredCursor(Cursor cursor) {
            super(cursor);

            shownIDs = new ArrayList<Integer>();
            wrappedCursor = cursor;

            refilter();
        }

        void refilter() {
            if (wrappedCursor.isClosed()) return;

            shownIDs.clear();

            int wrappedCursorPos = wrappedCursor.getPosition();
            int statusCodeIndex = wrappedCursor.getColumnIndexOrThrow(LogDatabase.KEY_STATUS_CODE);

            boolean show_plugged_in    = pfrag.sp_main.getBoolean("plugged_in",     true);
            boolean show_unplugged     = pfrag.sp_main.getBoolean("unplugged",      true);
            boolean show_charging      = pfrag.sp_main.getBoolean("charging",       true);
            boolean show_discharging   = pfrag.sp_main.getBoolean("discharging",    true);
            boolean show_fully_charged = pfrag.sp_main.getBoolean("fully_charged" , true);
            boolean show_boot          = pfrag.sp_main.getBoolean("boot_completed", true);
            boolean show_unknown       = pfrag.sp_main.getBoolean("unknown",        true);
            boolean show_not_charging  = pfrag.sp_main.getBoolean("not_charging",   true);

            for (wrappedCursor.moveToFirst(); !wrappedCursor.isAfterLast(); wrappedCursor.moveToNext()) {
                int statusCode    = wrappedCursor.getInt(statusCodeIndex);
                int[] statusCodes = LogDatabase.decodeStatus(statusCode);
                int status        = statusCodes[0];
                //int plugged       = statusCodes[1];
                int status_age    = statusCodes[2];

                if (status == BatteryInfo.STATUS_FULLY_CHARGED && show_fully_charged) {
                    shownIDs.add(wrappedCursor.getPosition());
                } else if (status == LogDatabase.STATUS_BOOT_COMPLETED && show_boot) {
                    shownIDs.add(wrappedCursor.getPosition());
                } else if (status == BatteryInfo.STATUS_NOT_CHARGING && show_not_charging) {
                    shownIDs.add(wrappedCursor.getPosition());
                } else if ((status == BatteryInfo.STATUS_UNKNOWN ||
                            status == BatteryInfo.STATUS_DISCHARGING ||
                            status > BatteryInfo.STATUS_MAX) &&
                           show_unknown) {
                    shownIDs.add(wrappedCursor.getPosition());
                } else if (status_age == LogDatabase.STATUS_OLD) {
                    if ((status == BatteryInfo.STATUS_UNPLUGGED && show_discharging) ||
                        (status == BatteryInfo.STATUS_CHARGING  && show_charging))
                        shownIDs.add(wrappedCursor.getPosition());
                } else if (status_age == LogDatabase.STATUS_NEW) {
                    if ((status == BatteryInfo.STATUS_UNPLUGGED && show_unplugged) ||
                        (status == BatteryInfo.STATUS_CHARGING  && show_plugged_in))
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

    private class TimeDeltaCursor extends CursorWrapper {
        static final String KEY_TIME_DELTA = "time_delta";

        private Cursor wrappedCursor;

        private int deltaColumnIndex;
        private String deltaColumnName = KEY_TIME_DELTA;

        private int statusCodeIndex, timeIndex;

        private long last_plugged, last_unplugged;

        private ArrayList<Long> deltas;

        TimeDeltaCursor(Cursor cursor) {
            super(cursor);

            deltas = new ArrayList<Long>();
            wrappedCursor = cursor;

            deltaColumnIndex = super.getColumnCount();

            gen_deltas();
        }

        private void gen_delta() {
            long time         = wrappedCursor.getLong(timeIndex);
            int statusCode    = wrappedCursor.getInt(statusCodeIndex);
            int[] statusCodes = LogDatabase.decodeStatus(statusCode);
            int status        = statusCodes[0];
            //int plugged       = statusCodes[1];
            int status_age    = statusCodes[2];

            if (status == BatteryInfo.STATUS_FULLY_CHARGED) {
                if (last_plugged > 0)
                   deltas.add(time - last_plugged);
                else
                   deltas.add(-1l);
            } else if (status_age == LogDatabase.STATUS_NEW && status == BatteryInfo.STATUS_UNPLUGGED) {
                if (last_plugged > 0)
                   deltas.add(time - last_plugged);
                else
                   deltas.add(-1l);

                last_unplugged = time;
            } else if (status_age == LogDatabase.STATUS_NEW && status == BatteryInfo.STATUS_CHARGING) {
                if (last_unplugged > 0)
                    deltas.add(time - last_unplugged);
                else
                    deltas.add(-1l);

                last_plugged = time;
            } else {
                deltas.add(-1l);
            }
        }

        private boolean wrappedIsChronological() {
            if (wrappedCursor.getCount() < 2) return true;
            boolean chrono = true;

            int pos = wrappedCursor.getPosition();

            wrappedCursor.moveToFirst();
            long time1 = wrappedCursor.getLong(timeIndex);
            wrappedCursor.moveToNext();
            long time2 = wrappedCursor.getLong(timeIndex);

            while (time2 == time1 && !wrappedCursor.isAfterLast()) {
                wrappedCursor.moveToNext();
                time2 = wrappedCursor.getLong(timeIndex);
            }

            if (time2 < time1)
                chrono = false;

            wrappedCursor.moveToPosition(pos);

            return chrono;
        }

        void gen_deltas() {
            if (wrappedCursor.isClosed()) return;

            deltas.clear();

            last_plugged = -1;
            last_unplugged = -1;

            int wrappedCursorPos = wrappedCursor.getPosition();
            statusCodeIndex = wrappedCursor.getColumnIndexOrThrow(LogDatabase.KEY_STATUS_CODE);
                  timeIndex = wrappedCursor.getColumnIndexOrThrow(LogDatabase.KEY_TIME);

            if (wrappedIsChronological())
                for (wrappedCursor.moveToFirst(); !wrappedCursor.isAfterLast(); wrappedCursor.moveToNext())
                    gen_delta();
            else
                for (wrappedCursor.moveToLast(); !wrappedCursor.isBeforeFirst(); wrappedCursor.moveToPrevious())
                    gen_delta();

            wrappedCursor.moveToPosition(wrappedCursorPos);
        }

        @Override
        public boolean requery() {
            boolean ret = super.requery();
            gen_deltas();
            return ret;
        }

        @Override
        public int getColumnCount() {
            return deltaColumnIndex + 1;
        }

        @Override
        public int getColumnIndex(String columnName) {
            if (deltaColumnName.equals(columnName))
                return deltaColumnIndex;
            else
                return super.getColumnIndex(columnName);
        }

        @Override
        public int getColumnIndexOrThrow(String columnName) throws java.lang.IllegalArgumentException {
            if (deltaColumnName.equals(columnName))
                return deltaColumnIndex;
            else
                return super.getColumnIndexOrThrow(columnName);
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == deltaColumnIndex)
                return deltaColumnName;
            else
                return super.getColumnName(columnIndex);
        }

        @Override
        public String[] getColumnNames() {
            String[] a = super.getColumnNames();
            String[] b = new String[a.length + 1];

            System.arraycopy(a, 0, b, 0, a.length);

            b[a.length] = deltaColumnName;

            return b;
        }

        @Override
        public long getLong(int columnIndex) {
            if (columnIndex == deltaColumnIndex){
                int pos = getPosition();

                if (! wrappedIsChronological())
                    pos = getCount() - 1 - pos;

                return deltas.get(pos);
            } else
                return super.getLong(columnIndex);
        }

        // TODO: This was introduced in API 11.  It seems to be safely ignored by APIs less than 11, other than
        //         taking a while for the verifier to no-op it.  Is it worth overriding to add only to
        //         API 11+ devices?  I think it only slows things down on the first run, and not noticeably on most devices.
        /*
        @Override
        public int getType(int columnIndex) {
            if (columnIndex == deltaColumnIndex)
                return Cursor.FIELD_TYPE_INTEGER;
            else
                return super.getType(columnIndex);
        }
        */

        @Override
        public boolean isNull(int columnIndex) {
            if (columnIndex == deltaColumnIndex)
                return false;
            else
                return super.isNull(columnIndex);
        }
    }

    private static class LogItemViewHolder {
        TextView status_tv;
        TextView percent_tv;
        TextView time_tv;
        TextView temp_volt_tv;
        TextView time_diff_tv;
    }

    // Based on https://stackoverflow.com/a/23438859/1427098
    private static String timeDateStringFromDate(Context ctx, Date d) {
        final String AM = new SimpleDateFormat().getDateFormatSymbols().getAmPmStrings()[Calendar.AM];
        final String PM = new SimpleDateFormat().getDateFormatSymbols().getAmPmStrings()[Calendar.PM];

        String androidDateTime = android.text.format.DateFormat.getTimeFormat(ctx).format(d);
        String javaDateTime = DateFormat.getDateTimeInstance().format(d);
        String AmPm = "";

        if (!Character.isDigit(androidDateTime.charAt(androidDateTime.length() - 1))) {
            if (androidDateTime.contains(AM))
                AmPm = " " + AM;
            else
                AmPm = " " + PM;

            androidDateTime = androidDateTime.replace(AmPm, "");
        }

        if (!Character.isDigit(javaDateTime.charAt(javaDateTime.length() - 1))) {
            javaDateTime = javaDateTime.replace(" " + AM, "");
            javaDateTime = javaDateTime.replace(" " + PM, "");
        }

        javaDateTime = javaDateTime.substring(javaDateTime.length() - 3);

        return androidDateTime.concat(javaDateTime).concat(AmPm);
    }

    private class LogAdapter extends CursorAdapter {
        int statusCodeIndex, chargeIndex, timeIndex, temperatureIndex, voltageIndex, timeDeltaIndex;
        DateFormat dateFormat, timeFormat;

        private Date d = new Date();

        LogAdapter(Context context, Cursor cursor) {
            super(context, cursor);

            dateFormat = android.text.format.DateFormat.getDateFormat(context);
            timeFormat = android.text.format.DateFormat.getTimeFormat(context);

             statusCodeIndex = cursor.getColumnIndexOrThrow(LogDatabase.KEY_STATUS_CODE);
                 chargeIndex = cursor.getColumnIndexOrThrow(LogDatabase.KEY_CHARGE);
                   timeIndex = cursor.getColumnIndexOrThrow(LogDatabase.KEY_TIME);
            temperatureIndex = cursor.getColumnIndexOrThrow(LogDatabase.KEY_TEMPERATURE);
                voltageIndex = cursor.getColumnIndexOrThrow(LogDatabase.KEY_VOLTAGE);
              timeDeltaIndex = cursor.getColumnIndexOrThrow(TimeDeltaCursor.KEY_TIME_DELTA);
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = mInflater.inflate(R.layout.log_item , parent, false);
            LogItemViewHolder vh = new LogItemViewHolder();

            vh.status_tv = (TextView) v.findViewById(R.id.status);
            vh.percent_tv = (TextView) v.findViewById(R.id.percent);
            vh.time_tv = (TextView) v.findViewById(R.id.time);
            vh.temp_volt_tv = (TextView) v.findViewById(R.id.temp_volt);
            vh.time_diff_tv = (TextView) v.findViewById(R.id.time_diff);

            v.setTag(vh);

            return v;
        }

        public void bindView(View view, Context context, Cursor cursor) {
            LogItemViewHolder vh = (LogItemViewHolder) view.getTag();

            TextView    status_tv = vh.status_tv;
            TextView   percent_tv = vh.percent_tv;
            TextView      time_tv = vh.time_tv;
            TextView temp_volt_tv = vh.temp_volt_tv;
            TextView time_diff_tv = vh.time_diff_tv;

            int statusCode = cursor.getInt(statusCodeIndex);
            int[] statusCodes = LogDatabase.decodeStatus(statusCode);
            int status     = statusCodes[0];
            int plugged    = statusCodes[1];
            int status_age = statusCodes[2];

            String s;

            if (status == LogDatabase.STATUS_BOOT_COMPLETED)
                percent_tv.setVisibility(View.GONE);
            else
                percent_tv.setVisibility(View.VISIBLE);

            if (status == LogDatabase.STATUS_BOOT_COMPLETED) {
                status_tv.setTextColor(col.boot);
                s = Str.status_boot_completed;

                time_diff_tv.setVisibility(View.GONE);
            } else if (status_age == LogDatabase.STATUS_OLD) {
                 status_tv.setTextColor(col.old_status);
                percent_tv.setTextColor(col.old_status);
                s = Str.log_statuses_old[status];

                time_diff_tv.setVisibility(View.GONE);
            } else {
                switch (status) {
                case 0:
                     status_tv.setTextColor(col.unplugged);
                    percent_tv.setTextColor(col.unplugged);
                    break;
                case 2:
                     status_tv.setTextColor(col.plugged);
                    percent_tv.setTextColor(col.plugged);
                    break;
                case 5:
                     status_tv.setTextColor(col.charged);
                    percent_tv.setTextColor(col.charged);
                    break;
                default:
                     status_tv.setTextColor(col.unknown);
                    percent_tv.setTextColor(col.unknown);
                }

                s = Str.log_statuses[status];
                long delta, secs, mins;

                switch (status) {
                case 0:
                case 5:
                    delta = cursor.getLong(timeDeltaIndex);

                    if (delta < 0) {
                        time_diff_tv.setVisibility(View.GONE);
                        break;
                    }

                    secs = delta / 1000;
                    mins = secs / 60;

                    if (mins >= 60)
                        time_diff_tv.setText(String.format(pfrag.res.getString(R.string.after_nh_mm_plugged_in),
                                                           mins / 60, mins % 60));
                    else
                        time_diff_tv.setText(String.format(pfrag.res.getString(R.string.after_nm_ms_plugged_in),
                                                           mins, secs % 60));

                    time_diff_tv.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    delta = cursor.getLong(timeDeltaIndex);

                    if (delta < 0) {
                        time_diff_tv.setVisibility(View.GONE);
                        break;
                    }

                    secs = delta / 1000;
                    mins = secs / 60;

                    if (mins >= 60)
                        time_diff_tv.setText(String.format(pfrag.res.getString(R.string.after_nh_mm_unplugged),
                                                           mins / 60, mins % 60));
                    else
                        time_diff_tv.setText(String.format(pfrag.res.getString(R.string.after_nm_ms_unplugged),
                                                           mins, secs % 60));

                    time_diff_tv.setVisibility(View.VISIBLE);
                    break;
                default:
                    time_diff_tv.setVisibility(View.GONE);
                }
            }

            if (plugged > 0)
                s += " " + Str.pluggeds[plugged];

            status_tv.setText(s);

            percent_tv.setText("" + cursor.getInt(chargeIndex) + "%");

            d.setTime(cursor.getLong(timeIndex));

            if (pfrag.sp_main.getBoolean(KEY_SHOW_SECONDS, false))
                time_tv.setText(dateFormat.format(d) + "  " + timeDateStringFromDate(context, d));
            else
                time_tv.setText(dateFormat.format(d) + "  " + timeFormat.format(d));

            int temperature = cursor.getInt(temperatureIndex);
            if (temperature != 0) temp_volt_tv.setText("" + Str.formatTemp(temperature, convertF));
            else temp_volt_tv.setText(""); /* TextViews are reused */

            int voltage = cursor.getInt(voltageIndex);
            if (voltage != 0) temp_volt_tv.setText(temp_volt_tv.getText().toString() +
                                                   " / " + Str.formatVoltage(voltage));
        }
    }

    private class Col {
        int old_status;
        int charged;
        int plugged;
        int unplugged;
        int unknown;
        int boot;

        Col() {
            old_status = pfrag.res.getColor(R.color.log_old_status);
            charged    = pfrag.res.getColor(R.color.log_charged);
            plugged    = pfrag.res.getColor(R.color.log_plugged);
            unplugged  = pfrag.res.getColor(R.color.log_unplugged);
            unknown    = pfrag.res.getColor(R.color.log_unknown);
            boot       = pfrag.res.getColor(R.color.log_boot_completed);
        }
    }
}
