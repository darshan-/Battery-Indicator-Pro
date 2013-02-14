/*
    Copyright (c) 2013 Darshan-Josiah Barber

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
import android.util.Log;

class BatteryInfo {
    public static final int STATUS_UNPLUGGED     = 0;
    public static final int STATUS_UNKNOWN       = 1;
    public static final int STATUS_CHARGING      = 2;
    public static final int STATUS_DISCHARGING   = 3;
    public static final int STATUS_NOT_CHARGING  = 4;
    public static final int STATUS_FULLY_CHARGED = 5;
    public static final int STATUS_MAX = STATUS_FULLY_CHARGED;

    public static final int PLUGGED_UNPLUGGED = 0;
    public static final int PLUGGED_AC        = 1;
    public static final int PLUGGED_USB       = 2;
    public static final int PLUGGED_UNKNOWN   = 3;
    public static final int PLUGGED_WIRELESS  = 4;
    public static final int PLUGGED_MAX       = PLUGGED_WIRELESS;

    public static final int HEALTH_UNKNOWN     = 1;
    public static final int HEALTH_GOOD        = 2;
    public static final int HEALTH_OVERHEAT    = 3;
    public static final int HEALTH_DEAD        = 4;
    public static final int HEALTH_OVERVOLTAGE = 5;
    public static final int HEALTH_FAILURE     = 6;
    public static final int HEALTH_COLD        = 7;
    public static final int HEALTH_MAX         = HEALTH_COLD;

    public static final String KEY_LAST_STATUS_CTM = "last_status_cTM";
    public static final String KEY_LAST_STATUS = "last_status";
    public static final String KEY_LAST_PERCENT = "last_percent";
    public static final String KEY_LAST_PLUGGED = "last_plugged";

    public static final long DEFAULT_LAST_STATUS_CTM = -1;
    public static final int DEFAULT_LAST_STATUS = -1;
    public static final int DEFAULT_LAST_PERCENT = -1;
    public static final int DEFAULT_LAST_PLUGGED = -1;

    private static final String EXTRA_LEVEL = "level";
    private static final String EXTRA_SCALE = "scale";
    private static final String EXTRA_STATUS = "status";
    private static final String EXTRA_HEALTH = "health";
    private static final String EXTRA_PLUGGED = "plugged";
    private static final String EXTRA_TEMPERATURE = "temperature";
    private static final String EXTRA_VOLTAGE = "voltage";
    private static final String EXTRA_TECHNOLOGY = "technology";

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

    private static final String LOG_TAG = "com.darshancomputing.BatteryIndicatorPro - BatteryInfo";

    public int
        percent,
        status,
        health,
        plugged,
        temperature,
        voltage,
        last_status,
        last_plugged,
        last_percent;

    public long last_status_cTM;
    public Prediction prediction = new Prediction();

    // If days > 0, then minutes is undefined and hours is rounded to the closest hour (rounding minutes up or down)
    public class Prediction {
        public static final int NONE          = 0;
        public static final int UNTIL_DRAINED = 1;
        public static final int UNTIL_CHARGED = 2;

        public int days, hours, minutes, what;

        public void update(int seconds) {
            if (status == STATUS_FULLY_CHARGED) what = NONE;
            else if (status == STATUS_CHARGING) what = UNTIL_CHARGED;
            else                                what = UNTIL_DRAINED;

            days = 0;
            hours = seconds / (60 * 60);
            minutes = (seconds / 60) % 60;

            if (hours < 24) {
                days = 0;
            } else {
                if (minutes >= 30) hours += 1;

                days = hours / 24;
                hours = days % 24;
            }
        }
    }

    public void load(Intent intent, SharedPreferences sp_store) {
        load(intent);
        load(sp_store);
    }

    public void load(Intent intent) {
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

        // Treat unplugged plugged as unpluggged status, unless charging wirelessly
        if (plugged == PLUGGED_UNPLUGGED) status = STATUS_UNPLUGGED;

        if (status  > STATUS_MAX) { status  = STATUS_UNKNOWN; }
        if (health  > HEALTH_MAX) { health  = HEALTH_UNKNOWN; }
        if (plugged > PLUGGED_MAX){ plugged = PLUGGED_UNKNOWN; }
    }

    public void load(SharedPreferences sp_store) {
        last_status = sp_store.getInt(KEY_LAST_STATUS, DEFAULT_LAST_STATUS);
        last_plugged = sp_store.getInt(KEY_LAST_PLUGGED, DEFAULT_LAST_PLUGGED);
        last_status_cTM = sp_store.getLong(KEY_LAST_STATUS_CTM, DEFAULT_LAST_STATUS_CTM);
        last_percent = sp_store.getInt(KEY_LAST_PERCENT, DEFAULT_LAST_PERCENT);
    }

    public Bundle toBundle() {
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

        bundle.putInt(FIELD_PREDICTION_DAYS, prediction.days);
        bundle.putInt(FIELD_PREDICTION_HOURS, prediction.hours);
        bundle.putInt(FIELD_PREDICTION_MINUTES, prediction.minutes);
        bundle.putInt(FIELD_PREDICTION_WHAT, prediction.what);

        return bundle;
    }

    public void loadBundle(Bundle bundle) {
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

        prediction.days = bundle.getInt(FIELD_PREDICTION_DAYS);
        prediction.hours = bundle.getInt(FIELD_PREDICTION_HOURS);
        prediction.minutes = bundle.getInt(FIELD_PREDICTION_MINUTES);
        prediction.what = bundle.getInt(FIELD_PREDICTION_WHAT);
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
                    Log.e(LOG_TAG, "charge_counter file exists but with value " + charge_counter +
                          " which is inconsistent with percent: " + percent);
                }
            } catch (java.io.FileNotFoundException e) {
                Log.e(LOG_TAG, "charge_counter file doesn't exist");
                e.printStackTrace();
            } catch (java.io.IOException e) {
                Log.e(LOG_TAG, "Error reading charge_counter file");
                e.printStackTrace();
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, "Read charge_counter file but couldn't convert contents to int");
                e.printStackTrace();
            }
        }

        return percent;
    }
}
