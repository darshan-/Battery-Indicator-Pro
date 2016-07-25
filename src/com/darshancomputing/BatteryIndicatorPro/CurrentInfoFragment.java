/*
    Copyright (c) 2009-2016 Darshan-Josiah Barber

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
import android.util.DisplayMetrics;
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

import java.lang.ref.WeakReference;

public class CurrentInfoFragment extends Fragment {
    private BatteryInfoActivity activity;
    private float dpScale;
    private Intent biServiceIntent;
    private Messenger serviceMessenger;
    private static final MessageHandler messageHandler = new MessageHandler();
    private static final Messenger messenger = new Messenger(messageHandler);
    private BatteryInfoService.RemoteConnection serviceConnection;
    private boolean serviceConnected;

    private static final Intent batteryUseIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    private static final IntentFilter batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private View view;
    private Button battery_use_b;
    private BatteryLevel bl;
    private ImageView blv;
    private View current_icon;
    private TextView tv_temp;
    private TextView tv_health;
    private TextView tv_voltage;
    private TextView tv_current;
    private ImageView plugged_icon;

    private BatteryInfo info = new BatteryInfo();
    private CurrentHack currentHack;

    //private String oldLanguage = null;

    private static final String LOG_TAG = "BatteryBot";

    private final Handler mHandler = new Handler();
    private final Runnable mARefresher = new Runnable() {
        public void run() {
            refreshCurrent();
            mHandler.postDelayed(mARefresher, 2000);
        }
    };

    public void bindService() {
        if (! serviceConnected) {
            activity.getApplicationContext().bindService(biServiceIntent, serviceConnection, 0);
            serviceConnected = true;
        }
    }

    private static class MessageHandler extends Handler {
        private WeakReference<CurrentInfoFragment> fragRef;

        public void updateFragRef(CurrentInfoFragment f) {
            fragRef = new WeakReference<CurrentInfoFragment>(f);
        }

        @Override
        public void handleMessage(Message incoming) {
            if (fragRef == null) {
                //Log.i(LOG_TAG, "fragRef is null; ignoring message: " + incoming);
            }

            CurrentInfoFragment cif = fragRef.get();

            if (cif == null) {
                //Log.i(LOG_TAG, "fragRef.get() returns null; ignoring message: " + incoming);
                return;
            }

            if (! cif.serviceConnected) {
                //Log.i(LOG_TAG, "serviceConected is false; ignoring message: " + incoming);
                return;
            }

            switch (incoming.what) {
            case BatteryInfoService.RemoteConnection.CLIENT_SERVICE_CONNECTED:
                cif.serviceMessenger = incoming.replyTo;
                cif.sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_REGISTER_CLIENT);
                break;
            case BatteryInfoService.RemoteConnection.CLIENT_BATTERY_INFO_UPDATED:
                cif.info.loadBundle(incoming.getData());
                cif.handleUpdatedBatteryInfo(cif.info);
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
        try { if (serviceMessenger != null) serviceMessenger.send(outgoing); } catch (android.os.RemoteException e) {}
    }

    @Override
    public void onAttach(android.app.Activity a) {
        super.onAttach(a);

        messageHandler.updateFragRef(this);
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        setSizes(newConfig);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.current_info, container, false);

        blv = (ImageView) view.findViewById(R.id.battery_level_view);
        blv.setImageBitmap(bl.getBitmap());

        battery_use_b = (Button) view.findViewById(R.id.battery_use_b);

        view.findViewById(R.id.vital_stats).setOnClickListener(vsListener);
        current_icon = view.findViewById(R.id.current_icon);

        tv_temp = (TextView) view.findViewById(R.id.temp);
        tv_health = (TextView) view.findViewById(R.id.health);
        tv_voltage = (TextView) view.findViewById(R.id.voltage);
        tv_current = (TextView) view.findViewById(R.id.current);
        plugged_icon = (ImageView) view.findViewById(R.id.plugged_icon);

        bindButtons();

        setSizes(getActivity().getResources().getConfiguration());

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = (BatteryInfoActivity) getActivity();
        dpScale = getActivity().getResources().getDisplayMetrics().density;


        bl = BatteryLevel.getInstance(activity, activity.res.getInteger(R.integer.bl_inSampleSize));

        currentHack = CurrentHack.getInstance(activity);
        currentHack.setPreferFS(activity.settings.getBoolean(SettingsActivity.KEY_CURRENT_HACK_PREFER_FS, false));

        setHasOptionsMenu(true);
        //setRetainInstance(true); // TODO: Sort out a clean way to do this?

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

        biServiceIntent = new Intent(activity, BatteryInfoService.class);
        activity.startService(biServiceIntent);
        bindService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (serviceConnected) {
            activity.getApplicationContext().unbindService(serviceConnection);
            serviceConnected = false;
        }
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

        Intent bc_intent = activity.registerReceiver(null, batteryChangedFilter);
        info.load(bc_intent);
        info.load(activity.sp_store);
        handleUpdatedBatteryInfo(info);

        if (activity.settings.getBoolean(SettingsActivity.KEY_ENABLE_CURRENT_HACK, false) &&
            activity.settings.getBoolean(SettingsActivity.KEY_DISPLAY_CURRENT_IN_MAIN_WINDOW, false) &&
            activity.settings.getBoolean(SettingsActivity.KEY_AUTO_REFRESH_CURRENT_IN_MAIN_WINDOW, false))
            mHandler.postDelayed(mARefresher, 2000);
    }

    @Override
    public void onPause() {
        super.onPause();

        mHandler.removeCallbacks(mARefresher);

        if (serviceMessenger != null)
            sendServiceMessage(BatteryInfoService.RemoteConnection.SERVICE_UNREGISTER_CLIENT);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem snItem = menu.findItem(R.id.menu_show_notification);

        if (activity.sp_store.getBoolean(BatteryInfoService.KEY_SHOW_NOTIFICATION, true)) {
            snItem.setIcon(R.drawable.ic_menu_stop);
            snItem.setTitle(R.string.menu_hide_notification);
        } else {
            snItem.setIcon(R.drawable.ic_menu_notifications);
            snItem.setTitle(R.string.menu_show_notification);
        }
    }

    private void toggleShowNotification() {
            SharedPreferences.Editor editor = activity.sp_store.edit();
            editor.putBoolean(BatteryInfoService.KEY_SHOW_NOTIFICATION,
                              ! activity.sp_store.getBoolean(BatteryInfoService.KEY_SHOW_NOTIFICATION, true));
            editor.commit();

            Message outgoing = Message.obtain();
            outgoing.what = BatteryInfoService.RemoteConnection.SERVICE_CANCEL_NOTIFICATION_AND_RELOAD_SETTINGS;
            try { if (serviceMessenger != null) serviceMessenger.send(outgoing); } catch (android.os.RemoteException e) {}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            mStartActivity(SettingsActivity.class);
            return true;
        case R.id.menu_close:
            DialogFragment df = new ConfirmCloseDialogFragment();
            // Setting target to this leaks the Fragment, but that's sort of good, as it allows pressing Okay
            //  to work even if the screen rotates.  Even if it rotates many times back and forth, only the
            //  first Fragment is leaked, which will do the closing if Okay is pressed.  Once the dialog is
            //  gone (even if canceled), then the it and the leaked Fragment will be garbage collected.
            df.setTargetFragment(this, 0);
            df.show(getFragmentManager(), "TODO: What is this string for?2");
            return true;
        case R.id.menu_help:
            mStartActivity(HelpActivity.class);
            return true;
        case R.id.menu_show_notification:
            toggleShowNotification();
            return true;
        case R.id.menu_rate_and_review:
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                                         Uri.parse("market://details?id=com.darshancomputing.BatteryIndicatorPro")));
            } catch (Exception e) {
                Toast.makeText(activity, "Sorry, can't launch Market!", Toast.LENGTH_SHORT).show();
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static class ConfirmCloseDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final BatteryInfoActivity activity = (BatteryInfoActivity) getActivity();

            return new AlertDialog.Builder(activity)
                .setTitle(activity.res.getString(R.string.confirm_close))
                .setMessage(activity.res.getString(R.string.confirm_close_hint))
                .setPositiveButton(activity.res.getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            CurrentInfoFragment cif = (CurrentInfoFragment) getTargetFragment();
                            cif.closeApp();
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
            activity.getApplicationContext().unbindService(serviceConnection);
            activity.stopService(biServiceIntent);
            serviceConnected = false;
        }

        activity.finish();
    }

    private void handleUpdatedBatteryInfo(BatteryInfo info) {
        bl.setLevel(info.percent);
        blv.invalidate();

        TextView tv = (TextView) view.findViewById(R.id.level);
        tv.setText("" + info.percent + activity.res.getString(R.string.percent_symbol));

        tv = (TextView) view.findViewById(R.id.time_remaining);
        tv.setText(activity.str.timeRemainingMainScreen(info));
        tv = (TextView) view.findViewById(R.id.until_what);
        tv.setText(activity.str.untilWhat(info));

        int secs = (int) ((System.currentTimeMillis() - info.last_status_cTM) / 1000);
        int hours = secs / (60 * 60);
        int mins = (secs / 60) % 60;

        String s = activity.str.statuses[info.last_status];

        if (info.last_status == BatteryInfo.STATUS_CHARGING)
            s += " " + activity.str.pluggeds[info.last_plugged];

        tv = (TextView) view.findViewById(R.id.status);
        tv.setText(s);

        if (info.last_percent >= 0) {
            s = "Since "; // TODO: Translatable

            if (info.last_status != BatteryInfo.STATUS_FULLY_CHARGED)
                s += info.last_percent + activity.str.percent_symbol + ", ";

            s += hours + "h " + mins + "m ago"; // TODO: Translatable

            tv = (TextView) view.findViewById(R.id.status_duration);
            tv.setText(s);
        }

        Boolean convertF = activity.settings.getBoolean(SettingsActivity.KEY_CONVERT_F,
                                                        activity.res.getBoolean(R.bool.default_convert_to_fahrenheit));

        tv_health.setText(activity.str.healths[info.health]);
        tv_temp.setText(activity.str.formatTemp(info.temperature, convertF));
        if (info.voltage > 500)
            tv_voltage.setText(activity.str.formatVoltage(info.voltage));

        if (info.last_status == BatteryInfo.STATUS_UNPLUGGED)
            plugged_icon.setImageResource(R.drawable.unplugged);
        else
            plugged_icon.setImageResource(R.drawable.not_unplugged);

        refreshCurrent();
    }

    private void refreshCurrent() {
        String s = "";

        if (activity.settings.getBoolean(SettingsActivity.KEY_ENABLE_CURRENT_HACK, false) &&
            activity.settings.getBoolean(SettingsActivity.KEY_DISPLAY_CURRENT_IN_MAIN_WINDOW, false))
        {
            current_icon.setVisibility(View.VISIBLE);

            Long current = null;

            if (activity.settings.getBoolean(SettingsActivity.KEY_PREFER_CURRENT_AVG_IN_MAIN_WINDOW, false))
                current = currentHack.getAvgCurrent();
            if (current == null) // Either don't prefer avg or avg isn't available
                current = currentHack.getCurrent();
            if (current != null)
                s += String.valueOf(current) + "mA";
        } else {
            current_icon.setVisibility(View.INVISIBLE);
        }

        // User may have just turned off hack or main window display of it, so we need to always set the text
        tv_current.setText(s);
    }

    /* mA TextView */
    private final OnClickListener vsListener = new OnClickListener() {
        public void onClick(View v) {
            refreshCurrent();
        }
    };

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

    /*
        case DIALOG_FIRST_RUN:
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(LAYOUT_INFLATER_SERVICE);
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
        ComponentName comp = new ComponentName(activity.getPackageName(), c.getName());
        //startActivity(new Intent().setComponent(comp));
        startActivityForResult(new Intent().setComponent(comp), 1);
        //activity.finish();
    }

    private void bindButtons() {
        if (activity.getPackageManager().resolveActivity(batteryUseIntent, 0) == null) {
            battery_use_b.setEnabled(false); /* TODO: change how the disabled button looks */
        } else {
            battery_use_b.setOnClickListener(buButtonListener);
        }
    }

    // Sets sizes of most Views based on current dimensions
    // Must be called from onCreateView() after inflation and from onConfigurationChanged()
    private void setSizes(Configuration config) {
        boolean portrait = config.orientation == Configuration.ORIENTATION_PORTRAIT;

        int screenWidth, screenHeight;

        if (android.os.Build.VERSION.SDK_INT < 13) {
            DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

            screenWidth = metrics.widthPixels;
            screenHeight = (int) (metrics.heightPixels * 0.95);
        } else {
            screenWidth = (int) (config.screenWidthDp * dpScale);
            screenHeight = (int) (config.screenHeightDp * dpScale);
        }


        int minDimen = Math.min(screenWidth, screenHeight);
        float aspectRatio = (float) screenWidth / screenHeight;

        // System.out.println("...................................................." +
        //                    "\n    config.screenWidthDp: " + config.screenWidthDp +
        //                    "\n    config.screenHeightDp: " + config.screenHeightDp +
        //                    "\n    config figured pixel width: " + screenWidth +
        //                    "\n    config figured pixel height: " + screenHeight +
        //                    "\n    aspectRatio: " + aspectRatio +
        //                    "\n    portrait: " + portrait
        //                    );

        int plugged_icon_height;
        int time_remaining_text_height, until_what_text_height;
        int status_text_height;
        int bu_height, bu_text_height;
        int vital_icon_height, vital_text_height;

        if (portrait) {
            plugged_icon_height = (int) (screenHeight * 0.1);

            time_remaining_text_height = (int) (screenHeight * 0.048);
            until_what_text_height = (int) (screenHeight * 0.032);

            status_text_height = (int) (screenHeight * 0.04);

            bu_height = (int) (screenHeight * 0.14);
            bu_text_height = (int) (screenHeight * 0.035);

            vital_icon_height = (int) (screenHeight * 0.05);
            vital_text_height = (int) (screenHeight * 0.03);
        } else {
            plugged_icon_height = (int) (screenHeight * 0.11);

            time_remaining_text_height = (int) (screenHeight * 0.06);
            until_what_text_height = (int) (screenHeight * 0.04);

            status_text_height = (int) (screenHeight * 0.05);

            bu_height = (int) (screenHeight * 0.18);
            bu_text_height = (int) (screenHeight * 0.045);

            vital_icon_height = (int) (screenHeight * 0.08);
            vital_text_height = (int) (screenHeight * 0.05);
        }

        TextView level = (TextView) view.findViewById(R.id.level);
        level.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, plugged_icon_height);

        View clock = view.findViewById(R.id.clock);
        clock.setLayoutParams(new LinearLayout.LayoutParams(plugged_icon_height,
                                                            ViewGroup.LayoutParams.MATCH_PARENT));

        TextView time_remaining = (TextView) view.findViewById(R.id.time_remaining);
        time_remaining.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, time_remaining_text_height);

        TextView until_what = (TextView) view.findViewById(R.id.until_what);
        until_what.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, until_what_text_height);

        TextView status = (TextView) view.findViewById(R.id.status);
        status.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, status_text_height);

        TextView status_duration = (TextView) view.findViewById(R.id.status_duration);
        status_duration.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, until_what_text_height);

        View plugged_icon = view.findViewById(R.id.plugged_icon);
        plugged_icon.setLayoutParams(new LinearLayout.LayoutParams(plugged_icon_height,
                                                                   ViewGroup.LayoutParams.MATCH_PARENT));

        View plugged_spacer = view.findViewById(R.id.plugged_spacer);
        plugged_spacer.setLayoutParams(new LinearLayout.LayoutParams(plugged_icon_height,
                                                                     plugged_icon_height));

        Button bu_button = (Button) view.findViewById(R.id.battery_use_b);
        bu_button.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                bu_height));
        bu_button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, bu_text_height);

        View temp_icon = view.findViewById(R.id.temp_icon);
        temp_icon.setLayoutParams(new LinearLayout.LayoutParams(vital_icon_height, vital_icon_height));
        TextView temp_text = (TextView) view.findViewById(R.id.temp);
        temp_text.setLayoutParams(new LinearLayout.LayoutParams(0, vital_icon_height, 0.5f));
        temp_text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, vital_text_height);

        View health_icon = view.findViewById(R.id.health_icon);
        health_icon.setLayoutParams(new LinearLayout.LayoutParams(vital_icon_height, vital_icon_height));
        TextView health_text = (TextView) view.findViewById(R.id.health);
        health_text.setLayoutParams(new LinearLayout.LayoutParams(0, vital_icon_height, 0.5f));
        health_text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, vital_text_height);

        View voltage_icon = view.findViewById(R.id.voltage_icon);
        voltage_icon.setLayoutParams(new LinearLayout.LayoutParams(vital_icon_height, vital_icon_height));
        TextView voltage_text = (TextView) view.findViewById(R.id.voltage);
        voltage_text.setLayoutParams(new LinearLayout.LayoutParams(0, vital_icon_height, 0.5f));
        voltage_text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, vital_text_height);

        View current_icon = view.findViewById(R.id.current_icon);
        current_icon.setLayoutParams(new LinearLayout.LayoutParams(vital_icon_height, vital_icon_height));
        TextView current_text = (TextView) view.findViewById(R.id.current);
        current_text.setLayoutParams(new LinearLayout.LayoutParams(0, vital_icon_height, 0.5f));
        current_text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, vital_text_height);

        // When in landscape but close to square, plugged icon gets cramped and cut off; make more room:
        if (!portrait && aspectRatio < 1.32)
            plugged_spacer.setVisibility(View.GONE);
        else
            plugged_spacer.setVisibility(View.INVISIBLE);
    }
}
