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

import java.util.LinkedList;

public class Predictor {
    private static final int DEFAULT_DURATION = 700 * 1000;
    private static final double WEIGHT_OLD_AVERAGE = 0.999;
    private static final double WEIGHT_NEW_DATA =  1 - WEIGHT_OLD_AVERAGE;
    private static final double WEIGHT_AVERAGE = 0.5;
    private static final double WEIGHT_RECENT = 1 - WEIGHT_AVERAGE;
    private static final int RECENT_SIZE = 10;
    private static final int MAX_RECENT_REPLACED = 3;

    private static final int STATUS_UNPLUGGED     = 0;
  //private static final int STATUS_UNKNOWN       = 1;
    private static final int STATUS_CHARGING      = 2;
  //private static final int STATUS_DISCHARGING   = 3;
  //private static final int STATUS_NOT_CHARGING  = 4;
    private static final int STATUS_FULLY_CHARGED = 5;

    private double ave_discharge;
    private double ave_recharge;
    private LinkedList<Double> recent;

    private long last_ms;
    private int  last_level;
    private int  last_status;

    public Predictor() {
        ave_discharge = DEFAULT_DURATION;
        recent = new LinkedList<Double>();

        for (int i = 0; i < RECENT_SIZE; i++) {
            recent.add(ave_discharge);
        }
    }

    public void update(int level, int status) {
        if (last_ms == 0 || status == STATUS_FULLY_CHARGED || status != last_status) {
            setLasts(level, status);
            return;
        }

        if (status == STATUS_UNPLUGGED) {
            int level_diff = last_level - level;
            double ms_diff = (double) (System.currentTimeMillis() - last_ms);
            ms_diff /= level_diff;

            for (int i = 0; i < level_diff; i++) {
                double sum = 0;
                int n_replaced = 0;
                do {
                    sum += recent.removeFirst();
                    recent.addLast(ms_diff);
                } while (ms_diff > sum + recent.peekFirst() && n_replaced <= MAX_RECENT_REPLACED);

                ave_discharge = ave_discharge * WEIGHT_OLD_AVERAGE + ms_diff * WEIGHT_NEW_DATA;
            }
        }

        if (status == STATUS_CHARGING) {
            double level_diff = (double) (level - last_level);
            double ms_diff = (double) (System.currentTimeMillis() - last_ms);
            ms_diff /= level_diff;

            for (int i = 0; i < level_diff; i++) {
                recent.removeFirst();
                recent.addLast(ave_discharge);

                ave_recharge = ave_recharge * WEIGHT_OLD_AVERAGE + ms_diff * WEIGHT_NEW_DATA;
            }
        }

        setLasts(level, status);
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

        return 60 * 60 * 4;
    }

    private void setLasts(int level, int status) {
        last_level = level;
        last_status = status;
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
