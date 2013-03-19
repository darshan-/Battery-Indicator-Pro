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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
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

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

public class CurrentInfoFragment extends Fragment {
    private static BatteryInfoActivity activity;
    private Intent biServiceIntent;
    private Messenger serviceMessenger;
    private final Messenger messenger = new Messenger(new MessageHandler());
    private BatteryInfoService.RemoteConnection serviceConnection;
    private boolean serviceConnected;

    private static final Intent batteryUseIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    private static final IntentFilter batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private View view;
    private Boolean disallowLockButton;
    private Button battery_use_b;
    private Button toggle_lock_screen_b;
    private BatteryLevel bl;
    private ImageView blv;
    private BatteryInfo info = new BatteryInfo();

    //private String oldLanguage = null;

    private static final String LOG_TAG = "BatteryBot";

    public void bindService() {
        if (! serviceConnected) {
            activity.context.bindService(biServiceIntent, serviceConnection, 0);
            serviceConnected = true;
        }
    }

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message incoming) {
            if (! serviceConnected) {
                Log.i(LOG_TAG, "serviceConected is false; ignoring message: " + incoming);
                return;
            }

            switch (incoming.what) {
            case BatteryInfoService.RemoteConnection.CLIENT_SERVICE_CONNECTED:
                serviceMessenger = incoming.replyTo;
                sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_REGISTER_CLIENT);
                break;
            case BatteryInfoService.RemoteConnection.CLIENT_BATTERY_INFO_UPDATED:
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

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.current_info, container, false);

        blv = (ImageView) view.findViewById(R.id.battery_level_view);
        blv.setImageBitmap(bl.getBitmap());

        toggle_lock_screen_b = (Button) view.findViewById(R.id.toggle_lock_screen_b);
        battery_use_b = (Button) view.findViewById(R.id.battery_use_b);

        updateLockscreenButton(); // TODO: This button should be disabled until Service is connected
        bindButtons();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = (BatteryInfoActivity) getActivity();

        bl = new BatteryLevel(activity.context, activity.res.getInteger(R.integer.bl_inSampleSize));

        setHasOptionsMenu(true);
        //setRetainInstance(true); // TODO: Sort out a clean way to do this?

        disallowLockButton = activity.settings.getBoolean(SettingsActivity.KEY_DISALLOW_DISABLE_LOCK_SCREEN, false);

        if (activity.settings.getBoolean(SettingsActivity.KEY_FIRST_RUN, true)) {
            // If you ever need a first-run dialog again, this is when you would show it
            SharedPreferences.Editor editor = activity.sp_store.edit();
            editor.putBoolean(SettingsActivity.KEY_FIRST_RUN, false);
            editor.commit();
        }

        // TODO: everything after here could happen in another thread?
        //   They tend to take about 70ms on the myTouch
        SharedPreferences.Editor editor = activity.sp_store.edit();
        editor.putBoolean(BatteryInfoService.KEY_SERVICE_DESIRED, true);
        editor.commit();

        serviceConnection = new BatteryInfoService.RemoteConnection(messenger);

        biServiceIntent = new Intent(activity.context, BatteryInfoService.class);
        activity.context.startService(biServiceIntent);
        bindService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serviceConnected) {
            activity.context.unbindService(serviceConnection);
            serviceConnected = false;
        }
        bl.recycle();
    }

    /*private void restartIfLanguageChanged() {
        String curLanguage = activity.settings.getString(SettingsActivity.KEY_LANGUAGE_OVERRIDE, "default");
        if (curLanguage.equals(oldLanguage))
            return;

        Str.overrideLanguage(activity.res, getWindowManager(), curLanguage);
        mStartActivity(BatteryInfoActivity.class);
        activity.finish();
    }*/

    @Override
    public void onResume() {
        super.onResume();

        if (serviceMessenger != null)
            sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_REGISTER_CLIENT);

        Intent bc_intent = activity.context.registerReceiver(null, batteryChangedFilter);
        info.load(bc_intent);
        info.load(activity.sp_store);
        handleUpdatedBatteryInfo(info);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (serviceMessenger != null)
            sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_UNREGISTER_CLIENT);
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
            DialogFragment df = new ConfirmCloseDialogFragment();
            df.show(getFragmentManager(), "TODO: What is this string for?2");
            return true;
        case R.id.menu_help:
            mStartActivity(HelpActivity.class);
            return true;
        case R.id.menu_rate_and_review:
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                                         Uri.parse("market://details?id=com.darshancomputing.BatteryIndicatorPro")));
            } catch (Exception e) {
                Toast.makeText(activity.getApplicationContext(), "Sorry, can't launch Market!", Toast.LENGTH_SHORT).show();
            }
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
        disallowLockButton = activity.settings.getBoolean(SettingsActivity.KEY_DISALLOW_DISABLE_LOCK_SCREEN, false);
    }
    */

    public static class ConfirmDisableKeyguardDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(activity)
                .setTitle(activity.res.getString(R.string.confirm_disable))
                .setMessage(activity.res.getString(R.string.confirm_disable_hint))
                .setCancelable(false) // TODO: Doesn't work... Just remove and accept being cancelable?
                .setPositiveButton(activity.res.getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            ((BatteryInfoActivity) activity).currentInfoFragment.setDisableLocking(true);
                            di.cancel();
                        }
                    })
                .setNegativeButton(activity.res.getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            di.cancel();
                        }
                    })
                .create();
        }
    }

    public static class ConfirmCloseDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(activity)
                .setTitle(activity.res.getString(R.string.confirm_close))
                .setMessage(activity.res.getString(R.string.confirm_close_hint))
                .setPositiveButton(activity.res.getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            ((BatteryInfoActivity) activity).currentInfoFragment.closeApp();
                            di.cancel();
                        }
                    })
                .setNegativeButton(activity.res.getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            di.cancel();
                        }
                    })
                .create();
        }
    }

    public void closeApp() {
        SharedPreferences.Editor editor = activity.sp_store.edit();
        editor.putBoolean(BatteryInfoService.KEY_SERVICE_DESIRED, false);
        editor.commit();

        activity.finishActivity(1);

        if (serviceConnected) {
            activity.context.unbindService(serviceConnection);
            activity.context.stopService(biServiceIntent);
            serviceConnected = false;
        }

        activity.finish();
    }

    private void handleUpdatedBatteryInfo(BatteryInfo info) {
        bl.setLevel(info.percent);
        blv.invalidate();

        TextView tv = (TextView) view.findViewById(R.id.level);
        tv.setText("" + info.percent + activity.res.getString(R.string.percent_symbol));

        if (info.prediction.what == BatteryInfo.Prediction.NONE) {
            tv = (TextView) view.findViewById(R.id.time_remaining);
            tv.setText(android.text.Html.fromHtml("<font color=\"#6fc14b\">" + activity.str.statuses[info.status] + "</font>")); // TODO: color
            tv = (TextView) view.findViewById(R.id.until_what);
            tv.setText("");
        } else {
            String until_text;

            if (info.prediction.what == BatteryInfo.Prediction.UNTIL_CHARGED)
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
                tv.setText(android.text.Html.fromHtml("<font color=\"#33b5e5\"><small>" + info.prediction.minutes + " mins</small></font>"));


            tv = (TextView) view.findViewById(R.id.until_what);
            tv.setText(until_text);
        }

        int secs = (int) ((System.currentTimeMillis() - info.last_status_cTM) / 1000);
        int hours = secs / (60 * 60);
        int mins = (secs / 60) % 60;

        String s = activity.str.statuses[info.status];

        if (info.status == BatteryInfo.STATUS_CHARGING)
            s += " " + activity.str.pluggeds[info.last_plugged];

        // TODO: Don't show 'since 100%' for Fully Charged status
        tv = (TextView) view.findViewById(R.id.status);
        tv.setText(s);

        s = "Since "; // TODO: Translatable

        if (info.status != BatteryInfo.STATUS_FULLY_CHARGED)
            s += info.last_percent + activity.str.percent_symbol + ", ";

        s += hours + "h " + mins + "m ago"; // TODO: Translatable

        tv = (TextView) view.findViewById(R.id.status_duration);
        tv.setText(s);

        updateLockscreenButton();
    }

    private void updateLockscreenButton() {
        activity.loadSettingsFiles();

        if (activity.sp_store.getBoolean(BatteryInfoService.KEY_DISABLE_LOCKING, false))
            toggle_lock_screen_b.setText(activity.res.getString(R.string.reenable_lock_screen));
        else
            toggle_lock_screen_b.setText(activity.res.getString(R.string.disable_lock_screen));
    }

    private void setDisableLocking(boolean b) {
        SharedPreferences.Editor editor = activity.sp_store.edit();
        editor.putBoolean(BatteryInfoService.KEY_DISABLE_LOCKING, b);
        editor.commit();

        Message outgoing = Message.obtain();
        outgoing.what = BatteryInfoService.RemoteConnection.SERVICE_RELOAD_SETTINGS;
        try { serviceMessenger.send(outgoing); } catch (android.os.RemoteException e) {}

        updateLockscreenButton();

        if (activity.settings.getBoolean(SettingsActivity.KEY_FINISH_AFTER_TOGGLE_LOCK, false)) activity.finish();
    }

    /* Battery Use */
    private final OnClickListener buButtonListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                startActivity(batteryUseIntent);
                if (activity.settings.getBoolean(SettingsActivity.KEY_FINISH_AFTER_BATTERY_USE, false)) activity.finish();
            } catch (Exception e) {
                battery_use_b.setEnabled(false);
            }
        }
    };

    /* Toggle Lock Screen */
    private final OnClickListener tlsButtonListener = new OnClickListener() {
        public void onClick(View v) {
            if (activity.sp_store.getBoolean(BatteryInfoService.KEY_DISABLE_LOCKING, false)) {
                setDisableLocking(false);
            } else {
                if (activity.settings.getBoolean(SettingsActivity.KEY_CONFIRM_DISABLE_LOCKING, true)) {
                    DialogFragment df = new ConfirmDisableKeyguardDialogFragment();
                    df.show(getFragmentManager(), "TODO: What is this string for?");
                } else {
                    setDisableLocking(true);
                }
            }
        }
    };


    // TODO: Re-implement dialogs
    /*
        case DIALOG_FIRST_RUN:
            LayoutInflater inflater = (LayoutInflater) activity.context.getSystemService(LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.first_run_message, (LinearLayout) view.findViewById(R.id.layout_root));

            builder.setTitle(activity.res.getString(R.string.first_run_title))
                .setView(layout)
                .setPositiveButton(activity.res.getString(R.string.okay), new DialogInterface.OnClickListener() {
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

    private void mStartActivity(Class c) {
        ComponentName comp = new ComponentName(activity.context.getPackageName(), c.getName());
        //startActivity(new Intent().setComponent(comp));
        startActivityForResult(new Intent().setComponent(comp), 1);
        //activity.finish();
    }

    private void bindButtons() {
        if (activity.context.getPackageManager().resolveActivity(batteryUseIntent, 0) == null) {
            battery_use_b.setEnabled(false); /* TODO: change how the disabled button looks */
        } else {
            battery_use_b.setOnClickListener(buButtonListener);
        }

        toggle_lock_screen_b.setOnClickListener(tlsButtonListener);
    }
}
