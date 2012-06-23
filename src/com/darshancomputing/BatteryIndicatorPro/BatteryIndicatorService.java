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
import android.util.Log;
import java.util.Date;

public class BatteryIndicatorService extends Service {
    private final IntentFilter batteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private final IntentFilter userPresent    = new IntentFilter(Intent.ACTION_USER_PRESENT);
    private Intent mainWindowIntent;
    private Intent alarmsIntent;

    private final PluginServiceConnection pluginServiceConnection = new PluginServiceConnection();
    private Intent pluginIntent;
    private String pluginPackage;

    private NotificationManager mNotificationManager;
    private SharedPreferences settings;
    private SharedPreferences sp_store;

    private KeyguardLock kl;
    private KeyguardManager km;
    private android.os.Vibrator mVibrator;
    private android.media.AudioManager mAudioManager;

    private Boolean keyguardDisabled = false;

    private Resources res;
    private Str str;
    private AlarmDatabase alarms;

    private static final String LOG_TAG = "BatteryIndicatorService";

    private static final int NOTIFICATION_PRIMARY      = 1;
    private static final int NOTIFICATION_ALARM_CHARGE = 2;
    private static final int NOTIFICATION_ALARM_HEALTH = 3;
    private static final int NOTIFICATION_ALARM_TEMP   = 4;

    private static final int STATUS_UNPLUGGED     = 0;
    private static final int STATUS_UNKNOWN       = 1;
    private static final int STATUS_CHARGING      = 2;
    private static final int STATUS_DISCHARGING   = 3;
    private static final int STATUS_NOT_CHARGING  = 4;
    private static final int STATUS_FULLY_CHARGED = 5;
    private static final int STATUS_MAX = STATUS_FULLY_CHARGED;

    private static final int PLUGGED_UNPLUGGED = 0;
    private static final int PLUGGED_AC        = 1;
    private static final int PLUGGED_USB       = 2;
    private static final int PLUGGED_UNKNOWN   = 3;
    private static final int PLUGGED_MAX = PLUGGED_UNKNOWN;

    private static final int HEALTH_UNKNOWN     = 1;
    private static final int HEALTH_GOOD        = 2;
    private static final int HEALTH_OVERHEAT    = 3;
    private static final int HEALTH_DEAD        = 4;
    private static final int HEALTH_OVERVOLTAGE = 5;
    private static final int HEALTH_FAILURE     = 6;
    private static final int HEALTH_MAX = HEALTH_FAILURE;


    public static final String KEY_LAST_STATUS_SINCE = "last_status_since";
    public static final String KEY_LAST_STATUS_CTM = "last_status_cTM";
    public static final String KEY_LAST_STATUS = "last_status";
    public static final String KEY_LAST_PERCENT = "last_percent";
    public static final String KEY_LAST_PLUGGED = "last_plugged";
    public static final String KEY_PREVIOUS_CHARGE = "previous_charge";
    public static final String KEY_PREVIOUS_TEMP = "previous_temp";
    public static final String KEY_PREVIOUS_HEALTH = "previous_health";
    public static final String KEY_DISABLE_LOCKING = "disable_lock_screen";
    public static final String KEY_SERVICE_DESIRED = "serviceDesired";


    private static final Object[] EMPTY_OBJECT_ARRAY = {};
    private static final  Class[]  EMPTY_CLASS_ARRAY = {};

    private static final int defaultIcon0 = R.drawable.b000;
    private int chargingIcon0;

    /* Global variables for these Notification Runnables */
    private Notification mainNotification;
    private int percent;
    private int status;
    private String mainNotificationTitle, mainNotificationText;
    private PendingIntent mainNotificationIntent;

