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

import android.content.SharedPreferences;

// Per-device corrections for battery readings: the current's sign and unit (measured by
//  CalibrationDialogFragment while discharging) and the two-cell battery fixes. Stored in the
//  settings file and applied in both processes; call apply() wherever settings are (re)loaded.
class Calibration {
    static final String KEY_CALIBRATED        = "current_calibrated";
    static final String KEY_PROMPTED          = "current_calibration_prompted";
    static final String KEY_SIGN              = "current_calibration_sign";        // 1, or -1 to flip
    static final String KEY_RAW_PER_MA        = "current_calibration_raw_per_ma";  // Raw units per mA
    static final String KEY_DUAL_CELL_CURRENT = "dual_cell_current";
    static final String KEY_DUAL_CELL_VOLTAGE = "dual_cell_voltage";
    static final String KEY_CALIBRATE         = "calibrate_readings"; // Settings entry that opens the dialog

    // BatteryManager documents µA; some devices report mA, a few nA
    static final int RAW_PER_MA_UA = 1000;
    static final int RAW_PER_MA_MA = 1;
    static final int RAW_PER_MA_NA = 1_000_000;

    // Above this, a single-cell phone can't be reporting its own voltage (mV)
    static final int DUAL_CELL_VOLTAGE_MV = 7000;

    static void apply(SharedPreferences settings) {
        int multiplier;
        try {
            multiplier = Integer.parseInt(settings.getString(SettingsFragment.KEY_CURRENT_HACK_MULTIPLIER, "1"));
        } catch (NumberFormatException e) {
            multiplier = 1;
        }

        boolean dualCurrent = settings.getBoolean(KEY_DUAL_CELL_CURRENT, false);

        CurrentHack.setMultiplier(multiplier);
        CurrentHack.setCorrection(settings.getInt(KEY_SIGN, 1) * (dualCurrent ? 2 : 1),
                                  settings.getInt(KEY_RAW_PER_MA, RAW_PER_MA_UA));
        UsageTracker.setCells(dualCurrent ? 2 : 1);
        BatteryInfo.setVoltageDivisor(settings.getBoolean(KEY_DUAL_CELL_VOLTAGE, false) ? 2 : 1);
    }

    // Raw readings taken while discharging with the screen on, so the phone draws at least tens of mA
    static int detectRawPerMa(long medianAbsRaw) {
        if (medianAbsRaw < 20_000)        // Would be under 20 mA in µA; must be mA
            return RAW_PER_MA_MA;
        if (medianAbsRaw > 20_000_000)    // Would be over 20 A in µA; must be nA
            return RAW_PER_MA_NA;
        return RAW_PER_MA_UA;
    }

    // Android's convention is negative while discharging
    static int detectSign(long medianRaw) {
        return medianRaw > 0 ? -1 : 1;
    }
}
