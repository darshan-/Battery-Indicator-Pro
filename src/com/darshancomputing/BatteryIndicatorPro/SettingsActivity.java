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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
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
import android.view.WindowManager;
import java.util.Locale;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    public static final String SETTINGS_FILE = "com.darshancomputing.BatteryIndicatorPro_preferences";
    public static final String SP_STORE_FILE = "sp_store";

    public static final String KEY_THEME_SETTINGS = "theme_settings";
    public static final String KEY_ALARM_SETTINGS = "alarm_settings";
    public static final String KEY_ALARM_EDIT_SETTINGS = "alarm_edit_settings";
    public static final String KEY_OTHER_SETTINGS = "other_settings";
    public static final String KEY_CONFIRM_DISABLE_LOCKING = "confirm_disable_lock_screen";
    public static final String KEY_FINISH_AFTER_TOGGLE_LOCK = "finish_after_toggle_lock";
    public static final String KEY_FINISH_AFTER_BATTERY_USE = "finish_after_battery_use";
    public static final String KEY_NOTIFY_WHEN_KG_DISABLED = "notify_when_kg_disabled";
    public static final String KEY_AUTO_DISABLE_LOCKING = "auto_disable_lock_screen";
    public static final String KEY_DISALLOW_DISABLE_LOCK_SCREEN = "disallow_disable_lock_screen";
    public static final String KEY_MAIN_NOTIFICATION_PRIORITY = "main_notification_priority";
    public static final String KEY_ENABLE_LOGGING = "enable_logging";
    public static final String KEY_MAX_LOG_AGE = "max_log_age";
    public static final String KEY_ICON_PLUGIN = "icon_plugin";
    public static final String KEY_ICON_SET = "icon_set";
    public static final String KEY_CONVERT_F = "convert_to_fahrenheit";
    public static final String KEY_AUTOSTART = "autostart";
    public static final String KEY_TEN_PERCENT_MODE = "ten_percent_mode";
    public static final String KEY_STATUS_DUR_EST = "status_dur_est";
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
    public static final String KEY_FIRST_RUN = "first_run";
    //public static final String KEY_LANGUAGE_OVERRIDE = "language_override";

    private static final String[] PARENTS    = {KEY_ENABLE_LOGGING, KEY_RED,        KEY_AMBER,        KEY_GREEN};
    private static final String[] DEPENDENTS = {KEY_MAX_LOG_AGE,    KEY_RED_THRESH, KEY_AMBER_THRESH, KEY_GREEN_THRESH};

    private static final String[] LIST_PREFS = {KEY_AUTOSTART, KEY_STATUS_DUR_EST,
                                                KEY_RED_THRESH, KEY_AMBER_THRESH, KEY_GREEN_THRESH,
                                                KEY_MAIN_NOTIFICATION_PRIORITY, KEY_ICON_SET,
                                                KEY_MAX_LOG_AGE/*, KEY_LANGUAGE_OVERRIDE*/};

    private static final String[] RESET_SERVICE = {KEY_CONVERT_F,
                                                   KEY_AUTO_DISABLE_LOCKING, KEY_RED, KEY_RED_THRESH,
                                                   KEY_AMBER, KEY_AMBER_THRESH, KEY_GREEN, KEY_GREEN_THRESH,
                                                   KEY_NOTIFY_WHEN_KG_DISABLED, KEY_ICON_SET,
                                                   KEY_INDICATE_CHARGING, KEY_TEN_PERCENT_MODE}; /* 10% mode changes color settings */

    private static final String[] RESET_SERVICE_WITH_CANCEL_NOTIFICATION = {KEY_MAIN_NOTIFICATION_PRIORITY};

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

    private static final int DIALOG_CONFIRM_TEN_PERCENT_ENABLE  = 0;
    private static final int DIALOG_CONFIRM_TEN_PERCENT_DISABLE = 1;

    private Intent biServiceIntent;
    private Messenger serviceMessenger;
    private final Messenger messenger = new Messenger(new MessageHandler());
    private final BatteryInfoService.RemoteConnection serviceConnection = new BatteryInfoService.RemoteConnection(messenger);

    private Resources res;
    private PreferenceScreen mPreferenceScreen;
    private SharedPreferences mSharedPreferences;

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

    private Boolean ten_percent_mode;

    private String currentPlugin;

    private int menu_res = R.menu.settings;

    private static final String[] fivePercents = {
        "5", "10", "15", "20", "25", "30", "35", "40", "45", "50",
        "55", "60", "65", "70", "75", "80", "85", "90", "95", "100"};

    /* Also includes 5 and 15, as the orginal Droid (and presumably similarly crippled devices)
       goes by 5% once you get below 20%. */
    private static final String[] tenPercentEntries = {
	"5", "10", "15", "20", "30", "40", "50",
	"60", "70", "80", "90", "100"};

    /* Setting Red and Amber values like this allows the Service to follow the same algorithm no matter what. */
    private static final String[] tenPercentValues = {
	"6", "11", "16", "21", "31", "41", "51",
	"61", "71", "81", "91", "101"};

    /* Returns a two-item array of the start and end indices into the above arrays. */
    private int[] indices(int x, int y) {
        int[] a = new int[2];
        int i; /* How many values to remove from the front */
        int j; /* How many values to remove from the end   */

        if (ten_percent_mode) {
            for (i = 0; i < tenPercentEntries.length - 1; i++)
                if (Integer.valueOf(tenPercentEntries[i]) >= Integer.valueOf(x)) break;
            j = (100 - y) / 10;
        } else {
            i = (x / 5) - 1;
            j = (100 - y) / 5;
        }

        a[0] = i;
        a[1] = j;
        return a;
    }

    private static final Object[] EMPTY_OBJECT_ARRAY = {};
    private static final  Class[]  EMPTY_CLASS_ARRAY = {};

    //private String oldLanguage = null;

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message incoming) {
            switch (incoming.what) {
            case BatteryInfoService.RemoteConnection.CLIENT_SERVICE_CONNECTED:
                serviceMessenger = incoming.replyTo;
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

            Boolean hasSettings = false;
            try {
        /* TODO: Convert to message
                hasSettings = biServiceConnection.biService.pluginHasSettings();
        */
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (! hasSettings) {
                PreferenceCategory cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_PLUGIN_SETTINGS);
                cat.removeAll();
                cat.setLayoutResource(R.layout.hidden);
            } else {
                Preference p = (Preference) mPreferenceScreen.findPreference(KEY_PLUGIN_SETTINGS);
                p.setEnabled(true);
            }

            mHandler.removeCallbacks(rShowPluginSettings);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        pref_screen = intent.getStringExtra(EXTRA_SCREEN);
        res = getResources();

        // Stranglely disabled by default for API level 14+
        if (android.os.Build.VERSION.SDK_INT >= 14)
            getActionBar().setHomeButtonEnabled(true);

        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesName(SETTINGS_FILE);
        pm.setSharedPreferencesMode(Context.MODE_MULTI_PROCESS);
        mSharedPreferences = pm.getSharedPreferences();

        //oldLanguage = mSharedPreferences.getString(KEY_LANGUAGE_OVERRIDE, "default");

        if (pref_screen == null) {
            setPrefScreen(R.xml.main_pref_screen);
            setWindowSubtitle(res.getString(R.string.settings_activity_subtitle));
        } else if (pref_screen.equals(KEY_THEME_SETTINGS)) {
            setPrefScreen(R.xml.theme_pref_screen);
            setWindowSubtitle(res.getString(R.string.theme_settings));

            menu_res = R.menu.theme_settings;

            ten_percent_mode = mSharedPreferences.getBoolean(KEY_TEN_PERCENT_MODE, false);

            cpbPref     = (ColorPreviewPreference) mPreferenceScreen.findPreference(KEY_COLOR_PREVIEW);
            if (ten_percent_mode) cpbPref.setLayoutResource(R.layout.hidden);

            ListPreference iconSetPref = (ListPreference) mPreferenceScreen.findPreference(KEY_ICON_SET);
            setPluginPrefEntriesAndValues(iconSetPref);
            currentPlugin = iconSetPref.getValue();

            redThresh   = (ListPreference) mPreferenceScreen.findPreference(KEY_RED_THRESH);
            amberThresh = (ListPreference) mPreferenceScreen.findPreference(KEY_AMBER_THRESH);
            greenThresh = (ListPreference) mPreferenceScreen.findPreference(KEY_GREEN_THRESH);

            if (currentPlugin.startsWith("builtin.")) {
                PreferenceCategory cat;

                cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_PLUGIN_SETTINGS);
                cat.removeAll();

                if (currentPlugin.equals("builtin.classic")) {
                    cat.setLayoutResource(R.layout.none);

                    redEnabled   = mSharedPreferences.getBoolean(  KEY_RED, false);
                    amberEnabled = mSharedPreferences.getBoolean(KEY_AMBER, false);
                    greenEnabled = mSharedPreferences.getBoolean(KEY_GREEN, false);

                    iRedThresh   = Integer.valueOf(  redThresh.getValue()); /* Entries don't exist yet */
                    iAmberThresh = Integer.valueOf(amberThresh.getValue());
                    iGreenThresh = Integer.valueOf(greenThresh.getValue());

                    mPreferenceScreen.findPreference(KEY_TEN_PERCENT_MODE)
                        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                            {
                                public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                                    showDialog((Boolean) newValue ? DIALOG_CONFIRM_TEN_PERCENT_ENABLE : DIALOG_CONFIRM_TEN_PERCENT_DISABLE);
                                    return false;
                                }
                            });

                    if (ten_percent_mode) {
                        /* These should always correspond to the logical (entry) value, not the actual stored value. */
                        iRedThresh--;
                        iAmberThresh--;
                    }

                    validateColorPrefs(null);

                    cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_CHARGING_INDICATOR);
                    cat.removeAll();
                    cat.setLayoutResource(R.layout.none);
                } else {
                    cat.setLayoutResource(R.layout.hidden);

                    cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_COLOR);
                    cat.removeAll();
                    cat.setLayoutResource(R.layout.none);
                }
            } else {
                PreferenceCategory cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_COLOR);
                cat.removeAll();
                cat.setLayoutResource(R.layout.none);
                cat = (PreferenceCategory) mPreferenceScreen.findPreference(KEY_CAT_CHARGING_INDICATOR);
                cat.removeAll();
                cat.setLayoutResource(R.layout.none);

                mHandler.postDelayed(rShowPluginSettings,  100);
                mHandler.postDelayed(rShowPluginSettings,  300);
                mHandler.postDelayed(rShowPluginSettings,  600);
                mHandler.postDelayed(rShowPluginSettings, 1000);
            }
        } else if (pref_screen.equals(KEY_OTHER_SETTINGS)) {
            setPrefScreen(R.xml.other_pref_screen);
            setWindowSubtitle(res.getString(R.string.other_settings));

            /*ListPreference lp = (ListPreference) mPreferenceScreen.findPreference(KEY_LANGUAGE_OVERRIDE);
            CharSequence[] values  = lp.getEntryValues();
            CharSequence[] entries = new CharSequence[values.length];

            for (int i=0; i < values.length; ++i) {
                if (values[i].toString().equals("default"))
                    entries[i] = res.getString(R.string.lang_system_selected);
                else {
                    Locale locale = codeToLocale(values[i].toString());
                    entries[i] = locale.getDisplayName(locale);
                }
            }

            lp.setEntries(entries);*/
        } else {
            setPrefScreen(R.xml.main_pref_screen);
        }

        for (int i=0; i < PARENTS.length; i++)
            setEnablednessOfDeps(i);

        for (int i=0; i < LIST_PREFS.length; i++)
            updateListPrefSummary(LIST_PREFS[i]);

        updateConvertFSummary();
        setEnablednessOfMutuallyExclusive(KEY_CONFIRM_DISABLE_LOCKING, KEY_FINISH_AFTER_TOGGLE_LOCK);

        biServiceIntent = new Intent(this, BatteryInfoService.class);
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

    /*private void restartIfLanguageChanged() {
        String curLanguage = mSharedPreferences.getString(KEY_LANGUAGE_OVERRIDE, "default");
        if (curLanguage.equals(oldLanguage))
            return;

        Str.overrideLanguage(res, getWindowManager(), curLanguage);
        restartThisScreen();
    }*/

    private void resetService() {
        resetService(false);
    }

    private void resetService(boolean cancelFirst) {
        mSharedPreferences.edit().commit(); // Force file to be saved

        Message outgoing = Message.obtain();

        if (cancelFirst)
            outgoing.what = BatteryInfoService.RemoteConnection.SERVICE_CANCEL_NOTIFICATION_AND_RELOAD_SETTINGS;
        else
            outgoing.what = BatteryInfoService.RemoteConnection.SERVICE_RELOAD_SETTINGS;

        try {
            serviceMessenger.send(outgoing);
        } catch (android.os.RemoteException e) {
            startService(new Intent(this, BatteryInfoService.class));
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

        //restartIfLanguageChanged();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
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
            startActivity(new Intent(this, BatteryInfoActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Str str = new Str(getResources());

        switch (id) {
        /* Android saves and reuses these dialogs; we want different titles for each, hence two IDs */
        case DIALOG_CONFIRM_TEN_PERCENT_ENABLE:
        case DIALOG_CONFIRM_TEN_PERCENT_DISABLE:
            builder.setTitle(ten_percent_mode ? str.confirm_ten_percent_disable : str.confirm_ten_percent_enable)
                .setMessage(str.confirm_ten_percent_hint)
                .setCancelable(false)
                .setPositiveButton(str.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        ten_percent_mode = ! ten_percent_mode;
                        ((CheckBoxPreference) mPreferenceScreen.findPreference(KEY_TEN_PERCENT_MODE)).setChecked(ten_percent_mode);
                        di.cancel();

                        restartThisScreen();
                    }
                })
                .setNegativeButton(str.cancel, new DialogInterface.OnClickListener() {
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

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (key == null) {
            return false;
        } else if (key.equals(KEY_THEME_SETTINGS) || key.equals(KEY_OTHER_SETTINGS)) {
            ComponentName comp = new ComponentName(getPackageName(), SettingsActivity.class.getName());
            startActivity(new Intent().setComponent(comp).putExtra(EXTRA_SCREEN, key));

            return true;
        } else if (key.equals(KEY_ALARM_SETTINGS)) {
            ComponentName comp = new ComponentName(getPackageName(), AlarmsActivity.class.getName());
            startActivity(new Intent().setComponent(comp));

            return true;
        } else if (key.equals(KEY_PLUGIN_SETTINGS)) {
            //TODO: convert biServiceConnection.biService.configurePlugin();
            return true;
        } else {
            return false;
        }
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

        if (key.equals(KEY_TEN_PERCENT_MODE))
            resetColorsToDefaults();

        if (key.equals(KEY_ICON_SET)) {
            resetService();
            restartThisScreen(); // To show/hide icon-set/plugin settings
        }

        for (int i=0; i < PARENTS.length; i++) {
            if (key.equals(PARENTS[i])) {
                setEnablednessOfDeps(i);
                if (i == 0) setEnablednessOfDeps(1); /* Doubled charge key */
                if (i == 2) setEnablednessOfDeps(3); /* Doubled charge key */
                break;
            }
        }

        for (int i=0; i < LIST_PREFS.length; i++) {
            if (key.equals(LIST_PREFS[i])) {
                updateListPrefSummary(LIST_PREFS[i]);
                break;
            }
        }

        if (key.equals(KEY_CONFIRM_DISABLE_LOCKING) || key.equals(KEY_FINISH_AFTER_TOGGLE_LOCK))
            setEnablednessOfMutuallyExclusive(KEY_CONFIRM_DISABLE_LOCKING, KEY_FINISH_AFTER_TOGGLE_LOCK);

        if (key.equals(KEY_CONVERT_F)) {
            updateConvertFSummary();
        }

        /*if (key.equals(KEY_LANGUAGE_OVERRIDE)) {
            Str.overrideLanguage(res, getWindowManager(), mSharedPreferences.getString(SettingsActivity.KEY_LANGUAGE_OVERRIDE, "default"));
            restartThisScreen();
        }*/

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
        Preference pref = (CheckBoxPreference) mPreferenceScreen.findPreference(KEY_CONVERT_F);
        if (pref == null) return;

        pref.setSummary(res.getString(R.string.currently_using) + " " +
                        (mSharedPreferences.getBoolean(KEY_CONVERT_F, false) ?
                         res.getString(R.string.fahrenheit) : res.getString(R.string.celsius)));
    }

    private void setEnablednessOfDeps(int index) {
        Preference dependent = mPreferenceScreen.findPreference(DEPENDENTS[index]);
        if (dependent == null) return;

        if (mSharedPreferences.getBoolean(PARENTS[index], false))
            dependent.setEnabled(true);
        else
            dependent.setEnabled(false);

        updateListPrefSummary(DEPENDENTS[index]);
    }

    private void setEnablednessOfMutuallyExclusive(String key1, String key2) {
        Preference pref1 = mPreferenceScreen.findPreference(key1);
        Preference pref2 = mPreferenceScreen.findPreference(key2);

        if (pref1 == null) return;

        if (mSharedPreferences.getBoolean(key1, false))
            pref2.setEnabled(false);
        else if (mSharedPreferences.getBoolean(key2, false))
            pref1.setEnabled(false);
        else {
            pref1.setEnabled(true);
            pref2.setEnabled(true);
        }
    }

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
                if (ten_percent_mode) iRedThresh--;
            }
        }

        if (changedKey == null || changedKey.equals(KEY_RED) || changedKey.equals(KEY_RED_THRESH) ||
            changedKey.equals(KEY_AMBER)) {
            if (amberEnabled) {
                lowest = setColorPrefEntriesAndValues(amberThresh, determineMin(AMBER), AMBER_SETTING_MAX);

                if (iAmberThresh < Integer.valueOf(lowest)) {
                    amberThresh.setValue(lowest);
                    iAmberThresh = Integer.valueOf(lowest);
                    if (ten_percent_mode) iAmberThresh--;
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

        if (ten_percent_mode) {
            entries = new String[tenPercentEntries.length - i - j];
            values  = new String[tenPercentEntries.length - i - j];
            System.arraycopy(tenPercentEntries, i, entries, 0, entries.length);
            System.arraycopy(tenPercentValues , i,  values, 0,  values.length);
            if (lpref.equals(greenThresh)) values = entries;
        } else {
            entries = values = new String[fivePercents.length - i - j];
            System.arraycopy(fivePercents, i, entries, 0, entries.length);
        }

        lpref.setEntries(entries);
        lpref.setEntryValues(values);

        return values[0];
    }

    private void setPluginPrefEntriesAndValues(ListPreference lpref) {
        String prefix = "BI Plugin - ";

        PackageManager pm = getPackageManager();
        java.util.List<PackageInfo> packages = pm.getInstalledPackages(0);

        java.util.List<String> entriesList = new java.util.ArrayList<String>();
        java.util.List<String>  valuesList = new java.util.ArrayList<String>();

        String[] icon_set_entries = res.getStringArray(R.array.icon_set_entries);
        String[] icon_set_values  = res.getStringArray(R.array.icon_set_values);

        for (int i = 0; i < icon_set_entries.length; i++) {
            entriesList.add(icon_set_entries[i]);
             valuesList.add(icon_set_values[i]);
        }

        int nPackages = packages.size();
        nPackages = 0; // TODO: Remove this line to re-enable plugins
        for (int i=0; i < nPackages; i++) {
            PackageInfo pi = packages.get(i);
            if (pi.packageName.matches("com\\.darshancomputing\\.BatteryIndicatorPro\\.IconPluginV1\\..+")){
                String entry = (String) pm.getApplicationLabel(pi.applicationInfo);
                if (entry.startsWith(prefix))
                    //entry = entry.substring(prefix.length());
                    entry = entry.substring(3); // Strip "BI "

                entriesList.add(entry);
                 valuesList.add(pi.packageName);
            }
        }

        lpref.setEntries    ((String[]) entriesList.toArray(new String[entriesList.size()]));
        lpref.setEntryValues((String[])  valuesList.toArray(new String[entriesList.size()]));

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

        if (mSharedPreferences.getBoolean(KEY_TEN_PERCENT_MODE, false)){
            editor.putString(  KEY_RED_THRESH, res.getString(R.string.default_red_thresh10  ));
            editor.putString(KEY_AMBER_THRESH, res.getString(R.string.default_amber_thresh10));
            editor.putString(KEY_GREEN_THRESH, res.getString(R.string.default_green_thresh10));
        } else {
            editor.putString(  KEY_RED_THRESH, res.getString(R.string.default_red_thresh  ));
            editor.putString(KEY_AMBER_THRESH, res.getString(R.string.default_amber_thresh));
            editor.putString(KEY_GREEN_THRESH, res.getString(R.string.default_green_thresh));
        }

        editor.commit();
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

            if (ten_percent_mode)
                /* We'll usually want +10, but it could be just +5. xToY10() will sort it out if +5 is too small. */
                return java.lang.Math.max(i + 5, GREEN_SETTING_MIN);
            else
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
