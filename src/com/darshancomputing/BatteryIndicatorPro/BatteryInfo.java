/*
    Copyright (c) 2015 Darshan-Josiah Barber

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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;

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
    private static final String FIELD_PREDICTION_WHEN = "prediction_when";

    private static final String BUILD_MODEL = android.os.Build.MODEL.toLowerCase(java.util.Locale.ENGLISH);

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

        public int what;
        public long when;
        public RelativeTime last_rtime = new RelativeTime();

        private static final int MIN_PREDICTION = 60 * 1000;

        public void update(long ts) {
            when = ts;

            if (status == STATUS_FULLY_CHARGED) what = NONE;
            else if (status == STATUS_CHARGING) what = UNTIL_CHARGED;
            else                                what = UNTIL_DRAINED;
        }

        public void updateRelativeTime() {
            long now = SystemClock.elapsedRealtime();

            if (when < now + MIN_PREDICTION)
                when = now + MIN_PREDICTION;

            last_rtime.update(when, now);
        }
    }

    public static class RelativeTime {
        public int days, hours, minutes;

        // If days > 0, then minutes is undefined and hours is rounded to the closest hour (rounding minutes up or down)
        public void update(long to, long from) {
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

    public void load(Intent intent, SharedPreferences sp_store) {
        load(intent);
        load(sp_store);
    }

    public void load(Intent intent) {
        int level = intent.getIntExtra(EXTRA_LEVEL, 50);
        int scale = intent.getIntExtra(EXTRA_SCALE, 100);

        status = intent.getIntExtra(EXTRA_STATUS, STATUS_UNKNOWN);
        health = intent.getIntExtra(EXTRA_HEALTH, HEALTH_UNKNOWN);
        plugged = intent.getIntExtra(EXTRA_PLUGGED, PLUGGED_UNKNOWN);
        temperature = intent.getIntExtra(EXTRA_TEMPERATURE, 0);
        voltage = intent.getIntExtra(EXTRA_VOLTAGE, 0);
        //technology = intent.getStringExtra(EXTRA_TECHNOLOGY);

        percent = level * 100 / scale;
        percent = attemptOnePercentHack(percent);
        if (percent > 100) percent = 100;

        // Treat unplugged plugged as unpluggged status
        if (plugged == PLUGGED_UNPLUGGED) status = STATUS_UNPLUGGED;

        if (status  > STATUS_MAX) { status  = STATUS_UNKNOWN; }
        if (health  > HEALTH_MAX) { health  = HEALTH_UNKNOWN; }
        if (plugged > PLUGGED_MAX){ plugged = PLUGGED_UNKNOWN; }

        if (last_status_cTM == 0) { // Brand new BatteryInfo
            last_status  = status;
            last_plugged = plugged;
            last_percent = percent;
            last_status_cTM = System.currentTimeMillis();
        }
    }

    public void load(SharedPreferences sp_store) {
        last_status = sp_store.getInt(KEY_LAST_STATUS, status);
        last_plugged = sp_store.getInt(KEY_LAST_PLUGGED, plugged);
        last_status_cTM = sp_store.getLong(KEY_LAST_STATUS_CTM, System.currentTimeMillis());
        last_percent = sp_store.getInt(KEY_LAST_PERCENT, percent);
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();

        bundle.putInt(FIELD_PERCENT, percent);
        bundle.putInt(FIELD_STATUS, status);
        bundle.putInt(FIELD_HEALTH, health);
        bundle.putInt(FIELD_PLUGGED, plugged);
        bundle.putInt(FIELD_TEMPERATURE, temperature);
        bundle.putInt(FIELD_VOLTAGE, voltage);
        bundle.putInt(FIELD_LAST_STATUS, last_status);
        bundle.putInt(FIELD_LAST_PLUGGED, last_plugged);
        bundle.putInt(FIELD_LAST_PERCENT, last_percent);

        bundle.putLong(FIELD_LAST_STATUS_CTM, last_status_cTM);

        bundle.putInt(FIELD_PREDICTION_DAYS, prediction.last_rtime.days);
        bundle.putInt(FIELD_PREDICTION_HOURS, prediction.last_rtime.hours);
        bundle.putInt(FIELD_PREDICTION_MINUTES, prediction.last_rtime.minutes);

        bundle.putInt( FIELD_PREDICTION_WHAT, prediction.what);
        bundle.putLong(FIELD_PREDICTION_WHEN, prediction.when);

        return bundle;
    }

    public void loadBundle(Bundle bundle) {
        percent = bundle.getInt(FIELD_PERCENT);
        status = bundle.getInt(FIELD_STATUS);
        health = bundle.getInt(FIELD_HEALTH);
        plugged = bundle.getInt(FIELD_PLUGGED);
        temperature = bundle.getInt(FIELD_TEMPERATURE);
        voltage = bundle.getInt(FIELD_VOLTAGE);
        last_status = bundle.getInt(FIELD_LAST_STATUS);
        last_plugged = bundle.getInt(FIELD_LAST_PLUGGED);
        last_percent = bundle.getInt(FIELD_LAST_PERCENT);

        last_status_cTM = bundle.getLong(FIELD_LAST_STATUS_CTM);

        prediction.last_rtime.days = bundle.getInt(FIELD_PREDICTION_DAYS);
        prediction.last_rtime.hours = bundle.getInt(FIELD_PREDICTION_HOURS);
        prediction.last_rtime.minutes = bundle.getInt(FIELD_PREDICTION_MINUTES);

        prediction.what = bundle.getInt( FIELD_PREDICTION_WHAT);
        prediction.when = bundle.getLong(FIELD_PREDICTION_WHEN);
    }

    private static int attemptOnePercentHack(int percent) {
        File hack_file = new File("/sys/class/power_supply/battery/charge_counter");

        if (hack_file.exists()) {
            try {
                java.io.FileReader fReader = new java.io.FileReader(hack_file);
                java.io.BufferedReader bReader = new java.io.BufferedReader(fReader, 8);
                String line = bReader.readLine();
                bReader.close();

                int charge_counter = Integer.valueOf(line);

                if (charge_counter < percent + 10 && charge_counter > percent - 10) {
                    if (charge_counter > 100) // This happens
                        charge_counter = 100;

                    if (charge_counter < 0)   // This could happen?
                        charge_counter = 0;

                    percent = charge_counter;
                } else {
                    /* The Log messages are only really useful to me and might as well be left hardwired here in English. */
                    Log.e(LOG_TAG, "charge_counter file exists but with value " + charge_counter +
                          " which is inconsistent with percent: " + percent);
                }
            } catch (java.io.FileNotFoundException e) {
                Log.e(LOG_TAG, "charge_counter file doesn't exist");
                e.printStackTrace();
            } catch (java.io.IOException e) {
                Log.e(LOG_TAG, "Error reading charge_counter file");
                e.printStackTrace();
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, "Read charge_counter file but couldn't convert contents to int");
                e.printStackTrace();
            }
        }

        return percent;
    }

    // Based on CurrentReaderFactory.java from CurrentWidget by Ran Manor (GPL v3)
    public static Long attemptCurrentHack(){
        File f;

        // Galaxy S3
        if (BUILD_MODEL.contains("gt-i9300")
            || BUILD_MODEL.contains("gt-i9300T")
            || BUILD_MODEL.contains("gt-i9305")
            || BUILD_MODEL.contains("gt-i9305N")
            || BUILD_MODEL.contains("gt-i9305T")
            || BUILD_MODEL.contains("shv-e210k")
            || BUILD_MODEL.contains("shv-e210l")
            || BUILD_MODEL.contains("shv-e210s")
            || BUILD_MODEL.contains("sgh-t999")
            || BUILD_MODEL.contains("sgh-t999l")
            || BUILD_MODEL.contains("sgh-t999v")
            || BUILD_MODEL.contains("sgh-i747")
            || BUILD_MODEL.contains("sgh-i747m")
            || BUILD_MODEL.contains("sgh-n064")
            || BUILD_MODEL.contains("sc-06d")
            || BUILD_MODEL.contains("sgh-n035")
            || BUILD_MODEL.contains("sc-03e")
            || BUILD_MODEL.contains("SCH-j021")
            || BUILD_MODEL.contains("scl21")
            || BUILD_MODEL.contains("sch-r530")
            || BUILD_MODEL.contains("sch-i535")
            || BUILD_MODEL.contains("sch-S960l")
            || BUILD_MODEL.contains("gt-i9308")
            || BUILD_MODEL.contains("sch-i939")
            || BUILD_MODEL.contains("sch-s968c")) {
            f = new File("/sys/class/power_supply/battery/current_max");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }

        if (BUILD_MODEL.contains("nexus 7")
            || BUILD_MODEL.contains("one")
            || BUILD_MODEL.contains("lg-d851")) {
            f = new File("/sys/class/power_supply/battery/current_now");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }

        if (BUILD_MODEL.contains("sl930")) {
            f = new File("/sys/class/power_supply/da9052-bat/current_avg");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }

        // Galaxy S4
        if (BUILD_MODEL.contains("sgh-i337")
            || BUILD_MODEL.contains("gt-i9505")
            || BUILD_MODEL.contains("gt-i9500")
            || BUILD_MODEL.contains("sch-i545")
            || BUILD_MODEL.contains("find 5")
            || BUILD_MODEL.contains("sgh-m919")
            || BUILD_MODEL.contains("sgh-i537")) {
            f = new File("/sys/class/power_supply/battery/current_now");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }

        if (BUILD_MODEL.contains("cynus")) {
            f = new File(
                         "/sys/devices/platform/mt6329-battery/FG_Battery_CurrentConsumption");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }
        // Zopo Zp900, etc.
        if (BUILD_MODEL.contains("zp900")
            || BUILD_MODEL.contains("jy-g3")
            || BUILD_MODEL.contains("zp800")
            || BUILD_MODEL.contains("zp800h")
            || BUILD_MODEL.contains("zp810")
            || BUILD_MODEL.contains("w100")
            || BUILD_MODEL.contains("zte v987")) {
            f = new File(
                         "/sys/class/power_supply/battery/BatteryAverageCurrent");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }

        // Samsung Galaxy Tab 2
        if (BUILD_MODEL.contains("gt-p31")
            || BUILD_MODEL.contains("gt-p51")) {
            f = new File("/sys/class/power_supply/battery/current_avg");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }

        // HTC One X
        if (BUILD_MODEL.contains("htc one x")) {
            f = new File("/sys/class/power_supply/battery/batt_attr_text");
            if (f.exists()) {
                Long value = CurrentHackBattAttrTextReader.getValue(f, "I_MBAT", "I_MBAT");
                if (value != null)
                    return value;
            }
        }

        // wildfire S
        if (BUILD_MODEL.contains("wildfire s")) {
            f = new File("/sys/class/power_supply/battery/smem_text");
            if (f.exists()) {
                Long value = CurrentHackBattAttrTextReader.getValue(f, "eval_current",
                                                         "batt_current");
                if (value != null)
                    return value;
            }
        }

        // trimuph with cm7, lg ls670, galaxy s3, galaxy note 2
        if (BUILD_MODEL.contains("triumph")
            || BUILD_MODEL.contains("ls670")
            || BUILD_MODEL.contains("gt-i9300")
            || BUILD_MODEL.contains("sm-n9005")
            || BUILD_MODEL.contains("gt-n7100")
            || BUILD_MODEL.contains("sgh-i317")) {
            f = new File("/sys/class/power_supply/battery/current_now");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }

        // htc desire hd / desire z / inspire?
        // htc evo view tablet
        if (BUILD_MODEL.contains("desire hd")
            || BUILD_MODEL.contains("desire z")
            || BUILD_MODEL.contains("inspire")
            || BUILD_MODEL.contains("pg41200")) {
            f = new File("/sys/class/power_supply/battery/batt_current");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }

        // nexus one cyangoenmod
        f = new File("/sys/devices/platform/ds2784-battery/getcurrent");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, true);
        }

        // sony ericsson xperia x1
        f = new File(
                     "/sys/devices/platform/i2c-adapter/i2c-0/0-0036/power_supply/ds2746-battery/current_now");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, false);
        }

        // xdandroid
        /* if (Build.MODEL.equalsIgnoreCase("MSM")) { */
        f = new File(
                     "/sys/devices/platform/i2c-adapter/i2c-0/0-0036/power_supply/battery/current_now");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, false);
        }
        /* } */

        // droid eris
        f = new File("/sys/class/power_supply/battery/smem_text");
        if (f.exists()) {
            Long value = CurrentHackSMTextReader.getValue();
            if (value != null)
                return value;
        }

        // htc sensation / evo 3d
        f = new File("/sys/class/power_supply/battery/batt_attr_text");
        if (f.exists()) {
            Long value = CurrentHackBattAttrTextReader.getValue(f,
                                                     "batt_discharge_current", "batt_current");
            if (value != null)
                return value;
        }

        // some htc devices
        f = new File("/sys/class/power_supply/battery/batt_current");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, false);
        }

        // Nexus One.
        // TODO: Make this not default but specific for N1 because of the normalization.
        f = new File("/sys/class/power_supply/battery/current_now");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, true);
        }

        // samsung galaxy vibrant
        f = new File("/sys/class/power_supply/battery/batt_chg_current");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, false);

        // sony ericsson x10
        f = new File("/sys/class/power_supply/battery/charger_current");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, false);

        // Nook Color
        f = new File("/sys/class/power_supply/max17042-0/current_now");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, false);

        // Xperia Arc
        f = new File("/sys/class/power_supply/bq27520/current_now");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, true);

        // Motorola Atrix
        f = new File(
                     "/sys/devices/platform/cpcap_battery/power_supply/usb/current_now");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, false);

        // Acer Iconia Tab A500
        f = new File("/sys/EcControl/BatCurrent");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, false);

        // charge current only, Samsung Note
        f = new File("/sys/class/power_supply/battery/batt_current_now");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, false);

        // galaxy note, galaxy s2
        f = new File("/sys/class/power_supply/battery/batt_current_adc");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, false);

        // intel
        f = new File("/sys/class/power_supply/max170xx_battery/current_now");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, true);

        // Sony Xperia U
        f = new File("/sys/class/power_supply/ab8500_fg/current_now");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, true);

        f = new File("/sys/class/power_supply/android-battery/current_now");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, false);
        }

        // Nexus 10, 4.4.
        f = new File("/sys/class/power_supply/ds2784-fuelgauge/current_now");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, true);
        }

        f = new File("/sys/class/power_supply/Battery/current_now");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, false);
        }

        return null;
    }

    // Based on OneLineReader.java from CurrentWidget by Ran Manor (GPL v3)
    private static class CurrentHackNormalFileReader {
        public static Long getValue(File f, boolean convertToMillis) {
            String line = null;
            Long value = null;

            try {
                java.io.FileReader fReader = new java.io.FileReader(f);
                java.io.BufferedReader bReader = new java.io.BufferedReader(fReader, 10);
                line = bReader.readLine();
                bReader.close();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error reading normal current hack file");
                e.printStackTrace();
            }

            if (line != null) {
                try {
                    value = Long.parseLong(line);
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, "Error parsing normal current hack file");
                }

                if (convertToMillis && value != null)
                    value = value / 1000;
            }

            return value;
        }
    }

    // Based on BattAttrTextReader.java from CurrentWidget by Ran Manor (GPL v3)
    private static class CurrentHackBattAttrTextReader {
        public static Long getValue(File f, String dischargeField, String chargeField) {
            String text;
            Long value = null;

            try {
                java.io.FileReader fReader = new java.io.FileReader(f);
                java.io.BufferedReader bReader = new java.io.BufferedReader(fReader);
                String line = bReader.readLine();

                final String chargeFieldHead = chargeField + ": ";
                final String dischargeFieldHead = dischargeField + ": ";

                while (line != null) {
                    if (line.contains(chargeField)) {
                        text = line.substring(line.indexOf(chargeFieldHead) + chargeFieldHead.length());
                        try {
                            value = Long.parseLong(text);
                            if (value != 0)
                                break;
                        } catch (NumberFormatException e) {
                            Log.e(LOG_TAG, "Error parsing BattAttr current hack file");
                        }
                    }

                    //  "batt_discharge_current:"
                    if (line.contains(dischargeField)) {
                        text = line.substring(line.indexOf(dischargeFieldHead) + dischargeFieldHead.length());
                        try {
                            value = (-1)*Math.abs(Long.parseLong(text));
                        } catch (NumberFormatException e) {
                            Log.e(LOG_TAG, "Error parsing BattAttr current hack file");
                        }

                        break;
                    }

                    line = bReader.readLine();
                }

                bReader.close();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error reading BattAttr current hack file");
                e.printStackTrace();
            }

            return value;
        }
    }

    // Based on SMTextReader.java from CurrentWidget by Ran Manor (GPL v3)
    private static class CurrentHackSMTextReader {
        public static Long getValue() {
            boolean success = false;
            String text = null;
            Long value = null;

            try {
                java.io.FileReader fReader = new java.io.FileReader("/sys/class/power_supply/battery/smem_text");
                java.io.BufferedReader bReader = new java.io.BufferedReader(fReader);
                String line = bReader.readLine();

                while (line != null) {
                    if (line.contains("I_MBAT")) {
                        text = line.substring(line.indexOf("I_MBAT: ") + 8);
                        success = true;
                        break;
                    }

                    line = bReader.readLine();
                }

                bReader.close();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error reading SMText current hack file");
                e.printStackTrace();
            }

            if (success) {
                try {
                    value = Long.parseLong(text);
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, "Error parsing SMText current hack file");
                }
            }

            return value;
        }
    }
}
