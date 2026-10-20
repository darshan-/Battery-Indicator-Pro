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

// The battery's rated capacity: what the user entered in settings, otherwise what the system's
//  power profile declares (the OEM's power_profile.xml, read through the hidden PowerProfile class).
class DesignCapacity {
    private static final String POWER_PROFILE = "com.android.internal.os.PowerProfile";

    // AOSP's default power_profile.xml says 1000 mAh, so that (or less) means the OEM didn't set it
    private static final int MIN_PLAUSIBLE_MAH = 1001;
    private static final int MAX_PLAUSIBLE_MAH = 30000;

    private static final int NOT_LOOKED_UP = 0;
    private static int systemMah = NOT_LOOKED_UP; // Doesn't change while the app runs, so cached

    // Manual setting if set, else the system's value; -1 if neither is known
    static int get(Context context, SharedPreferences settings) {
        int manual = manualMah(settings);
        return manual > 0 ? manual : systemMah(context);
    }

    static int manualMah(SharedPreferences settings) {
        try {
            int mah = Integer.parseInt(settings.getString(SettingsFragment.KEY_DESIGN_CAPACITY_MAH, "").trim());
            return mah > 0 ? mah : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    static int systemMah(Context context) {
        if (systemMah == NOT_LOOKED_UP)
            systemMah = readPowerProfile(context.getApplicationContext());
        return systemMah;
    }

    private static int readPowerProfile(Context context) {
        double mah;

        try {
            Class<?> c = Class.forName(POWER_PROFILE);
            Object profile = c.getConstructor(Context.class).newInstance(context);

            try {
                mah = (Double) c.getMethod("getBatteryCapacity").invoke(profile);
            } catch (NoSuchMethodException e) {
                mah = (Double) c.getMethod("getAveragePower", String.class).invoke(profile, "battery.capacity");
            }
        } catch (Throwable t) { // Hidden-API blocks, missing class, OEM changes: all just mean "unknown"
            return -1;
        }

        int rounded = (int) Math.round(mah);
        return rounded >= MIN_PLAUSIBLE_MAH && rounded <= MAX_PLAUSIBLE_MAH ? rounded : -1;
    }
}
