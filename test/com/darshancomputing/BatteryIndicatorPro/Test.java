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
        bi.percent = 98;
        pc.update(bi, now);
        print();

        for (int i = 0; i < 10; i++) {
            bi.percent -= 1;
            now += 60 * 1000;
            pc.update(bi, now);
            print();
        }

        bi.percent = 0;
        now += 60 * 1000;
        pc.update(bi, now);
        print();

        bi.percent = 88;
        now += 5;
        pc.update(bi, now);
        print();

        for (int i = 0; i < 40; i++) {
            bi.percent -= 1;
            now += 60 * 1000;
            pc.update(bi, now);
            print();
        }
    }

    private static void print() {
        System.out.println("" + bi.percent + "%; drained in " + pc.secondsUntilDrained() + " seconds.");
    }
}
