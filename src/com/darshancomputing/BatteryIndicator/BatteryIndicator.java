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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BatteryIndicator extends Activity {
    Intent biServiceIntent;
    SharedPreferences settings;

    static final int DIALOG_CONFIRM_DISABLE_KEYGUARD = 0;

    final Handler mHandler = new Handler();
    final Runnable mUpdateStatus = new Runnable() {
        public void run() {
            updateStatus();
            updateLockscreenButton();
        }
    };

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
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
        biServiceIntent = new Intent(this, BatteryIndicatorService.class);
        startService(biServiceIntent);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button button = (Button)findViewById(R.id.stop_service_b);
        button.setOnClickListener(ssButtonListener);

        button = (Button)findViewById(R.id.hide_window_b);
        button.setOnClickListener(hwButtonListener);

        button = (Button)findViewById(R.id.more_info_b);
        button.setOnClickListener(miButtonListener);

        button = (Button)findViewById(R.id.battery_use_b);
        if (getPackageManager().resolveActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY),0) == null) {
            button.setEnabled(false);
        } else {
            button.setOnClickListener(buButtonListener);
        }

        button = (Button)findViewById(R.id.toggle_lock_screen_b);
        button.setOnClickListener(tlsButtonListener);

        button = (Button)findViewById(R.id.edit_settings_b);
        button.setOnClickListener(esButtonListener);

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("serviceDesired", true);
        editor.commit();
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
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
        case DIALOG_CONFIRM_DISABLE_KEYGUARD:
            builder.setTitle("Really disable lock screen?")
                .setMessage("Hint: Disable this confirmation dialog in the settings...")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface di, int id) {
                            setEnablednessOfKG(true);
                            di.cancel();
                        }
                    })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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

        TextView status_since = (TextView)findViewById(R.id.status_since_t);
        TextView title = (TextView)findViewById(R.id.title_t);
        if (last_percent >= 0 && last_status >= 0) {
            String s = "";
            String lp = "" + last_percent + "%";
            if (last_status == 0)
                s = getResources().getString(R.string.discharging_from) + " " + lp;
            else if (last_status == 2)
                s = getResources().getString(R.string.charging_from) + " " + lp;
            else
                s = getResources().getString(R.string.fully_charged);

            status_since.setText(s);

            title.setPadding(title.getPaddingLeft(), title.getPaddingRight(), title.getPaddingTop(), 2);
            status_since.setVisibility(android.view.View.VISIBLE);
        } else {
            title.setPadding(title.getPaddingLeft(), title.getPaddingRight(), title.getPaddingTop(), 10);
            status_since.setVisibility(android.view.View.GONE);
        }
    }

    private void updateLockscreenButton() {
        Button button = (Button)findViewById(R.id.toggle_lock_screen_b);

        if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false))
            button.setText("Reenable\nLock Screen");
        else
            button.setText("Disable\nLock Screen");
    }

    private void setEnablednessOfKG(boolean b) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SettingsActivity.KEY_DISABLE_LOCKING, b);
        editor.commit();
        stopService(biServiceIntent);
        startService(biServiceIntent);
        /* Now that I've decided to call finish() here, there's no need to call this anymore */
        //updateLockscreenButton();
        finish();
    }

    /* More Info (Now called "About") */
    private OnClickListener miButtonListener = new OnClickListener() {
        public void onClick(View v) {
            ComponentName comp = new ComponentName(getPackageName(),
                                                   InfoActivity.class.getName());
            startActivity(new Intent().setComponent(comp));
            finish();
        }
    };

    /* Battery Use */
    private OnClickListener buButtonListener = new OnClickListener() {
        public void onClick(View v) {
            try {
                startActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY));
                finish();
                //} catch (android.content.ActivityNotFoundException e) {
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.one_six_needed), Toast.LENGTH_SHORT).show();
                ((Button)findViewById(R.id.battery_use_b)).setEnabled(false);
            }
        }
    };

    /* Toggle Lock Screen */
    private OnClickListener tlsButtonListener = new OnClickListener() {
        public void onClick(View v) {
            Button button = (Button)findViewById(R.id.toggle_lock_screen_b);

            if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false)) {
                setEnablednessOfKG(false);
            } else {
                if (settings.getBoolean(SettingsActivity.KEY_CONFIRM_DISABLE_LOCKING, true)) {
                    showDialog(DIALOG_CONFIRM_DISABLE_KEYGUARD);
                } else {
                    setEnablednessOfKG(true);
                }
            }
        }
    };

    /* Edit Settings */
    private OnClickListener esButtonListener = new OnClickListener() {
        public void onClick(View v) {
            ComponentName comp = new ComponentName(getPackageName(),
                                                   SettingsActivity.class.getName());
            startActivity(new Intent().setComponent(comp));
            finish();
        }
    };

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
}
