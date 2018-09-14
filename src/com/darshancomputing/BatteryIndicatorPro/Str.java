/*
    Copyright (c) 2010-2018 Darshan-Josiah Barber

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General License for more details.
*/

package com.darshancomputing.BatteryIndicatorPro;

import android.content.res.Resources;

class Str {
    private static Resources res;

    static String degree_symbol;
    static String fahrenheit_symbol;
    static String celsius_symbol;
    static String volt_symbol;
    static String percent_symbol;
    static String since;
    static String default_status_dur_est;
    static String default_red_thresh;
    static String default_amber_thresh;
    static String default_green_thresh;
    static String default_max_log_age;
    static String default_prediction_type;

    static String logs_empty;
    static String confirm_clear_logs;
    static String configure_log_filter;
    static String yes;
    static String cancel;
    static String okay;

    static String currently_set_to;
    static String alarm_pref_not_used;

    static String alarm_fully_charged;
    static String alarm_charge_drops;
    static String alarm_charge_rises;
    static String alarm_temp_drops;
    static String alarm_temp_rises;
    static String alarm_health_failure;
    static String alarm_text;

    static String inaccessible_storage;
    static String inaccessible_w_reason;
    static String read_only_storage;
    static String no_storage_permission;
    static String file_written;

    static String time;
    static String date;
    static String status;
    static String charge;
    static String temperature;
    static String temperature_f;
    static String voltage;

    static String status_boot_completed;
    
    static String[] statuses;
    static String[] log_statuses;
    static String[] log_statuses_old;
    static String[] healths;
    static String[] pluggeds;
    static String[] alarm_types_display;
    static String[] alarm_type_entries;
    static String[] alarm_type_values;
    static String[] temp_alarm_entries;
    static String[] temp_alarm_values;
    static String[] log_filter_pref_keys;

    static void setResources(Resources r) {
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
        default_prediction_type = res.getString(R.string.default_prediction_type);

        logs_empty         = res.getString(R.string.logs_empty);
        confirm_clear_logs = res.getString(R.string.confirm_clear_logs);
        yes                = res.getString(R.string.yes);
        cancel             = res.getString(R.string.cancel);
        okay               = res.getString(R.string.okay);

        configure_log_filter = res.getString(R.string.configure_log_filter);

        currently_set_to    = res.getString(R.string.currently_set_to);
        alarm_pref_not_used = res.getString(R.string.alarm_pref_not_used);

        alarm_fully_charged  = res.getString(R.string.alarm_fully_charged);
        alarm_charge_drops   = res.getString(R.string.alarm_charge_drops);
        alarm_charge_rises   = res.getString(R.string.alarm_charge_rises);
        alarm_temp_drops     = res.getString(R.string.alarm_temp_drops);
        alarm_temp_rises     = res.getString(R.string.alarm_temp_rises);
        alarm_health_failure = res.getString(R.string.alarm_health_failure);
        alarm_text           = res.getString(R.string.alarm_text);

        inaccessible_storage  = res.getString(R.string.inaccessible_storage);
        inaccessible_w_reason = res.getString(R.string.inaccessible_w_reason);
        read_only_storage     = res.getString(R.string.read_only_storage);
        no_storage_permission = res.getString(R.string.no_storage_permission);
        file_written          = res.getString(R.string.file_written);

        date          = res.getString(R.string.date);
        time          = res.getString(R.string.time);
        status        = res.getString(R.string.status);
        charge        = res.getString(R.string.charge);
        temperature   = res.getString(R.string.temperature);
        temperature_f = res.getString(R.string.temperature_f);
        voltage       = res.getString(R.string.voltage);

        status_boot_completed = res.getString(R.string.status_boot_completed);

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

        log_filter_pref_keys = res.getStringArray(R.array.log_filter_pref_keys);
    }

    static String for_n_hours(int n) {
        return String.format(res.getQuantityString(R.plurals.for_n_hours, n), n);
    }

    static String n_hours_m_minutes_long(int n, int m) {
        return (String.format(res.getQuantityString(R.plurals.n_hours_long, n), n) +
                String.format(res.getQuantityString(R.plurals.n_minutes_long, m), m));
    }

