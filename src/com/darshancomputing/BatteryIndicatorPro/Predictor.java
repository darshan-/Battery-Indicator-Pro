/*
    Copyright (c) 2012 Josiah Barber (aka Darshan)

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

import java.util.LinkedList;

public class Predictor {
    private static final int DEFAULT_DURATION = 900 * 1000;
    private static final double WEIGHT_OLD_AVERAGE = 0.999;
    private static final double WEIGHT_NEW_DATA =  1 - WEIGHT_OLD_AVERAGE;
    private static final double WEIGHT_AVERAGE = 0.5;
    private static final double WEIGHT_RECENTS = 1 - WEIGHT_AVERAGE;
    private static final int RECENTS_SIZE = 10;

    private double average;
    private LinkedList recent;

    public Predictor() {
        average = DEFAULT_DURATION;
        recent = new LikedList();

        for (int i = 0; i < RECENTS_SIZE; i++) {
            list.add(average);
        }
    }

    public void update(int level, int status) {
    }

    public int secondsUntilDrained() {
        return 60 * 60 * 24;
    }

    public int secondsUntilCharged() {
        return 60 * 60 * 4;
    }
}

/*
...9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9
4,4,4,4,4,4,4,4,3,3

Charge 1%:
...9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,4
4,4,4,4,4,4,4,3,3,9

Charge 2%:
...9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,4,4
4,4,4,4,4,4,3,3,9,9

Then unplugged and discharge 1%:
...9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,4,4
4,4,4,4,4,4,3,3,9,9


*/