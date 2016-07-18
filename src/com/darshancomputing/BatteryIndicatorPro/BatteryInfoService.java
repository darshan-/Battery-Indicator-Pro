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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Date;
import java.util.HashSet;

public class BatteryInfoService extends Service {
    private final IntentFilter batteryChanged = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private final IntentFilter userPresent    = new IntentFilter(Intent.ACTION_USER_PRESENT);
    private PendingIntent currentInfoPendingIntent, updatePredictorPendingIntent, alarmsPendingIntent;

    private NotificationManager mNotificationManager;
    private AlarmManager alarmManager;
    private SharedPreferences settings;
    private SharedPreferences sp_store;
    private SharedPreferences.Editor sps_editor;

    private android.os.Vibrator mVibrator;
    private android.media.AudioManager mAudioManager;

    private Resources res;
    private Str str;
    private AlarmDatabase alarms;
    private LogDatabase log_db;
    private BatteryLevel bl;
    private CurrentHack currentHack;
    private CircleWidgetBackground cwbg;
    private BatteryInfo info;
    private long now;
    private boolean updated_lasts;
    private static java.util.HashSet<Messenger> clientMessengers;
    private static Messenger messenger;

    private static HashSet<Integer> widgetIds = new HashSet<Integer>();
    private static AppWidgetManager widgetManager;

    private static final String LOG_TAG = "com.darshancomputing.BatteryIndicatorPro - BatteryInfoService";

    private static final int NOTIFICATION_PRIMARY      = 1;
    private static final int NOTIFICATION_ALARM_CHARGE = 3;
    private static final int NOTIFICATION_ALARM_HEALTH = 4;
    private static final int NOTIFICATION_ALARM_TEMP   = 5;

    private static final int RC_MAIN   = 100;
    private static final int RC_ALARMS = 101;

    public static final String KEY_PREVIOUS_CHARGE = "previous_charge";
    public static final String KEY_PREVIOUS_TEMP = "previous_temp";
    public static final String KEY_PREVIOUS_HEALTH = "previous_health";
    public static final String KEY_SERVICE_DESIRED = "serviceDesired";
    public static final String KEY_SHOW_NOTIFICATION = "show_notification";
    public static final String LAST_SDK_API = "last_sdk_api";


    private static final String EXTRA_UPDATE_PREDICTOR = "com.darshancomputing.BatteryBotPro.EXTRA_UPDATE_PREDICTOR";

    public static final String EXTRA_CURRENT_INFO = "com.darshancomputing.BatteryBotPro.EXTRA_CURRENT_INFO";
    public static final String EXTRA_EDIT_ALARMS  = "com.darshancomputing.BatteryBotPro.EXTRA_EDIT_ALARMS";


    private static final Object[] EMPTY_OBJECT_ARRAY = {};
    private static final  Class[]  EMPTY_CLASS_ARRAY = {};

    private static final int plainIcon0 = R.drawable.plain000;
    private static final int small_plainIcon0 = R.drawable.small_plain000;
    private static final int chargingIcon0 = R.drawable.charging000;
    private static final int small_chargingIcon0 = R.drawable.small_charging000;

    /* Global variables for these Notification Runnables */
    private Notification.Builder mainNotificationB;
    private String mainNotificationTopLine, mainNotificationBottomLine;
    private RemoteViews notificationRV;

    private Predictor predictor;

    private final Handler mHandler = new Handler();

    private final Runnable mNotify = new Runnable() {
        public void run() {
            startForeground(NOTIFICATION_PRIMARY, mainNotificationB.build());
            mHandler.removeCallbacks(mNotify);
        }
    };

    private final Runnable runRenotify = new Runnable() {
        public void run() {
            registerReceiver(mBatteryInfoReceiver, batteryChanged);
        }
    };