    static String n_minutes_long(int n) {
        return String.format(res.getQuantityString(R.plurals.n_minutes_long, n), n);
    }

    static String n_hours_m_minutes_medium(int n, int m) {
        return (String.format(res.getQuantityString(R.plurals.n_hours_medium, n), n) +
                String.format(res.getQuantityString(R.plurals.n_minutes_medium, m), m));
    }

    static String n_hours_long_m_minutes_medium(int n, int m) {
        return (String.format(res.getQuantityString(R.plurals.n_hours_long, n), n) +
                String.format(res.getQuantityString(R.plurals.n_minutes_medium, m), m));
    }

    static String n_hours_m_minutes_short(int n, int m) {
        return (String.format(res.getQuantityString(R.plurals.n_hours_short, n), n) +
                String.format(res.getQuantityString(R.plurals.n_minutes_short, m), m));
    }

    static String n_days_m_hours(int n, int m) {
        return (String.format(res.getQuantityString(R.plurals.n_days, n), n) +
                String.format(res.getQuantityString(R.plurals.n_hours, m), m));
    }

    static String n_log_items(int n) {
        return String.format(res.getQuantityString(R.plurals.n_log_items, n), n);
    }

    /* temperature is the integer number of tenths of degrees Celcius, as returned by BatteryManager */
    static String formatTemp(int temperature, boolean convertF, boolean includeTenths) {
        double d;
        String s;

        if (convertF){
            d = java.lang.Math.round(temperature * 9 / 5.0) / 10.0 + 32.0;
            s = degree_symbol + fahrenheit_symbol;
        } else {
            d = temperature / 10.0;
            s = degree_symbol + celsius_symbol;
        }

        return (includeTenths ? String.valueOf(d) : String.valueOf(java.lang.Math.round(d))) + s;
    }

    static String formatTemp(int temperature, boolean convertF) {
        return formatTemp(temperature, convertF, true);
    }

    static String formatVoltage(int voltage) {
        return String.valueOf(voltage / 1000.0) + volt_symbol;
    }

    static int indexOf(String[] a, String key) {
        for (int i=0, size=a.length; i < size; i++)
            if (key.equals(a[i])) return i;

        return -1;
    }

    static android.text.Spanned timeRemaining(BatteryInfo info) {
        if (info.prediction.what == BatteryInfo.Prediction.NONE) {
            return android.text.Html.fromHtml("<font color=\"#6fc14b\">" + statuses[info.status] + "</font>");
        } else {
            BatteryInfo.RelativeTime predicted = info.prediction.last_rtime;

            if (predicted.days > 0)
                return android.text.Html.fromHtml("<font color=\"#6fc14b\">" + predicted.days + "d</font> " +
                                                  "<font color=\"#33b5e5\"><small>" + predicted.hours + "h</small></font>");
            else if (predicted.hours > 0)
                return android.text.Html.fromHtml("<font color=\"#6fc14b\">" + predicted.hours + "h</font> " +
                                                  "<font color=\"#33b5e5\"><small>" + predicted.minutes + "m</small></font>");
            else
                return android.text.Html.fromHtml("<font color=\"#33b5e5\"><small>" + predicted.minutes + " mins</small></font>");
        }
    }

    // Shows mdash rather than "Fully Charged" when no prediction.
    //   The widget still wants the old behavior.
    static android.text.Spanned timeRemainingMainScreen(BatteryInfo info) {
        if (info.prediction.what == BatteryInfo.Prediction.NONE)
            return android.text.Html.fromHtml("&nbsp;&nbsp;&nbsp;&mdash;&nbsp;&nbsp;&nbsp;");
        else
            return timeRemaining(info);
    }

    static String untilWhat(BatteryInfo info) {
        if (info.prediction.what == BatteryInfo.Prediction.NONE)
            return "";
        else if (info.prediction.what == BatteryInfo.Prediction.UNTIL_CHARGED)
            return res.getString(R.string.activity_until_charged);
        else
            return res.getString(R.string.activity_until_drained);
    }
}
