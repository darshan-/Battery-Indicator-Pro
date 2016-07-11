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
        SharedPreferences sp_store = context.getSharedPreferences("sp_store", 0);

        String startPref = settings.getString(SettingsActivity.KEY_AUTOSTART, "auto");

        // Note: Regardless of anything here, Android will start the Service on boot if there are any desktop widgets
        if (startPref.equals("always") ||
            (startPref.equals("auto") && sp_store.getBoolean(BatteryInfoService.KEY_SERVICE_DESIRED, false))){
            ComponentName comp = new ComponentName(context.getPackageName(),
                                                   BatteryInfoService.class.getName());
            context.startService(new Intent().setComponent(comp));
        }

        // This receiver is called on PACKAGE_REPLACED, too, but we don't want to log boot in that case
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && settings.getBoolean(SettingsActivity.KEY_ENABLE_LOGGING, true))
            new LogDatabase(context).logBoot();
    }
}
