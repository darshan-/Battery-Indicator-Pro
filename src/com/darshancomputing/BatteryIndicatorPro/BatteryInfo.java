/*
    Copyright (c) 2013-2017 Darshan Computing, LLC

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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
//import android.util.Log;

class BatteryInfo {
    static final int STATUS_UNPLUGGED     = 0;
    static final int STATUS_UNKNOWN       = 1;
    static final int STATUS_CHARGING      = 2;
    static final int STATUS_DISCHARGING   = 3;
    static final int STATUS_NOT_CHARGING  = 4;
    static final int STATUS_FULLY_CHARGED = 5;
    static final int STATUS_MAX = STATUS_FULLY_CHARGED;

    static final int PLUGGED_UNPLUGGED = 0;
    //static final int PLUGGED_AC        = 1;
    static final int PLUGGED_USB       = 2;
    static final int PLUGGED_UNKNOWN   = 3;
    static final int PLUGGED_WIRELESS  = 4;
    static final int PLUGGED_MAX       = PLUGGED_WIRELESS;

    static final int HEALTH_UNKNOWN     = 1;
    static final int HEALTH_GOOD        = 2;
    //static final int HEALTH_OVERHEAT    = 3;
    //static final int HEALTH_DEAD        = 4;
    //static final int HEALTH_OVERVOLTAGE = 5;
    //static final int HEALTH_FAILURE     = 6;
    static final int HEALTH_COLD        = 7;
    static final int HEALTH_MAX         = HEALTH_COLD;

    static final String KEY_LAST_STATUS_CTM = "last_status_cTM";
    static final String KEY_LAST_STATUS = "last_status";
    static final String KEY_LAST_PERCENT = "last_percent";
    static final String KEY_LAST_PLUGGED = "last_plugged";

    private static final String EXTRA_LEVEL = "level";
    private static final String EXTRA_SCALE = "scale";
    private static final String EXTRA_STATUS = "status";
    private static final String EXTRA_HEALTH = "health";
    private static final String EXTRA_PLUGGED = "plugged";
    private static final String EXTRA_TEMPERATURE = "temperature";
    private static final String EXTRA_VOLTAGE = "voltage";
    //private static final String EXTRA_TECHNOLOGY = "technology";

    private static final String FIELD_PERCENT = "percent";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_HEALTH = "health";
    private static final String FIELD_PLUGGED = "plugged";
    private static final String FIELD_TEMPERATURE = "temperature";
    private static final String FIELD_VOLTAGE = "voltage";
    private static final String FIELD_LAST_STATUS = "last_status";
    private static final String FIELD_LAST_PLUGGED = "last_plugged";
    private static final String FIELD_LAST_PERCENT = "last_percent";

    private static final String FIELD_LAST_STATUS_CTM = "last_status_cTM";

    private static final String FIELD_PREDICTION_DAYS = "prediction_days";
    private static final String FIELD_PREDICTION_HOURS = "prediction_hours";
    private static final String FIELD_PREDICTION_MINUTES = "prediction_minutes";
    private static final String FIELD_PREDICTION_WHAT = "prediction_what";
    private static final String FIELD_PREDICTION_WHEN = "prediction_when";

    //private static final String LOG_TAG = "com.darshancomputing.BatteryIndicatorPro - BatteryInfo";

    int
        percent,
        status,
        health,
        plugged,
        temperature,
        voltage,
        last_status,
        last_plugged,
        last_percent;

    long last_status_cTM;
    Prediction prediction = new Prediction();

    class Prediction {
        static final int NONE          = 0;
        static final int UNTIL_DRAINED = 1;
        static final int UNTIL_CHARGED = 2;

        int what;
        long when;
        RelativeTime last_rtime = new RelativeTime();

        private static final int MIN_PREDICTION = 60 * 1000;

        void update(long ts) {
            when = ts;

            if (status == STATUS_FULLY_CHARGED) what = NONE;
            else if (status == STATUS_CHARGING) what = UNTIL_CHARGED;
            else                                what = UNTIL_DRAINED;
        }

        void updateRelativeTime() {
            long now = SystemClock.elapsedRealtime();

            if (when < now + MIN_PREDICTION)
                when = now + MIN_PREDICTION;

            last_rtime.update(when, now);
        }
    }

    static class RelativeTime {
        int days, hours, minutes;

        // If days > 0, then minutes is undefined and hours is rounded to the closest hour (rounding minutes up or down)
        void update(long to, long from) {
            int seconds = (int) ((to - from) / 1000);
            days = 0;
            hours = seconds / (60 * 60);
            minutes = (seconds / 60) % 60;

            if (hours >= 24) {
                if (minutes >= 30) hours += 1;

                days = hours / 24;
                hours = hours % 24;
            }
        }
    }

    void load(Intent intent, SharedPreferences sp) {
        load(intent);
        load(sp);
    }

    void load(Intent intent) {
        int level = intent.getIntExtra(EXTRA_LEVEL, 50);
        int scale = intent.getIntExtra(EXTRA_SCALE, 100);

        status = intent.getIntExtra(EXTRA_STATUS, STATUS_UNKNOWN);
        health = intent.getIntExtra(EXTRA_HEALTH, HEALTH_UNKNOWN);
        plugged = intent.getIntExtra(EXTRA_PLUGGED, PLUGGED_UNKNOWN);
        temperature = intent.getIntExtra(EXTRA_TEMPERATURE, 0);
        voltage = intent.getIntExtra(EXTRA_VOLTAGE, 0);
        //technology = intent.getStringExtra(EXTRA_TECHNOLOGY);

        percent = level * 100 / scale;
        percent = attemptOnePercentHack(percent);
        if (percent > 100) percent = 100;
        if (percent < 0  ) percent = 0; // Developer console shows negative percent can actually be reported

        // Treat unplugged plugged as unpluggged status
        if (plugged == PLUGGED_UNPLUGGED) status = STATUS_UNPLUGGED;

        if (status  > STATUS_MAX) { status  = STATUS_UNKNOWN; }
        if (health  > HEALTH_MAX) { health  = HEALTH_UNKNOWN; }
        if (plugged > PLUGGED_MAX){ plugged = PLUGGED_UNKNOWN; }

        if (last_status_cTM == 0) { // Brand new BatteryInfo
            last_status  = status;
            last_plugged = plugged;
            last_percent = percent;
            last_status_cTM = System.currentTimeMillis();
        }
    }

    void load(SharedPreferences sp) {
        last_status = sp.getInt(KEY_LAST_STATUS, status);
        last_plugged = sp.getInt(KEY_LAST_PLUGGED, plugged);
        last_status_cTM = sp.getLong(KEY_LAST_STATUS_CTM, System.currentTimeMillis());
        last_percent = sp.getInt(KEY_LAST_PERCENT, percent);
    }

    Bundle toBundle() {
        Bundle bundle = new Bundle();

        bundle.putInt(FIELD_PERCENT, percent);
        bundle.putInt(FIELD_STATUS, status);
        bundle.putInt(FIELD_HEALTH, health);
        bundle.putInt(FIELD_PLUGGED, plugged);
        bundle.putInt(FIELD_TEMPERATURE, temperature);
        bundle.putInt(FIELD_VOLTAGE, voltage);
        bundle.putInt(FIELD_LAST_STATUS, last_status);
        bundle.putInt(FIELD_LAST_PLUGGED, last_plugged);
        bundle.putInt(FIELD_LAST_PERCENT, last_percent);

        bundle.putLong(FIELD_LAST_STATUS_CTM, last_status_cTM);

        bundle.putInt(FIELD_PREDICTION_DAYS, prediction.last_rtime.days);
        bundle.putInt(FIELD_PREDICTION_HOURS, prediction.last_rtime.hours);
        bundle.putInt(FIELD_PREDICTION_MINUTES, prediction.last_rtime.minutes);

        bundle.putInt( FIELD_PREDICTION_WHAT, prediction.what);
        bundle.putLong(FIELD_PREDICTION_WHEN, prediction.when);

        return bundle;
    }

    void loadBundle(Bundle bundle) {
        percent = bundle.getInt(FIELD_PERCENT);
        status = bundle.getInt(FIELD_STATUS);
        health = bundle.getInt(FIELD_HEALTH);
        plugged = bundle.getInt(FIELD_PLUGGED);
        temperature = bundle.getInt(FIELD_TEMPERATURE);
        voltage = bundle.getInt(FIELD_VOLTAGE);
        last_status = bundle.getInt(FIELD_LAST_STATUS);
        last_plugged = bundle.getInt(FIELD_LAST_PLUGGED);
        last_percent = bundle.getInt(FIELD_LAST_PERCENT);

        last_status_cTM = bundle.getLong(FIELD_LAST_STATUS_CTM);

        prediction.last_rtime.days = bundle.getInt(FIELD_PREDICTION_DAYS);
        prediction.last_rtime.hours = bundle.getInt(FIELD_PREDICTION_HOURS);
        prediction.last_rtime.minutes = bundle.getInt(FIELD_PREDICTION_MINUTES);

        prediction.what = bundle.getInt( FIELD_PREDICTION_WHAT);
        prediction.when = bundle.getLong(FIELD_PREDICTION_WHEN);
    }

    private static int attemptOnePercentHack(int percent) {
        java.io.File hack_file = new java.io.File("/sys/class/power_supply/battery/charge_counter");

        if (hack_file.exists()) {
            try {
                java.io.FileReader fReader = new java.io.FileReader(hack_file);
                java.io.BufferedReader bReader = new java.io.BufferedReader(fReader, 8);
                String line = bReader.readLine();
                bReader.close();

                int charge_counter = Integer.valueOf(line);

                if (charge_counter < percent + 10 && charge_counter > percent - 10) {
                    if (charge_counter > 100) // This happens
                        charge_counter = 100;

                    if (charge_counter < 0)   // This could happen?
                        charge_counter = 0;

                    percent = charge_counter;
                } else {
                    /* The Log messages are only really useful to me and might as well be left hardwired here in English. */
                    // Log.e(LOG_TAG, "charge_counter file exists but with value " + charge_counter + " which is inconsistent with percent: " + percent);
                }
            } catch (java.io.FileNotFoundException e) {
                //Log.e(LOG_TAG, "charge_counter file doesn't exist");
                //e.printStackTrace();
            } catch (java.io.IOException e) {
                //Log.e(LOG_TAG, "Error reading charge_counter file");
                //e.printStackTrace();
            } catch (NumberFormatException e) {
                //Log.e(LOG_TAG, "Read charge_counter file but couldn't convert contents to int");
                //e.printStackTrace();
            }
        }

        return percent;
    }
}