    private final Handler mHandler = new Handler();
    private final Runnable mPluginNotify = new Runnable() {
        public void run() {
            try {
                mNotificationManager.cancel(NOTIFICATION_PRIMARY);
                if (pluginServiceConnection.service == null) return;

                Class<?> c = pluginServiceConnection.service.getClass();
                java.lang.reflect.Method m = c.getMethod("notify", new Class[] {int.class, int.class,
                                                                                String.class, String.class,
                                                                                PendingIntent.class});
                m.invoke(pluginServiceConnection.service, new Object[] {percent, status,
                                                                        mainNotificationTitle, mainNotificationText,
                                                                        mainNotificationIntent});

                mHandler.removeCallbacks(mPluginNotify);
                mHandler.removeCallbacks(mNotify);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private final Runnable mNotify = new Runnable() {
        public void run() {
            if (! pluginPackage.equals("none")) disconnectPlugin();

            mNotificationManager.notify(NOTIFICATION_PRIMARY, mainNotification);
            mHandler.removeCallbacks(mPluginNotify);
            mHandler.removeCallbacks(mNotify);
        }
    };

    @Override
    public void onCreate() {
        res = getResources();
        str = new Str(res);
        Context context = getApplicationContext();

        alarms = new AlarmDatabase(context);

        try {
            java.lang.reflect.Field f = R.drawable.class.getField("charging000");
            chargingIcon0 = f.getInt(R.drawable.class);
        } catch (Exception e) {
            chargingIcon0 = defaultIcon0;
        }

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mVibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mAudioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        sp_store = context.getSharedPreferences("sp_store", 0);

        mainWindowIntent = new Intent(context, BatteryIndicator.class);
        alarmsIntent = new Intent(context, AlarmsActivity.class);

        km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        kl = km.newKeyguardLock(getPackageName());

        if (sp_store.getBoolean(KEY_DISABLE_LOCKING, false))
            setEnablednessOfKeyguard(false);

        pluginPackage = "none";

        registerReceiver(mBatteryInfoReceiver, batteryChanged);
    }

    @Override
    public void onDestroy() {
        setEnablednessOfKeyguard(true);
        alarms.close();
        if (! pluginPackage.equals("none")) disconnectPlugin();
        unregisterReceiver(mBatteryInfoReceiver);
        mNotificationManager.cancelAll();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public BatteryIndicatorService getService() {
            return BatteryIndicatorService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    private final BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.removeCallbacks(mPluginNotify);
            mHandler.removeCallbacks(mNotify);

            String desiredPluginPackage = settings.getString(SettingsActivity.KEY_ICON_PLUGIN, "none");

            if (! pluginPackage.equals(desiredPluginPackage) && ! pluginPackage.equals("none")) disconnectPlugin();

            if (! pluginPackage.equals(desiredPluginPackage) && ! desiredPluginPackage.equals("none")) {
                try {
                    Context pluginContext = getApplicationContext().createPackageContext(desiredPluginPackage, Context.CONTEXT_INCLUDE_CODE);
                    ClassLoader pluginClassLoader = pluginContext.getClassLoader();
                    Class pluginClass = pluginClassLoader.loadClass(desiredPluginPackage + ".PluginService");
                    pluginIntent = new Intent(pluginContext, pluginClass);

                    startService(pluginIntent);
                    if (! bindService(pluginIntent, pluginServiceConnection, 0)) {
                        stopService(pluginIntent);
                        throw new Exception();
                    }

                    pluginPackage = desiredPluginPackage;
                } catch (Exception e) {
                    e.printStackTrace();
                    pluginPackage = "none";
                }
            }

            SharedPreferences.Editor editor = sp_store.edit();
            String action = intent.getAction();
            if (! Intent.ACTION_BATTERY_CHANGED.equals(action)) return;

            int level = intent.getIntExtra("level", 50);
            int scale = intent.getIntExtra("scale", 100);
                status = intent.getIntExtra("status", STATUS_UNKNOWN);
            int health = intent.getIntExtra("health", HEALTH_UNKNOWN);
            int plugged = intent.getIntExtra("plugged", PLUGGED_UNKNOWN);
            int temperature = intent.getIntExtra("temperature", 0);
            int voltage = intent.getIntExtra("voltage", 0);
            //String technology = intent.getStringExtra("technology");

            percent = level * 100 / scale;

            if (settings.getBoolean(SettingsActivity.KEY_ONE_PERCENT_HACK, false)) {
                try {
                    java.io.FileReader fReader = new java.io.FileReader("/sys/class/power_supply/battery/charge_counter");
                    java.io.BufferedReader bReader = new java.io.BufferedReader(fReader);
                    int charge_counter = Integer.valueOf(bReader.readLine());

                    if (charge_counter > SettingsActivity.CHARGE_COUNTER_LEGIT_MAX) {
                        disableOnePercentHack("charge_counter is too big to be actual charge");
                    } else {
                        if (charge_counter > 100)
                            charge_counter = 100;

                        percent = charge_counter;
                    }
                } catch (java.io.FileNotFoundException e) {
                    /* These error messages are only really useful to me and might as well be left hardwired here in English. */
                    disableOnePercentHack("charge_counter file doesn't exist");
                } catch (java.io.IOException e) {
                    disableOnePercentHack("Error reading charge_counter file");
                }
            }

            /* Just treating any unplugged status as simply "Unplugged" now.
               Note that the main activity now assumes that the status is always 0, 2, or 5 TODO */
            if (plugged == PLUGGED_UNPLUGGED) status = STATUS_UNPLUGGED;

            if (status  > STATUS_MAX) { status  = STATUS_UNKNOWN; }
            if (health  > HEALTH_MAX) { health  = HEALTH_UNKNOWN; }
            if (plugged > PLUGGED_MAX){ plugged = PLUGGED_UNKNOWN; }

            /* I take advantage of (count on) R.java having resources alphabetical and incrementing by one */

            int icon;

            ///*v11*//* // Hackish, ugly way to hide this in v11 version; Musn't use multi-line comments within this section
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
            ///*v11*/ // End v11 hidden section

            ///*v11*/ icon = ((status == STATUS_CHARGING) ? chargingIcon0 : defaultIcon0) + percent;

            String statusStr = str.statuses[status];
            if (status == STATUS_CHARGING)
                statusStr += " " + str.pluggeds[plugged]; /* Add '(AC)' or '(USB)' */

            int last_status = sp_store.getInt(KEY_LAST_STATUS, -1);
            /* There's a bug, at least on 1.5, or maybe depending on the hardware (I've noticed it on the MyTouch with 1.5)
               where USB is recognized as AC at first, then quickly changed to USB.  So we need to test if plugged changed. */
            int last_plugged = sp_store.getInt(KEY_LAST_PLUGGED, -1);
            long last_status_cTM = sp_store.getLong(KEY_LAST_STATUS_CTM, -1);
            int last_percent = sp_store.getInt(KEY_LAST_PERCENT, -1);
            int previous_charge = sp_store.getInt(KEY_PREVIOUS_CHARGE, 100);
            long currentTM = System.currentTimeMillis();
            long statusDuration;
            String last_status_since = sp_store.getString(KEY_LAST_STATUS_SINCE, null);
            LogDatabase logs = new LogDatabase(context);

            if (last_status != status || last_status_cTM == -1 || last_percent == -1 ||
                last_status_cTM > currentTM || last_status_since == null || last_plugged != plugged ||
                (plugged == PLUGGED_UNPLUGGED && percent > previous_charge + 20))
            {
                last_status_since = formatTime(new Date());
                statusDuration = 0;

                if (settings.getBoolean(SettingsActivity.KEY_ENABLE_LOGGING, false)) {
                    logs.logStatus(status, plugged, percent, temperature, voltage, currentTM, LogDatabase.STATUS_NEW);
                    if (status != last_status && last_status == STATUS_UNPLUGGED)
                        logs.prune(Integer.valueOf(settings.getString(SettingsActivity.KEY_MAX_LOG_AGE, str.default_max_log_age)));
                }

                editor.putString(KEY_LAST_STATUS_SINCE, last_status_since);
                editor.putLong(KEY_LAST_STATUS_CTM, currentTM);
                editor.putInt(KEY_LAST_STATUS, status);
                editor.putInt(KEY_LAST_PERCENT, percent);
                editor.putInt(KEY_LAST_PLUGGED, plugged);
                editor.putInt(KEY_PREVIOUS_CHARGE, percent);
                editor.putInt(KEY_PREVIOUS_TEMP, temperature);
                editor.putInt(KEY_PREVIOUS_HEALTH, health);

                last_status_cTM = currentTM;

                /* TODO: Af first glance, I think I want to do this, but think about it a bit and decide for sure... */
                mNotificationManager.cancel(NOTIFICATION_ALARM_CHARGE);

                if (last_status != status && settings.getBoolean(SettingsActivity.KEY_AUTO_DISABLE_LOCKING, false)) {
                    if (last_status == STATUS_UNPLUGGED) {
                        editor.putBoolean(KEY_DISABLE_LOCKING, true);
                        setEnablednessOfKeyguard(false);
                    } else if (status == STATUS_UNPLUGGED) {
                        editor.putBoolean(KEY_DISABLE_LOCKING, false);
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

                if (percent % 10 == 0) {
                    editor.putInt(KEY_PREVIOUS_CHARGE, percent);
                    editor.putInt(KEY_PREVIOUS_TEMP, temperature);
                    editor.putInt(KEY_PREVIOUS_HEALTH, health);
                }
            }
            logs.close();

            /* Add half an hour, then divide.  Should end up rounding to the closest hour. */
            int statusDurationHours = (int)((statusDuration + (1000 * 60 * 30)) / (1000 * 60 * 60));

            mainNotificationTitle = "";

            if (settings.getBoolean(SettingsActivity.KEY_CHARGE_AS_TEXT, false))
                mainNotificationTitle += percent + str.percent_symbol + " ";

            int status_dur_est = Integer.valueOf(settings.getString(SettingsActivity.KEY_STATUS_DUR_EST,
                                        str.default_status_dur_est));
            if (statusDurationHours < status_dur_est) {
                mainNotificationTitle += statusStr + " " + str.since + " " + last_status_since;
            } else {
                mainNotificationTitle += statusStr + " " + str.for_n_hours(statusDurationHours);
            }

            Boolean convertF = settings.getBoolean(SettingsActivity.KEY_CONVERT_F, false);
            mainNotificationText = str.healths[health] + " / " +
                                       str.formatTemp(temperature, convertF) + " / " +
                                       str.formatVoltage(voltage);

            long when = 0;

            if (settings.getBoolean(SettingsActivity.KEY_SHOW_NOTIFICATION_TIME, false))
                when = System.currentTimeMillis();

            mainNotification = new Notification(icon, null, when);

            mainNotification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
            mainNotificationIntent = PendingIntent.getActivity(context, 0, mainWindowIntent, 0);

            mainNotification.setLatestEventInfo(context, mainNotificationTitle, mainNotificationText, mainNotificationIntent);

            if (! pluginPackage.equals("none")) {
                mHandler.postDelayed(mPluginNotify,  100);
                mHandler.postDelayed(mPluginNotify,  300);
                mHandler.postDelayed(mPluginNotify,  900);
                mHandler.postDelayed(mNotify,       1000);
            } else {
                mHandler.post(mNotify);
            }

            if (alarms.anyActiveAlarms()) {
                Cursor c;
                Notification notification;
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, alarmsIntent, 0);

                if (status == STATUS_FULLY_CHARGED && last_status == STATUS_CHARGING) {
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
                    editor.putInt(KEY_PREVIOUS_CHARGE, percent);
                    notification = parseAlarmCursor(c);
                    notification.setLatestEventInfo(context, str.alarm_charge_drops + c.getInt(alarms.INDEX_THRESHOLD) + str.percent_symbol,
                                                    str.alarm_text, contentIntent);
                    mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, notification);
                    c.close();
                }                

                c = alarms.activeAlarmChargeRises(percent, previous_charge);
                if (c != null) {
                    editor.putInt(KEY_PREVIOUS_CHARGE, percent);
                    notification = parseAlarmCursor(c);
                    notification.setLatestEventInfo(context, str.alarm_charge_rises + c.getInt(alarms.INDEX_THRESHOLD) + str.percent_symbol,
                                                    str.alarm_text, contentIntent);
                    mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, notification);
                    c.close();
                }                

                c = alarms.activeAlarmTempRises(temperature, sp_store.getInt(KEY_PREVIOUS_TEMP, 1));
                if (c != null) {
                    editor.putInt(KEY_PREVIOUS_TEMP, temperature);
                    notification = parseAlarmCursor(c);
                    notification.setLatestEventInfo(context, str.alarm_temp_rises +
                                                    str.formatTemp(c.getInt(alarms.INDEX_THRESHOLD), convertF, false),
                                                    str.alarm_text, contentIntent);
                    mNotificationManager.notify(NOTIFICATION_ALARM_TEMP, notification);
                    c.close();
                }                

                if (health > HEALTH_GOOD && health != sp_store.getInt(KEY_PREVIOUS_HEALTH, HEALTH_GOOD)) {
                    c = alarms.activeAlarmFailure();
                    if (c != null) {
                        editor.putInt(KEY_PREVIOUS_HEALTH, health);
                        notification = parseAlarmCursor(c);
                        notification.setLatestEventInfo(context, str.alarm_health_failure + str.healths[health],
                                                        str.alarm_text, contentIntent);
                        mNotificationManager.notify(NOTIFICATION_ALARM_HEALTH, notification);
                        c.close();
                    }
                }
            }

            editor.commit();
        }
    };

    private void disableOnePercentHack(String reason) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SettingsActivity.KEY_ONE_PERCENT_HACK, false);
        editor.commit();

        Log.e(LOG_TAG, "Disabled one percent hack due to: " + reason);
    }

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

