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
    private static final int DEFAULT_DISCHARGE    = 24 * 60 * 60 * 1000 / 100;
    private static final int DEFAULT_RECHARGE_AC  =  3 * 60 * 60 * 1000 / 100;
    private static final int DEFAULT_RECHARGE_WL  =  4 * 60 * 60 * 1000 / 100;
    private static final int DEFAULT_RECHARGE_USB =  6 * 60 * 60 * 1000 / 100;

    private static final int RECENT_DURATION = 5 * 60 * 1000;

    private double ave_discharge;
    private double ave_recharge_ac;
    private double ave_recharge_wl;
    private double ave_recharge_usb;

    private LinkedList<Double> recents;

    private static final String KEY_AVE_DISCHARGE    = "key_ave_discharge";
    private static final String KEY_AVE_RECHARGE_AC  = "key_ave_recharge_ac";
    private static final String KEY_AVE_RECHARGE_WL  = "key_ave_recharge_wl";
    private static final String KEY_AVE_RECHARGE_USB = "key_ave_recharge_usb";

    private long last_ms;
    private int last_level;
    private int last_status;
    private int last_plugged;
    private boolean full_data_point;

    private SharedPreferences sp_store;
    private SharedPreferences.Editor editor;

    public Predictor(Context context) {
        sp_store = context.getSharedPreferences("predictor_sp_store", 0);
        editor = sp_store.edit();

        ave_discharge    = sp_store.getFloat(KEY_AVE_DISCHARGE, DEFAULT_DISCHARGE);
        ave_recharge_ac  = sp_store.getFloat(KEY_AVE_RECHARGE_AC, DEFAULT_RECHARGE_AC);
        ave_recharge_wl  = sp_store.getFloat(KEY_AVE_RECHARGE_WL, DEFAULT_RECHARGE_WL);
        ave_recharge_usb = sp_store.getFloat(KEY_AVE_RECHARGE_USB, DEFAULT_RECHARGE_USB);

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

        if (info.status == BatteryInfo.STATUS_UNPLUGGED) {
            int level_diff = last_level - info.percent;
            double ms_diff = (double) (System.currentTimeMillis() - last_ms);
            ms_diff /= level_diff;

            if (!full_data_point && ms_diff < ave_discharge) {
                finishUpdate(info, true);
                return;
            }

            for (int i = 0; i < level_diff; i++) {
                double sum = 0;
                int n_replaced = 0;
                do {
                    sum += recents.removeFirst();
                    recents.addLast(ms_diff);
                    n_replaced += 1;
                } while (ms_diff > sum + recents.peek() && n_replaced <= MAX_RECENT_REPLACED);

                ave_discharge = ave_discharge * WEIGHT_OLD_AVERAGE + ms_diff * WEIGHT_NEW_DATA;
                editor.putFloat(KEY_AVE_DISCHARGE, (float) ave_discharge);
            }
        }

        if (info.status == BatteryInfo.STATUS_CHARGING) {
            double level_diff = (double) (info.percent - last_level);
            double ms_diff = (double) (System.currentTimeMillis() - last_ms);
            ms_diff /= level_diff;

            if (last_plugged == PLUGGED_USB) ms_diff /= 2;

            if (!full_data_point && ms_diff < ave_recharge) {
                finishUpdate(info, true);
                return;
            }

            for (int i = 0; i < level_diff; i++) {
                recents.removeFirst();
                recents.addLast(ave_discharge);

                ave_recharge = ave_recharge * WEIGHT_OLD_AVERAGE + ms_diff * WEIGHT_NEW_DATA;
                editor.putFloat(KEY_AVE_RECHARGE, (float) ave_recharge);
            }
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

        double predicted = (recentAverage() * WEIGHT_RECENT + ave_discharge * WEIGHT_AVERAGE);

        if (predicted > ave_discharge)
            predicted = ave_discharge;

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

        if (needed_ms > 0) {
            if (last_status == BatteryInfo.STATUS_CHARGING) {
                if (last_plugged == BatteryInfo.PLUGGED_USB)
                    total_points == needed_ms / ave_recharge_usb;
                else if (last_plugged == BatteryInfo.PLUGGED_WIRELESS)
                    total_points == needed_ms / ave_recharge_wireless;
                else
                    total_points == needed_ms / ave_recharge_ac;
            } else {
                total_points += needed_ms / ave_discharge;
            }
        }

        return RECENT_DURATION / total_points;
    }
}
