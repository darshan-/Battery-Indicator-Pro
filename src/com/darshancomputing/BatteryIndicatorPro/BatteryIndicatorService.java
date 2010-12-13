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
import android.database.Cursor;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import java.util.Date;

public class BatteryIndicatorService extends Service {
    private final IntentFilter batteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private final IntentFilter userPresent    = new IntentFilter(Intent.ACTION_USER_PRESENT);
    private Intent mainWindowIntent;
    private Intent alarmsIntent;

    private NotificationManager mNotificationManager;
    private SharedPreferences settings;
    private KeyguardLock kl;
    private KeyguardManager km;
    private android.os.Vibrator mVibrator;
    private android.media.AudioManager mAudioManager;

    private Boolean keyguardDisabled = false;

    private Resources res;
    private Str str;
    private AlarmDatabase alarms;

    private static final int NOTIFICATION_PRIMARY      = 1;
    private static final int NOTIFICATION_ALARM_CHARGE = 2;
    private static final int NOTIFICATION_ALARM_HEALTH = 3;
    private static final int NOTIFICATION_ALARM_TEMP   = 4;

    @Override
    public void onCreate() {
        res = getResources();
        str = new Str(res);
        Context context = getApplicationContext();

        alarms = new AlarmDatabase(context);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mVibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mAudioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);

        registerReceiver(mBatteryInfoReceiver, batteryChanged);
        settings = PreferenceManager.getDefaultSharedPreferences(context);

        mainWindowIntent = new Intent(context, BatteryIndicator.class);
        alarmsIntent = new Intent(context, AlarmsActivity.class);

        km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock(getPackageName());

