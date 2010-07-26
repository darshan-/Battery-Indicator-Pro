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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.preference.PreferenceManager;
import java.util.Date;

public class BatteryIndicatorService extends Service{
    private NotificationManager mNotificationManager;
    private SharedPreferences settings;
    private KeyguardLock kl;

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock(getPackageName());

        if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false))
            kl.disableKeyguard();
    }

    @Override
    public void onDestroy() {
        kl.reenableKeyguard();

        unregisterReceiver(mBatteryInfoReceiver);
        mNotificationManager.cancelAll();
    }

    /* Apparently I need this... */
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                int status = intent.getIntExtra("status", 0);
                int health = intent.getIntExtra("health", 0);
                int plugged = intent.getIntExtra("plugged", 0);
                int temperature = intent.getIntExtra("temperature", 0);
                int voltage = intent.getIntExtra("voltage", 0);
                //String technology = intent.getStringExtra("technology");

                int percent = level * 100 / scale;

                /* I Take advantage of (count on) R.java having resources alphabetical and incrementing by one */

                int icon;
                if (settings.getBoolean(SettingsActivity.KEY_RED, false) &&
                    (percent < Integer.valueOf(settings.getString(SettingsActivity.KEY_RED_THRESH, "0"))) &&
                    (percent < SettingsActivity.RED_MAX)){ /* Red max has decreased in newer version, so we must check */
                    icon = R.drawable.r000 + percent;
                } else if (settings.getBoolean(SettingsActivity.KEY_AMBER, false) &&
                           (percent < Integer.valueOf(settings.getString(SettingsActivity.KEY_AMBER_THRESH, "0")))){
                    icon = R.drawable.a010 + percent - 10;
                } else if (settings.getBoolean(SettingsActivity.KEY_GREEN, false) &&
                           (percent >= Integer.valueOf(settings.getString(SettingsActivity.KEY_GREEN_THRESH, "101"))) &&
                           (percent >= SettingsActivity.GREEN_MIN)) {
                    icon = R.drawable.g030 + percent - 30;
                } else {
                  icon = R.drawable.b000 + percent;
                }

                //String[] statuses = {"", "", " Charging", " Discharging", " Not Charging", " Fully Charged"};
                String[] statuses = {"Unplugged", "", "Charging", "Discharging", "Not Charging", "Fully Charged"};
                String[] healths = {"", "", "Good Health", "Overheat", "Dead", "Overvoltage", "Failure"};
                String[] pluggeds = {"", " (AC)", " (USB)"};

                if (plugged > 0) {
                    statuses[2] += pluggeds[plugged];
                } else {
                    status = 0; /* Just treating any unplugged status as simply "Unplugged" now */
                    /* Note that the main activity now assumes that the status is always 0, 2, or 5 */
                }

                String temp_s;
                if (settings.getBoolean(SettingsActivity.KEY_CONVERT_F, false)){
                    temp_s = String.valueOf((java.lang.Math.round(temperature * 9 / 5.0) / 10.0) + 32.0) +
                        getResources().getString(R.string.degree_symbol) + "F";
                } else {
                    temp_s = String.valueOf(temperature / 10.0) + getResources().getString(R.string.degree_symbol) + "C";
                }

                int last_status = settings.getInt("last_status", -1);
                long last_status_cTM = settings.getLong("last_status_cTM", -1);
                int last_percent = settings.getInt("last_percent", -1);
                long currentTM = System.currentTimeMillis();
                long statusDuration = 0;
                String last_status_since = "";
                String curTimeStr = formatTime(new Date());

                /* Main activity assumes that last_percent is always above -1 if service is running --
                   if it gets a negative value, it restarts until it gets a non-negative value */
                if (last_status != status || last_status_cTM == -1 || last_percent == -1) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("last_status_since", curTimeStr);
                    editor.putLong("last_status_cTM", currentTM);
                    editor.putInt("last_status", status);
                    editor.putInt("last_percent", percent);
                    editor.commit();

                    /* If this was -1, this is the first run with this feature. Treat the status as having begun now. */
                    last_status_cTM = currentTM;
                }

                if (last_status_cTM > currentTM) {
                    /* This can happen by travelling west by enough timezones quickly enough, or simply by manually
                         setting the clock earlier.  Either way, the simplest thing to do, and what I'll do for now,
                         is to just start counting from now.  I may eventually try to keep track of timezones. */
                    statusDuration = 0;
                    last_status_since = curTimeStr;
                } else {
                    statusDuration = currentTM - last_status_cTM;
                    last_status_since = settings.getString("last_status_since", curTimeStr);
                }

                /* Add half an hour, then divide.  Should end up rounding to the closest hour. */
                int statusDurationHours = (int)((statusDuration + (1000 * 60 * 30)) / (1000 * 60 * 60));

                CharSequence contentTitle;

                int status_dur_est = Integer.valueOf(settings.getString(SettingsActivity.KEY_STATUS_DUR_EST, "12"));
                if (statusDurationHours < status_dur_est) {
                    contentTitle = statuses[status] + " Since " + last_status_since;
                } else {
                    contentTitle = statuses[status] + " For " + String.valueOf(statusDurationHours) + " Hour";
                    if (statusDurationHours != 1){contentTitle = contentTitle + "s";}
                }

                CharSequence contentText = healths[health] + " / " +
                                           temp_s + " / " +
                                           String.valueOf(voltage / 1000.0) + "V";

                Notification notification = new Notification(icon, null /* Ticker Text */,
                                                             System.currentTimeMillis());

                notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
                Intent notificationIntent = new Intent(context, BatteryIndicator.class);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

                notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

                mNotificationManager.notify(1, notification);
            }
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
}
