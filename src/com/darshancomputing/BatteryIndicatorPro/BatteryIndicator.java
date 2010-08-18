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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BatteryIndicator extends Activity {
    private Intent biServiceIntent;
    private SharedPreferences settings;
    private final BIServiceConnection biServiceConnection = new BIServiceConnection();
    private Resources res;
    private Context context;
    private Str str;
    private String themeName;
    MainWindowTheme.Theme theme;
    private int percent = -1;
    private Intent batteryUseIntent;
    private DisplayMetrics metrics;
    private Button battery_use_b;
    private Button toggle_lock_screen_b;

    private static final int DIALOG_CONFIRM_DISABLE_KEYGUARD = 0;

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateStatus = new Runnable() {
        public void run() {
            updateStatus();
            updateLockscreenButton();
            updateTimes();
        }
    };

    private final BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (! Intent.ACTION_BATTERY_CHANGED.equals(action)) return;

            int level = intent.getIntExtra("level", -1);
            int scale = intent.getIntExtra("scale", 100);

            percent = level * 100 / scale;

            mHandler.post(mUpdateStatus);
            /* Give the service a second to process the update */
            mHandler.postDelayed(mUpdateStatus, 1 * 1000);
            /* Just in case 1 second wasn't enough */
            mHandler.postDelayed(mUpdateStatus, 4 * 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        res = getResources();
        str = new Str();
        context = getApplicationContext();
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        batteryUseIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);

        settings = PreferenceManager.getDefaultSharedPreferences(context);

        themeName = settings.getString(SettingsActivity.KEY_MW_THEME, "default");
        setTheme();

        biServiceIntent = new Intent(this, BatteryIndicatorService.class);
        startService(biServiceIntent);
        bindService(biServiceIntent, biServiceConnection, 0);

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
        updateTimes();
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
        case R.id.menu_settings:
            mStartActivity(SettingsActivity.class);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String oldThemeName = themeName;
        themeName = settings.getString(SettingsActivity.KEY_MW_THEME, "default");

        if (! oldThemeName.equals(themeName)) {
            setTheme();
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
        int last_status = settings.getInt("last_status", 0);

        TextView status_since = (TextView) findViewById(R.id.status_since_t);

        String s;
        if (last_status == 0)
            s = str.discharging_from + " " + last_percent + str.percent_symbol;
        else if (last_status == 2)
            s = str.charging_from + " " + last_percent + str.percent_symbol;
        else
            s = str.fully_charged;

        status_since.setText(s);
    }

    private void updateLockscreenButton() {
        if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false))
            toggle_lock_screen_b.setText(str.reenable_lock_screen);
        else
            toggle_lock_screen_b.setText(str.disable_lock_screen);
    }

    private void setDisableLocking(boolean b) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SettingsActivity.KEY_DISABLE_LOCKING, b);
        editor.commit();

        biServiceConnection.biService.reloadSettings();

        updateLockscreenButton();

        if (settings.getBoolean(SettingsActivity.KEY_FINISH_AFTER_TOGGLE_LOCK, false)) finish();
    }

    /* Battery Use */
    private OnClickListener buButtonListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                startActivity(batteryUseIntent);
            } catch (Exception e) {
                Toast.makeText(context, str.one_six_needed, Toast.LENGTH_SHORT).show();
                battery_use_b.setEnabled(false);
            }
        }
    };

    /* Toggle Lock Screen */
    private OnClickListener tlsButtonListener = new OnClickListener() {
        public void onClick(View v) {
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

    private void mStartActivity(Class c) {
        ComponentName comp = new ComponentName(getPackageName(), c.getName());
        //startActivity(new Intent().setComponent(comp));
        startActivityForResult(new Intent().setComponent(comp), 1);
        //finish();
    }

    private void bindButtons() {
        if (getPackageManager().resolveActivity(batteryUseIntent,0) == null) {
            battery_use_b.setEnabled(false); /* TODO: change how the disabled button looks */
        } else {
            battery_use_b.setOnClickListener(buButtonListener);
        }

        toggle_lock_screen_b.setOnClickListener(tlsButtonListener);
    }

    private void setTheme() {
        theme = (new MainWindowTheme(themeName, metrics, res)).theme;

        LinearLayout main_layout = (LinearLayout) findViewById(R.id.main_layout);
        main_layout.removeAllViews();
        LinearLayout main_frame = (LinearLayout) View.inflate(context, theme.mainFrameLayout, main_layout);

        main_layout.setPadding(theme.mainLayoutPaddingLeft, theme.mainLayoutPaddingTop,
                               theme.mainLayoutPaddingRight, theme.mainLayoutPaddingBottom);

        updateTimes();

        battery_use_b = (Button) main_frame.findViewById(R.id.battery_use_b);
        toggle_lock_screen_b = (Button) main_frame.findViewById(R.id.toggle_lock_screen_b);

        bindButtons();
    }

    private void updateTimes() {
        for (int i = 0; i < theme.timeRemainingIds.length; i++) {
            LinearLayout ll = (LinearLayout) findViewById(theme.timeRemainingIds[i]);
            if (ll != null) {
                TextView label = (TextView) ll.findViewById(R.id.label);
                TextView time = (TextView) ll.findViewById(R.id.time);

                if (theme.timeRemainingVisible(i, settings)) {
                    label.setText(res.getString(theme.timeRemainingStrings[i]));
                    label.setTextColor(res.getColor(theme.timeRemainingColors[i]));
                    time.setText(theme.timeRemaining(i, settings, percent));
                    time.setTextColor(res.getColor(theme.timeRemainingColors[i]));
                    label.setVisibility(View.VISIBLE);
                    time.setVisibility(View.VISIBLE);
                } else {
                    label.setVisibility(View.GONE);
                    time.setVisibility(View.GONE);
                }
            }
        }
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
        public String battery_use_b;

        public Str() {
            discharging_from     = res.getString(R.string.discharging_from);
            charging_from        = res.getString(R.string.charging_from);
            fully_charged        = res.getString(R.string.fully_charged);
            percent_symbol       = res.getString(R.string.percent_symbol);
            reenable_lock_screen = res.getString(R.string.reenable_lock_screen);
            disable_lock_screen  = res.getString(R.string.disable_lock_screen);
            one_six_needed       = res.getString(R.string.one_six_needed);
            confirm_disable      = res.getString(R.string.confirm_disable);
            confirm_disable_hint = res.getString(R.string.confirm_disable_hint);
            yes                  = res.getString(R.string.yes);
            cancel               = res.getString(R.string.cancel);
            battery_use_b        = res.getString(R.string.battery_use_b);
        }
    }
}
