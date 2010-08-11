/*
    Copyright (c) 2009, 2010 Josiah Barber (aka Darshan)

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
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
//import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BatteryIndicator extends Activity {
    private Intent biServiceIntent;
    private SharedPreferences settings;
    private final BIServiceConnection biServiceConnection = new BIServiceConnection();

    private static final int DIALOG_CONFIRM_DISABLE_KEYGUARD = 0;

    private Str str;

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateStatus = new Runnable() {
        public void run() {
            updateStatus();
            updateLockscreenButton();
        }
    };

    private final BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /* Give the service a second to process the update first */
            mHandler.postDelayed(mUpdateStatus, 1 * 1000);
            /* Just in case 1 second wasn't enough */
            mHandler.postDelayed(mUpdateStatus, 3 * 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        str = new Str(getResources());

        biServiceIntent = new Intent(this, BatteryIndicatorService.class);
        startService(biServiceIntent);
        bindService(biServiceIntent, biServiceConnection, 0);

        Button button;
        //Button button = (Button) findViewById(R.id.stop_service_b);
        //button.setOnClickListener(ssButtonListener);

        //button = (Button) findViewById(R.id.hide_window_b);
        //button.setOnClickListener(hwButtonListener);

        //button = (Button) findViewById(R.id.more_info_b);
        //button.setOnClickListener(miButtonListener);

        /* TODO: do something like this to disable the menu item */
        //button = (Button) findViewById(R.id.battery_use_b);
        //if (getPackageManager().resolveActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY),0) == null) {
        //    button.setEnabled(false);
        //} else {
        //    button.setOnClickListener(buButtonListener);
        //}

        button = (Button) findViewById(R.id.toggle_lock_screen_b);
        button.setOnClickListener(tlsButtonListener);

        //button = (Button) findViewById(R.id.edit_settings_b);
        //button.setOnClickListener(esButtonListener);

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("serviceDesired", true);
        editor.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(biServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        updateLockscreenButton();
        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        case R.id.menu_battery_use:
            startActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
            return true;
        case R.id.menu_settings:
            mStartActivity(SettingsActivity.class);
            /* TODO: onActivityResult() should reload this page (probably just launch an intent)
                 at least if the requestcode was for the settings activity. */
            return true;
        case R.id.menu_about:
            mStartActivity(InfoActivity.class);
            return true;
        case R.id.menu_close:
            /* TODO: confirm */
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("serviceDesired", false);
            editor.commit();

            finishActivity(1);
            stopService(biServiceIntent);
            finish();

            return true;
        case R.id.menu_help:
            mStartActivity(InfoActivity.class);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
        case DIALOG_CONFIRM_DISABLE_KEYGUARD:
            builder.setTitle(str.confirm_disable) /* TODO: strings.xml */
                .setMessage(str.confirm_disable_hint)  /* TODO */
                .setCancelable(false)
                .setPositiveButton(str.yes, new DialogInterface.OnClickListener() { /* TODO */
                        public void onClick(DialogInterface di, int id) {
                            setDisableLocking(true);
                            di.cancel();
                        }
                    })
                .setNegativeButton(str.cancel, new DialogInterface.OnClickListener() { /* TODO */
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

    private void updateStatus() {
        int last_percent = settings.getInt("last_percent", -1);
        int last_status = settings.getInt("last_status", -1);

        if (last_percent < 0) {
            finish();
            startActivity(getIntent());
        }

        TextView status_since = (TextView) findViewById(R.id.status_since_t);
        TextView title = (TextView) findViewById(R.id.title_t);
        if (last_percent >= 0 && last_status >= 0) {
            String s = "";
            if (last_status == 0)
                s = str.discharging_from + " " + last_percent + str.percent_symbol;
            else if (last_status == 2)
                s = str.charging_from + " " + last_percent + str.percent_symbol;
            else
                s = str.fully_charged;

            status_since.setText(s);

            title.setPadding(title.getPaddingLeft(), title.getPaddingRight(), title.getPaddingTop(), 2);
            status_since.setVisibility(android.view.View.VISIBLE);
        } else {
            title.setPadding(title.getPaddingLeft(), title.getPaddingRight(), title.getPaddingTop(), 10);
            status_since.setVisibility(android.view.View.GONE);
        }
    }

    private void updateLockscreenButton() {
        Button button = (Button) findViewById(R.id.toggle_lock_screen_b);

        if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false))
            button.setText(str.reenable_lock_screen);
        else
            button.setText(str.disable_lock_screen);
    }

    private void setDisableLocking(boolean b) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SettingsActivity.KEY_DISABLE_LOCKING, b);
        editor.commit();

        biServiceConnection.biService.reloadSettings();

        /* Now that I've decided to call finish() here, there's no need to call this anymore */
        //updateLockscreenButton();
        finish();
    }

    /* More Info (Now called "About") */
    private OnClickListener miButtonListener = new OnClickListener() {
        public void onClick(View v) {
            ComponentName comp = new ComponentName(getPackageName(), InfoActivity.class.getName());
            startActivity(new Intent().setComponent(comp));
            finish();
        }
    };

    /* Battery Use */
    /*private OnClickListener buButtonListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                startActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
                finish();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), str.one_six_needed, Toast.LENGTH_SHORT).show();
                ((Button)findViewById(R.id.battery_use_b)).setEnabled(false);
            }
        }
        };*/

    /* Toggle Lock Screen */
    private OnClickListener tlsButtonListener = new OnClickListener() {
        public void onClick(View v) {
            Button button = (Button) findViewById(R.id.toggle_lock_screen_b);

            if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false)) {
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

    /* Edit Settings */
    private OnClickListener esButtonListener = new OnClickListener() {
        public void onClick(View v) {
            ComponentName comp = new ComponentName(getPackageName(), SettingsActivity.class.getName());
            startActivity(new Intent().setComponent(comp));
            finish();
        }
    };

    /* TODO: delete unnecessary onclicklisteners */
    /* Stop Service */
    private OnClickListener ssButtonListener = new OnClickListener() {
        public void onClick(View v) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("serviceDesired", false);
            editor.commit();

            stopService(biServiceIntent);
            finish();
        }
    };

    /* Hide Window */
    private OnClickListener hwButtonListener = new OnClickListener() {
        public void onClick(View v) {
            finish();
        }
    };

    private void mStartActivity(Class c) {
        ComponentName comp = new ComponentName(getPackageName(), c.getName());
        //startActivity(new Intent().setComponent(comp));
        startActivityForResult(new Intent().setComponent(comp), 1);
        //finish();
    }

    private class Str {
        public String discharging_from;
        public String charging_from;
        public String fully_charged;
        public String percent_symbol;
        public String reenable_lock_screen;
        public String disable_lock_screen;
        public String one_six_needed;
        public String confirm_disable;
        public String confirm_disable_hint;
        public String yes;
        public String cancel;

        public Str(Resources  r) {
            discharging_from     = r.getString(R.string.discharging_from);
            charging_from        = r.getString(R.string.charging_from);
            fully_charged        = r.getString(R.string.fully_charged);
            percent_symbol       = r.getString(R.string.percent_symbol);
            reenable_lock_screen = r.getString(R.string.reenable_lock_screen);
            disable_lock_screen  = r.getString(R.string.disable_lock_screen);
            one_six_needed       = r.getString(R.string.one_six_needed);
            confirm_disable      = r.getString(R.string.confirm_disable);
            confirm_disable_hint = r.getString(R.string.confirm_disable_hint);
            yes                  = r.getString(R.string.yes);
            cancel               = r.getString(R.string.cancel);
        }
    }
}