    @Override
    public void onCreate() {
        res = getResources();
        str = new Str(res);
        log_db = new LogDatabase(this);

        info = new BatteryInfo();

        messenger = new Messenger(new MessageHandler());
        clientMessengers = new java.util.HashSet<Messenger>();

        predictor = new Predictor(this);
        bl = BatteryLevel.getInstance(this, BatteryLevel.SIZE_NOTIFICATION);
        cwbg = new CircleWidgetBackground(this);

        alarms = new AlarmDatabase(this);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mainNotificationB = new Notification.Builder(this);

        mVibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mAudioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        loadSettingsFiles();
        sdkVersioning();

        currentHack = CurrentHack.getInstance(this);
        currentHack.setPreferFS(settings.getBoolean(SettingsActivity.KEY_CURRENT_HACK_PREFER_FS, false));

        Intent currentInfoIntent = new Intent(this, BatteryInfoActivity.class).putExtra(EXTRA_CURRENT_INFO, true);
        currentInfoPendingIntent = PendingIntent.getActivity(this, RC_MAIN, currentInfoIntent, 0);

        Intent updatePredictorIntent = new Intent(this, BatteryInfoService.class);
        updatePredictorIntent.putExtra(EXTRA_UPDATE_PREDICTOR, true);
        updatePredictorPendingIntent = PendingIntent.getService(this, 0, updatePredictorIntent, 0);

        Intent alarmsIntent = new Intent(this, BatteryInfoActivity.class).putExtra(EXTRA_EDIT_ALARMS, true);
        alarmsPendingIntent = PendingIntent.getActivity(this, RC_ALARMS, alarmsIntent, 0);

        widgetManager = AppWidgetManager.getInstance(this);

        Class[] appWidgetProviders = {BatteryInfoAppWidgetProvider.class, /* Circle widget! */
                                             FullAppWidgetProvider.class};

        for (int i = 0; i < appWidgetProviders.length; i++) {
            int[] ids = widgetManager.getAppWidgetIds(new ComponentName(this, appWidgetProviders[i]));

            for (int j = 0; j < ids.length; j++) {
                widgetIds.add(ids[j]);
            }
        }

        Intent bc_intent = registerReceiver(mBatteryInfoReceiver, batteryChanged);
        info.load(bc_intent, sp_store);
    }

