/*
    Copyright (c) 2016 Darshan-Josiah Barber

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

public class NotificationWizardFragment extends DialogFragment {
    private static PersistentFragment pfrag;
    private String[] titles;
    private String[] summaries;
    private ListView lv;

    private static final int VALUE_DEFAULT = 0; // Maintain to match index in list
    private static final int VALUE_MINIMAL = 1;
    private static final int VALUE_NONE    = 2;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.notification_wizard_content, null);
        lv = (ListView) v.findViewById(android.R.id.list);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setValue(position);
            }
        });

        return new AlertDialog.Builder(getActivity())
            .setView(v)
            .setTitle(R.string.notification_wizard_title)
            .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        di.cancel();
                    }
                })
            .create();
    }

    @Override
    public void onPause() {
        super.onPause();

        // On Nexus One (and presumably other older Android versions and/or slower devices) the ListView
        //   kept calling the adapter's getView() after the Fragment was destroyed.  This resolves that.
        lv.setAdapter(null);
    }

    @Override
    public void onResume() {
        super.onResume();

        lv.setAdapter(new MyAdapter());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pfrag = PersistentFragment.getInstance(getFragmentManager());

        titles = new String[] {pfrag.res.getString(R.string.notification_wizard_title_default),
                               pfrag.res.getString(R.string.notification_wizard_title_minimal),
                               pfrag.res.getString(R.string.notification_wizard_title_none)};

        summaries = new String[] {pfrag.res.getString(R.string.notification_wizard_summary_default),
                                  pfrag.res.getString(R.string.notification_wizard_summary_minimal),
                                  pfrag.res.getString(R.string.notification_wizard_summary_none)};

        if (android.os.Build.VERSION.SDK_INT < 16)
            summaries[1] += " " + pfrag.res.getString(R.string.requires_api_level_16);
    }

    private int getValue() {
        int priority = Integer.valueOf(pfrag.settings.getString(SettingsActivity.KEY_MAIN_NOTIFICATION_PRIORITY,
                                                                pfrag.str.default_main_notification_priority));

        boolean show_notification = pfrag.sp_store.getBoolean(BatteryInfoService.KEY_SHOW_NOTIFICATION, true);

        if (! show_notification)
            return VALUE_NONE;

        if (priority == NotificationCompat.PRIORITY_MIN)
            return VALUE_MINIMAL;

        return VALUE_DEFAULT;
    }

    private void setValue(int value) {
        SharedPreferences.Editor sps_editor = pfrag.sp_store.edit();
        SharedPreferences.Editor settings_editor = pfrag.settings.edit();

        switch(value) {
        case VALUE_NONE:
            sps_editor.putBoolean(BatteryInfoService.KEY_SHOW_NOTIFICATION, false);

            break;
        case VALUE_MINIMAL:
            sps_editor.putBoolean(BatteryInfoService.KEY_SHOW_NOTIFICATION, true);
            settings_editor.putString(SettingsActivity.KEY_MAIN_NOTIFICATION_PRIORITY,
                                      "" + NotificationCompat.PRIORITY_MIN);

            break;
        default:
            sps_editor.putBoolean(BatteryInfoService.KEY_SHOW_NOTIFICATION, true);
            int priority = Integer.valueOf(pfrag.settings.getString(SettingsActivity.KEY_MAIN_NOTIFICATION_PRIORITY,
                                                                    pfrag.str.default_main_notification_priority));
            if (priority == NotificationCompat.PRIORITY_MIN)
                settings_editor.putString(SettingsActivity.KEY_MAIN_NOTIFICATION_PRIORITY,
                                          "" + NotificationCompat.PRIORITY_LOW);
        }

        sps_editor.commit();
        settings_editor.commit();
        pfrag.sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_CANCEL_NOTIFICATION_AND_RELOAD_SETTINGS);
    }

    private class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public String getItem(int position) {
            return titles[position];
        }

        @Override
        public long getItemId(int position) {
            return titles[position].hashCode();
        }

        @Override
        public boolean isEnabled(int position) {
            if (position == 1 && android.os.Build.VERSION.SDK_INT < 16)
                return false;

            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null)
                convertView = getActivity().getLayoutInflater().inflate(R.layout.single_choice_checkable_item,
                                                                        container, false);

            ((TextView) convertView.findViewById(android.R.id.title)).setText(getItem(position));
            ((TextView) convertView.findViewById(android.R.id.summary)).setText(summaries[position]);

            if (position == getValue())
                ((ListView) container).setItemChecked(position, true);

            if (position == 1 && android.os.Build.VERSION.SDK_INT < 16)
                convertView.setEnabled(false);
            else
                convertView.setEnabled(true);

            return convertView;
        }
    }
}
