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

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.Date;

public class Logger {
    private FileWriter fout;
    private DateFormat dateFormat, timeFormat;
    private String tag;

    Logger(Context context) {
        this(context, "");
    }

    Logger(Context context, String log_tag) {
        try {
            tag = log_tag;
            File f = new File(android.os.Environment.getExternalStorageDirectory(), "BI_Logger.txt");
            f.createNewFile();
            fout = new FileWriter(f, true);
            dateFormat = android.text.format.DateFormat.getDateFormat(context);
            timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        } catch (Exception e) {e.printStackTrace();}
    }

    public void log(String s) {
        try {
            Date date = new Date();
            fout.write(dateFormat.format(date) + " " + timeFormat.format(date) + ": ");
            fout.write(tag + ": ");
            fout.write(s);
            fout.write("\n");
            fout.flush();
        } catch (Exception e) {e.printStackTrace();}
    }
}
