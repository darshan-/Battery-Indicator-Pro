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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BatteryInfoActivity extends Activity implements BatteryInfoService.OnBatteryInfoUpdatedListener {
    private Intent biServiceIntent;
    private SharedPreferences settings;
    private SharedPreferences sp_store;
    private final CallbackServiceConnection biServiceConnection = new CallbackServiceConnection();

    private static final Intent batteryUseIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    private static final IntentFilter batteryChangedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private Resources res;
    private Context context;
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

    private final BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (! Intent.ACTION_BATTERY_CHANGED.equals(action)) return;

            // TODO: Make sure Service is running?  Or else remove this altogether
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        res = getResources();
        str = new Str(res);
        context = getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        bl = new BatteryLevel(context);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.battery_info);
        blv = (ImageView) findViewById(R.id.battery_level_view);
        blv.setImageBitmap(bl.getBitmap());

        sp_store = context.getSharedPreferences("sp_store", 0);

        disallowLockButton = settings.getBoolean(SettingsActivity.KEY_DISALLOW_DISABLE_LOCK_SCREEN, false);

        toggle_lock_screen_b = (Button) findViewById(R.id.toggle_lock_screen_b);
        battery_use_b = (Button) findViewById(R.id.battery_use_b);

        if (settings.getBoolean(SettingsActivity.KEY_FIRST_RUN, true)) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(SettingsActivity.KEY_FIRST_RUN, false);
            editor.commit();
        }

        biServiceIntent = new Intent(this, BatteryInfoService.class);
        startService(biServiceIntent);
        bindService(biServiceIntent, biServiceConnection, 0);

        SharedPreferences.Editor editor = sp_store.edit();
        editor.putBoolean(BatteryInfoService.KEY_SERVICE_DESIRED, true);
        editor.commit();

        bindButtons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(biServiceConnection);
        bl.getBitmap().recycle();
    }

    /*private void restartIfLanguageChanged() {
        String curLanguage = settings.getString(SettingsActivity.KEY_LANGUAGE_OVERRIDE, "default");
        if (curLanguage.equals(oldLanguage))
            return;

        Str.overrideLanguage(res, getWindowManager(), curLanguage);
        mStartActivity(BatteryInfoActivity.class);
        finish();
    }*/

    private class CallbackServiceConnection extends BIServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            super.onServiceConnected(name, service);
            biService.registerOnBatteryInfoUpdatedListener(BatteryInfoActivity.this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (biServiceConnection.biService != null)
            biServiceConnection.biService.registerOnBatteryInfoUpdatedListener(this);

        registerReceiver(mBatteryInfoReceiver, batteryChangedFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        biServiceConnection.biService.unregisterOnBatteryInfoUpdatedListener(this);
        unregisterReceiver(mBatteryInfoReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_logs:
            mStartActivity(LogViewActivity.class);
            return true;
        case R.id.menu_settings:
            mStartActivity(SettingsActivity.class);
            return true;
        case R.id.menu_close:
            showDialog(DIALOG_CONFIRM_CLOSE);
            return true;
        case R.id.menu_help:
            mStartActivity(HelpActivity.class);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Boolean oldDisallow = disallowLockButton;
        disallowLockButton = settings.getBoolean(SettingsActivity.KEY_DISALLOW_DISABLE_LOCK_SCREEN, false);
    }

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
                        finish();

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
            View layout = inflater.inflate(R.layout.first_run_message, (LinearLayout) findViewById(R.id.layout_root));

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

    public void onBatteryInfoUpdated(BatteryInfo info) {
        bl.setLevel(info.percent);
        blv.invalidate();

        TextView tv = (TextView) findViewById(R.id.level);
        tv.setText("" + info.percent + res.getString(R.string.percent_symbol));

        int[] prediction = biServiceConnection.biService.getPrediction();

        int hours  = prediction[0];
        int mins   = prediction[1];
        //int status = prediction[2];

        if (info.status == BatteryInfo.STATUS_FULLY_CHARGED) {
            tv = (TextView) findViewById(R.id.time_remaining);
            tv.setText(android.text.Html.fromHtml("<font color=\"#6fc14b\">" + str.statuses[info.status] + "</font>")); // TODO: color
        } else {
            String until_text;

            if (info.status == BatteryInfo.STATUS_CHARGING)
                until_text = "until charged"; // TODO: Translatable
            else
                until_text = "until drained"; // TODO: Translatable

            tv = (TextView) findViewById(R.id.time_remaining);
            // TODO: Translatable ("h" and "m"); color
            tv.setText(android.text.Html.fromHtml("<font color=\"#6fc14b\">" + hours + "h</font> " +
                                                  "<font color=\"#33b5e5\"><small>" +  mins + "m</small></font>"));

            tv = (TextView) findViewById(R.id.until_what);
            tv.setText(until_text);
        }

        int secs = (int) ((System.currentTimeMillis() - info.last_status_cTM) / 1000);
           hours = secs / (60 * 60);
            mins = (secs / 60) % 60;

        String s = str.statuses[info.status];

        if (info.status == BatteryInfo.STATUS_CHARGING)
            s += " " + str.pluggeds[info.last_plugged]; /* Add '(AC)', '(USB)', '(?)', or '(Wireless)' */

        // TODO: Don't show 'since 100%' for Fully Charged status
        tv = (TextView) findViewById(R.id.status);
        tv.setText(s);

        s = "Since "; // TODO: Translatable

        if (info.status != BatteryInfo.STATUS_FULLY_CHARGED)
            s += info.last_percent + str.percent_symbol + ", ";

        s += hours + "h " + mins + "m ago"; // TODO: Translatable

        tv = (TextView) findViewById(R.id.status_duration);
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

        biServiceConnection.biService.reloadSettings();

        updateLockscreenButton();

        if (settings.getBoolean(SettingsActivity.KEY_FINISH_AFTER_TOGGLE_LOCK, false)) finish();
    }

    /* Battery Use */
    private final OnClickListener buButtonListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                startActivity(batteryUseIntent);
                if (settings.getBoolean(SettingsActivity.KEY_FINISH_AFTER_BATTERY_USE, false)) finish();
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
                    showDialog(DIALOG_CONFIRM_DISABLE_KEYGUARD);
                } else {
                    setDisableLocking(true);
                }
            }
        }
    };

    private void mStartActivity(Class c) {
        ComponentName comp = new ComponentName(getPackageName(), c.getName());
        //startActivity(new Intent().setComponent(comp));
        startActivityForResult(new Intent().setComponent(comp), 1);
        //finish();
    }

    private void bindButtons() {
        if (getPackageManager().resolveActivity(batteryUseIntent, 0) == null) {
            battery_use_b.setEnabled(false); /* TODO: change how the disabled button looks */
        } else {
            battery_use_b.setOnClickListener(buButtonListener);
        }

        toggle_lock_screen_b.setOnClickListener(tlsButtonListener);
    }
}
