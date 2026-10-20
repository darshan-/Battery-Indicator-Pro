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
import android.os.BatteryManager;
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

    // 2 when the device reports both cells of a two-cell battery in series (see Calibration)
    private static int voltageDivisor = 1;

    static void setVoltageDivisor(int d) {
        voltageDivisor = d > 0 ? d : 1;
    }

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

    // Values of BatteryManager.EXTRA_CHARGING_STATUS (API 34+); they mirror the health HAL's
    //  BatteryChargingState, which has no public constants in the SDK.
    static final int CHARGING_STATE_NORMAL    = 1;
    static final int CHARGING_STATE_TOO_COLD  = 2;
    static final int CHARGING_STATE_TOO_HOT   = 3;
    static final int CHARGING_STATE_LONG_LIFE = 4;
    static final int CHARGING_STATE_ADAPTIVE  = 5;

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

    private static final String FIELD_CYCLE_COUNT = "cycle_count";
    private static final String FIELD_CHARGING_STATE = "charging_state";
    private static final String FIELD_CHARGE_COUNTER = "charge_counter_uah";
    private static final String FIELD_SCREEN_ON_DRAIN = "screen_on_drain_uah";
    private static final String FIELD_SCREEN_ON_MS = "screen_on_ms";
    private static final String FIELD_SCREEN_OFF_DRAIN = "screen_off_drain_uah";
    private static final String FIELD_SCREEN_OFF_MS = "screen_off_ms";
    private static final String FIELD_EST_CAPACITY = "est_capacity_uah";
    private static final String FIELD_EST_CAPACITY_SAMPLES = "est_capacity_samples";

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

    // -1 when the device doesn't report them (cycle count and charging state need API 34+)
    int cycle_count = -1;
    int charging_state = -1;
    long charge_counter_uah = -1;

    // Charge drained since the last status change, split by screen state; negative while charging.
    //  Filled in by UsageTracker in the Service.
    long screen_on_drain_uah, screen_on_ms;
    long screen_off_drain_uah, screen_off_ms;

    // Full-charge capacity estimated from past charge sessions; -1 until one has been measured
    long est_capacity_uah = -1;
    int est_capacity_samples;

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
        voltage = intent.getIntExtra(EXTRA_VOLTAGE, 0) / voltageDivisor;
        //technology = intent.getStringExtra(EXTRA_TECHNOLOGY);

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            cycle_count = intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1);
            charging_state = intent.getIntExtra(BatteryManager.EXTRA_CHARGING_STATUS, -1);
        }

        percent = level * 100 / scale;
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

        bundle.putInt(FIELD_CYCLE_COUNT, cycle_count);
        bundle.putInt(FIELD_CHARGING_STATE, charging_state);
        bundle.putLong(FIELD_CHARGE_COUNTER, charge_counter_uah);
        bundle.putLong(FIELD_SCREEN_ON_DRAIN, screen_on_drain_uah);
        bundle.putLong(FIELD_SCREEN_ON_MS, screen_on_ms);
        bundle.putLong(FIELD_SCREEN_OFF_DRAIN, screen_off_drain_uah);
        bundle.putLong(FIELD_SCREEN_OFF_MS, screen_off_ms);
        bundle.putLong(FIELD_EST_CAPACITY, est_capacity_uah);
        bundle.putInt(FIELD_EST_CAPACITY_SAMPLES, est_capacity_samples);

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

        cycle_count = bundle.getInt(FIELD_CYCLE_COUNT, -1);
        charging_state = bundle.getInt(FIELD_CHARGING_STATE, -1);
        charge_counter_uah = bundle.getLong(FIELD_CHARGE_COUNTER, -1);
        screen_on_drain_uah = bundle.getLong(FIELD_SCREEN_ON_DRAIN);
        screen_on_ms = bundle.getLong(FIELD_SCREEN_ON_MS);
        screen_off_drain_uah = bundle.getLong(FIELD_SCREEN_OFF_DRAIN);
        screen_off_ms = bundle.getLong(FIELD_SCREEN_OFF_MS);
        est_capacity_uah = bundle.getLong(FIELD_EST_CAPACITY, -1);
        est_capacity_samples = bundle.getInt(FIELD_EST_CAPACITY_SAMPLES);

        prediction.last_rtime.days = bundle.getInt(FIELD_PREDICTION_DAYS);
        prediction.last_rtime.hours = bundle.getInt(FIELD_PREDICTION_HOURS);
        prediction.last_rtime.minutes = bundle.getInt(FIELD_PREDICTION_MINUTES);

        prediction.what = bundle.getInt( FIELD_PREDICTION_WHAT);
        prediction.when = bundle.getLong(FIELD_PREDICTION_WHEN);
    }
}