        if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false))
            setEnablednessOfKeyguard(false);
    }

    @Override
    public void onDestroy() {
        setEnablednessOfKeyguard(true);

        unregisterReceiver(mBatteryInfoReceiver);
        mNotificationManager.cancelAll();
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
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences.Editor editor = settings.edit();
            Notification notification;
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

            /* I Take advantage of (count on) R.java having resources alphabetical and incrementing by one */

            int icon;
            if (settings.getBoolean(SettingsActivity.KEY_RED, res.getBoolean(R.bool.default_use_red)) &&
                percent < Integer.valueOf(settings.getString(SettingsActivity.KEY_RED_THRESH, str.default_red_thresh)) &&
                percent <= SettingsActivity.RED_ICON_MAX) {
                icon = R.drawable.r000 + percent - 0;
            } else if (settings.getBoolean(SettingsActivity.KEY_AMBER, res.getBoolean(R.bool.default_use_amber)) &&
                       percent < Integer.valueOf(settings.getString(SettingsActivity.KEY_AMBER_THRESH, str.default_amber_thresh)) &&
                       percent <= SettingsActivity.AMBER_ICON_MAX &&
                       percent >= SettingsActivity.AMBER_ICON_MIN){
                icon = R.drawable.a000 + percent - 0;
            } else if (settings.getBoolean(SettingsActivity.KEY_GREEN, res.getBoolean(R.bool.default_use_green)) &&
                       percent >= Integer.valueOf(settings.getString(SettingsActivity.KEY_GREEN_THRESH, str.default_green_thresh)) &&
                       percent >= SettingsActivity.GREEN_ICON_MIN) {
                icon = R.drawable.g020 + percent - 20;
            } else {
                icon = R.drawable.b000 + percent;
            }

            /* Just treating any unplugged status as simply "Unplugged" now.
               Note that the main activity now assumes that the status is always 0, 2, or 5 */
            if (plugged == 0) status = 0; /* TODO: use static class CONSTANTS instead of numbers */

            /* TODO: I guess I should make sure status, plugged, and health are all within array limits...
                     If one is out of bounds, set it to 1 or 0, whichever is "Unknown"... */
            String statusStr = str.statuses[status];
            if (status == 2) statusStr += " " + str.pluggeds[plugged]; /* Add '(AC)' or '(USB)' if charging */

            int last_status = settings.getInt("last_status", -1);
            /* There's a bug, at least on 1.5, or maybe depending on the hardware (I've noticed it on the MyTouch with 1.5)
               where USB is recognized as AC at first, then quickly changed to USB.  So we need to test if plugged changed. */
            int last_plugged = settings.getInt("last_plugged", -1);
            long last_status_cTM = settings.getLong("last_status_cTM", -1);
            int last_percent = settings.getInt("last_percent", -1);
            int previous_charge = settings.getInt("previous_charge", 100);
            long currentTM = System.currentTimeMillis();
            long statusDuration;
            String last_status_since = settings.getString("last_status_since", null);
            LogDatabase logs = new LogDatabase(context);

            if (last_status != status || last_status_cTM == -1 || last_percent == -1 ||
                last_status_cTM > currentTM || last_status_since == null || last_plugged != plugged ||
                (plugged == 0 && percent > previous_charge + 20))
            {
                last_status_since = formatTime(new Date());
                statusDuration = 0;

                if (settings.getBoolean(SettingsActivity.KEY_ENABLE_LOGGING, false)) {
                    logs.logStatus(status, plugged, percent, temperature, voltage, currentTM, LogDatabase.STATUS_NEW);
                    if (status != last_status && last_status == 0)
                        logs.prune(Integer.valueOf(settings.getString(SettingsActivity.KEY_MAX_LOG_AGE, str.default_max_log_age)));
                }

                editor.putString("last_status_since", last_status_since);
                editor.putLong("last_status_cTM", currentTM);
                editor.putInt("last_status", status);
                editor.putInt("last_percent", percent);
                editor.putInt("last_plugged", plugged);
                editor.putInt("previous_charge", percent);

                last_status_cTM = currentTM;

                /* TODO: Af first glance, I think I want to do this, but think about it a bit and decide for sure... */
                mNotificationManager.cancel(NOTIFICATION_ALARM_CHARGE);

                if (last_status != status && settings.getBoolean(SettingsActivity.KEY_AUTO_DISABLE_LOCKING, false)) {
                    if (last_status == 0) {
                        editor.putBoolean(SettingsActivity.KEY_DISABLE_LOCKING, true);
                        setEnablednessOfKeyguard(false);
                    } else if (status == 0) {
                        editor.putBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false);
                        setEnablednessOfKeyguard(true);

                        /* If the screen was on, "inside" the keyguard, when the keyguard was disabled, then we're
                             still inside it now, even if the screen is off.  So we aquire a wakelock that forces the
                             screen to turn on, then release it.  If the screen is on now, this has no effect, but
                             if it's off, then either the user will press the power button or the screen will turn
                             itself off after the normal timeout.  Either way, when the screen goes off, the keyguard
                             will now be enabled properly. */
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                                                                  PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                                                  PowerManager.ON_AFTER_RELEASE, getPackageName());
                        wl.acquire();
                        wl.release();
                    }
                }
            } else {
                statusDuration = currentTM - last_status_cTM;

                if (settings.getBoolean(SettingsActivity.KEY_ENABLE_LOGGING, false) &&
                    settings.getBoolean(SettingsActivity.KEY_LOG_EVERYTHING, false))
                    logs.logStatus(status, plugged, percent, temperature, voltage, currentTM, LogDatabase.STATUS_OLD);

                if (percent % 10 == 0)
                    editor.putInt("previous_charge", percent);
            }
            logs.close();

            /* Add half an hour, then divide.  Should end up rounding to the closest hour. */
            int statusDurationHours = (int)((statusDuration + (1000 * 60 * 30)) / (1000 * 60 * 60));

            String contentTitle = "";

            if (settings.getBoolean(SettingsActivity.KEY_CHARGE_AS_TEXT, false))
                contentTitle += percent + " ";

            int status_dur_est = Integer.valueOf(settings.getString(SettingsActivity.KEY_STATUS_DUR_EST,
                                        str.default_status_dur_est));
            if (statusDurationHours < status_dur_est) {
                contentTitle += statusStr + " " + str.since + " " + last_status_since;
            } else {
                contentTitle += statusStr + " " + str.for_n_hours(statusDurationHours);
            }

            Boolean convertF = settings.getBoolean(SettingsActivity.KEY_CONVERT_F, false);
            CharSequence contentText = str.healths[health] + " / " +
                                       str.formatTemp(temperature, convertF) + " / " +
                                       str.formatVoltage(voltage);

            notification = new Notification(icon, null, System.currentTimeMillis());

            notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, mainWindowIntent, 0);

            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
            mNotificationManager.notify(NOTIFICATION_PRIMARY, notification);

            if (alarms.anyActiveAlarms()) {
                Cursor c;
                contentIntent = PendingIntent.getActivity(context, 0, alarmsIntent, 0);

                if (status == 5 && last_status != 5) {
                    c = alarms.activeAlarmFull();
                    if (c != null) {
                        notification = parseAlarmCursor(c);
                        notification.setLatestEventInfo(context, str.alarm_fully_charged, str.alarm_text, contentIntent);
                        mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, notification);
                        c.close();
                    }
                }

                c = alarms.activeAlarmChargeDrops(percent, previous_charge);
                if (c != null) {
                    editor.putInt("previous_charge", percent);
                    notification = parseAlarmCursor(c);
                    notification.setLatestEventInfo(context, str.alarm_charge_drops + c.getInt(alarms.INDEX_THRESHOLD) + str.percent_symbol,
                                                    str.alarm_text, contentIntent);
                    mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, notification);
                    c.close();
                }                

                c = alarms.activeAlarmChargeRises(percent, previous_charge);
                if (c != null) {
                    editor.putInt("previous_charge", percent);
                    notification = parseAlarmCursor(c);
                    notification.setLatestEventInfo(context, str.alarm_charge_rises + c.getInt(alarms.INDEX_THRESHOLD) + str.percent_symbol,
                                                    str.alarm_text, contentIntent);
                    mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, notification);
                    c.close();
                }                
            }

            editor.commit();
        }
    };

    private Notification parseAlarmCursor(Cursor c) {
        Notification notification = new Notification(R.drawable.stat_notify_alarm, null, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        String ringtone = c.getString(alarms.INDEX_RINGTONE);
        if (! ringtone.equals(""))
            notification.sound = android.net.Uri.parse(ringtone);

        if (c.getInt(alarms.INDEX_VIBRATE) == 1)
            if (mAudioManager.getRingerMode() != mAudioManager.RINGER_MODE_SILENT)
                /* I couldn't get the Notification to vibrate, so I do it myself... */
                mVibrator.vibrate(new long[] {0, 200, 200, 400}, -1);

        if (c.getInt(alarms.INDEX_LIGHTS) == 1) {
            notification.flags    |= Notification.FLAG_SHOW_LIGHTS;
            notification.defaults |= Notification.DEFAULT_LIGHTS;
        }

        return notification;
    }

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
                if (km.inKeyguardRestrictedInputMode()) {
                    registerReceiver(mUserPresentReceiver, userPresent);
                } else {
                    kl.disableKeyguard();
                    keyguardDisabled = true;
                }
            }
        }
    }

    private final BroadcastReceiver mUserPresentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())){
                unregisterReceiver(mUserPresentReceiver);
                setEnablednessOfKeyguard(false);
            }
        }
    };

    public void reloadSettings() {
        if (settings.getBoolean(SettingsActivity.KEY_DISABLE_LOCKING, false))
            setEnablednessOfKeyguard(false);
        else
            setEnablednessOfKeyguard(true);

        //unregisterReceiver(mBatteryInfoReceiver); /* It appears that there's no need to unregister first */
        registerReceiver(mBatteryInfoReceiver, batteryChanged);
    }
}
