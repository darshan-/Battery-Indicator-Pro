/*
    Copyright (c) 2015-2016 Darshan-Josiah Barber

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

/* This file is largely based on CurrentWidget by Ran Manor (GPL v3) */

package com.darshancomputing.BatteryIndicatorPro;

import android.content.Context;
import android.os.BatteryManager;
import android.util.Log;

import java.io.File;

// Initially based on CurrentReaderFactory.java from CurrentWidget by Ran Manor (GPL v3)
class CurrentHack {
    private static final String LOG_TAG = "com.darshancomputing.BatteryIndicatorPro - CurrentHack";
    private static final String BUILD_MODEL = android.os.Build.MODEL.toLowerCase(java.util.Locale.ENGLISH);

    public static final int HACK_METHOD_NONE = -1;
    public static final int HACK_METHOD_BOTH = 0;
    public static final int HACK_METHOD_FILE_SYSTEM = 1;
    public static final int HACK_METHOD_BATTERY_MANAGER = 2;

    private static BatteryManager batteryManager;
    private static boolean preferFS = false;
    private static int method = HACK_METHOD_NONE;

    private static CurrentHack instance;

    protected CurrentHack(Context c) {
        Context context = c.getApplicationContext();
        if (android.os.Build.VERSION.SDK_INT >= 21)
            batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
    }

    public static CurrentHack getInstance(Context c) {
        if (instance == null)
            instance = new CurrentHack(c);

        return instance;
    }

    public static void setPreferFS(boolean pfs) {
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

    public static int getHackMethodsAvailable() {
        boolean fs = false, bm = false;

        if (getBMCurrent() != null)
            bm = true;

        if (getFSCurrent() != null)
            fs = true;

        if (bm && fs)
            return HACK_METHOD_BOTH;

        if (bm)
            return HACK_METHOD_BATTERY_MANAGER;

        if (fs)
            return HACK_METHOD_FILE_SYSTEM;

        return HACK_METHOD_NONE;
    }

    public static Long getCurrent() {
        if (method == HACK_METHOD_NONE)
            return null;

        if (method == HACK_METHOD_FILE_SYSTEM)
            return getFSCurrent();

        return getBMCurrent();
    }

    public static Long getAvgCurrent() {
        if (method == HACK_METHOD_NONE)
            return null;

        if (method == HACK_METHOD_FILE_SYSTEM)
            return getFSAvgCurrent();

        return getBMAvgCurrent();
    }

    private static Long getBMCurrent() {
        if (android.os.Build.VERSION.SDK_INT < 21)
            return null;

        int current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

        if (current > Integer.MIN_VALUE)
            return Long.valueOf(current) / 1000;
        else
            return null;
    }

    private static Long getBMAvgCurrent() {
        if (android.os.Build.VERSION.SDK_INT < 21)
            return null;

        int current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);

        if (current > Integer.MIN_VALUE)
            return Long.valueOf(current) / 1000;
        else
            return null;
    }

