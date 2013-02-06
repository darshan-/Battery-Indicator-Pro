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
import android.widget.RemoteViews;
import java.util.Date;

public class BatteryIndicatorService extends Service {
    private final IntentFilter batteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private final IntentFilter userPresent    = new IntentFilter(Intent.ACTION_USER_PRESENT);
    private PendingIntent mainWindowPendingIntent;
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

    private Notification kgUnlockedNotification;

    private Context context;
    private Resources res;
    private Str str;
    private AlarmDatabase alarms;
    private LogDatabase log_db;
    private BatteryLevel bl;
    private BatteryInfo info;

    private static final String LOG_TAG = "BatteryIndicatorService";

    private static final int NOTIFICATION_PRIMARY      = 1;
    private static final int NOTIFICATION_KG_UNLOCKED  = 2;
    private static final int NOTIFICATION_ALARM_CHARGE = 3;
    private static final int NOTIFICATION_ALARM_HEALTH = 4;
    private static final int NOTIFICATION_ALARM_TEMP   = 5;

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

    private static final int plainIcon0 = R.drawable.plain000;
    private static final int small_plainIcon0 = R.drawable.small_plain000;
    private static final int chargingIcon0 = R.drawable.charging000;
    private static final int small_chargingIcon0 = R.drawable.small_charging000;

    /* Global variables for these Notification Runnables */
    private Notification mainNotification;
    private String mainNotificationTitle, mainNotificationText;
    private RemoteViews notificationRV;

    private Predictor predictor;

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
                m.invoke(pluginServiceConnection.service, new Object[] {info.percent, info.status,
                                                                        mainNotificationTitle, mainNotificationText,
                                                                        mainWindowPendingIntent});

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

    private final Runnable runDisableKeyguard = new Runnable() {
        public void run() {
            kl = km.newKeyguardLock(getPackageName());
            kl.disableKeyguard();
            updateKeyguardNotification();
        }
    };


