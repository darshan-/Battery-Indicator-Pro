/*
    Copyright (c) 2009-2018 Darshan-Josiah Barber

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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import java.util.Locale;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    public static final String SETTINGS_FILE = "com.darshancomputing.BatteryIndicatorPro_preferences";
    public static final String SP_SERVICE_FILE = "sp_store";   // Only write from Service process
    public static final String SP_MAIN_FILE = "sp_store_main"; // Only write from main process

    public static final String KEY_NOTIFICATION_SETTINGS = "notification_settings";
    public static final String KEY_STATUS_BAR_ICON_SETTINGS = "status_bar_icon_settings";
    public static final String KEY_CURRENT_HACK_SETTINGS = "current_hack_settings";
    public static final String KEY_ALARMS_SETTINGS = "alarms_settings";
    public static final String KEY_ALARM_EDIT_SETTINGS = "alarm_edit_settings";
    public static final String KEY_OTHER_SETTINGS = "other_settings";
    public static final String KEY_ENABLE_LOGGING = "enable_logging";
    public static final String KEY_MAX_LOG_AGE = "max_log_age";
    public static final String KEY_ICON_PLUGIN = "icon_plugin";
    public static final String KEY_ICON_SET = "icon_set";
    public static final String KEY_CONVERT_F = "convert_to_fahrenheit";
    public static final String KEY_NOTIFY_STATUS_DURATION = "notify_status_duration";
    public static final String KEY_AUTOSTART = "autostart";
    public static final String KEY_PREDICTION_TYPE = "prediction_type";
    public static final String KEY_CLASSIC_COLOR_MODE = "classic_color_mode";
    public static final String KEY_STATUS_DUR_EST = "status_dur_est";
    public static final String KEY_CAT_CLASSIC_COLOR_MODE = "category_classic_color_mode";
    public static final String KEY_CAT_COLOR = "category_color";
    public static final String KEY_CAT_CHARGING_INDICATOR = "category_charging_indicator";
    public static final String KEY_CAT_PLUGIN_SETTINGS = "category_plugin_settings";
    public static final String KEY_PLUGIN_SETTINGS = "plugin_settings";
    public static final String KEY_INDICATE_CHARGING = "indicate_charging";
    public static final String KEY_RED = "use_red";
    public static final String KEY_RED_THRESH = "red_threshold";
    public static final String KEY_AMBER = "use_amber";
    public static final String KEY_AMBER_THRESH = "amber_threshold";
    public static final String KEY_GREEN = "use_green";
    public static final String KEY_GREEN_THRESH = "green_threshold";
    public static final String KEY_COLOR_PREVIEW = "color_preview";
    public static final String KEY_USE_SYSTEM_NOTIFICATION_LAYOUT = "use_system_notification_layout";
    public static final String KEY_ICON_AREA = "icon_area";
    public static final String KEY_TOP_LINE = "top_line";
    public static final String KEY_BOTTOM_LINE = "bottom_line";
    public static final String KEY_TIME_REMAINING_VERBOSITY = "time_remaining_verbosity";
    public static final String KEY_STATUS_DURATION_IN_VITAL_SIGNS = "status_duration_in_vital_signs";
    public static final String KEY_CAT_NOTIFICATION_APPEARANCE = "category_notification_appearance";
    public static final String KEY_NOTIFICATION_PERCENTAGE_TEXT_COLOR = "notification_percentage_text_color";
    public static final String KEY_CUSTOM_PERCENTAGE_TEXT_COLOR = "custom_percentage_text_color";
    public static final String KEY_SHOW_BOX_AROUND_ICON_AREA = "show_box_around_icon_area";
    public static final String KEY_NOTIFICATION_TOP_LINE_COLOR = "notification_top_line_color";
    public static final String KEY_CUSTOM_TOP_LINE_COLOR = "custom_top_line_color";
    public static final String KEY_NOTIFICATION_BOTTOM_LINE_COLOR = "notification_bottom_line_color";
    public static final String KEY_CUSTOM_BOTTOM_LINE_COLOR = "custom_bottom_line_color";
    public static final String KEY_CAT_CURRENT_HACK_MAIN = "category_current_hack_main";
    public static final String KEY_CAT_CURRENT_HACK_UNSUPPORTED = "category_current_hack_unsupported";
    public static final String KEY_ENABLE_CURRENT_HACK = "enable_current_hack";
    public static final String KEY_CURRENT_HACK_PREFER_FS = "current_hack_prefer_fs";
    public static final String KEY_CAT_CURRENT_HACK_NOTIFICATION = "category_current_hack_notification";
    public static final String KEY_DISPLAY_CURRENT_IN_VITAL_STATS = "display_current_in_vital_stats";
    public static final String KEY_PREFER_CURRENT_AVG_IN_VITAL_STATS = "prefer_current_avg_in_vital_stats";
    public static final String KEY_CAT_CURRENT_HACK_MAIN_WINDOW = "category_current_hack_main_window";
    public static final String KEY_DISPLAY_CURRENT_IN_MAIN_WINDOW = "display_current_in_main_window";
    public static final String KEY_PREFER_CURRENT_AVG_IN_MAIN_WINDOW = "prefer_current_avg_in_main_window";
    public static final String KEY_AUTO_REFRESH_CURRENT_IN_MAIN_WINDOW = "auto_refresh_current_in_main_window";
    public static final String KEY_FIRST_RUN = "first_run";
    public static final String KEY_MIGRATED_SERVICE_DESIRED = "service_desired_migrated_to_sp_main";
    public static final String KEY_ENABLE_NOTIFS_B = "enable_notifications_button";
    public static final String KEY_ENABLE_NOTIFS_SUMMARY = "enable_notifications_summary";

    private static final String[] PARENTS    = {KEY_ENABLE_LOGGING,
                                                KEY_DISPLAY_CURRENT_IN_VITAL_STATS,
                                                KEY_DISPLAY_CURRENT_IN_MAIN_WINDOW,
                                                KEY_RED,
                                                KEY_AMBER,
                                                KEY_GREEN
    };
    private static final String[][] DEPENDENTS = {{KEY_MAX_LOG_AGE},
                                                  {KEY_PREFER_CURRENT_AVG_IN_VITAL_STATS},
                                                  {KEY_PREFER_CURRENT_AVG_IN_MAIN_WINDOW, KEY_AUTO_REFRESH_CURRENT_IN_MAIN_WINDOW},
                                                  {KEY_RED_THRESH},
                                                  {KEY_AMBER_THRESH},
                                                  {KEY_GREEN_THRESH}
    };

    private static final String[] CURRENT_HACK_DEPENDENTS = {KEY_CURRENT_HACK_PREFER_FS,
                                                             KEY_DISPLAY_CURRENT_IN_VITAL_STATS,
                                                             KEY_PREFER_CURRENT_AVG_IN_VITAL_STATS,
                                                             KEY_DISPLAY_CURRENT_IN_MAIN_WINDOW,
                                                             KEY_PREFER_CURRENT_AVG_IN_MAIN_WINDOW,
                                                             KEY_AUTO_REFRESH_CURRENT_IN_MAIN_WINDOW
    };

    private static final String[] INVERSE_PARENTS    = {KEY_USE_SYSTEM_NOTIFICATION_LAYOUT
    };
    private static final String[] INVERSE_DEPENDENTS = {KEY_ICON_AREA
    };

    private static final String[] COLOR_PARENTS    = {KEY_NOTIFICATION_PERCENTAGE_TEXT_COLOR,
                                                      KEY_NOTIFICATION_TOP_LINE_COLOR,
                                                      KEY_NOTIFICATION_BOTTOM_LINE_COLOR
    };
    private static final String[] COLOR_DEPENDENTS = {KEY_CUSTOM_PERCENTAGE_TEXT_COLOR,
                                                      KEY_CUSTOM_TOP_LINE_COLOR,
                                                      KEY_CUSTOM_BOTTOM_LINE_COLOR
    };

    private static final String[] LIST_PREFS = {KEY_AUTOSTART, KEY_STATUS_DUR_EST,
                                                KEY_RED_THRESH, KEY_AMBER_THRESH, KEY_GREEN_THRESH,
                                                KEY_ICON_SET,
                                                KEY_MAX_LOG_AGE, KEY_ICON_AREA, KEY_TOP_LINE, KEY_BOTTOM_LINE,
                                                KEY_TIME_REMAINING_VERBOSITY,
                                                KEY_NOTIFICATION_PERCENTAGE_TEXT_COLOR,
                                                KEY_NOTIFICATION_TOP_LINE_COLOR,
                                                KEY_NOTIFICATION_BOTTOM_LINE_COLOR,
                                                KEY_PREDICTION_TYPE
    };

    private static final String[] RESET_SERVICE = {KEY_CONVERT_F, KEY_NOTIFY_STATUS_DURATION,
                                                   KEY_RED, KEY_RED_THRESH,
                                                   KEY_AMBER, KEY_AMBER_THRESH, KEY_GREEN, KEY_GREEN_THRESH,
                                                   KEY_ICON_SET,
                                                   KEY_INDICATE_CHARGING,
                                                   KEY_TOP_LINE, KEY_BOTTOM_LINE,
                                                   KEY_ENABLE_LOGGING,
                                                   KEY_TIME_REMAINING_VERBOSITY,
                                                   KEY_STATUS_DURATION_IN_VITAL_SIGNS,
                                                   KEY_CUSTOM_PERCENTAGE_TEXT_COLOR,
                                                   KEY_CUSTOM_TOP_LINE_COLOR,
                                                   KEY_CUSTOM_BOTTOM_LINE_COLOR,
                                                   KEY_ENABLE_CURRENT_HACK,
                                                   KEY_CURRENT_HACK_PREFER_FS,
                                                   KEY_DISPLAY_CURRENT_IN_VITAL_STATS,
                                                   KEY_PREFER_CURRENT_AVG_IN_VITAL_STATS,
                                                   KEY_PREDICTION_TYPE
    };

    private static final String[] RESET_SERVICE_WITH_CANCEL_NOTIFICATION = {KEY_ICON_AREA,
                                                                            KEY_NOTIFICATION_PERCENTAGE_TEXT_COLOR,
                                                                            KEY_NOTIFICATION_TOP_LINE_COLOR,
                                                                            KEY_NOTIFICATION_BOTTOM_LINE_COLOR,
                                                                            KEY_SHOW_BOX_AROUND_ICON_AREA,
                                                                            KEY_USE_SYSTEM_NOTIFICATION_LAYOUT
    };

    public static final String EXTRA_SCREEN = "com.darshancomputing.BatteryIndicatorPro.PrefScreen";

    public static final int   RED = 0;
    public static final int AMBER = 1;
    public static final int GREEN = 2;

    /* Red must go down to 0 and green must go up to 100,
       which is why they aren't listed here. */
    public static final int   RED_ICON_MAX = 30;
    public static final int AMBER_ICON_MIN =  0;
    public static final int AMBER_ICON_MAX = 50;
    public static final int GREEN_ICON_MIN = 20;

    public static final int   RED_SETTING_MIN =  5;
    public static final int   RED_SETTING_MAX = 30;
    public static final int AMBER_SETTING_MIN = 10;
    public static final int AMBER_SETTING_MAX = 50;
    public static final int GREEN_SETTING_MIN = 20;
    /* public static final int GREEN_SETTING_MAX = 100; /* TODO: use this, and possibly set it to 95. */

    private Messenger serviceMessenger;
    private final Messenger messenger = new Messenger(new MessageHandler(this));
    private final BatteryInfoService.RemoteConnection serviceConnection = new BatteryInfoService.RemoteConnection(messenger);

    private Resources res;
    private PreferenceScreen mPreferenceScreen;
    private SharedPreferences mSharedPreferences;
    private NotificationManager mNotificationManager;
    private NotificationChannel mainChan;
    private boolean appNotifsEnabled;
    private boolean mainNotifsEnabled;

    private String pref_screen;

    private ColorPreviewPreference cpbPref;

    private ListPreference   redThresh;
    private ListPreference amberThresh;
    private ListPreference greenThresh;

    private Boolean   redEnabled;
    private Boolean amberEnabled;
    private Boolean greenEnabled;

    private int   iRedThresh;
    private int iAmberThresh;
    private int iGreenThresh;

    private int menu_res = R.menu.settings;

    private static final String[] fivePercents = {
        "5", "10", "15", "20", "25", "30", "35", "40", "45", "50",
        "55", "60", "65", "70", "75", "80", "85", "90", "95", "100"};

    /* Returns a two-item array of the start and end indices into the above arrays. */
    private int[] indices(int x, int y) {
        int[] a = new int[2];
        int i; /* How many values to remove from the front */
        int j; /* How many values to remove from the end   */

        i = (x / 5) - 1;
        j = (100 - y) / 5;

        a[0] = i;
        a[1] = j;
        return a;
    }

    //private static final Object[] EMPTY_OBJECT_ARRAY = {};
    //private static final  Class<?>[]  EMPTY_CLASS_ARRAY = {};

    private static class MessageHandler extends Handler {
        private SettingsActivity sa;

        MessageHandler(SettingsActivity a) {
            sa = a;
        }

        @Override
        public void handleMessage(Message incoming) {
            switch (incoming.what) {
            case BatteryInfoService.RemoteConnection.CLIENT_SERVICE_CONNECTED:
                sa.serviceMessenger = incoming.replyTo;
                break;
            default:
                super.handleMessage(incoming);
            }
        }
    }

    private final Handler mHandler = new Handler();
    Runnable rShowPluginSettings = new Runnable() {
        public void run() {
        /* TODO: Convert to message
            if (biServiceConnection.biService == null) {
                bindService(biServiceIntent, biServiceConnection, 0);
                return;
            }
        */

            //Boolean hasSettings = false;
        //    try {
        /* TODO: Convert to message
                hasSettings = biServiceConnection.biService.pluginHasSettings();
        */
        //    } catch (Exception e) {
        //        e.printStackTrace();
        //    }

        //    if (! hasSettings) {
                PreferenceCategory cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_PLUGIN_SETTINGS);
                cat.removeAll();
                cat.setLayoutResource(R.layout.hidden);
        //    } else {
        //        Preference p = (Preference) mPreferenceScreen.findPreference(KEY_PLUGIN_SETTINGS);
        //        p.setEnabled(true);
        //    }

            mHandler.removeCallbacks(rShowPluginSettings);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        pref_screen = intent.getStringExtra(EXTRA_SCREEN);
        res = getResources();

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mainChan = mNotificationManager.getNotificationChannel(BatteryInfoService.CHAN_ID_MAIN);

        appNotifsEnabled = mNotificationManager.areNotificationsEnabled();
        mainNotifsEnabled = mainChan.getImportance() > 0;

        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesName(SETTINGS_FILE);
        pm.setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);
        mSharedPreferences = pm.getSharedPreferences();

        CurrentHack.setContext(this);
        CurrentHack.setPreferFS(mSharedPreferences.getBoolean(SettingsActivity.KEY_CURRENT_HACK_PREFER_FS,
                                                              res.getBoolean(R.bool.default_prefer_fs_current_hack)));

        if (pref_screen == null) {
            setPrefScreen(R.xml.main_pref_screen);
            setWindowSubtitle(res.getString(R.string.settings_activity_subtitle));
        } else if ((pref_screen.equals(KEY_STATUS_BAR_ICON_SETTINGS) ||
                    pref_screen.equals(KEY_NOTIFICATION_SETTINGS)) &&
                   (!appNotifsEnabled || !mainNotifsEnabled)) {
            setPrefScreen(R.xml.main_notifs_disabled_pref_screen);
            Preference prefb = mPreferenceScreen.findPreference(KEY_ENABLE_NOTIFS_B);
            Preference prefs = mPreferenceScreen.findPreference(KEY_ENABLE_NOTIFS_SUMMARY);
            if (!appNotifsEnabled) {
                prefs.setSummary(R.string.app_notifs_disabled_summary);
                prefb.setSummary(R.string.app_notifs_disabled_b);
            } else {
                prefs.setSummary(R.string.main_notifs_disabled_summary);
                prefb.setSummary(R.string.main_notifs_disabled_b);
            }
        } else if (pref_screen.equals(KEY_STATUS_BAR_ICON_SETTINGS)) {
            setPrefScreen(R.xml.status_bar_icon_pref_screen);
            setWindowSubtitle(res.getString(R.string.status_bar_icon_settings));

            menu_res = R.menu.status_bar_icon_settings;

            cpbPref     = (ColorPreviewPreference) mPreferenceScreen.findPreference(KEY_COLOR_PREVIEW);

            ListPreference iconSetPref = (ListPreference) mPreferenceScreen.findPreference(KEY_ICON_SET);
            setPluginPrefEntriesAndValues(iconSetPref);
            String currentPlugin = iconSetPref.getValue();
            if (currentPlugin == null)
                currentPlugin = "builtin.plain_number";

            redThresh   = (ListPreference) mPreferenceScreen.findPreference(KEY_RED_THRESH);
            amberThresh = (ListPreference) mPreferenceScreen.findPreference(KEY_AMBER_THRESH);
            greenThresh = (ListPreference) mPreferenceScreen.findPreference(KEY_GREEN_THRESH);

            PreferenceCategory cat;

            cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_PLUGIN_SETTINGS);
            cat.removeAll();

            if (currentPlugin.equals("builtin.classic")) {
                cat.setLayoutResource(R.layout.none);

                if (!mSharedPreferences.getBoolean(KEY_CLASSIC_COLOR_MODE, false)) {
                    cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_COLOR);
                    cat.removeAll();
                    cat.setLayoutResource(R.layout.none);
                } else {
                    redEnabled   = mSharedPreferences.getBoolean(  KEY_RED, false);
                    amberEnabled = mSharedPreferences.getBoolean(KEY_AMBER, false);
                    greenEnabled = mSharedPreferences.getBoolean(KEY_GREEN, false);

                    iRedThresh   = Integer.valueOf(  redThresh.getValue()); /* Entries don't exist yet */
                    iAmberThresh = Integer.valueOf(amberThresh.getValue());
                    iGreenThresh = Integer.valueOf(greenThresh.getValue());

                    validateColorPrefs(null);
                }

                cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_CHARGING_INDICATOR);
                cat.removeAll();
                cat.setLayoutResource(R.layout.none);
            } else {
                cat.setLayoutResource(R.layout.hidden);

                cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_COLOR);
                cat.removeAll();
                cat.setLayoutResource(R.layout.none);
                cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_CLASSIC_COLOR_MODE);
                cat.removeAll();
                cat.setLayoutResource(R.layout.none);
            }
        } else if (pref_screen.equals(KEY_NOTIFICATION_SETTINGS)) {
            setPrefScreen(R.xml.notification_pref_screen);
            setWindowSubtitle(res.getString(R.string.notification_settings));

            Preference prefb = mPreferenceScreen.findPreference(KEY_ENABLE_NOTIFS_B);
            prefb.setSummary(R.string.pref_manage_main_channel);

            if (mSharedPreferences.getBoolean(KEY_USE_SYSTEM_NOTIFICATION_LAYOUT, false)) {
                PreferenceCategory cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_NOTIFICATION_APPEARANCE);
                cat.removeAll();
                cat.setLayoutResource(R.layout.none);
            }
        } else if (pref_screen.equals(KEY_CURRENT_HACK_SETTINGS)) {
            setPrefScreen(R.xml.current_hack_pref_screen);
            setWindowSubtitle(res.getString(R.string.current_hack_settings));

            if (CurrentHack.getCurrent() == null) {
                PreferenceCategory cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_CURRENT_HACK_MAIN);
                cat.removeAll();
                cat.setLayoutResource(R.layout.none);
                cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_CURRENT_HACK_NOTIFICATION);
                cat.removeAll();
                cat.setLayoutResource(R.layout.none);
                cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_CURRENT_HACK_MAIN_WINDOW);
                cat.removeAll();
                cat.setLayoutResource(R.layout.none);
            } else {
                PreferenceCategory cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_CURRENT_HACK_UNSUPPORTED);
                cat.removeAll();
                cat.setLayoutResource(R.layout.none);
            }
        } else if (pref_screen.equals(KEY_OTHER_SETTINGS)) {
            setPrefScreen(R.xml.other_pref_screen);
            setWindowSubtitle(res.getString(R.string.other_settings));
        } else {
            setPrefScreen(R.xml.main_pref_screen);
        }

        for (int i=0; i < PARENTS.length; i++)
            setEnablednessOfDeps(i);

        for (int i=0; i < INVERSE_PARENTS.length; i++)
            setEnablednessOfInverseDeps(i);

        for (int i=0; i < COLOR_PARENTS.length; i++)
            setEnablednessOfColorDeps(i);

        for (int i=0; i < LIST_PREFS.length; i++)
            updateListPrefSummary(LIST_PREFS[i]);

        if (pref_screen != null &&
            pref_screen.equals(KEY_CURRENT_HACK_SETTINGS) &&
            !mSharedPreferences.getBoolean(KEY_ENABLE_CURRENT_HACK, false))
            setEnablednessOfCurrentHackDeps(false);

        setEnablednessOfPercentageTextColor();

        updateConvertFSummary();

        Intent biServiceIntent = new Intent(this, BatteryInfoService.class);
        bindService(biServiceIntent, serviceConnection, 0);
    }

    public static Locale codeToLocale (String code) {
        String[] parts = code.split("_");

        if (parts.length > 1)
            return new Locale(parts[0], parts[1]);
        else
            return new Locale(parts[0]);
    }

    private void setWindowSubtitle(String subtitle) {
        if (res.getBoolean(R.bool.long_activity_names))
            setTitle(res.getString(R.string.app_full_name) + " - " + subtitle);
        else
            setTitle(subtitle);
    }

    private void setPrefScreen(int resource) {
        addPreferencesFromResource(resource);

        mPreferenceScreen  = getPreferenceScreen();
    }

    private void restartThisScreen() {
        ComponentName comp = new ComponentName(getPackageName(), SettingsActivity.class.getName());
        Intent intent = new Intent().setComponent(comp);
        intent.putExtra(EXTRA_SCREEN, pref_screen);
        startActivity(intent);
        finish();
    }

    private void resetService() {
        resetService(false);
    }

    private void resetService(boolean cancelFirst) {
        mSharedPreferences.edit().commit(); // commit() synchronously before messaging Service

        Message outgoing = Message.obtain();

        if (cancelFirst)
            outgoing.what = BatteryInfoService.RemoteConnection.SERVICE_CANCEL_NOTIFICATION_AND_RELOAD_SETTINGS;
        else
            outgoing.what = BatteryInfoService.RemoteConnection.SERVICE_RELOAD_SETTINGS;

        try {
            serviceMessenger.send(outgoing);
        } catch (Exception e) {
            startForegroundService(new Intent(this, BatteryInfoService.class));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serviceConnection != null) unbindService(serviceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Turns out mainChan is unchangeable, so getImportance() just returns the importance at the time getNotificationChannel was called
        mainChan = mNotificationManager.getNotificationChannel(BatteryInfoService.CHAN_ID_MAIN);

        if (appNotifsEnabled != mNotificationManager.areNotificationsEnabled() ||
            mainNotifsEnabled != mainChan.getImportance() > 0) { // Doesn't seem worth checking which screen
            resetService();
            restartThisScreen();
        } else {
            mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menu_res, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_help:
            ComponentName comp = new ComponentName(getPackageName(), SettingsHelpActivity.class.getName());
            Intent intent = new Intent().setComponent(comp);

            if (pref_screen != null) intent.putExtra(EXTRA_SCREEN, pref_screen);

            startActivity(intent);

            return true;
        case R.id.menu_get_plugins:
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse
                                     ("http://bi-icon-plugins.darshancomputing.com/")));
            return true;
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (key == null) {
            return false;
        } else if (key.equals(KEY_NOTIFICATION_SETTINGS) || key.equals(KEY_STATUS_BAR_ICON_SETTINGS) ||
                   key.equals(KEY_CURRENT_HACK_SETTINGS) ||
                   key.equals(KEY_OTHER_SETTINGS)) {
            ComponentName comp = new ComponentName(getPackageName(), SettingsActivity.class.getName());
            startActivity(new Intent().setComponent(comp).putExtra(EXTRA_SCREEN, key));

            return true;
        } else //TODO: convert biServiceConnection.biService.configurePlugin();
            return key.equals(KEY_PLUGIN_SETTINGS);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

        if (key.equals(KEY_RED)) {
            redEnabled = mSharedPreferences.getBoolean(KEY_RED, false);
        } else if (key.equals(KEY_AMBER)) {
            amberEnabled = mSharedPreferences.getBoolean(KEY_AMBER, false);
        } else if (key.equals(KEY_GREEN)) {
            greenEnabled = mSharedPreferences.getBoolean(KEY_GREEN, false);
        } else if (key.equals(KEY_RED_THRESH)) {
            iRedThresh = Integer.valueOf((String) redThresh.getEntry());
        } else if (key.equals(KEY_AMBER_THRESH)) {
            iAmberThresh = Integer.valueOf((String) amberThresh.getEntry());
        } else if (key.equals(KEY_GREEN_THRESH)) {
            iGreenThresh = Integer.valueOf((String) greenThresh.getEntry());
        }

        if (key.equals(KEY_RED) || key.equals(KEY_RED_THRESH) ||
            key.equals(KEY_AMBER) || key.equals(KEY_AMBER_THRESH) ||
            key.equals(KEY_GREEN) || key.equals(KEY_GREEN_THRESH)) {
            validateColorPrefs(key);
        }

        if (key.equals(KEY_CLASSIC_COLOR_MODE)) {
            resetService();
            restartThisScreen(); // To show/hide icon-set/plugin settings
        }

        if (key.equals(KEY_ICON_SET)) {
            resetService();
            restartThisScreen(); // To show/hide icon-set/plugin settings
        }

        if (key.equals(KEY_USE_SYSTEM_NOTIFICATION_LAYOUT))
            restartThisScreen();

        if (key.equals(KEY_ICON_AREA))
            setEnablednessOfPercentageTextColor();

        for (int i=0; i < PARENTS.length; i++) {
            if (key.equals(PARENTS[i])) {
                setEnablednessOfDeps(i);
                break;
            }
        }

        for (int i=0; i < INVERSE_PARENTS.length; i++) {
            if (key.equals(INVERSE_PARENTS[i])) {
                setEnablednessOfInverseDeps(i);
                break;
            }
        }

        for (int i=0; i < COLOR_PARENTS.length; i++) {
            if (key.equals(COLOR_PARENTS[i])) {
                setEnablednessOfColorDeps(i);
                break;
            }
        }

        for (int i=0; i < LIST_PREFS.length; i++) {
            if (key.equals(LIST_PREFS[i])) {
                updateListPrefSummary(LIST_PREFS[i]);
                break;
            }
        }

        if (key.equals(KEY_CONVERT_F)) {
            updateConvertFSummary();
        }

        if (key.equals(KEY_ENABLE_CURRENT_HACK)) {
            if (mSharedPreferences.getBoolean(KEY_ENABLE_CURRENT_HACK, false))
                setEnablednessOfCurrentHackDeps(true);

            for (int i=0; i < PARENTS.length; i++)
                setEnablednessOfDeps(i);

            if (!mSharedPreferences.getBoolean(KEY_ENABLE_CURRENT_HACK, false))
                setEnablednessOfCurrentHackDeps(false);
        }

        if (key.equals(KEY_CURRENT_HACK_PREFER_FS))
            CurrentHack.setPreferFS(mSharedPreferences.getBoolean(SettingsActivity.KEY_CURRENT_HACK_PREFER_FS,
                                                                  res.getBoolean(R.bool.default_prefer_fs_current_hack)));

        for (int i=0; i < RESET_SERVICE.length; i++) {
            if (key.equals(RESET_SERVICE[i])) {
                resetService();
                break;
            }
        }

        for (int i=0; i < RESET_SERVICE_WITH_CANCEL_NOTIFICATION.length; i++) {
            if (key.equals(RESET_SERVICE_WITH_CANCEL_NOTIFICATION[i])) {
                resetService(true);
                break;
            }
        }

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void updateConvertFSummary() {
        Preference pref = mPreferenceScreen.findPreference(KEY_CONVERT_F);
        if (pref == null) return;

        pref.setSummary(res.getString(R.string.currently_using) + " " +
                        (mSharedPreferences.getBoolean(KEY_CONVERT_F, res.getBoolean(R.bool.default_convert_to_fahrenheit)) ?
                         res.getString(R.string.fahrenheit) : res.getString(R.string.celsius)));
    }

    private void setEnablednessOfDeps(int index) {
        for (int i = 0; i < DEPENDENTS[index].length; i++) {
            Preference dependent = mPreferenceScreen.findPreference(DEPENDENTS[index][i]);
            if (dependent == null) return;

            if (mSharedPreferences.getBoolean(PARENTS[index], false))
                dependent.setEnabled(true);
            else
                dependent.setEnabled(false);

            updateListPrefSummary(DEPENDENTS[index][i]);
        }
    }

    private void setEnablednessOfCurrentHackDeps(boolean enabled) {
        for (int i = 0; i < CURRENT_HACK_DEPENDENTS.length; i++) {
            Preference dependent = mPreferenceScreen.findPreference(CURRENT_HACK_DEPENDENTS[i]);

            if (dependent == null) return;

            dependent.setEnabled(enabled);
        }
    }

    private void setEnablednessOfInverseDeps(int index) {
        Preference dependent = mPreferenceScreen.findPreference(INVERSE_DEPENDENTS[index]);
        if (dependent == null) return;

        if (mSharedPreferences.getBoolean(INVERSE_PARENTS[index], false))
            dependent.setEnabled(false);
        else
            dependent.setEnabled(true);

        updateListPrefSummary(INVERSE_DEPENDENTS[index]);
    }

    private void setEnablednessOfColorDeps(int index) {
        Preference dependent = mPreferenceScreen.findPreference(COLOR_DEPENDENTS[index]);
        if (dependent == null) return;

        if (mSharedPreferences.getString(COLOR_PARENTS[index], "default").equals("custom"))
            dependent.setEnabled(true);
        else
            dependent.setEnabled(false);

        updateListPrefSummary(COLOR_DEPENDENTS[index]);
    }

    private void setEnablednessOfPercentageTextColor() {
        Preference dependent = mPreferenceScreen.findPreference(KEY_NOTIFICATION_PERCENTAGE_TEXT_COLOR);
        if (dependent == null) return;

        if (mSharedPreferences.getString(KEY_ICON_AREA, res.getString(R.string.default_icon_area_content)) .equals("graphic"))
            dependent.setEnabled(false);
        else
            dependent.setEnabled(true);
    }

    // private void setEnablednessOfMutuallyExclusive(String key1, String key2) {
    //     Preference pref1 = mPreferenceScreen.findPreference(key1);
    //     Preference pref2 = mPreferenceScreen.findPreference(key2);

    //     if (pref1 == null) return;

    //     if (mSharedPreferences.getBoolean(key1, false))
    //         pref2.setEnabled(false);
    //     else if (mSharedPreferences.getBoolean(key2, false))
    //         pref1.setEnabled(false);
    //     else {
    //         pref1.setEnabled(true);
    //         pref2.setEnabled(true);
    //     }
    // }

    private void updateListPrefSummary(String key) {
        ListPreference pref;
        try { /* Code is simplest elsewhere if we call this on all dependents, but some aren't ListPreferences. */
            pref = (ListPreference) mPreferenceScreen.findPreference(key);
        } catch (java.lang.ClassCastException e) {
            return;
        }

        if (pref == null) return;

        if (pref.isEnabled()) {
            pref.setSummary(res.getString(R.string.currently_set_to) + pref.getEntry());
        } else {
            pref.setSummary(res.getString(R.string.currently_disabled));
        }
    }

    private void validateColorPrefs(String changedKey) {
        if (redThresh == null) return;
        String lowest;

        if (changedKey == null) {
            setColorPrefEntriesAndValues(redThresh, RED_SETTING_MIN, RED_SETTING_MAX);

            /* Older version had a higher max; user's setting could be too high. */
            if (iRedThresh > RED_SETTING_MAX) {
                redThresh.setValue("" + RED_SETTING_MAX);
                iRedThresh = RED_SETTING_MAX;
            }
        }

        if (changedKey == null || changedKey.equals(KEY_RED) || changedKey.equals(KEY_RED_THRESH) ||
            changedKey.equals(KEY_AMBER)) {
            if (amberEnabled) {
                lowest = setColorPrefEntriesAndValues(amberThresh, determineMin(AMBER), AMBER_SETTING_MAX);

                if (iAmberThresh < Integer.valueOf(lowest)) {
                    amberThresh.setValue(lowest);
                    iAmberThresh = Integer.valueOf(lowest);
                    updateListPrefSummary(KEY_AMBER_THRESH);
                }
            }
        }

        if (changedKey == null || !changedKey.equals(KEY_GREEN_THRESH)) {
            if (greenEnabled) {
                lowest = setColorPrefEntriesAndValues(greenThresh, determineMin(GREEN), 100);

                if (iGreenThresh < Integer.valueOf(lowest)) {
                    greenThresh.setValue(lowest);
                    iGreenThresh = Integer.valueOf(lowest);
                    updateListPrefSummary(KEY_GREEN_THRESH);
                }
            }
        }

        updateColorPreviewBar();
    }

    /* Does the obvious and returns the lowest value. */
    private String setColorPrefEntriesAndValues(ListPreference lpref, int min, int max) {
        String[] entries, values;
        int i, j;
        int[] a;

        a = indices(min, max);
        i = a[0];
        j = a[1];

        entries = values = new String[fivePercents.length - i - j];
        System.arraycopy(fivePercents, i, entries, 0, entries.length);

        lpref.setEntries(entries);
        lpref.setEntryValues(values);

        return values[0];
    }

    private void setPluginPrefEntriesAndValues(ListPreference lpref) {
        //String prefix = "BI Plugin - ";

        // PackageManager pm = getPackageManager();
        // java.util.List<PackageInfo> packages = pm.getInstalledPackages(0);

        java.util.List<String> entriesList = new java.util.ArrayList<String>();
        java.util.List<String>  valuesList = new java.util.ArrayList<String>();

        String[] icon_set_entries = res.getStringArray(R.array.icon_set_entries);
        String[] icon_set_values  = res.getStringArray(R.array.icon_set_values);

        for (int i = 0; i < icon_set_entries.length; i++) {
            entriesList.add(icon_set_entries[i]);
             valuesList.add(icon_set_values[i]);
        }

        // int nPackages = packages.size();
        // nPackages = 0; // TODO: Remove this line to re-enable plugins
        // for (int i=0; i < nPackages; i++) {
        //     PackageInfo pi = packages.get(i);
        //     if (pi.packageName.matches("com\\.darshancomputing\\.BatteryIndicatorPro\\.IconPluginV1\\..+")){
        //         String entry = (String) pm.getApplicationLabel(pi.applicationInfo);
        //         if (entry.startsWith(prefix))
        //             //entry = entry.substring(prefix.length());
        //             entry = entry.substring(3); // Strip "BI "

        //         entriesList.add(entry);
        //          valuesList.add(pi.packageName);
        //     }
        // }

        lpref.setEntries    (entriesList.toArray(new String[entriesList.size()]));
        lpref.setEntryValues(valuesList.toArray(new String[entriesList.size()]));

        /* TODO: I think it's safe to skip this: if the previously selected plugin is uninstalled, null
           should be picked up by the Service and converted to proper default, I think/hope.
        // If the previously selected plugin was uninstalled, revert to "None"
        //if (! valuesList.contains(lpref.getValue())) lpref.setValueIndex(0);
        if (lpref.getEntry() == null) lpref.setValueIndex(0);
        */
    }

    private void resetColorsToDefaults() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        editor.putBoolean(KEY_RED,   res.getBoolean(R.bool.default_use_red));
        editor.putBoolean(KEY_AMBER, res.getBoolean(R.bool.default_use_amber));
        editor.putBoolean(KEY_GREEN, res.getBoolean(R.bool.default_use_green));

        editor.putString(  KEY_RED_THRESH, res.getString(R.string.default_red_thresh  ));
        editor.putString(KEY_AMBER_THRESH, res.getString(R.string.default_amber_thresh));
        editor.putString(KEY_GREEN_THRESH, res.getString(R.string.default_green_thresh));

        editor.apply();
    }

    public void enableNotifsButtonClick(android.view.View v) {
        Intent intent;
        if (!appNotifsEnabled) {
            intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        } else {
            intent = new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, mainChan.getId());
        }

        intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
    }

    /* Determine the minimum valid threshold setting for a particular color, based on other active settings,
         with red being independent, amber depending on red, and green depending on both others. */
    private int determineMin(int color) {
        switch (color) {
        case RED:
            return RED_SETTING_MIN;
        case AMBER:
            if (redEnabled)
                /* In 10% mode, we might want +10, but xToY10() will sort it out if +5 is too small. */
                return java.lang.Math.max(iRedThresh + 5, AMBER_SETTING_MIN);
            else
                return AMBER_SETTING_MIN;
        case GREEN:
            int i;

            if (amberEnabled)
                i = iAmberThresh;
            else if (redEnabled)
                i = iRedThresh;
            else
                return GREEN_SETTING_MIN;

            return java.lang.Math.max(i, GREEN_SETTING_MIN);
        default:
                return GREEN_SETTING_MIN;
        }
    }

    private void updateColorPreviewBar() {
        if (cpbPref == null) return;

        cpbPref.redThresh   =   redEnabled ?   iRedThresh :   0;
        cpbPref.amberThresh = amberEnabled ? iAmberThresh :   0;
        cpbPref.greenThresh = greenEnabled ? iGreenThresh : 100;
    }
}
