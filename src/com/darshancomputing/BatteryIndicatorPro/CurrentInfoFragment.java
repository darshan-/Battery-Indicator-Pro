/*
    Copyright (c) 2009-2013 Darshan-Josiah Barber

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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.app.Fragment;

public class CurrentInfoFragment extends Fragment {
    private Intent biServiceIntent;
    private SharedPreferences settings;
    private SharedPreferences sp_store;

    private Messenger serviceMessenger;
    private final Messenger messenger = new Messenger(new MessageHandler());
    private final BatteryInfoService.RemoteConnection serviceConnection = new BatteryInfoService.RemoteConnection(messenger);

    private static final Intent batteryUseIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    private static final IntentFilter batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private Resources res;
    private Context context;
    private View view;
    private Str str;
    private Boolean disallowLockButton;
    private Button battery_use_b;
    private Button toggle_lock_screen_b;
    private BatteryLevel bl;
    private ImageView blv;

    //private String oldLanguage = null;

    private static final int DIALOG_CONFIRM_DISABLE_KEYGUARD = 0;
    private static final int DIALOG_CONFIRM_CLOSE = 1;
    private static final int DIALOG_FIRST_RUN = 2;

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message incoming) {
            switch (incoming.what) {
            case BatteryInfoService.RemoteConnection.CLIENT_SERVICE_CONNECTED:
                serviceMessenger = incoming.replyTo;
                sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_REGISTER_CLIENT);
                break;
            case BatteryInfoService.RemoteConnection.CLIENT_BATTERY_INFO_UPDATED:
                BatteryInfo info = new BatteryInfo();
                info.loadBundle(incoming.getData());
                handleUpdatedBatteryInfo(info);
                break;
            default:
                super.handleMessage(incoming);
            }
        }
    }

    private void sendServiceMessage(int what) {
        Message outgoing = Message.obtain();
        outgoing.what = what;
        outgoing.replyTo = messenger;
        try { serviceMessenger.send(outgoing); } catch (android.os.RemoteException e) {}
    }

    private final BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (! Intent.ACTION_BATTERY_CHANGED.equals(action)) return;

            // TODO: Make sure Service is running?  Or else remove this altogether
        }
    };

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.current_info, container, false);

        blv = (ImageView) view.findViewById(R.id.battery_level_view);
        blv.setImageBitmap(bl.getBitmap());

        toggle_lock_screen_b = (Button) view.findViewById(R.id.toggle_lock_screen_b);
        battery_use_b = (Button) view.findViewById(R.id.battery_use_b);

        bindButtons();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        res = getResources();
        str = new Str(res);
        context = getActivity().getApplicationContext();
        bl = new BatteryLevel(context, res.getInteger(R.integer.bl_inSampleSize));

        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        settings = context.getSharedPreferences(SettingsActivity.SETTINGS_FILE, Context.MODE_MULTI_PROCESS);
        sp_store = context.getSharedPreferences(SettingsActivity.SP_STORE_FILE, Context.MODE_MULTI_PROCESS);

        disallowLockButton = settings.getBoolean(SettingsActivity.KEY_DISALLOW_DISABLE_LOCK_SCREEN, false);

        if (settings.getBoolean(SettingsActivity.KEY_FIRST_RUN, true)) {
            SharedPreferences.Editor editor = sp_store.edit();
            editor.putBoolean(SettingsActivity.KEY_FIRST_RUN, false);
            editor.commit();
        }

        biServiceIntent = new Intent(getActivity(), BatteryInfoService.class);
        getActivity().startService(biServiceIntent);
        getActivity().bindService(biServiceIntent, serviceConnection, 0);

        SharedPreferences.Editor editor = sp_store.edit();
        editor.putBoolean(BatteryInfoService.KEY_SERVICE_DESIRED, true);
        editor.commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(serviceConnection);
        bl.recycle();
    }

    /*private void restartIfLanguageChanged() {
        String curLanguage = settings.getString(SettingsActivity.KEY_LANGUAGE_OVERRIDE, "default");
        if (curLanguage.equals(oldLanguage))
            return;

        Str.overrideLanguage(res, getWindowManager(), curLanguage);
        mStartActivity(BatteryInfoActivity.class);
        getActivity().finish();
    }*/

    @Override
    public void onResume() {
        super.onResume();

        if (serviceMessenger != null) sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_REGISTER_CLIENT);
        getActivity().registerReceiver(mBatteryInfoReceiver, batteryChangedFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (serviceMessenger != null) sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_UNREGISTER_CLIENT);
        getActivity().unregisterReceiver(mBatteryInfoReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            mStartActivity(SettingsActivity.class);
            return true;
        case R.id.menu_close:
            //showDialog(DIALOG_CONFIRM_CLOSE); // TODO
            return true;
        case R.id.menu_help:
            mStartActivity(HelpActivity.class);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    // TODO
    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Boolean oldDisallow = disallowLockButton;
        disallowLockButton = settings.getBoolean(SettingsActivity.KEY_DISALLOW_DISABLE_LOCK_SCREEN, false);
    }
    */

    // TODO: Re-implement dialogs
    /* 
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
        case DIALOG_CONFIRM_DISABLE_KEYGUARD:
            builder.setTitle(res.getString(R.string.confirm_disable))
                .setMessage(res.getString(R.string.confirm_disable_hint))
                .setCancelable(false)
                .setPositiveButton(res.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        setDisableLocking(true);
                        di.cancel();
                    }
                })
                .setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        di.cancel();
                    }
                });

            dialog = builder.create();
            break;
        case DIALOG_CONFIRM_CLOSE:
            builder.setTitle(res.getString(R.string.confirm_close))
                .setMessage(res.getString(R.string.confirm_close_hint))
                .setPositiveButton(res.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        SharedPreferences.Editor editor = sp_store.edit();
                        editor.putBoolean(BatteryInfoService.KEY_SERVICE_DESIRED, false);
                        editor.commit();

                        finishActivity(1);
                        stopService(biServiceIntent);
                        getActivity().finish();

                        di.cancel();
                    }
                })
                .setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        di.cancel();
                    }
                });

            dialog = builder.create();
            break;
        case DIALOG_FIRST_RUN:
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.first_run_message, (LinearLayout) view.findViewById(R.id.layout_root));

            builder.setTitle(res.getString(R.string.first_run_title))
                .setView(layout)
                .setPositiveButton(res.getString(R.string.okay), new DialogInterface.OnClickListener() {
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
    */

    private void handleUpdatedBatteryInfo(BatteryInfo info) {
        bl.setLevel(info.percent);
        blv.invalidate();

        TextView tv = (TextView) view.findViewById(R.id.level);
        tv.setText("" + info.percent + res.getString(R.string.percent_symbol));

        if (info.prediction.what == Predictor.Prediction.NONE) {
            tv = (TextView) view.findViewById(R.id.time_remaining);
            tv.setText(android.text.Html.fromHtml("<font color=\"#6fc14b\">" + str.statuses[info.status] + "</font>")); // TODO: color
        } else {
            String until_text;

            if (info.prediction.what == Predictor.Prediction.UNTIL_CHARGED)
                until_text = "until charged"; // TODO: Translatable
            else
                until_text = "until drained"; // TODO: Translatable

            tv = (TextView) view.findViewById(R.id.time_remaining);
            if (info.prediction.days > 0)
                // TODO: Translatable, color, better layout
                tv.setText(android.text.Html.fromHtml("<font color=\"#6fc14b\">" + info.prediction.days + "d</font> " +
                                                      "<font color=\"#33b5e5\"><small>" + info.prediction.hours + "h</small></font>"));
            else if (info.prediction.hours > 0)
                // TODO: Translatable ("h" and "m"); color
                tv.setText(android.text.Html.fromHtml("<font color=\"#6fc14b\">" + info.prediction.hours + "h</font> " +
                                                      "<font color=\"#33b5e5\"><small>" + info.prediction.minutes + "m</small></font>"));
            else
                // TODO: Translatable, color, better layout
                tv.setText(android.text.Html.fromHtml("<font color=\"#33b5e5\"><small>" + info.prediction.minutes + "mins</small></font>"));


            tv = (TextView) view.findViewById(R.id.until_what);
            tv.setText(until_text);
        }

        int secs = (int) ((System.currentTimeMillis() - info.last_status_cTM) / 1000);
        int hours = secs / (60 * 60);
        int mins = (secs / 60) % 60;

        String s = str.statuses[info.status];

        if (info.status == BatteryInfo.STATUS_CHARGING)
            s += " " + str.pluggeds[info.last_plugged];

        // TODO: Don't show 'since 100%' for Fully Charged status
        tv = (TextView) view.findViewById(R.id.status);
        tv.setText(s);

        s = "Since "; // TODO: Translatable

        if (info.status != BatteryInfo.STATUS_FULLY_CHARGED)
            s += info.last_percent + str.percent_symbol + ", ";

        s += hours + "h " + mins + "m ago"; // TODO: Translatable

        tv = (TextView) view.findViewById(R.id.status_duration);
        tv.setText(s);

        updateLockscreenButton();
    }

    private void updateLockscreenButton() {
        if (sp_store.getBoolean(BatteryInfoService.KEY_DISABLE_LOCKING, false))
            toggle_lock_screen_b.setText(res.getString(R.string.reenable_lock_screen));
        else
            toggle_lock_screen_b.setText(res.getString(R.string.disable_lock_screen));
    }

    private void setDisableLocking(boolean b) {
        SharedPreferences.Editor editor = sp_store.edit();
        editor.putBoolean(BatteryInfoService.KEY_DISABLE_LOCKING, b);
        editor.commit();

        Message outgoing = Message.obtain();
        outgoing.what = BatteryInfoService.RemoteConnection.SERVICE_RELOAD_SETTINGS;
        try { serviceMessenger.send(outgoing); } catch (android.os.RemoteException e) {}

        updateLockscreenButton();

        if (settings.getBoolean(SettingsActivity.KEY_FINISH_AFTER_TOGGLE_LOCK, false)) getActivity().finish();
    }

    /* Battery Use */
    private final OnClickListener buButtonListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                startActivity(batteryUseIntent);
                if (settings.getBoolean(SettingsActivity.KEY_FINISH_AFTER_BATTERY_USE, false)) getActivity().finish();
            } catch (Exception e) {
                battery_use_b.setEnabled(false);
            }
        }
    };

    /* Toggle Lock Screen */
    private final OnClickListener tlsButtonListener = new OnClickListener() {
        public void onClick(View v) {
            if (sp_store.getBoolean(BatteryInfoService.KEY_DISABLE_LOCKING, false)) {
                setDisableLocking(false);
            } else {
                if (settings.getBoolean(SettingsActivity.KEY_CONFIRM_DISABLE_LOCKING, true)) {
                    //showDialog(DIALOG_CONFIRM_DISABLE_KEYGUARD); // TODO
                } else {
                    setDisableLocking(true);
                }
            }
        }
    };

    private void mStartActivity(Class c) {
        ComponentName comp = new ComponentName(getActivity().getPackageName(), c.getName());
        //startActivity(new Intent().setComponent(comp));
        startActivityForResult(new Intent().setComponent(comp), 1);
        //getActivity().finish();
    }

    private void bindButtons() {
        if (getActivity().getPackageManager().resolveActivity(batteryUseIntent, 0) == null) {
            battery_use_b.setEnabled(false); /* TODO: change how the disabled button looks */
        } else {
            battery_use_b.setOnClickListener(buButtonListener);
        }

        toggle_lock_screen_b.setOnClickListener(tlsButtonListener);
    }
}
