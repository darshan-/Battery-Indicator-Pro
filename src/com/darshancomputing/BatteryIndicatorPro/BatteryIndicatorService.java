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

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import java.util.Date;

public class BatteryIndicatorService extends Service {
    private final IntentFilter batteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private Intent notificationIntent;

    private NotificationManager mNotificationManager;
    private SharedPreferences settings;
    private KeyguardLock kl;

    private Boolean keyguardDisabled = false;

    private String degree_symbol;
    private String fahrenheit_symbol;
    private String celsius_symbol;
    private String volt_symbol;

    @Override
    public void onCreate() {
        //android.os.Debug.startMethodTracing();

        loadResources();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        registerReceiver(mBatteryInfoReceiver, batteryChanged);
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        notificationIntent = new Intent(getApplicationContext(), BatteryIndicator.class);

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock(getPackageName());

        if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false))
            setEnablednessOfKeyguard(false);
    }

    @Override
    public void onDestroy() {
        setEnablednessOfKeyguard(true);

        unregisterReceiver(mBatteryInfoReceiver);
        mNotificationManager.cancelAll();
        //android.os.Debug.stopMethodTracing();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        BatteryIndicatorService getService() {
            return BatteryIndicatorService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    private final BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        /* TODO: move these to strings.xml */
        private final String[] statuses = {"Unplugged", "", "Charging", "Discharging", "Not Charging", "Fully Charged"};
        private final String[] healths = {"", "", "Good Health", "Overheat", "Dead", "Overvoltage", "Failure"};
        private final String[] pluggeds = {"", " (AC)", " (USB)"};

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (! Intent.ACTION_BATTERY_CHANGED.equals(action)) return;

            int level = intent.getIntExtra("level", 0);
            int scale = intent.getIntExtra("scale", 100);
            int status = intent.getIntExtra("status", 0);
            int health = intent.getIntExtra("health", 0);
            int plugged = intent.getIntExtra("plugged", 0);
            int temperature = intent.getIntExtra("temperature", 0);
            int voltage = intent.getIntExtra("voltage", 0);
            //String technology = intent.getStringExtra("technology");

            int percent = level * 100 / scale;

            if (percent % 5 == 0) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("previous_charge", percent);
                editor.commit();
            }

            /* I Take advantage of (count on) R.java having resources alphabetical and incrementing by one */

            int icon;
            if (settings.getBoolean(SettingsActivity.KEY_RED, false) &&
                percent < Integer.valueOf(settings.getString(SettingsActivity.KEY_RED_THRESH, "0")) &&
                percent <= SettingsActivity.RED_ICON_MAX) {
                icon = R.drawable.r000 + percent - 0;
            } else if (settings.getBoolean(SettingsActivity.KEY_AMBER, false) &&
                       percent < Integer.valueOf(settings.getString(SettingsActivity.KEY_AMBER_THRESH, "0")) &&
                       percent <= SettingsActivity.AMBER_ICON_MAX &&
                       percent >= SettingsActivity.AMBER_ICON_MIN){
                icon = R.drawable.a000 + percent - 0;
            } else if (settings.getBoolean(SettingsActivity.KEY_GREEN, false) &&
                       percent >= Integer.valueOf(settings.getString(SettingsActivity.KEY_GREEN_THRESH, "101")) &&
                       percent >= SettingsActivity.GREEN_ICON_MIN) {
                icon = R.drawable.g020 + percent - 20;
            } else {
                icon = R.drawable.b000 + percent;
            }

            /* Just treating any unplugged status as simply "Unplugged" now.
               Note that the main activity now assumes that the status is always 0, 2, or 5 */
            if (plugged == 0) status = 0;

            String statusStr = statuses[status];
            if (status == 2) statusStr += pluggeds[plugged]; /* Add '(AC)' or '(USB)' if charging */

            String temp_s;
            if (settings.getBoolean(SettingsActivity.KEY_CONVERT_F, false)){
                temp_s = String.valueOf((java.lang.Math.round(temperature * 9 / 5.0) / 10.0) + 32.0) +
                    degree_symbol + fahrenheit_symbol;
            } else {
                temp_s = String.valueOf(temperature / 10.0) + degree_symbol + celsius_symbol;
            }

            int last_status = settings.getInt("last_status", -1);
            long last_status_cTM = settings.getLong("last_status_cTM", -1);
            int last_percent = settings.getInt("last_percent", -1);
            int previous_charge = settings.getInt("previous_charge", 100);
            long currentTM = System.currentTimeMillis();
            long statusDuration;
            String last_status_since = settings.getString("last_status_since", null);

            if (last_status != status || last_status_cTM == -1 || last_percent == -1 ||
                last_status_cTM > currentTM || last_status_since == null ||
                (plugged == 0 && percent > previous_charge + 1))
            {
                last_status_since = formatTime(new Date());
                statusDuration = 0;

                SharedPreferences.Editor editor = settings.edit();
                editor.putString("last_status_since", last_status_since);
                editor.putLong("last_status_cTM", currentTM);
                editor.putInt("last_status", status);
                editor.putInt("last_percent", percent);
                editor.putInt("previous_charge", percent);

                last_status_cTM = currentTM;

                if (last_status != status && settings.getBoolean(SettingsActivity.KEY_AUTO_DISABLE_LOCKING, false)) {
                    if (last_status == 0) {
                        editor.putBoolean(SettingsActivity.KEY_DISABLE_LOCKING, true);
                        setEnablednessOfKeyguard(false);
                    } else if (status == 0) {
                        editor.putBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false);
                        setEnablednessOfKeyguard(true);

                        /* If the screen was on (no active keyguard) when the device was plugged in (disabling the
                             keyguard), and the screen is off now, then the keyguard is still disabled. That's
                             stupid.  As a workaround, let's aquire a wakelock that forces the screen to turn on,
                             then release it. This is unfortunate but seems better than not doing it, which would
                             result in no keyguard when you unplug and throw your phone in your pocket. */

                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                                                                  PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                                                  PowerManager.ON_AFTER_RELEASE, getPackageName());
                        wl.acquire();
                        wl.release();
                    }
                }

                editor.commit();
            } else {
                statusDuration = currentTM - last_status_cTM;
            }

            /* Add half an hour, then divide.  Should end up rounding to the closest hour. */
            int statusDurationHours = (int)((statusDuration + (1000 * 60 * 30)) / (1000 * 60 * 60));

            CharSequence contentTitle;

            int status_dur_est = Integer.valueOf(settings.getString(SettingsActivity.KEY_STATUS_DUR_EST, "12"));
            if (statusDurationHours < status_dur_est) { /* TODO: Since, For, Hour, Hours (use Plurals) */
                contentTitle = statusStr + " Since " + last_status_since;
            } else {
                contentTitle = statusStr + " For " + String.valueOf(statusDurationHours) + " Hour";
                if (statusDurationHours != 1){contentTitle = contentTitle + "s";}
            }

            CharSequence contentText = healths[health] + " / " + temp_s + " / " +
                                       String.valueOf(voltage / 1000.0) + volt_symbol;

            Notification notification = new Notification(icon, null, System.currentTimeMillis());

            notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

            mNotificationManager.notify(1, notification);
        }
    };

    private String formatTime(Date d) {
        String format = android.provider.Settings.System.getString(getContentResolver(),
                                                                android.provider.Settings.System.TIME_12_24);
        if (format == null || format.equals("12")) {
            return java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT,
                                                        java.util.Locale.getDefault()).format(d);
        } else {
            return (new java.text.SimpleDateFormat("HH:mm")).format(d);
        }
    }

    /* Old versions of Android (haven't experimented to determine exactly which are included), at least on
         the emulator, really don't want you to call reenableKeyguard() if you haven't first disabled it.
         So to stay compatible with older devices, let's add an extra setting and add this function. */
    private void setEnablednessOfKeyguard(boolean enabled) {
        if (enabled) {
            if (keyguardDisabled) {
                kl.reenableKeyguard();
                keyguardDisabled = false;
            }
        } else {
            if (! keyguardDisabled) {
                kl.disableKeyguard();
                keyguardDisabled = true;
            }
        }
    }

    public void reloadSettings() {
        if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false))
            setEnablednessOfKeyguard(false);
        else
            setEnablednessOfKeyguard(true);

        //unregisterReceiver(mBatteryInfoReceiver); /* It appears that there's no need to unregister first */
        registerReceiver(mBatteryInfoReceiver, batteryChanged);
    }

    private void loadResources() {
        Resources r = getResources();

        degree_symbol = r.getString(R.string.degree_symbol);
        fahrenheit_symbol = r.getString(R.string.fahrenheit_symbol);
        celsius_symbol = r.getString(R.string.celsius_symbol);
        volt_symbol = r.getString(R.string.volt_symbol);
    }
}
