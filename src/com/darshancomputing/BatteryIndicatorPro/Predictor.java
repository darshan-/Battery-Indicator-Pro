/*
    Copyright (c) 2012 Josiah Barber (aka Darshan)

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
    private static final double WEIGHT_AVERAGE = 0.6;
    private static final double WEIGHT_RECENT = 1 - WEIGHT_AVERAGE;
    private static final int RECENT_SIZE = 10;
    private static final int MAX_RECENT_REPLACED = 3;

    private static final int STATUS_UNPLUGGED     = 0;
    private static final int STATUS_CHARGING      = 2;
    private static final int STATUS_FULLY_CHARGED = 5;

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

    public Predictor(Context context) {
        sp_store = context.getSharedPreferences("predictor_sp_store", 0);
        editor = sp_store.edit();

        ave_discharge = sp_store.getFloat(KEY_AVE_DISCHARGE, DEFAULT_DISCHARGE);
        ave_recharge  = sp_store.getFloat(KEY_AVE_RECHARGE,  DEFAULT_RECHARGE);
        System.out.println("..................... Starting with: ave_d: " + ave_discharge + " ave_r: " + ave_recharge);
        recent = new LinkedList<Double>();

        for (int i = 0; i < RECENT_SIZE; i++) {
            recent.add(ave_discharge);
        }
    }

    public void update(int level, int status, int plugged) {
        if (last_ms == 0 || status == STATUS_FULLY_CHARGED || status != last_status) {
            setLasts(level, status, plugged);
            full_data_point = false;
            return;
        }

        if (status == STATUS_UNPLUGGED) {
            int level_diff = last_level - level;
            double ms_diff = (double) (System.currentTimeMillis() - last_ms);
            ms_diff /= level_diff;

            if (!full_data_point && ms_diff < ave_discharge) {
                full_data_point = true;
                setLasts(level, status, plugged);
                return;
            }

            for (int i = 0; i < level_diff; i++) {
                double sum = 0;
                int n_replaced = 0;
                do {
                    sum += recent.removeFirst();
                    recent.addLast(ms_diff);
                    n_replaced += 1;
                } while (ms_diff > sum + recent.peekFirst() && n_replaced <= MAX_RECENT_REPLACED);

                ave_discharge = ave_discharge * WEIGHT_OLD_AVERAGE + ms_diff * WEIGHT_NEW_DATA;
                editor.putFloat(KEY_AVE_DISCHARGE, (float) ave_discharge);
            }
        }

        if (status == STATUS_CHARGING) {
            double level_diff = (double) (level - last_level);
            double ms_diff = (double) (System.currentTimeMillis() - last_ms);
            ms_diff /= level_diff;

            if (last_plugged == PLUGGED_USB) ms_diff /= 2;

            if (!full_data_point && ms_diff < ave_recharge) {
                full_data_point = true;
                setLasts(level, status, plugged);
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
        full_data_point = true;
        setLasts(level, status, plugged);
    }

    public int secondsUntilDrained() {
        if (last_status != STATUS_UNPLUGGED) {
            return -1;
        }

        double ms_remaining = (recentAverage() * WEIGHT_RECENT + ave_discharge * WEIGHT_AVERAGE) * last_level;
        return (int) (ms_remaining / 1000);
    }

    public int secondsUntilCharged() {
        if (last_status == STATUS_FULLY_CHARGED) {
            return 0;
        }

        if (last_status != STATUS_CHARGING) {
            return -1;
        }

        double ms_remaining = (100 - last_level) * ave_recharge / 1000;
        if (last_plugged == PLUGGED_USB) ms_remaining *= 2;
        return (int) ms_remaining;
    }

    private void setLasts(int level, int status, int plugged) {
        last_level = level;
        last_status = status;
        last_plugged = plugged;
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