    @Override
    public void onCreate() {
        res = getResources();
        str = new Str(res);
        context = getApplicationContext();
        log_db = new LogDatabase(context);

        info = new BatteryInfo();

        predictor = new Predictor(context);
        bl = new BatteryLevel(context, BatteryLevel.SIZE_NOTIFICATION);
        notificationRV = new RemoteViews(getPackageName(), R.layout.main_notification);
        notificationRV.setImageViewBitmap(R.id.battery_level_view, bl.getBitmap());

        alarms = new AlarmDatabase(context);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mVibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mAudioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        sp_store = context.getSharedPreferences("sp_store", 0);

        Intent mainWindowIntent = new Intent(context, BatteryInfoActivity.class);
        mainWindowPendingIntent = PendingIntent.getActivity(context, 0, mainWindowIntent, 0);

        alarmsIntent = new Intent(context, AlarmsActivity.class);

        kgUnlockedNotification = new Notification(R.drawable.kg_unlocked, null, 0);
        kgUnlockedNotification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        kgUnlockedNotification.setLatestEventInfo(context, "Lock Screen Disabled",
                                                  "Press to re-enable", mainWindowPendingIntent);

        km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

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
        mHandler.removeCallbacks(mPluginNotify);
        mHandler.removeCallbacks(mNotify);
        mNotificationManager.cancelAll();
        log_db.close();
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

    // TODO: Set up normal public method in Service to grab data -- or call with data stucture
    public interface OnBatteryInfoUpdatedListener {
        public void onBatteryInfoUpdated(/* BatteryInfo bi */); // TODO
    }

    private final BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context receiver_context, Intent intent) { // TODO: Should I use receiver_context over context?
            if (! Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) return;

            mHandler.removeCallbacks(mPluginNotify);
            mHandler.removeCallbacks(mNotify);

            String desiredPluginPackage = settings.getString(SettingsActivity.KEY_ICON_PLUGIN, "none");
            if (! desiredPluginPackage.equals("none")) {
                SharedPreferences.Editor settings_editor = settings.edit();
                settings_editor.putString(SettingsActivity.KEY_ICON_SET, desiredPluginPackage);
                settings_editor.putString(SettingsActivity.KEY_ICON_PLUGIN, "none");
                settings_editor.commit();
            }

            desiredPluginPackage = settings.getString(SettingsActivity.KEY_ICON_SET, "none");
            if (desiredPluginPackage.startsWith("builtin.")) desiredPluginPackage = "none";

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

            updateBatteryInfo(intent);

            predictor.update(info.percent, info.status, info.plugged);

            int icon = iconFor(info.percent);

            if (statusChanged())
                handleUpdateWithChangedStatus();
            else
                handleUpdateWithSameStatus();

            int[] prediction = getPrediction();

            if (info.status == BatteryInfo.STATUS_FULLY_CHARGED) {
                mainNotificationTitle = str.statuses[info.status];
            } else {
                // TODO: Pro option to choose between long, medium, and short
                if (prediction[0] == 0) {
                    mainNotificationTitle = str.n_minutes_long(prediction[1]);
                } else if (prediction[0] < 24) {
                    mainNotificationTitle = str.n_hours_m_minutes_short(prediction[0], prediction[1]);
                    //mainNotificationTitle = "" + prediction[0] + ":" + prediction[1] + " hours";
                } else {
                    if (prediction[1] >= 30) prediction[0] += 1;
                    mainNotificationTitle = str.n_days_m_hours(prediction[0] / 24, prediction[0] % 24);
                }

                if (info.status == BatteryInfo.STATUS_CHARGING)
                    mainNotificationTitle += " until charged"; // TODO: Translatable
                else
                    mainNotificationTitle += " left"; // TODO: Translatable
            }

            Boolean convertF = settings.getBoolean(SettingsActivity.KEY_CONVERT_F, false);
            mainNotificationText = str.healths[info.health] + " / " + str.formatTemp(info.temperature, convertF);

            if (info.voltage > 500)
                mainNotificationText += " / " + str.formatVoltage(info.voltage);

            long when = 0;

            mainNotification = new Notification(icon, null, when);

            if (android.os.Build.VERSION.SDK_INT < 11) {
                notificationRV = new RemoteViews(getPackageName(), R.layout.main_notification);
                notificationRV.setImageViewBitmap(R.id.battery_level_view, bl.getBitmap());
            }

            if (android.os.Build.VERSION.SDK_INT >= 16) {
                mainNotification.priority = Integer.valueOf(settings.getString(SettingsActivity.KEY_MAIN_NOTIFICATION_PRIORITY,
                                                                               str.default_main_notification_priority));
            }

            mainNotification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

            bl.setLevel(info.percent);

            notificationRV.setTextViewText(R.id.percent, "" + info.percent + str.percent_symbol);
            notificationRV.setTextViewText(R.id.top_line, android.text.Html.fromHtml(mainNotificationTitle));
            notificationRV.setTextViewText(R.id.bottom_line, mainNotificationText);

            mainNotification.contentIntent = mainWindowPendingIntent;
            mainNotification.contentView = notificationRV;

            if (! pluginPackage.equals("none")) {
                mHandler.postDelayed(mPluginNotify,  100);
                mHandler.postDelayed(mPluginNotify,  300);
                mHandler.postDelayed(mPluginNotify,  900);
                mHandler.postDelayed(mNotify,       1000);
            } else {
                mHandler.post(mNotify);
            }

            if (alarms.anyActiveAlarms())
                handleAlarms();
        }
    };

    /* I take advantage of (count on) R.java having resources alphabetical and incrementing by one. */
    private int iconFor(int percent) {
        String default_set = "builtin.classic";
        if (android.os.Build.VERSION.SDK_INT >= 11)
            default_set = "builtin.plain_number";

        String icon_set = settings.getString(SettingsActivity.KEY_ICON_SET, "null");
        if (icon_set.equals("null")) {
            icon_set = default_set;

            SharedPreferences.Editor settings_editor = settings.edit();
            settings_editor.putString(SettingsActivity.KEY_ICON_SET, default_set);
            settings_editor.commit();
        }

        Boolean indicate_charging = settings.getBoolean(SettingsActivity.KEY_INDICATE_CHARGING, true);

        if (icon_set.equals("builtin.plain_number")) {
            return ((info.status == BatteryInfo.STATUS_CHARGING && indicate_charging) ? chargingIcon0 : plainIcon0) + info.percent;
        } else if (icon_set.equals("builtin.smaller_number")) {
            return ((info.status == BatteryInfo.STATUS_CHARGING && indicate_charging) ? small_chargingIcon0 : small_plainIcon0) + info.percent;
        } else {
            if (settings.getBoolean(SettingsActivity.KEY_RED, res.getBoolean(R.bool.default_use_red)) &&
                info.percent < Integer.valueOf(settings.getString(SettingsActivity.KEY_RED_THRESH, str.default_red_thresh)) &&
                info.percent <= SettingsActivity.RED_ICON_MAX) {
                return R.drawable.r000 + info.percent - 0;
            } else if (settings.getBoolean(SettingsActivity.KEY_AMBER, res.getBoolean(R.bool.default_use_amber)) &&
                       info.percent < Integer.valueOf(settings.getString(SettingsActivity.KEY_AMBER_THRESH, str.default_amber_thresh)) &&
                       info.percent <= SettingsActivity.AMBER_ICON_MAX &&
                       info.percent >= SettingsActivity.AMBER_ICON_MIN){
                return R.drawable.a000 + info.percent - 0;
            } else if (settings.getBoolean(SettingsActivity.KEY_GREEN, res.getBoolean(R.bool.default_use_green)) &&
                       info.percent >= Integer.valueOf(settings.getString(SettingsActivity.KEY_GREEN_THRESH, str.default_green_thresh)) &&
                       info.percent >= SettingsActivity.GREEN_ICON_MIN) {
                return R.drawable.g020 + info.percent - 20;
            } else {
                return R.drawable.b000 + info.percent;
            }
        }
    }

    private void updateBatteryInfo(Intent intent) {
        int level = intent.getIntExtra("level", 50);
        int scale = intent.getIntExtra("scale", 100);

        info.status = intent.getIntExtra("status", BatteryInfo.STATUS_UNKNOWN);
        info.health = intent.getIntExtra("health", BatteryInfo.HEALTH_UNKNOWN);
        info.plugged = intent.getIntExtra("plugged", BatteryInfo.PLUGGED_UNKNOWN);
        info.temperature = intent.getIntExtra("temperature", 0);
        info.voltage = intent.getIntExtra("voltage", 0);
        //info.technology = intent.getStringExtra("technology");

        info.percent = level * 100 / scale;
        info.percent = attemptOnePercentHack(info.percent);

        // TODO: This block is untested under actual wireless charging
        // Treat unplugged plugged as unpluggged status, unless charging wirelessly
        if (info.plugged == BatteryInfo.PLUGGED_UNPLUGGED) {
            if (info.status == BatteryInfo.STATUS_CHARGING)
                info.plugged = BatteryInfo.PLUGGED_WIRELESS; // Some devices say they're unplugged
            else
                info.status = BatteryInfo.STATUS_UNPLUGGED;
        }

        if (info.status  > BatteryInfo.STATUS_MAX) { info.status  = BatteryInfo.STATUS_UNKNOWN; }
        if (info.health  > BatteryInfo.HEALTH_MAX) { info.health  = BatteryInfo.HEALTH_UNKNOWN; }
        if (info.plugged > BatteryInfo.PLUGGED_MAX){ info.plugged = BatteryInfo.PLUGGED_UNKNOWN; }

        info.last_status = sp_store.getInt(KEY_LAST_STATUS, -1);
        info.last_plugged = sp_store.getInt(KEY_LAST_PLUGGED, -1);
        info.last_status_cTM = sp_store.getLong(KEY_LAST_STATUS_CTM, -1);
        info.last_percent = sp_store.getInt(KEY_LAST_PERCENT, -1);

    }

    private boolean statusChanged() {
        int previous_charge = sp_store.getInt(KEY_PREVIOUS_CHARGE, 100);

        return (info.last_status != info.status || info.last_status_cTM == -1 || info.last_percent == -1 ||
                info.last_status_cTM > System.currentTimeMillis() || info.last_plugged != info.plugged ||
                (info.plugged == BatteryInfo.PLUGGED_UNPLUGGED && info.percent > previous_charge + 20));
    }

    private void handleUpdateWithChangedStatus() {
        SharedPreferences.Editor editor = sp_store.edit();
        long time = System.currentTimeMillis();

        if (settings.getBoolean(SettingsActivity.KEY_ENABLE_LOGGING, true)) {
            log_db.logStatus(info, time, LogDatabase.STATUS_NEW);

            if (info.status != info.last_status && info.last_status == BatteryInfo.STATUS_UNPLUGGED)
                log_db.prune(Integer.valueOf(settings.getString(SettingsActivity.KEY_MAX_LOG_AGE, str.default_max_log_age)));
        }

        editor.putLong(KEY_LAST_STATUS_CTM, time);
        editor.putInt(KEY_LAST_STATUS, info.status);
        editor.putInt(KEY_LAST_PERCENT, info.percent);
        editor.putInt(KEY_LAST_PLUGGED, info.plugged);
        editor.putInt(KEY_PREVIOUS_CHARGE, info.percent);
        editor.putInt(KEY_PREVIOUS_TEMP, info.temperature);
        editor.putInt(KEY_PREVIOUS_HEALTH, info.health);

        info.last_status_cTM = time;

        /* TODO: Af first glance, I think I want to do this, but think about it a bit and decide for sure... */
        mNotificationManager.cancel(NOTIFICATION_ALARM_CHARGE);

        if (info.last_status != info.status && settings.getBoolean(SettingsActivity.KEY_AUTO_DISABLE_LOCKING, false)) {
            if (info.last_status == BatteryInfo.STATUS_UNPLUGGED) {
                editor.putBoolean(KEY_DISABLE_LOCKING, true);
                setEnablednessOfKeyguard(false);
            } else if (info.status == BatteryInfo.STATUS_UNPLUGGED) {
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

        editor.commit();
    }

    private void handleUpdateWithSameStatus() {
        SharedPreferences.Editor editor = sp_store.edit();
        long time = System.currentTimeMillis();

        if (settings.getBoolean(SettingsActivity.KEY_ENABLE_LOGGING, true))
            log_db.logStatus(info, time, LogDatabase.STATUS_OLD);

        if (info.percent % 10 == 0) {
            editor.putInt(KEY_PREVIOUS_CHARGE, info.percent);
            editor.putInt(KEY_PREVIOUS_TEMP, info.temperature);
            editor.putInt(KEY_PREVIOUS_HEALTH, info.health);
        }

        editor.commit();
    }

    private void handleAlarms() {
        Cursor c;
        Notification notification;
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, alarmsIntent, 0);
        SharedPreferences.Editor editor = sp_store.edit();
        int previous_charge = sp_store.getInt(KEY_PREVIOUS_CHARGE, 100);

        if (info.status == BatteryInfo.STATUS_FULLY_CHARGED && info.last_status == BatteryInfo.STATUS_CHARGING) {
            c = alarms.activeAlarmFull();
            if (c != null) {
                notification = parseAlarmCursor(c);
                notification.setLatestEventInfo(context, str.alarm_fully_charged, str.alarm_text, contentIntent);
                mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, notification);
                c.close();
            }
        }

        c = alarms.activeAlarmChargeDrops(info.percent, previous_charge);
        if (c != null) {
            editor.putInt(KEY_PREVIOUS_CHARGE, info.percent);
            notification = parseAlarmCursor(c);
            notification.setLatestEventInfo(context, str.alarm_charge_drops + c.getInt(alarms.INDEX_THRESHOLD) + str.percent_symbol,
                                            str.alarm_text, contentIntent);
            mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, notification);
            c.close();
        }

        c = alarms.activeAlarmChargeRises(info.percent, previous_charge);
        if (c != null && info.status != BatteryInfo.STATUS_UNPLUGGED) {
            editor.putInt(KEY_PREVIOUS_CHARGE, info.percent);
            notification = parseAlarmCursor(c);
            notification.setLatestEventInfo(context, str.alarm_charge_rises + c.getInt(alarms.INDEX_THRESHOLD) + str.percent_symbol,
                                            str.alarm_text, contentIntent);
            mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, notification);
            c.close();
        }

        c = alarms.activeAlarmTempRises(info.temperature, sp_store.getInt(KEY_PREVIOUS_TEMP, 1));
        if (c != null) {
            Boolean convertF = settings.getBoolean(SettingsActivity.KEY_CONVERT_F, false);
            editor.putInt(KEY_PREVIOUS_TEMP, info.temperature);
            notification = parseAlarmCursor(c);
            notification.setLatestEventInfo(context, str.alarm_temp_rises +
                                            str.formatTemp(c.getInt(alarms.INDEX_THRESHOLD), convertF, false),
                                            str.alarm_text, contentIntent);
            mNotificationManager.notify(NOTIFICATION_ALARM_TEMP, notification);
            c.close();
        }

        if (info.health > BatteryInfo.HEALTH_GOOD && info.health != sp_store.getInt(KEY_PREVIOUS_HEALTH, BatteryInfo.HEALTH_GOOD)) {
            c = alarms.activeAlarmFailure();
            if (c != null) {
                editor.putInt(KEY_PREVIOUS_HEALTH, info.health);
                notification = parseAlarmCursor(c);
                notification.setLatestEventInfo(context, str.alarm_health_failure + str.healths[info.health],
                                                str.alarm_text, contentIntent);
                mNotificationManager.notify(NOTIFICATION_ALARM_HEALTH, notification);
                c.close();
            }
        }

        editor.commit();
    }

    private int attemptOnePercentHack(int percent) {
        java.io.File hack_file = new java.io.File("/sys/class/power_supply/battery/charge_counter");

        if (hack_file.exists()) {
            /* The Log messages are only really useful to me and might as well be left hardwired here in English. */
            try {
                java.io.FileReader fReader = new java.io.FileReader(hack_file);
                java.io.BufferedReader bReader = new java.io.BufferedReader(fReader);
                int charge_counter = Integer.valueOf(bReader.readLine());

                if (charge_counter < percent + 10 && charge_counter > percent - 10) {
                    if (charge_counter > 100) // This happens
                        charge_counter = 100;

                    if (charge_counter < 0)   // This could happen?
                        charge_counter = 0;

                    percent = charge_counter;
                } else {
                    Log.e(LOG_TAG, "charge_counter file exists but with value " + charge_counter +
                          " which is inconsistent with percent: " + percent);
                }
            } catch (java.io.FileNotFoundException e) {
                Log.e(LOG_TAG, "charge_counter file doesn't exist");
            } catch (java.io.IOException e) {
                Log.e(LOG_TAG, "Error reading charge_counter file");
            }
        }

        return percent;
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

    private void setEnablednessOfKeyguard(boolean enabled) {
        if (enabled) {
            if (kl != null) {
                unregisterReceiver(mUserPresentReceiver);
                mHandler.removeCallbacks(runDisableKeyguard);
                kl.reenableKeyguard();
                kl = null;
            }
        } else {
            if (km.inKeyguardRestrictedInputMode()) {
                registerReceiver(mUserPresentReceiver, userPresent);
            } else {
                if (kl != null)
                    kl.reenableKeyguard();
                else
                    registerReceiver(mUserPresentReceiver, userPresent);

                mHandler.postDelayed(runDisableKeyguard,  300);
            }
        }

        updateKeyguardNotification();
    }

    private void updateKeyguardNotification() {
        if (kl != null && settings.getBoolean(SettingsActivity.KEY_NOTIFY_WHEN_KG_DISABLED, true))
            mNotificationManager.notify(NOTIFICATION_KG_UNLOCKED, kgUnlockedNotification);
        else
            mNotificationManager.cancel(NOTIFICATION_KG_UNLOCKED);
    }

    private final BroadcastReceiver mUserPresentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())){
                if (sp_store.getBoolean(KEY_DISABLE_LOCKING, false))
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
        reloadSettings(false);
    }

    public void reloadSettings(boolean cancelFirst) {
        str = new Str(res); /* Language override may have changed */

        if (cancelFirst) mNotificationManager.cancel(NOTIFICATION_PRIMARY);

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

    public int[] getPrediction() {
        int secs_left;

        if (info.status == BatteryInfo.STATUS_CHARGING) {
            secs_left = predictor.secondsUntilCharged();
        } else {
            secs_left = predictor.secondsUntilDrained();
        }

        int hours_left = secs_left / (60 * 60);
        int  mins_left = (secs_left / 60) % 60;

        return new int[] {hours_left, mins_left, info.status};
    }
}
