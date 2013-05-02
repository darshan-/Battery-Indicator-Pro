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

    public class Prediction {
        public static final int NONE          = 0;
        public static final int UNTIL_DRAINED = 1;
        public static final int UNTIL_CHARGED = 2;

        public long when;
        public int what;

        public void update(long ts) {
            when = ts;

            if (status == STATUS_FULLY_CHARGED) what = NONE;
            else if (status == STATUS_CHARGING) what = UNTIL_CHARGED;
            else                                what = UNTIL_DRAINED;
        }

        public RelativeTime getRelativeTime(long from) {
            return new RelativeTime(when, from);
        }
    }

    public class RelativeTime {
        public int days, hours, minutes;

        // If days > 0, then minutes is undefined and hours is rounded to the closest hour (rounding minutes up or down)
        public RelativeTime(long to, long from) {
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
}
