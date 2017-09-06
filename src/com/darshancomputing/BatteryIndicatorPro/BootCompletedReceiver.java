/*
    Copyright (c) 2009-2017 Darshan-Josiah Barber

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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences sp_main = context.getSharedPreferences(SettingsActivity.SP_MAIN_FILE, 0);
        SharedPreferences sp_service = context.getSharedPreferences(SettingsActivity.SP_SERVICE_FILE, 0);

        String startPref = settings.getString(SettingsActivity.KEY_AUTOSTART, "auto");

        boolean service_desired;

        // Actual "migration" has to be done from main process, so we will read the old value here until such a time as it is migrated.
        if (! sp_main.getBoolean(SettingsActivity.KEY_MIGRATED_SERVICE_DESIRED, false))
            service_desired = sp_service.getBoolean(BatteryInfoService.KEY_SERVICE_DESIRED, false);
        else
            service_desired = sp_main.getBoolean(BatteryInfoService.KEY_SERVICE_DESIRED, false);

        // Note: Regardless of anything here, Android will start the Service on boot if there are any desktop widgets
        if (startPref.equals("always") || (startPref.equals("auto") && service_desired)){
            ComponentName comp = new ComponentName(context.getPackageName(), BatteryInfoService.class.getName());
            if (android.os.Build.VERSION.SDK_INT < 26)
                context.startService(new Intent().setComponent(comp));
            else
                context.startForegroundService(new Intent().setComponent(comp));
        }

        // This receiver is called on PACKAGE_REPLACED, too, but we don't want to log boot in that case
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && settings.getBoolean(SettingsActivity.KEY_ENABLE_LOGGING, true))
            new LogDatabase(context).logBoot();
    }
}
