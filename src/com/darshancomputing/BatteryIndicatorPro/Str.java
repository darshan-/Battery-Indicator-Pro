/*
    Copyright (c) 2010-2016 Darshan-Josiah Barber

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
import android.view.WindowManager;

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
    public String default_main_notification_priority;
    public String default_prediction_type;

    public String logs_empty;
    public String confirm_clear_logs;
    public String confirm_ten_percent_enable;
    public String confirm_ten_percent_disable;
    public String confirm_ten_percent_hint;
    public String configure_log_filter;
    public String yes;
    public String cancel;
    public String okay;

    public String currently_set_to;
    public String alarm_pref_not_used;

    public String silent;

    public String alarm_fully_charged;
    public String alarm_charge_drops;
    public String alarm_charge_rises;
    public String alarm_temp_rises;
    public String alarm_health_failure;
    public String alarm_text;

    public String inaccessible_storage;
    public String inaccessible_w_reason;
    public String read_only_storage;
    public String no_storage_permission;
    public String file_written;

    public String time;
    public String date;
    public String status;
    public String charge;
    public String temperature;
    public String temperature_f;
    public String voltage;

    public String status_boot_completed;
    
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
    public String[] log_filter_pref_keys;

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
        default_main_notification_priority = res.getString(R.string.default_main_notification_priority);
        default_prediction_type = res.getString(R.string.default_prediction_type);

        logs_empty         = res.getString(R.string.logs_empty);
        confirm_clear_logs = res.getString(R.string.confirm_clear_logs);
        yes                = res.getString(R.string.yes);
        cancel             = res.getString(R.string.cancel);
        okay               = res.getString(R.string.okay);

        confirm_ten_percent_enable  = res.getString(R.string.confirm_ten_percent_enable);
        confirm_ten_percent_disable = res.getString(R.string.confirm_ten_percent_disable);
        confirm_ten_percent_hint    = res.getString(R.string.confirm_ten_percent_hint);

        configure_log_filter = res.getString(R.string.configure_log_filter);

        currently_set_to    = res.getString(R.string.currently_set_to);
        alarm_pref_not_used = res.getString(R.string.alarm_pref_not_used);

        silent = res.getString(R.string.silent);

        alarm_fully_charged  = res.getString(R.string.alarm_fully_charged);
        alarm_charge_drops   = res.getString(R.string.alarm_charge_drops);
        alarm_charge_rises   = res.getString(R.string.alarm_charge_rises);
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

    public String for_n_hours(int n) {
        return String.format(res.getQuantityString(R.plurals.for_n_hours, n), n);
    }

    public String n_hours_m_minutes_long(int n, int m) {
        return (String.format(res.getQuantityString(R.plurals.n_hours_long, n), n) +
                String.format(res.getQuantityString(R.plurals.n_minutes_long, m), m));
    }

    public String n_minutes_long(int n) {
        return String.format(res.getQuantityString(R.plurals.n_minutes_long, n), n);
    }

    public String n_hours_m_minutes_medium(int n, int m) {
        return (String.format(res.getQuantityString(R.plurals.n_hours_medium, n), n) +
                String.format(res.getQuantityString(R.plurals.n_minutes_medium, m), m));
    }

    public String n_hours_long_m_minutes_medium(int n, int m) {
        return (String.format(res.getQuantityString(R.plurals.n_hours_long, n), n) +
                String.format(res.getQuantityString(R.plurals.n_minutes_medium, m), m));
    }

    public String n_hours_m_minutes_short(int n, int m) {
        return (String.format(res.getQuantityString(R.plurals.n_hours_short, n), n) +
                String.format(res.getQuantityString(R.plurals.n_minutes_short, m), m));
    }

    public String n_days_m_hours(int n, int m) {
        return (String.format(res.getQuantityString(R.plurals.n_days, n), n) +
                String.format(res.getQuantityString(R.plurals.n_hours, m), m));
    }

    public String n_log_items(int n) {
        return String.format(res.getQuantityString(R.plurals.n_log_items, n), n);
    }

    /* temperature is the integer number of tenths of degrees Celcius, as returned by BatteryManager */
    public String formatTemp(int temperature, boolean convertF, boolean includeTenths) {
        double d;
        String s;

        if (convertF){
            d = java.lang.Math.round(temperature * 9 / 5.0) / 10.0 + 32.0;
            s = degree_symbol + fahrenheit_symbol;
        } else {
            d = temperature / 10.0;
            s = degree_symbol + celsius_symbol;
        }

        // Weird: the ternary operator seems to compile down to a "function" that has to return a single particular type
        //return "" + (includeTenths ? d : java.lang.Math.round(d)) + s;
        return (includeTenths ? String.valueOf(d) : String.valueOf(java.lang.Math.round(d))) + s;
    }

    public String formatTemp(int temperature, boolean convertF) {
        return formatTemp(temperature, convertF, true);
    }

    public String formatVoltage(int voltage) {
        return String.valueOf(voltage / 1000.0) + volt_symbol;
    }

    public static int indexOf(String[] a, String key) {
        for (int i=0, size=a.length; i < size; i++)
            if (key.equals(a[i])) return i;

        return -1;
    }

    public static void overrideLanguage(Resources res, WindowManager wm, String lang_override) {
        android.content.res.Configuration conf = res.getConfiguration();
        if (! lang_override.equals("default")) {
            conf.locale = SettingsActivity.codeToLocale(lang_override);
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            res.updateConfiguration(conf, metrics);
        } else {
            /* TODO: Somehow set to system default */
            /* Perhaps showing a confirmation dialog, saying the app needs to close in order for change to take effect.
               You'd actually do that from SettingsActivity, so execution would never actually get here. */
        }
    }

    public android.text.Spanned timeRemaining(BatteryInfo info) {
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
    public android.text.Spanned timeRemainingMainScreen(BatteryInfo info) {
        if (info.prediction.what == BatteryInfo.Prediction.NONE)
            return android.text.Html.fromHtml("&nbsp;&nbsp;&nbsp;&mdash;&nbsp;&nbsp;&nbsp;");
        else
            return timeRemaining(info);
    }

    public String untilWhat(BatteryInfo info) {
        if (info.prediction.what == BatteryInfo.Prediction.NONE)
            return "";
        else if (info.prediction.what == BatteryInfo.Prediction.UNTIL_CHARGED)
            return res.getString(R.string.activity_until_charged);
        else
            return res.getString(R.string.activity_until_drained);
    }
}