    // This usually returns an instantaneous reading of the current (current_now), but
    //  in some cases is probably a recent average.
    private static Long getFSCurrent() {
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
            || (BUILD_MODEL.contains("one") && !BUILD_MODEL.contains("nexus"))
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
            f = new File("/sys/devices/platform/mt6329-battery/FG_Battery_CurrentConsumption");
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
            f = new File("/sys/class/power_supply/battery/BatteryAverageCurrent");
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

        // nexus one cyanogenmod
        f = new File("/sys/devices/platform/ds2784-battery/getcurrent");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, true);
        }

        // sony ericsson xperia x1
        f = new File("/sys/devices/platform/i2c-adapter/i2c-0/0-0036/power_supply/ds2746-battery/current_now");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, false);
        }

        // xdandroid
        /* if (Build.MODEL.equalsIgnoreCase("MSM")) { */
        f = new File("/sys/devices/platform/i2c-adapter/i2c-0/0-0036/power_supply/battery/current_now");
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

    private static Long getFSAvgCurrent() {
        File f;
        if (BUILD_MODEL.contains("nexus 7")
            || (BUILD_MODEL.contains("one") && !BUILD_MODEL.contains("nexus"))
            || BUILD_MODEL.contains("lg-d851")) {
            f = new File("/sys/class/power_supply/battery/current_avg");
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
            f = new File("/sys/class/power_supply/battery/current_avg");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }

        // trimuph with cm7, lg ls670, galaxy s3, galaxy note 2
        if (BUILD_MODEL.contains("triumph")
            || BUILD_MODEL.contains("ls670")
            || BUILD_MODEL.contains("gt-i9300")
            || BUILD_MODEL.contains("sm-n9005")
            || BUILD_MODEL.contains("gt-n7100")
            || BUILD_MODEL.contains("sgh-i317")) {
            f = new File("/sys/class/power_supply/battery/current_avg");
            if (f.exists()) {
                return CurrentHackNormalFileReader.getValue(f, false);
            }
        }

        // sony ericsson xperia x1
        f = new File("/sys/devices/platform/i2c-adapter/i2c-0/0-0036/power_supply/ds2746-battery/current_avg");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, false);
        }

        // xdandroid
        /* if (Build.MODEL.equalsIgnoreCase("MSM")) { */
        f = new File("/sys/devices/platform/i2c-adapter/i2c-0/0-0036/power_supply/battery/current_avg");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, false);
        }
        /* } */

        // Nexus One.
        // TODO: Make this not default but specific for N1 because of the normalization.
        f = new File("/sys/class/power_supply/battery/current_avg");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, true);
        }

        // Nook Color
        f = new File("/sys/class/power_supply/max17042-0/current_avg");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, false);

        // Xperia Arc
        f = new File("/sys/class/power_supply/bq27520/current_avg");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, true);

        // Motorola Atrix
        f = new File(
                     "/sys/devices/platform/cpcap_battery/power_supply/usb/current_avg");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, false);

        f = new File("/sys/class/power_supply/max170xx_battery/current_avg");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, true);

        // Sony Xperia U
        f = new File("/sys/class/power_supply/ab8500_fg/current_avg");
        if (f.exists())
            return CurrentHackNormalFileReader.getValue(f, true);

        f = new File("/sys/class/power_supply/android-battery/current_avg");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, false);
        }

        // Nexus 10, 4.4.
        f = new File("/sys/class/power_supply/ds2784-fuelgauge/current_avg");
        if (f.exists()) {
            return CurrentHackNormalFileReader.getValue(f, true);
        }

        f = new File("/sys/class/power_supply/Battery/current_avg");
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
                //Log.e(LOG_TAG, "Error reading normal current hack file");
                //e.printStackTrace();
            }

            if (line != null) {
                try {
                    value = Long.parseLong(line);
                } catch (NumberFormatException e) {
                    //Log.e(LOG_TAG, "Error parsing normal current hack file");
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
                            //Log.e(LOG_TAG, "Error parsing BattAttr current hack file");
                        }
                    }

                    //  "batt_discharge_current:"
                    if (line.contains(dischargeField)) {
                        text = line.substring(line.indexOf(dischargeFieldHead) + dischargeFieldHead.length());
                        try {
                            value = (-1)*Math.abs(Long.parseLong(text));
                        } catch (NumberFormatException e) {
                            //Log.e(LOG_TAG, "Error parsing BattAttr current hack file");
                        }

                        break;
                    }

                    line = bReader.readLine();
                }

                bReader.close();
            } catch (Exception e) {
                //Log.e(LOG_TAG, "Error reading BattAttr current hack file");
                //e.printStackTrace();
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
                //Log.e(LOG_TAG, "Error reading SMText current hack file");
                //e.printStackTrace();
            }

            if (success) {
                try {
                    value = Long.parseLong(text);
                } catch (NumberFormatException e) {
                    //Log.e(LOG_TAG, "Error parsing SMText current hack file");
                }
            }

            return value;
        }
    }
}
