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
    public Predictor.Prediction prediction = new Predictor.Prediction();
}