    @Override
    public void onDestroy() {
        alarmManager.cancel(updatePredictorPendingIntent);
        alarms.close();
        unregisterReceiver(mBatteryInfoReceiver);
        mHandler.removeCallbacks(mNotify);
        mHandler.removeCallbacks(runRenotify);
        mNotificationManager.cancelAll();
        log_db.close();
        updateWidgets(null);
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: Do I need a filter, or is it okay to just update(null) every time?
        //if (intent != null && intent.getBooleanExtra(EXTRA_UPDATE_PREDICTOR, false))
        update(null);

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message incoming) {
            switch (incoming.what) {
            case RemoteConnection.SERVICE_CLIENT_CONNECTED:
                sendClientMessage(incoming.replyTo, RemoteConnection.CLIENT_SERVICE_CONNECTED);
                break;
            case RemoteConnection.SERVICE_REGISTER_CLIENT:
                clientMessengers.add(incoming.replyTo);
                sendClientMessage(incoming.replyTo, RemoteConnection.CLIENT_BATTERY_INFO_UPDATED, info.toBundle());
                break;
            case RemoteConnection.SERVICE_UNREGISTER_CLIENT:
                clientMessengers.remove(incoming.replyTo);
                break;
            case RemoteConnection.SERVICE_RELOAD_SETTINGS:
                reloadSettings(false);
                break;
            case RemoteConnection.SERVICE_CANCEL_NOTIFICATION_AND_RELOAD_SETTINGS:
                reloadSettings(true);
                break;
            default:
                super.handleMessage(incoming);
            }
        }
    }

    private static void sendClientMessage(Messenger clientMessenger, int what) {
        sendClientMessage(clientMessenger, what, null);
    }

    private static void sendClientMessage(Messenger clientMessenger, int what, Bundle data) {
        Message outgoing = Message.obtain();
        outgoing.what = what;
        outgoing.replyTo = messenger;
        outgoing.setData(data);
        try { clientMessenger.send(outgoing); } catch (android.os.RemoteException e) {}
    }

    public static class RemoteConnection implements ServiceConnection {
        // Messages clients send to the service
        public static final int SERVICE_CLIENT_CONNECTED = 0;
        public static final int SERVICE_REGISTER_CLIENT = 1;
        public static final int SERVICE_UNREGISTER_CLIENT = 2;
        public static final int SERVICE_RELOAD_SETTINGS = 3;
        public static final int SERVICE_CANCEL_NOTIFICATION_AND_RELOAD_SETTINGS = 4;

        // Messages the service sends to clients
        public static final int CLIENT_SERVICE_CONNECTED = 0;
        public static final int CLIENT_BATTERY_INFO_UPDATED = 1;

        public Messenger serviceMessenger;
        private Messenger clientMessenger;

        public RemoteConnection(Messenger m) {
            clientMessenger = m;
        }

        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            serviceMessenger = new Messenger(iBinder);

            Message outgoing = Message.obtain();
            outgoing.what = SERVICE_CLIENT_CONNECTED;
            outgoing.replyTo = clientMessenger;
            try { serviceMessenger.send(outgoing); } catch (android.os.RemoteException e) {}
        }

        public void onServiceDisconnected(ComponentName name) {
            serviceMessenger = null;
        }
    }

    private void loadSettingsFiles() {
        settings = getSharedPreferences(SettingsActivity.SETTINGS_FILE, Context.MODE_MULTI_PROCESS);
        sp_store = getSharedPreferences(SettingsActivity.SP_STORE_FILE, Context.MODE_MULTI_PROCESS);
    }

    private void reloadSettings(boolean cancelFirst) {
        loadSettingsFiles();
        currentHack.setPreferFS(settings.getBoolean(SettingsActivity.KEY_CURRENT_HACK_PREFER_FS, false));

        str = new Str(res); // Language override may have changed

        if (cancelFirst) {
            stopForeground(true);
            mainNotificationB = new Notification.Builder(this);
        }

        registerReceiver(mBatteryInfoReceiver, batteryChanged);
    }

    private final BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (! Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) return;

            update(intent);
        }
    };

    // Does anything needed when SDK API level increases and sets LAST_SDK_API
    private void sdkVersioning(){
        SharedPreferences.Editor sps_editor = sp_store.edit();
        SharedPreferences.Editor settings_editor = settings.edit();

        if (sp_store.getInt(LAST_SDK_API, 0) < 21 && android.os.Build.VERSION.SDK_INT >= 21) {
            settings_editor.putBoolean(SettingsActivity.KEY_USE_SYSTEM_NOTIFICATION_LAYOUT, true);
        }

        sps_editor.putInt(LAST_SDK_API, android.os.Build.VERSION.SDK_INT);

        sps_editor.commit();
        settings_editor.commit();
    }

    private void update(Intent intent) {
        now = System.currentTimeMillis();
        sps_editor = sp_store.edit();
        updated_lasts = false;

        if (intent != null)
            info.load(intent, sp_store);

        predictor.setPredictionType(settings.getString(SettingsActivity.KEY_PREDICTION_TYPE,
                                                       str.default_prediction_type));
        predictor.update(info);
        info.prediction.updateRelativeTime();

        if (statusHasChanged())
            handleUpdateWithChangedStatus();
        else
            handleUpdateWithSameStatus();

        if (sp_store.getBoolean(KEY_SHOW_NOTIFICATION, true)) {
            prepareNotification();
            doNotify();
        }

        if (alarms.anyActiveAlarms())
            handleAlarms();

        updateWidgets(info);

        syncSpsEditor(); // Important to sync after other Service code that uses 'lasts' but before sending info to client

        for (Messenger messenger : clientMessengers) {
            // TODO: Can I send the same message to multiple clients instead of sending duplicates?
            sendClientMessage(messenger, RemoteConnection.CLIENT_BATTERY_INFO_UPDATED, info.toBundle());
        }

        alarmManager.set(AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + (2 * 60 * 1000), updatePredictorPendingIntent);
    }

    private void updateWidgets(BatteryInfo info) {
        //Intent mainWindowIntent = new Intent(this, BatteryInfoActivity.class);
        //PendingIntent mainWindowPendingIntent = PendingIntent.getActivity(this, RC_MAIN, mainWindowIntent, 0);
        //PendingIntent currentInfoPendingIntent = PendingIntent.getActivity(this, RC_MAIN, currentInfoIntent, 0);

        if (info == null) {
            cwbg.setLevel(0);
        } else {
            bl.setLevel(info.percent);
            cwbg.setLevel(info.percent);
        }

        for (Integer widgetId : widgetIds) {
            RemoteViews rv;

            android.appwidget.AppWidgetProviderInfo awpInfo = widgetManager.getAppWidgetInfo(widgetId);
            if (awpInfo == null) continue; // Based on Developer Console crash reports, this can be null sometimes

            int initLayout = awpInfo.initialLayout;

            if (initLayout == R.layout.circle_app_widget) {
                rv = new RemoteViews(getPackageName(), R.layout.circle_app_widget);
                rv.setImageViewBitmap(R.id.circle_widget_image_view, cwbg.getBitmap());
            } else {
                rv = new RemoteViews(getPackageName(), R.layout.full_app_widget);

                if (info == null) {
                    rv.setImageViewBitmap(R.id.battery_level_view, cwbg.getBitmap());
                    rv.setTextViewText(R.id.fully_charged, "");
                    rv.setTextViewText(R.id.time_remaining, "");
                    rv.setTextViewText(R.id.until_what, "");
                } else {
                    rv.setImageViewBitmap(R.id.battery_level_view, bl.getBitmap());

                    if (info.prediction.what == BatteryInfo.Prediction.NONE) {
                        rv.setTextViewText(R.id.fully_charged, str.timeRemaining(info));
                        rv.setTextViewText(R.id.time_remaining, "");
                        rv.setTextViewText(R.id.until_what, "");
                    } else {
                        rv.setTextViewText(R.id.fully_charged, "");
                        rv.setTextViewText(R.id.time_remaining, str.timeRemaining(info));
                        rv.setTextViewText(R.id.until_what, str.untilWhat(info));
                    }
                }
            }

            if (info == null)
                rv.setTextViewText(R.id.level, "XX" + str.percent_symbol);
            else
                rv.setTextViewText(R.id.level, "" + info.percent + str.percent_symbol);

            rv.setOnClickPendingIntent(R.id.widget_layout, currentInfoPendingIntent);
            widgetManager.updateAppWidget(widgetId, rv);
        }
    }

    private void syncSpsEditor() {
        sps_editor.commit();

        if (updated_lasts) {
            info.last_status_cTM = now;
            info.last_status = info.status;
            info.last_percent = info.percent;
            info.last_plugged = info.plugged;
        }
    }

    private void prepareNotification() {
        mainNotificationTopLine = lineFor(SettingsActivity.KEY_TOP_LINE);
        mainNotificationBottomLine = lineFor(SettingsActivity.KEY_BOTTOM_LINE);

        mainNotificationB.setSmallIcon(iconFor(info.percent));

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            mainNotificationB.setPriority(Integer.valueOf(settings.getString(SettingsActivity.KEY_MAIN_NOTIFICATION_PRIORITY,
                                                                             str.default_main_notification_priority)));
        }

        mainNotificationB.setOngoing(true);

        if (settings.getBoolean(SettingsActivity.KEY_USE_SYSTEM_NOTIFICATION_LAYOUT,
                                res.getBoolean(R.bool.default_use_system_notification_layout))) {
            mainNotificationB.setContentTitle(mainNotificationTopLine)
                .setContentText(mainNotificationBottomLine)
                .setContentIntent(currentInfoPendingIntent);

            if (android.os.Build.VERSION.SDK_INT >= 21)
                mainNotificationB.setVisibility(Notification.VISIBILITY_PUBLIC);
        } else {
            String icon_area = settings.getString(SettingsActivity.KEY_ICON_AREA, res.getString(R.string.default_icon_area_content));

            int layout_id = R.layout.main_notification;
            if (icon_area.equals("percentage_first"))
                layout_id = R.layout.main_notification_percentage_first;

            notificationRV = new RemoteViews(getPackageName(), layout_id);

            if (icon_area.equals("percentage")) {
                notificationRV.setViewVisibility(R.id.percent, View.VISIBLE);
                notificationRV.setViewVisibility(R.id.battery, View.GONE);
            } else if (icon_area.equals("graphic")) {
                notificationRV.setViewVisibility(R.id.percent, View.GONE);
                notificationRV.setViewVisibility(R.id.battery, View.VISIBLE);
            }

            notificationRV.setImageViewBitmap(R.id.battery, bl.getBitmap());
            bl.setLevel(info.percent);

            notificationRV.setTextViewText(R.id.percent, "" + info.percent + str.percent_symbol);
            notificationRV.setTextViewText(R.id.top_line, android.text.Html.fromHtml(mainNotificationTopLine));
            notificationRV.setTextViewText(R.id.bottom_line, mainNotificationBottomLine);

            int color;
            color = colorFor(SettingsActivity.KEY_NOTIFICATION_PERCENTAGE_TEXT_COLOR, SettingsActivity.KEY_CUSTOM_PERCENTAGE_TEXT_COLOR);
            if (color != 0)
                notificationRV.setTextColor(R.id.percent, color);
            color = colorFor(SettingsActivity.KEY_NOTIFICATION_TOP_LINE_COLOR, SettingsActivity.KEY_CUSTOM_TOP_LINE_COLOR);
            if (color != 0)
                notificationRV.setTextColor(R.id.top_line, color);
            color = colorFor(SettingsActivity.KEY_NOTIFICATION_BOTTOM_LINE_COLOR, SettingsActivity.KEY_CUSTOM_BOTTOM_LINE_COLOR);
            if (color != 0)
                notificationRV.setTextColor(R.id.bottom_line, color);

            boolean default_show_box = res.getBoolean(R.bool.default_show_box_around_icon_area);
            boolean show_box = settings.getBoolean(SettingsActivity.KEY_SHOW_BOX_AROUND_ICON_AREA, default_show_box);

            if (show_box) {
                color = res.getColor(R.color.notification_box_default_color);
                if (! icon_area.equals("battery_first"))
                    notificationRV.setInt(R.id.percent, "setBackgroundColor", color);
                if (! icon_area.equals("percentage_first"))
                    notificationRV.setInt(R.id.battery, "setBackgroundColor", color);
            }

            mainNotificationB.setContent(notificationRV)
                .setContentIntent(currentInfoPendingIntent);
        }
    }

    // Since alpha values aren't permitted, return 0 for default
    private int colorFor(String colorKey, String customKey) {
        String colorString = settings.getString(colorKey, "default");

        if (colorString.charAt(0) == '#')
            return colorFromHex(colorString);
        else if (colorString.equals("custom"))
            return settings.getInt(customKey, R.color.main_notification_default_custom_text_color);
        else
            return 0;
    }

    private static int colorFromHex(String hex) {
        if (hex.length() != 7) return 0;
        if (hex.charAt(0) != '#') return 0;

        int color = 0xff;

        for (int i = 1; i <= 6; i++) {
            color <<= 4;
            char c = hex.charAt(i);

            if (c >= '0' && c <= '9')
                color += c - '0';
            else if (c >= 'A' && c <= 'F')
                color += c - 'A' + 10;
            else if (c >= 'a' && c <= 'f')
                color += c - 'a' + 10;
        }

        return color;
    }

    private String lineFor(String key) {
        String req = settings.getString(key, key.equals(SettingsActivity.KEY_TOP_LINE) ? "remaining" : "vitals");

        if (req.equals("remaining"))
            return predictionLine();
        else if (req.equals("vitals"))
            return vitalStatsLine();
        else
            return statusDurationLine();
    }

    private String predictionLine() {
        String line;
        BatteryInfo.RelativeTime predicted = info.prediction.last_rtime;

        if (info.prediction.what == BatteryInfo.Prediction.NONE) {
            line = str.statuses[info.status];
        } else {
            if (predicted.days > 0)
                line = str.n_days_m_hours(predicted.days, predicted.hours);
            else if (predicted.hours > 0) {
                String verbosity = settings.getString(SettingsActivity.KEY_TIME_REMAINING_VERBOSITY,
                                                      res.getString(R.string.default_time_remaining_verbosity));
                if (verbosity.equals("condensed"))
                    line = str.n_hours_m_minutes_medium(predicted.hours, predicted.minutes);
                else if (verbosity.equals("verbose"))
                    line = str.n_hours_m_minutes_long(predicted.hours, predicted.minutes);
                else
                    line = str.n_hours_long_m_minutes_medium(predicted.hours, predicted.minutes);
            } else
                line = str.n_minutes_long(predicted.minutes);

            if (info.prediction.what == BatteryInfo.Prediction.UNTIL_CHARGED)
                line += res.getString(R.string.notification_until_charged);
            else
                line += res.getString(R.string.notification_until_drained);
        }

        return line;
    }

    private String vitalStatsLine() {
        Boolean convertF = settings.getBoolean(SettingsActivity.KEY_CONVERT_F, false);
        String line = str.healths[info.health] + " / " + str.formatTemp(info.temperature, convertF);

        if (info.voltage > 500)
            line += " / " + str.formatVoltage(info.voltage);
        if (settings.getBoolean(SettingsActivity.KEY_ENABLE_CURRENT_HACK, false) &&
            settings.getBoolean(SettingsActivity.KEY_DISPLAY_CURRENT_IN_VITAL_STATS, false)) {
            Long current = null;
            if (settings.getBoolean(SettingsActivity.KEY_PREFER_CURRENT_AVG_IN_VITAL_STATS, false))
                current = currentHack.getAvgCurrent();
            if (current == null) // Either don't prefer avg or avg isn't available
                current = currentHack.getCurrent();
            if (current != null)
                line += " / " + String.valueOf(current) + "mA";
        }
        if (settings.getBoolean(SettingsActivity.KEY_STATUS_DURATION_IN_VITAL_SIGNS, false)) {
            float statusDurationHours = (now - info.last_status_cTM) / (60 * 60 * 1000f);
            line += " / " + String.format("%.1f", statusDurationHours) + "h"; // TODO: Translatable 'h'
        }

        return line;
    }

    private String statusDurationLine() {
        long statusDuration = now - info.last_status_cTM;
        int statusDurationHours = (int) ((statusDuration + (1000 * 60 * 30)) / (1000 * 60 * 60));
        String line = str.statuses[info.status] + " ";

        if (statusDuration < 1000 * 60 * 60)
            line += str.since + " " + formatTime(new Date(info.last_status_cTM));
        else
            line += str.for_n_hours(statusDurationHours);

        return line;
    }

    private void doNotify() {
        mHandler.post(mNotify);
    }

    // I take advantage of (count on) R.java having resources alphabetical and incrementing by one.
    private int iconFor(int percent) {
        String default_set = "builtin.plain_number";

        String icon_set = settings.getString(SettingsActivity.KEY_ICON_SET, "null");
        if (! icon_set.startsWith("builtin.")) icon_set = "null"; // TODO: Remove this line to re-enable plugins

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
        } else if (android.os.Build.VERSION.SDK_INT >= 21 &&
                   !settings.getBoolean(SettingsActivity.KEY_CLASSIC_COLOR_MODE, false)) {
            // Classic set is desired, but colors break notification icons on API level 21+
            return R.drawable.w000 + info.percent;
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

    private boolean statusHasChanged() {
        int previous_charge = sp_store.getInt(KEY_PREVIOUS_CHARGE, 100);

        return (info.last_status != info.status ||
                info.last_status_cTM >= now ||
                info.last_plugged != info.plugged ||
                (info.plugged == BatteryInfo.PLUGGED_UNPLUGGED && info.percent > previous_charge + 20));
    }

    private void handleUpdateWithChangedStatus() {
        if (settings.getBoolean(SettingsActivity.KEY_ENABLE_LOGGING, true)) {
            log_db.logStatus(info, now, LogDatabase.STATUS_NEW);

            if (info.status != info.last_status && info.last_status == BatteryInfo.STATUS_UNPLUGGED)
                log_db.prune(Integer.valueOf(settings.getString(SettingsActivity.KEY_MAX_LOG_AGE, str.default_max_log_age)));
        }

        if (settings.getBoolean(SettingsActivity.KEY_ENABLE_CURRENT_HACK, false) &&
            settings.getBoolean(SettingsActivity.KEY_DISPLAY_CURRENT_IN_VITAL_STATS, false)) {
            mHandler.postDelayed(runRenotify, 1000);
            mHandler.postDelayed(runRenotify, 3000);
            mHandler.postDelayed(runRenotify, 9000);
            mHandler.postDelayed(runRenotify, 27000);
        }

        /* TODO: Af first glance, I think I want to do this, but think about it a bit and decide for sure... */
        if (info.status != info.last_status && info.status == BatteryInfo.STATUS_UNPLUGGED)
            mNotificationManager.cancel(NOTIFICATION_ALARM_CHARGE);

        updated_lasts = true;
        sps_editor.putLong(BatteryInfo.KEY_LAST_STATUS_CTM, now);
        sps_editor.putInt(BatteryInfo.KEY_LAST_STATUS, info.status);
        sps_editor.putInt(BatteryInfo.KEY_LAST_PERCENT, info.percent);
        sps_editor.putInt(BatteryInfo.KEY_LAST_PLUGGED, info.plugged);
        sps_editor.putInt(KEY_PREVIOUS_CHARGE, info.percent);
        sps_editor.putInt(KEY_PREVIOUS_TEMP, info.temperature);
        sps_editor.putInt(KEY_PREVIOUS_HEALTH, info.health);
    }

    private void handleUpdateWithSameStatus() {
        if (settings.getBoolean(SettingsActivity.KEY_ENABLE_LOGGING, true))
            log_db.logStatus(info, now, LogDatabase.STATUS_OLD);

        if (info.percent % 10 == 0) {
            sps_editor.putInt(KEY_PREVIOUS_CHARGE, info.percent);
            sps_editor.putInt(KEY_PREVIOUS_TEMP, info.temperature);
            sps_editor.putInt(KEY_PREVIOUS_HEALTH, info.health);
        }
    }

    private void handleAlarms() {
        Cursor c;
        Notification.Builder nb;

        int previous_charge = sp_store.getInt(KEY_PREVIOUS_CHARGE, 100);

        if (info.status == BatteryInfo.STATUS_FULLY_CHARGED && info.status != info.last_status) {
            c = alarms.activeAlarmFull();
            if (c != null) {
                nb = parseAlarmCursor(c);
                nb.setContentTitle(str.alarm_fully_charged)
                    .setContentText(str.alarm_text)
                    .setContentIntent(alarmsPendingIntent);

                if (android.os.Build.VERSION.SDK_INT >= 21)
                    nb.setVisibility(Notification.VISIBILITY_PUBLIC);

                mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, nb.build());
                c.close();
            }
        }

        c = alarms.activeAlarmChargeDrops(info.percent, previous_charge);
        if (c != null) {
            sps_editor.putInt(KEY_PREVIOUS_CHARGE, info.percent);
            nb = parseAlarmCursor(c);
            nb.setContentTitle(str.alarm_charge_drops + c.getInt(alarms.INDEX_THRESHOLD) + str.percent_symbol)
                .setContentText(str.alarm_text)
                .setContentIntent(alarmsPendingIntent);

            if (android.os.Build.VERSION.SDK_INT >= 21)
                nb.setVisibility(Notification.VISIBILITY_PUBLIC);

            mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, nb.build());
            c.close();
        }

        c = alarms.activeAlarmChargeRises(info.percent, previous_charge);
        if (c != null && info.status != BatteryInfo.STATUS_UNPLUGGED) {
            sps_editor.putInt(KEY_PREVIOUS_CHARGE, info.percent);
            nb = parseAlarmCursor(c);
            nb.setContentTitle(str.alarm_charge_rises + c.getInt(alarms.INDEX_THRESHOLD) + str.percent_symbol)
                .setContentText(str.alarm_text)
                .setContentIntent(alarmsPendingIntent);

            if (android.os.Build.VERSION.SDK_INT >= 21)
                nb.setVisibility(Notification.VISIBILITY_PUBLIC);

            mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, nb.build());
            c.close();
        }

        c = alarms.activeAlarmTempRises(info.temperature, sp_store.getInt(KEY_PREVIOUS_TEMP, 1));
        if (c != null) {
            Boolean convertF = settings.getBoolean(SettingsActivity.KEY_CONVERT_F, false);
            sps_editor.putInt(KEY_PREVIOUS_TEMP, info.temperature);
            nb = parseAlarmCursor(c);
            nb.setContentTitle(str.alarm_temp_rises + str.formatTemp(c.getInt(alarms.INDEX_THRESHOLD), convertF, false))
                .setContentText(str.alarm_text)
                .setContentIntent(alarmsPendingIntent);

            if (android.os.Build.VERSION.SDK_INT >= 21)
                nb.setVisibility(Notification.VISIBILITY_PUBLIC);

            mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, nb.build());
            c.close();
        }

        if (info.health > BatteryInfo.HEALTH_GOOD && info.health != sp_store.getInt(KEY_PREVIOUS_HEALTH, BatteryInfo.HEALTH_GOOD)) {
            c = alarms.activeAlarmFailure();
            if (c != null) {
                sps_editor.putInt(KEY_PREVIOUS_HEALTH, info.health);
                nb = parseAlarmCursor(c);
                nb.setContentTitle(str.alarm_health_failure + str.healths[info.health])
                    .setContentText(str.alarm_text)
                    .setContentIntent(alarmsPendingIntent);

                if (android.os.Build.VERSION.SDK_INT >= 21)
                    nb.setVisibility(Notification.VISIBILITY_PUBLIC);

                mNotificationManager.notify(NOTIFICATION_ALARM_CHARGE, nb.build());
                c.close();
            }
        }
    }

    private Notification.Builder parseAlarmCursor(Cursor c) {
        Notification.Builder nb = new Notification.Builder(this)
            .setSmallIcon(R.drawable.stat_notify_alarm)
            .setAutoCancel(true);

        String ringtone = c.getString(alarms.INDEX_RINGTONE);
        if (! ringtone.equals(""))
            nb.setSound(android.net.Uri.parse(ringtone));

        if (c.getInt(alarms.INDEX_VIBRATE) == 1)
            if (mAudioManager.getRingerMode() != mAudioManager.RINGER_MODE_SILENT)
                /* I couldn't get the Notification to vibrate, so I do it myself... */
                mVibrator.vibrate(new long[] {0, 200, 200, 400}, -1);

        if (c.getInt(alarms.INDEX_LIGHTS) == 1)
            nb.setDefaults(Notification.DEFAULT_LIGHTS);

        return nb;
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

    public static void onWidgetUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        widgetManager = appWidgetManager;

        for (int i = 0; i < appWidgetIds.length; i++) {
            widgetIds.add(appWidgetIds[i]);
        }

        context.startService(new Intent(context, BatteryInfoService.class));
    }

    public static void onWidgetDeleted(Context context, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            widgetIds.remove(appWidgetIds[i]);
        }
    }
}
