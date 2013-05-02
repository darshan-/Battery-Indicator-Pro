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

public class Test {
    private static PredictorCore pc;
    private static BatteryInfo bi;
    private static long now = 0l;

    public static void main(String[] args) {
        pc = new PredictorCore(-1, -1, -1, -1);
        bi = new BatteryInfo();
        now = 0l;

        /*
        bi.percent = 98;
        pc.update(bi, now);
        print();

        now += 60 * 1000;
        bi.percent = 98;
        bi.plugged = BatteryInfo.PLUGGED_AC;
        bi.status  = BatteryInfo.STATUS_CHARGING;
        pc.update(bi, now);
        print();

        now += 60 * 1000;
        bi.percent = 98;
        pc.update(bi, now);
        print();

        now += 60 * 1000;
        bi.percent = 98;
        pc.update(bi, now);
        print();

        now += 60 * 1000;
        bi.percent = 99;
        pc.update(bi, now);
        print();

        now += 60 * 1000;
        bi.percent = 99;
        pc.update(bi, now);
        print();

        now += 60 * 1000;
        bi.percent = 100;
        pc.update(bi, now);
        print();

        now += 60 * 1000;
        bi.percent = 100;
        pc.update(bi, now);
        print();

        now += 60 * 1000;
        bi.status = BatteryInfo.STATUS_FULLY_CHARGED;
        pc.update(bi, now);
        print();
        */

        int level_by_minute[] = {98, 98,
                                 97, 97,
                                 96, 96,
                                 95,
                                 94, 94, 94, 94, 94, 94, 94, 94, 94, 94,
                                 96, 96,
                                 95, 95, 95, 95, 95, 95, 95, 95,
                                 94, 94, 94, 94, 94, 94, 94, 94,
                                 93, 93, 93, 93, 93, 93, 93, 93,
                                 92, 92, 92, 92, 92,
                                 91, 91, 91, 91, 91,
                                 90, 90,
                                 89, 89,
                                 88, 88, 88, 88, 88, 88, 88,
                                 87
        };

        for (int l : level_by_minute) {
            bi.percent = l;
            pc.update(bi, now);
            print();
            now += 60 * 1000;
        }

        bi.status = BatteryInfo.STATUS_CHARGING;
        bi.plugged = BatteryInfo.PLUGGED_AC;

        int level_by_minute2[] = {87, 87,
                                  88, 88, 88,
                                  90, 90, 90,
                                  91, 91, 91,
                                  92, 92, 92,
                                  93, 93, 93, 93,
                                  94, 94,
                                  95, 95, 95,
                                  100, 100
        };

        for (int l : level_by_minute2) {
            bi.percent = l;
            pc.update(bi, now);
            now += 60 * 1000;
            print();
        }

        bi.status = BatteryInfo.STATUS_UNPLUGGED;
        bi.plugged = BatteryInfo.PLUGGED_UNPLUGGED;

        int level_by_minute3[] = {100, 100,
                                  99, 99,
                                  100
        };

        for (int l : level_by_minute3) {
            bi.percent = l;
            pc.update(bi, now);
            now += 60 * 1000;
            print();
        }
    }

    private static void print() {
        switch(bi.status) {
        case  BatteryInfo.STATUS_UNPLUGGED:
            System.out.println("" + (now/60000.0) + ": " + bi.percent + "%; drained in " + prettyTimeRemaining(bi));
            break;
        case  BatteryInfo.STATUS_CHARGING:
            System.out.println("" + (now/60000.0) + ": " + bi.percent + "%; charged in " + prettyTimeRemaining(bi));
            break;
        case  BatteryInfo.STATUS_FULLY_CHARGED:
            System.out.println("" + (now/60000.0) + ": " + bi.percent + "%; fully charged.");
            break;
        default:
            System.out.println("" + (now/60000.0) + ": unknown status: " + bi.status);
        }
    }

    private static String prettyTimeRemaining(BatteryInfo info) {
        BatteryInfo.RelativeTime predicted = info.prediction.getRelativeTime(now);

        return "" +
            (predicted.days * 24 + predicted.hours) + "h " +
            predicted.minutes + "m";
    }
}
