/*
    Copyright (c) 2012-2016 Darshan-Josiah Barber

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

public class PredictorCore {
    public static final int DISCHARGE    = 0;
    public static final int RECHARGE_AC  = 1;
    public static final int RECHARGE_WL  = 2;
    public static final int RECHARGE_USB = 3;

    public static final int ONE_MINUTE      = 60 * 1000;
    public static final int FIVE_MINUTES    = ONE_MINUTE *  5;
    public static final int TEN_MINUTES     = ONE_MINUTE * 10;
    public static final int FIFTEEN_MINUTES = ONE_MINUTE * 15;
    public static final int THIRTY_MINUTES  = ONE_MINUTE * 30;
    public static final int ONE_HOUR        = ONE_MINUTE * 60;
    public static final int TWO_HOURS       = ONE_HOUR *  2;
    public static final int THREE_HOURS     = ONE_HOUR *  3;
    public static final int FOUR_HOURS      = ONE_HOUR *  4;
    public static final int SIX_HOURS       = ONE_HOUR *  6;
    public static final int EIGHT_HOURS     = ONE_HOUR *  8;
    public static final int TWELVE_HOURS    = ONE_HOUR * 12;
    public static final int ONE_DAY         = ONE_HOUR * 24;

    public static final int SINCE_STATUS_CHANGE = -1;
    public static final int LONG_TERM = -2;
    public static final int AUTOMAGIC = -3;
    //public static final int WEIGHTED_FIVE = -4;

    private static final int MIN_PREDICTION = ONE_MINUTE;

    private static final double WEIGHT_OLD_AVERAGE = 0.998;
    private static final double WEIGHT_NEW_DATA =  1 - WEIGHT_OLD_AVERAGE;

    private static final int[] DEFAULT = { 24 * 60 * 60 * 1000 / 100,
                                            3 * 60 * 60 * 1000 / 100,
                                            4 * 60 * 60 * 1000 / 100,
                                            6 * 60 * 60 * 1000 / 100 };

    private int prediction_type = AUTOMAGIC;

    private long[] timestamps = new long[101];
    private int ts_head;

    private double[] average = new double[4];

    private BatteryInfo cur_info;
    private int last_level;
    private int last_status = -1; // Impossible value, so first update knows it's the first update
    private int last_plugged;
    private long last_prediction;
    private double last_recent_average;
    private int dir_inc; // -1 if charging; 1 if discharging; unspecified otherwise. For iterating over timestamps.
    private long now;
    private boolean use_partial;
    private boolean initial;

    public int cur_charging_status; // TODO make getters and make private

    public PredictorCore(float ave_discharge, float ave_recharge_ac, float ave_recharge_wl, float ave_recharge_usb) {
        average[DISCHARGE]    = ave_discharge    == -1 ? DEFAULT[DISCHARGE]    : ave_discharge;
        average[RECHARGE_AC]  = ave_recharge_ac  == -1 ? DEFAULT[RECHARGE_AC]  : ave_recharge_ac;
        average[RECHARGE_WL]  = ave_recharge_wl  == -1 ? DEFAULT[RECHARGE_WL]  : ave_recharge_wl;
        average[RECHARGE_USB] = ave_recharge_usb == -1 ? DEFAULT[RECHARGE_USB] : ave_recharge_usb;
    }

    public void setPredictionType(int type) {
        if (type == prediction_type)
            return;

        prediction_type = type;

        if (cur_info == null)
            return;

        use_partial = false;
        last_prediction = prediction();

        if (timestamps[last_level] != now && shouldUsePartial()) {
            use_partial = true;
            last_prediction = prediction();
        }

        saveLastPredictionToInfo();
    }

    public void update(BatteryInfo info, long when) {
        if (info.status == BatteryInfo.STATUS_UNKNOWN)
            return;

        cur_info = info;
        cur_charging_status = chargingStatusForCurInfo();
        now = when;

        if (last_prediction < now + MIN_PREDICTION) {
            last_prediction = now + MIN_PREDICTION;
            info.prediction.update(last_prediction);
        }

        if (info.status != last_status ||
            info.plugged != last_plugged ||
            info.status == BatteryInfo.STATUS_FULLY_CHARGED ||
            (info.status == BatteryInfo.STATUS_CHARGING && info.percent < ts_head) ||
            (info.status == BatteryInfo.STATUS_UNPLUGGED && info.percent > ts_head))
        {
            initial = true;
            use_partial = false;

            ts_head = info.percent;
            dir_inc = info.status == BatteryInfo.STATUS_CHARGING ? -1 : 1;

            timestamps[info.percent] = now;

            updateInfoPrediction();
            return;
        }

        if ((info.status == BatteryInfo.STATUS_CHARGING && info.percent < last_level) ||
            (info.status != BatteryInfo.STATUS_CHARGING && info.percent > last_level))
        {
            use_partial = false;
            timestamps[info.percent] = now;
            updateInfoPrediction();
            return;
        }

        int level_diff = Math.abs(last_level - info.percent);

        if (level_diff == 0) {
            if (shouldUsePartial())
                use_partial = true;
            else
                return;
        } else {
            use_partial = false;
            double ms_diff = (double) (now - timestamps[last_level]);
            double ms_per_point = ms_diff / level_diff;

            for (int i = 0; i < level_diff; i += 1)
                timestamps[info.percent + (i * dir_inc)] = now - (long) (i * ms_per_point);

            // Initial level change may happen promptly and should not shorten prediction
            if (initial && ms_per_point < last_recent_average) {
                initial = false;
                ts_head = info.percent;
                setLasts();
                return;
            }

            initial = false;

            for (int i = 0; i < level_diff; i++)
                average[cur_charging_status] = average[cur_charging_status] * WEIGHT_OLD_AVERAGE + ms_per_point * WEIGHT_NEW_DATA;
        }

        updateInfoPrediction();
    }

    public double getLongTermAverage() {
        return average[cur_charging_status];
    }

    private boolean shouldUsePartial() {
        if (use_partial) return true;

        double ms_diff = (double) (now - timestamps[last_level]);
        if (ms_diff <= last_recent_average) return false;
        if (predictionIfPartial() <= last_prediction) return false;
        return true;
    }

    private long predictionIfPartial() {
        return predictionIfPartialIs(true);
    }

    private long predictionIfPartialIs(boolean supposed) {
        boolean old_partial = use_partial;
        use_partial = supposed;
        long ret = prediction();
        use_partial = old_partial;
        return ret;
    }

    private void updateInfoPrediction() {
        last_prediction = prediction();

        saveLastPredictionToInfo();
        setLasts();
    }

    private void saveLastPredictionToInfo() {
        if (last_prediction < now + MIN_PREDICTION)
            last_prediction = now + MIN_PREDICTION;

        cur_info.prediction.update(last_prediction);
    }

    private long prediction() {
        if (cur_info.status == BatteryInfo.STATUS_CHARGING)
            return whenCharged();
        else if (cur_info.status == BatteryInfo.STATUS_UNPLUGGED)
            return whenDrained();
        else
            return 0;
    }

    private long whenDrained() {
        int level = cur_info.percent;
        long from = timestamps[cur_info.percent];

        if (use_partial) {
            level -= dir_inc;
            from = now;
        }

        return from + (long) (recentAverage() * level);
    }

    private long whenCharged() {
        int level = cur_info.percent;
        long from = timestamps[cur_info.percent];

        if (use_partial) {
            level -= dir_inc;
            from = now;
        }

        return from + (long) ((101 - level) * recentAverage());
    }

    private void setLasts() {
        last_level = cur_info.percent; // TODO: Resolve level/percent discrepancy?
        last_status = cur_info.status;
        last_plugged = cur_info.plugged;
        last_recent_average = recentAverage();
    }

    private double recentAverage() {
        if (prediction_type > 100)
            return recentAverageByTime(prediction_type);
        else if (prediction_type > 0)
            return recentAverageByPoints(prediction_type);
        else if (prediction_type == SINCE_STATUS_CHANGE)
            //return recentAverageByPoints(Math.abs(ts_head - cur_info.percent));
            return recentAverageBySession();
        else if (prediction_type == AUTOMAGIC)
            return middleOf(recentAverageByTime(FIVE_MINUTES), recentAverageByPoints(5), average[cur_charging_status]);
        //else if (prediction_type == WEIGHTED_FIVE)
        //    return weightedAverageFivePoints();
        else //if (prediction_type == LONG_TERM)
            return average[cur_charging_status];
    }

    private double middleOf(double first, double second, double third) {
        if ((first >= second && second >= third) || (third >= second && second >= first))
            return second;
        else if ((second >= first && first >= third) || (third >= first && first >= second))
            return first;
        else
            return third;
    }

/*    private double recents[] = new double[5]; // Don't want to allocate every time
    private static final double recencyWeights[]  = {0.39, 0.27, 0.18, 0.11, 0.05}; // Most recent to least recent
    //private static final double durationWeights[] = {0.09, 0.13, 0.18, 0.25, 0.30}; // Shortest to longest
    private double weightedAverageFivePoints() {
        double total_ms = 0d;
        double needed_points = recents.length;

        int start = cur_info.percent;
        if (use_partial) start -= dir_inc;

        for (int i = start, ri = 0; ri < recents.length; ri++, i += dir_inc) {
            if (i >= ts_head || i < 0)
                recents[ri] = average[cur_charging_status];
            else if (i == start && use_partial)
                recents[ri] = now - timestamps[cur_info.percent];
            else
                recents[ri] = timestamps[i] - timestamps[i + dir_inc];
        }

        double average = 0.0;
        for (int ri = 0; ri < recents.length; ri++)
            average += recents[ri] * recencyWeights[ri];

        //java.util.Arrays.sort(recents);

        //for (int ri = 0; ri < recents.length; ri++)
        //    average += recents[ri] * durationWeights[ri];

        return average;// / 2.0;
    }
*/
    private double recentAverageByTime(double duration_in_ms) {
        double total_points = 0d;
        double total_ms = 0d;
        double needed_ms = duration_in_ms;

        int start = cur_info.percent;
        if (use_partial) start -= dir_inc;

        for (int i = start; i != ts_head; i += dir_inc) {
            double potential_ms;

            if ((i == start && use_partial) || (i + dir_inc > 100) || (i + dir_inc < 0))
                potential_ms = now - timestamps[cur_info.percent];
            else
                potential_ms = timestamps[i] - timestamps[i + dir_inc];

            if (potential_ms > needed_ms) {
                total_points += needed_ms / potential_ms;
                total_ms += needed_ms;
                needed_ms = 0;
                break;
            }

            total_points += 1;
            total_ms += potential_ms;
            needed_ms -= potential_ms;
        }

        if (needed_ms > 0)
            total_points += needed_ms / average[cur_charging_status];

        return duration_in_ms / total_points;
    }

    private double recentAverageByPoints(double duration_in_points) {
        double total_ms = 0d;
        double needed_points = duration_in_points;

        int start = cur_info.percent;
        if (use_partial) start -= dir_inc;

        if (start == ts_head || needed_points < 1)
            return average[cur_charging_status];

        for (int i = start; i != ts_head && needed_points > 0; i += dir_inc) {
            double new_ms;

            if ((i == start && use_partial) || (i + dir_inc > 100) || (i + dir_inc < 0))
                new_ms = now - timestamps[cur_info.percent];
            else
                new_ms = timestamps[i] - timestamps[i + dir_inc];

            total_ms += new_ms;
            needed_points -= 1;
        }

        if (needed_points > 0)
            total_ms += needed_points * average[cur_charging_status];

        return total_ms / duration_in_points;
    }

    private double recentAverageBySession() {
        double total_ms = 0d;
        double total_points = 0d;

        if (use_partial) {
            total_ms += now - timestamps[cur_info.percent];
            total_points += 1;
        }

        total_ms += timestamps[cur_info.percent] - timestamps[ts_head];
        total_points += ts_head - cur_info.percent;

        if (total_points < 1)
            return average[cur_charging_status];

        return total_ms / total_points;
    }

    private int chargingStatusForCurInfo() {
        if (cur_info.status == BatteryInfo.STATUS_CHARGING) {
            if (cur_info.plugged == BatteryInfo.PLUGGED_USB)
                return RECHARGE_USB;
            else if (cur_info.plugged == BatteryInfo.PLUGGED_WIRELESS)
                return RECHARGE_WL;
            else
                return RECHARGE_AC;
        } else {
            return DISCHARGE;
        }
    }
}
