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

        if (startPref.equals("always") ||
            (startPref.equals("auto") && sp_store.getBoolean(BatteryIndicatorService.KEY_SERVICE_DESIRED, false))){
            ComponentName comp = new ComponentName(context.getPackageName(),
                                                   BatteryIndicatorService.class.getName());
            context.startService(new Intent().setComponent(comp));
        }
    }
}
