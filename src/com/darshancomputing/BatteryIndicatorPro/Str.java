/*
    Copyright (c) 2010 Josiah Barber (aka Darshan)

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

import android.content.res.Resources;

/* TODO?: have a public instance in the service and grab the server's instance from all other classes? */
public class Str {
    private Resources res;

    public String degree_symbol;
    public String fahrenheit_symbol;
    public String celsius_symbol;
    public String volt_symbol;
    public String percent_symbol;
    public String since;
    public String default_status_dur_est;
    public String default_red_thresh;
    public String default_amber_thresh;
    public String default_green_thresh;
    public String default_max_log_age;

    public String logs_empty;
    public String confirm_clear_logs;
    public String confirm_ten_percent_enable;
    public String confirm_ten_percent_disable;
    public String confirm_ten_percent_hint;
    public String yes;
    public String cancel;

    public String currently_set_to;

    public String inaccessible_storage;
    public String inaccessible_w_reason;
    public String read_only_storage;
    public String file_written;

    public String time;
    public String date;
    public String status;
    public String charge;
    public String temperature;
    public String voltage;
    
    public String[] statuses;
    public String[] log_statuses;
    public String[] log_statuses_old;
    public String[] healths;
    public String[] pluggeds;
    public String[] alarm_types_display;
    public String[] alarm_type_entries;
    public String[] alarm_type_values;
    public String[] temp_alarm_entries;
    public String[] temp_alarm_values;

    public Str(Resources r) {
        res = r;

        degree_symbol          = res.getString(R.string.degree_symbol);
        fahrenheit_symbol      = res.getString(R.string.fahrenheit_symbol);
        celsius_symbol         = res.getString(R.string.celsius_symbol);
        volt_symbol            = res.getString(R.string.volt_symbol);
        percent_symbol         = res.getString(R.string.percent_symbol);
        since                  = res.getString(R.string.since);
        default_status_dur_est = res.getString(R.string.default_status_dur_est);
        default_red_thresh     = res.getString(R.string.default_red_thresh);
        default_amber_thresh   = res.getString(R.string.default_amber_thresh);
        default_green_thresh   = res.getString(R.string.default_green_thresh);
        default_max_log_age    = res.getString(R.string.default_max_log_age);

        logs_empty         = res.getString(R.string.logs_empty);
        confirm_clear_logs = res.getString(R.string.confirm_clear_logs);
        yes                = res.getString(R.string.yes);
        cancel             = res.getString(R.string.cancel);

        confirm_ten_percent_enable  = res.getString(R.string.confirm_ten_percent_enable);
        confirm_ten_percent_disable = res.getString(R.string.confirm_ten_percent_disable);
        confirm_ten_percent_hint    = res.getString(R.string.confirm_ten_percent_hint);

        currently_set_to = res.getString(R.string.currently_set_to);

        inaccessible_storage  = res.getString(R.string.inaccessible_storage);
        inaccessible_w_reason = res.getString(R.string.inaccessible_w_reason);
        read_only_storage     = res.getString(R.string.read_only_storage);
        file_written          = res.getString(R.string.file_written);

        date        = res.getString(R.string.date);
        time        = res.getString(R.string.time);
        status      = res.getString(R.string.status);
        charge      = res.getString(R.string.charge);
        temperature = res.getString(R.string.temperature);
        voltage     = res.getString(R.string.voltage);

        statuses            = res.getStringArray(R.array.statuses);
        log_statuses        = res.getStringArray(R.array.log_statuses);
        log_statuses_old    = res.getStringArray(R.array.log_statuses_old);
        healths             = res.getStringArray(R.array.healths);
        pluggeds            = res.getStringArray(R.array.pluggeds);
        alarm_types_display = res.getStringArray(R.array.alarm_types_display);
        alarm_type_entries  = res.getStringArray(R.array.alarm_type_entries);
        alarm_type_values   = res.getStringArray(R.array.alarm_type_values);
        temp_alarm_entries  = res.getStringArray(R.array.temp_alarm_entries);
        temp_alarm_values   = res.getStringArray(R.array.temp_alarm_values);
    }

    public String for_n_hours(int n) {
        return String.format(res.getQuantityString(R.plurals.for_n_hours, n), n);
    }

    public String n_log_items(int n) {
        return String.format(res.getQuantityString(R.plurals.n_log_items, n), n);
    }

    public String formatTemp(int temperature, boolean convertF) {
        if (convertF){
            return String.valueOf((java.lang.Math.round(temperature * 9 / 5.0) / 10.0) + 32.0) +
                degree_symbol + fahrenheit_symbol;
        } else {
            return String.valueOf(temperature / 10.0) + degree_symbol + celsius_symbol;
        }
    }

    public String formatVoltage(int voltage) {
        return String.valueOf(voltage / 1000.0) + volt_symbol;
    }
}
