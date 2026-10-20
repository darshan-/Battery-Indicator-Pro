/*
    Copyright (c) 2026 Darshan Computing, LLC

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
import android.os.BatteryManager;
import android.os.SystemClock;

// Tracks charge used (in µAh, from the fuel gauge's charge counter) since the last status change,
//  split by screen on / screen off, and estimates full-charge capacity from charge sessions.
//  Only used from the Service process, which is the only writer of sp_service.
class UsageTracker {
    private static final String KEY_LAST_COUNTER   = "ut_last_counter";
    private static final String KEY_LAST_ELAPSED   = "ut_last_elapsed";
    private static final String KEY_LAST_SCREEN_ON = "ut_last_screen_on";
    private static final String KEY_ON_DRAIN       = "ut_on_drain";
    private static final String KEY_ON_MS          = "ut_on_ms";
    private static final String KEY_OFF_DRAIN      = "ut_off_drain";
    private static final String KEY_OFF_MS         = "ut_off_ms";
    private static final String KEY_START_COUNTER  = "ut_start_counter";
    private static final String KEY_START_PERCENT  = "ut_start_percent";
    private static final String KEY_START_STATUS   = "ut_start_status";
    private static final String KEY_EST_CAPACITY   = "ut_est_capacity";
    private static final String KEY_EST_SAMPLES    = "ut_est_samples";
    private static final String KEY_CELLS          = "ut_cells";

    // A charge session must cover at least this many points to give a usable capacity estimate
    private static final int MIN_PERCENT_FOR_ESTIMATE = 20;
    // Weight of a new charge session in the running capacity estimate
    private static final double NEW_ESTIMATE_WEIGHT = 0.3;
    // A single sample moving more than this is a glitch (or a counter reset), not real usage
    private static final long MAX_SAMPLE_DELTA_UAH = 3_000_000;

    // 2 when a two-cell battery reports only one cell's charge (see Calibration)
    private static int cells = 1;

    static void setCells(int c) {
        cells = c > 0 ? c : 1;
    }

    private final BatteryManager bm;
    private final SharedPreferences sp;

    UsageTracker(Context context, SharedPreferences sp_service) {
        bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        sp = sp_service;
    }

    // Returns the charge counter in µAh, or -1 if this device doesn't report a usable one
    long readChargeCounter(int percent) {
        return counterToUah(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER), percent);
    }

    static long counterToUah(int v, int percent) {
        long uah = rawCounterToUah(v, percent);
        return uah < 0 ? -1 : uah * cells;
    }

    // As reported, without the two-cell correction
    static long rawCounterToUah(int v, int percent) {
        if (v == Integer.MIN_VALUE || v <= 0) return -1;

        long uah = v;
        // Some devices report mAh instead of µAh; an implied full capacity under 50 mAh can't be µAh
        if (percent > 0 && uah * 100 / percent < 50_000)
            uah *= 1000;

        return uah;
    }

    // Attributes charge used since the last sample to the screen state at that sample, then
    //  records screenOnNow for the next one.
    void sample(BatteryInfo info, boolean screenOnNow) {
        long counter = readChargeCounter(info.percent);
        long elapsed = SystemClock.elapsedRealtime();
        info.charge_counter_uah = counter;

        long lastCounter = sp.getLong(KEY_LAST_COUNTER, -1);
        long lastElapsed = sp.getLong(KEY_LAST_ELAPSED, -1);
        boolean lastScreenOn = sp.getBoolean(KEY_LAST_SCREEN_ON, screenOnNow);

        SharedPreferences.Editor ed = sp.edit();

        // The two-cell setting changed the counter's scale; earlier counters can't be compared
        if (sp.getInt(KEY_CELLS, 1) != cells) {
            lastCounter = -1;
            ed.putLong(KEY_START_COUNTER, -1);
            ed.putInt(KEY_CELLS, cells);
        }

        // elapsed < lastElapsed means we rebooted; just start a new baseline
        if (lastElapsed >= 0 && elapsed >= lastElapsed) {
            long dt = elapsed - lastElapsed;
            long drain = 0;

            if (counter >= 0 && lastCounter >= 0) {
                drain = lastCounter - counter;
                if (Math.abs(drain) > MAX_SAMPLE_DELTA_UAH) drain = 0;
            }

            String drainKey = lastScreenOn ? KEY_ON_DRAIN : KEY_OFF_DRAIN;
            String msKey    = lastScreenOn ? KEY_ON_MS    : KEY_OFF_MS;
            ed.putLong(drainKey, sp.getLong(drainKey, 0) + drain);
            ed.putLong(msKey,    sp.getLong(msKey,    0) + dt);
        }

        ed.putLong(KEY_LAST_COUNTER, counter);
        ed.putLong(KEY_LAST_ELAPSED, elapsed);
        ed.putBoolean(KEY_LAST_SCREEN_ON, screenOnNow);
        ed.apply();

        fill(info);
    }

    // Call after sample() when the status has changed: closes the old session (updating the
    //  capacity estimate if it was a long enough charge) and starts a new one.
    void startSession(BatteryInfo info) {
        SharedPreferences.Editor ed = sp.edit();

        int startStatus = sp.getInt(KEY_START_STATUS, -1);
        int startPercent = sp.getInt(KEY_START_PERCENT, -1);
        long startCounter = sp.getLong(KEY_START_COUNTER, -1);
        long counter = info.charge_counter_uah;

        if (startStatus == BatteryInfo.STATUS_CHARGING && startCounter >= 0 && counter >= 0) {
            int dp = info.percent - startPercent;
            long dc = counter - startCounter;

            if (dp >= MIN_PERCENT_FOR_ESTIMATE && dc > 0) {
                long estimate = dc * 100 / dp;
                long old = sp.getLong(KEY_EST_CAPACITY, -1);
                int samples = sp.getInt(KEY_EST_SAMPLES, 0);

                if (old < 0 || samples == 0)
                    ed.putLong(KEY_EST_CAPACITY, estimate);
                else
                    ed.putLong(KEY_EST_CAPACITY, Math.round(old * (1 - NEW_ESTIMATE_WEIGHT) + estimate * NEW_ESTIMATE_WEIGHT));

                ed.putInt(KEY_EST_SAMPLES, samples + 1);
            }
        }

        ed.putInt(KEY_START_STATUS, info.status);
        ed.putInt(KEY_START_PERCENT, info.percent);
        ed.putLong(KEY_START_COUNTER, counter);
        ed.putLong(KEY_ON_DRAIN, 0);
        ed.putLong(KEY_ON_MS, 0);
        ed.putLong(KEY_OFF_DRAIN, 0);
        ed.putLong(KEY_OFF_MS, 0);
        ed.apply();

        fill(info);
    }

    void fill(BatteryInfo info) {
        info.screen_on_drain_uah  = sp.getLong(KEY_ON_DRAIN, 0);
        info.screen_on_ms         = sp.getLong(KEY_ON_MS, 0);
        info.screen_off_drain_uah = sp.getLong(KEY_OFF_DRAIN, 0);
        info.screen_off_ms        = sp.getLong(KEY_OFF_MS, 0);
        info.est_capacity_uah     = sp.getLong(KEY_EST_CAPACITY, -1);
        info.est_capacity_samples = sp.getInt(KEY_EST_SAMPLES, 0);
    }
}
