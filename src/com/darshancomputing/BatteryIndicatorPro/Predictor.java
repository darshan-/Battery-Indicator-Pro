/*
    Copyright (c) 2012-2013 Darshan-Josiah Barber

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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

public class Predictor {
    /* Indexed by PredictorCore.DISCHARGE et al */
    private static final String[] KEY_AVERAGE = { "key_ave_discharge",
                                                  "key_ave_recharge_ac",
                                                  "key_ave_recharge_wl",
                                                  "key_ave_recharge_usb" };

    private SharedPreferences sp_store;
    private SharedPreferences.Editor editor;

    private PredictorCore pc;

    public Predictor(Context context) {
        sp_store = context.getSharedPreferences("predictor_sp_store", 0);
        editor = sp_store.edit();

        pc = new PredictorCore(sp_store.getFloat(KEY_AVERAGE[PredictorCore.DISCHARGE],    -1),
                               sp_store.getFloat(KEY_AVERAGE[PredictorCore.RECHARGE_AC],  -1),
                               sp_store.getFloat(KEY_AVERAGE[PredictorCore.RECHARGE_WL],  -1),
                               sp_store.getFloat(KEY_AVERAGE[PredictorCore.RECHARGE_USB], -1));
    }

    public void update(BatteryInfo info) {
        pc.update(info, SystemClock.elapsedRealtime());
        editor.putFloat(KEY_AVERAGE[pc.last_charging_status],
                        (float) pc.getLongTermAverage());
        editor.commit();
    }
}
