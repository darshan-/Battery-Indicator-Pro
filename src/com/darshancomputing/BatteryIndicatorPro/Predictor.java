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
    private static final int DEFAULT_DISCHARGE = 864 * 1000;
    private static final int DEFAULT_RECHARGE = 108 * 1000;
    private static final double WEIGHT_OLD_AVERAGE = 0.998;
    private static final double WEIGHT_NEW_DATA =  1 - WEIGHT_OLD_AVERAGE;
    private static final double WEIGHT_AVERAGE = 0.1;
    private static final double WEIGHT_RECENT = 1 - WEIGHT_AVERAGE;
    private static final int RECENT_SIZE = 5;
    private static final int MAX_RECENT_REPLACED = 5;

    private static final int PLUGGED_USB = 2;

    private double ave_discharge;
    private double ave_recharge;
    private LinkedList<Double> recent;

    private static final String KEY_AVE_DISCHARGE = "key_ave_discharge";
    private static final String KEY_AVE_RECHARGE  = "key_ave_recharge";

    private long last_ms;
    private int last_level;
    private int last_status;
    private int last_plugged;
    private boolean full_data_point;

    private SharedPreferences sp_store;
    private SharedPreferences.Editor editor;
    private Logger logger;

    public Predictor(Context context) {
        sp_store = context.getSharedPreferences("predictor_sp_store", 0);
        editor = sp_store.edit();

        logger = new Logger(context, "Predictor");

        ave_discharge = sp_store.getFloat(KEY_AVE_DISCHARGE, DEFAULT_DISCHARGE);
        ave_recharge  = sp_store.getFloat(KEY_AVE_RECHARGE,  DEFAULT_RECHARGE);
        logger.log("Starting with: ave_d: " + ave_discharge + " ave_r: " + ave_recharge);
        recent = new LinkedList<Double>();

        for (int i = 0; i < RECENT_SIZE; i++) {
            recent.add(ave_discharge);
        }
    }

    public void update(BatteryInfo info) {
        logger.log("info.percent = " + info.percent);
        if (info.percent == last_level && info.status == last_status) {
            logger.log("No change; returning immediately");
            updateInfoPrediction(info);
            return;
        }
        if (last_ms == 0 || info.status == BatteryInfo.STATUS_FULLY_CHARGED || info.status != last_status) {
            logger.log("Initial update or fully charged: ");
            logger.log("last_ms = " + last_ms + ", info.status = " + info.status + ", last_status = " + last_status);
            finishUpdate(info, false);
            return;
        }

        if (info.status == BatteryInfo.STATUS_UNPLUGGED) {
            int level_diff = last_level - info.percent;
            double ms_diff = (double) (System.currentTimeMillis() - last_ms);
            ms_diff /= level_diff;

            if (!full_data_point && ms_diff < ave_discharge) {
                logger.log("Incomplete data point");
                finishUpdate(info, true);
                return;
            }

            for (int i = 0; i < level_diff; i++) {
                double sum = 0;
                int n_replaced = 0;
                do {
                    sum += recent.removeFirst();
                    recent.addLast(ms_diff);
                    n_replaced += 1;
                } while (ms_diff > sum + recent.peek() && n_replaced <= MAX_RECENT_REPLACED);

                ave_discharge = ave_discharge * WEIGHT_OLD_AVERAGE + ms_diff * WEIGHT_NEW_DATA;
                editor.putFloat(KEY_AVE_DISCHARGE, (float) ave_discharge);

                logger.log("ave_discharge = " + ave_discharge);
                logger.log("recent = " + recent);
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
                recent.removeFirst();
                recent.addLast(ave_discharge);

                ave_recharge = ave_recharge * WEIGHT_OLD_AVERAGE + ms_diff * WEIGHT_NEW_DATA;
                editor.putFloat(KEY_AVE_RECHARGE, (float) ave_recharge);
            }
        }

        editor.commit();
        finishUpdate(info, true);
    }

    private void finishUpdate(BatteryInfo info, boolean full) {
        full_data_point = full;
        setLasts(info);
        updateInfoPrediction(info);
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
        // TODO: It's probably not appropriate to assume USB charging is exactly half as fast as AC charging.
        //  Should probably keep separte AC / USB / Wireless charging averages, which are the default when plugged in
        //  The should also probably be usage-based, as the discharge ones are.  Change required here and in update(), at least.
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
        double sum = 0;

        for (int i = 0; i < recent.size(); i++) {
            sum += recent.get(i);
        }

        return sum / recent.size();
    }
}
