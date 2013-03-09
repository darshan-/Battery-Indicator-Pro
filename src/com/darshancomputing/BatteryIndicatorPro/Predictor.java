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

import java.util.LinkedList;

public class Predictor {
    private static final int DISCHARGE    = 0;
    private static final int RECHARGE_AC  = 1;
    private static final int RECHARGE_WL  = 2;
    private static final int RECHARGE_USB = 3;

    private double[] average = new double[4];

    private static final int[] DEFAULT = { 24 * 60 * 60 * 1000 / 100,
                                           3 * 60 * 60 * 1000 / 100,
                                           4 * 60 * 60 * 1000 / 100,
                                           6 * 60 * 60 * 1000 / 100 }

    private static final String[] KEY_AVERAGE = { "key_ave_discharge",
                                                  "key_ave_recharge_ac",
                                                  "key_ave_recharge_wl",
                                                  "key_ave_recharge_usb" }

    private static final int RECENT_DURATION = 5 * 60 * 1000;

    private static final double WEIGHT_OLD_AVERAGE = 0.998;
    private static final double WEIGHT_NEW_DATA =  1 - WEIGHT_OLD_AVERAGE;

    private LinkedList<Double> recents;

    private long last_ms;
    private int last_level;
    private int last_status;
    private int last_plugged;
    private int last_index;
    private boolean full_data_point;

    private SharedPreferences sp_store;
    private SharedPreferences.Editor editor;

    public Predictor(Context context) {
        sp_store = context.getSharedPreferences("predictor_sp_store", 0);
        editor = sp_store.edit();

        average[DISCHARGE]    = sp_store.getFloat(KEY_AVE[DISCHARGE],    DEFAULT[DISCHARGE]);
        average[RECHARGE_AC]  = sp_store.getFloat(KEY_AVE[RECHARGE_AC],  DEFAULT[RECHARGE_AC]);
        average[RECHARGE_WL]  = sp_store.getFloat(KEY_AVE[RECHARGE_WL],  DEFAULT[RECHARGE_WL]);
        average[RECHARGE_USB] = sp_store.getFloat(KEY_AVE[RECHARGE_USB], DEFAULT[RECHARGE_USB]);

        recents = new LinkedList<Double>();
    }

    public void update(BatteryInfo info) {
        if (info.status != last_status || info.plugged != last_plugged || last_ms == 0 || info.status == BatteryInfo.STATUS_FULLY_CHARGED) {
            recents.clear();
            setLasts(info);
            updateInfoPrediction(info);
            return;
        }

        if (info.percent == last_level) {
            // Handle partial data point: return early if too small, otherwise set flag, fall through, and treat mostly as normal
        }

        int status = indexFor(info.status, info.plugged);
        int level_diff = java.lang.Math.abs(last_level - info.percent);
        ms_diff /= level_diff;

        if (!full_data_point && ms_diff < average[status]) {
            finishUpdate(info, true);
            return;
        }

        for (int i = 0; i < level_diff; i++) {
            recents.addLast(ms_diff)

            average[status] = average[status] * WEIGHT_OLD_AVERAGE + ms_diff * WEIGHT_NEW_DATA;
            editor.putFloat(KEY_AVERAGE[status], (float) average[status]);
        }

        editor.commit();
        finishUpdate(info, true);
    }

    private void updateInfoPrediction(BatteryInfo info) {
        int secs_left;

        if (info.status == BatteryInfo.STATUS_CHARGING)
            secs_left = secondsUntilCharged();
        else
            secs_left = secondsUntilDrained();

        info.prediction.update(secs_left);
    }

    public int secondsUntilDrained() {
        if (last_status != BatteryInfo.STATUS_UNPLUGGED) {
            return -1;
        }

        double predicted = (recentAverage() * WEIGHT_RECENT + average[DISCHARGE] * WEIGHT_AVERAGE);

        if (predicted > average[DISCHARGE])
            predicted = average[DISCHARGE];

        return (int) (predicted * last_level / 1000);
    }

    public int secondsUntilCharged() {
        if (last_status == BatteryInfo.STATUS_FULLY_CHARGED) {
            return 0;
        }

        if (last_status != BatteryInfo.STATUS_CHARGING) {
            return -1;
        }

        double ms_remaining = (100 - last_level) * ave_recharge / 1000;

        if (last_plugged == PLUGGED_USB) ms_remaining *= 2;
        return (int) ms_remaining;
    }

    private void setLasts(BatteryInfo info) {
        last_level = info.percent;
        last_status = info.status;
        last_plugged = info.plugged;
        last_index = indexFor(last_status, last_plugged);
        last_ms = System.currentTimeMillis();
    }

    private double recentAverage() {
        double total_points, total_ms;
        double needed_ms = RECENT_DURATION;

        int i;
        for (i = 0; i < recents.size(); i++) {
            double t = recents.get(i);

            if (t > needed_ms) {
                total_points += needed_ms / t;
                total_ms += needed_ms;
                needed_ms = 0;
                break;
            }

            total_points += 1;
            total_ms += t;
            needed_ms -= t;
        }

        while (recents.size() > i) // This is a convenient place to trim recents
            recents.remove(i);

        if (needed_ms > 0)
            total_points += average[last_index];

        return RECENT_DURATION / total_points;
    }

    private int indexFor(ing status, int plugged) {
        if (status == BatteryInfo.STATUS_CHARGING) {
            if (plugged == BatteryInfo.PLUGGED_USB)
                return RECHARGE_USB;
            else if (plugged == BatteryInfo.PLUGGED_WIRELESS)
                return RECHARGE_WIRELESS
            else
                return RECHARGE_AC;
        } else {
            return DISCHARGING;
        }
    }
}
