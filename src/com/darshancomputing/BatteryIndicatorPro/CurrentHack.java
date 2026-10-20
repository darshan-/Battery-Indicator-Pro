/*
    Copyright (c) 2015-2026 Darshan Computing, LLC

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
import android.os.BatteryManager;

import java.io.File;

// Initially based on CurrentReaderFactory.java from CurrentWidget by Ran Manor (GPL v3)
// The many model-specific sysfs paths that used to live here were all for devices that can't run
//  Android 8 (our minSdk), so only BatteryManager and the generic power_supply files remain.
class CurrentHack {
    static final int HACK_METHOD_NONE = -1;
    static final int HACK_METHOD_BOTH = 0;
    static final int HACK_METHOD_FILE_SYSTEM = 1;
    static final int HACK_METHOD_BATTERY_MANAGER = 2;

    private static final File FS_CURRENT_NOW = new File("/sys/class/power_supply/battery/current_now");
    private static final File FS_CURRENT_AVG = new File("/sys/class/power_supply/battery/current_avg");

    private static BatteryManager batteryManager;
    private static boolean preferFS = false;
    private static int multiplier = 1;
    private static int correction = 1;   // Calibrated sign, times 2 for a two-cell battery
    private static int rawPerMa = Calibration.RAW_PER_MA_UA;
    private static int method = HACK_METHOD_NONE;

    static void setContext(Context c) {
        batteryManager = (BatteryManager) c.getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
    }

    static void setPreferFS(boolean pfs) {
        preferFS = pfs;
        int avail = getHackMethodsAvailable();

        if (avail == HACK_METHOD_BOTH)
            if (preferFS)
                method = HACK_METHOD_FILE_SYSTEM;
            else
                method = HACK_METHOD_BATTERY_MANAGER;
        else
            method = avail; // Only one or none supported
    }

    static void setMultiplier(int m) {
        multiplier = m;
    }

    static void setCorrection(int c, int rpm) {
        correction = c;
        rawPerMa = rpm > 0 ? rpm : Calibration.RAW_PER_MA_UA;
    }

    static int getHackMethodsAvailable() {
        boolean bm = getBMCurrent() != null;
        boolean fs = getFSCurrent() != null;

        if (bm && fs)
            return HACK_METHOD_BOTH;

        if (bm)
            return HACK_METHOD_BATTERY_MANAGER;

        if (fs)
            return HACK_METHOD_FILE_SYSTEM;

        return HACK_METHOD_NONE;
    }

    static Long getCurrent() {
        return toMa(getRawCurrent());
    }

    static Long getAvgCurrent() {
        if (method == HACK_METHOD_NONE)
            return null;

        if (method == HACK_METHOD_FILE_SYSTEM)
            return toMa(readFS(FS_CURRENT_AVG));

        return toMa(getBMProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE));
    }

    // The reading as the device reports it, before multiplier and calibration; for calibrating
    static Long getRawCurrent() {
        if (method == HACK_METHOD_NONE)
            return null;

        if (method == HACK_METHOD_FILE_SYSTEM)
            return readFS(FS_CURRENT_NOW);

        return getBMProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
    }

    // Raw units (µA unless calibrated otherwise) to mA, with sign and two-cell corrections
    private static Long toMa(Long raw) {
        if (raw == null)
            return null;

        return raw * multiplier * correction / rawPerMa;
    }

    private static Long getBMCurrent() {
        return getBMProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
    }

    private static Long getBMProperty(int property) {
        if (batteryManager == null)
            return null;

        int current = batteryManager.getIntProperty(property);

        if (current > Integer.MIN_VALUE)
            return (long) current;
        else
            return null;
    }

    private static Long getFSCurrent() {
        return readFS(FS_CURRENT_NOW);
    }

    // Kernel power_supply files report µA; returns the raw value, or null if unreadable (SELinux blocks this on most newer devices)
    private static Long readFS(File f) {
        if (!f.exists())
            return null;

        try {
            java.io.BufferedReader bReader = new java.io.BufferedReader(new java.io.FileReader(f), 16);
            String line = bReader.readLine();
            bReader.close();

            if (line == null)
                return null;

            return Long.parseLong(line.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