    private void disconnectPlugin() {
        unbindService(pluginServiceConnection);
        stopService(pluginIntent);
        pluginServiceConnection.service = null;
        pluginPackage = "none";
    }

    public void reloadSettings() {
        str = new Str(res); /* Language override may have changed */

        if (sp_store.getBoolean(KEY_DISABLE_LOCKING, false))
            setEnablednessOfKeyguard(false);
        else
            setEnablednessOfKeyguard(true);

        //unregisterReceiver(mBatteryInfoReceiver); /* It appears that there's no need to unregister first */
        registerReceiver(mBatteryInfoReceiver, batteryChanged);
    }

    public Boolean pluginHasSettings() {
        if (pluginServiceConnection.service == null) return false;

        try {
            Class<?> c = pluginServiceConnection.service.getClass();
            java.lang.reflect.Method m = c.getMethod("hasSettings", EMPTY_CLASS_ARRAY);
            return (Boolean) m.invoke(pluginServiceConnection.service, EMPTY_OBJECT_ARRAY);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void configurePlugin() {
        if (pluginServiceConnection.service == null) return;

        try {
            Class<?> c = pluginServiceConnection.service.getClass();
            java.lang.reflect.Method m = c.getMethod("configure", EMPTY_CLASS_ARRAY);
            m.invoke(pluginServiceConnection.service, EMPTY_OBJECT_ARRAY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
