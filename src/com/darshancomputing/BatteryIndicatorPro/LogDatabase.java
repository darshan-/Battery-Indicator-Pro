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

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LogDatabase {
    private static final String DATABASE_NAME = "logs.db";
    private static final int DATABASE_VERSION = 3;
    private static final String LOG_TABLE_NAME = "logs";

    private static final String KEY_ID = "_id";
    public  static final String KEY_STATUS_CODE = "status";
    public  static final String KEY_CHARGE      = "charge";
    public  static final String KEY_TIME        = "time";

    public static final int STATUS_NEW = 0;
    public static final int STATUS_OLD = 1;

    private final SQLOpenHelper mSQLOpenHelper;

    public LogDatabase(Context context) {
        mSQLOpenHelper = new SQLOpenHelper(context);
    }

    public Cursor getAllLogs(Boolean reversed) {
        String order = "DESC";
        if (reversed) order = "ASC";

        return mSQLOpenHelper.getReadableDatabase().query(LOG_TABLE_NAME,
                                                         null,
                                                         null, null,
                                                         null, null,
                                                         KEY_TIME + " " + order);
    }

    public void logStatus(int status, int plugged, int charge, long time, int status_age) {
        mSQLOpenHelper.getReadableDatabase().execSQL("INSERT INTO " + LOG_TABLE_NAME
                                                     + " VALUES (NULL, "
                                                     + encodeStatus(status, plugged, status_age) + " ,"
                                                     + charge + " ,"
                                                     + time
                                                     + ")");
    }

    public void prune() {
        /* Do this in a separate thread? */

        long currentTM = System.currentTimeMillis();
    }

    /* My cursor adapter was getting a bit complicated since it could only see one datum at a time, and
       how I want to present the data depends on several interrelated factors.  Storing all three of
       these items together simplifies things. */
    private static int encodeStatus(int status, int plugged, int status_age) {
        return status + (plugged * 10) + (status_age * 100);
    }

    /* Returns [status, plugged, status_age] */
    public static int[] decodeStatus(int statusCode) {
        int[] a = new int[3];

        a[2] = statusCode / 100;
        statusCode -= a[2] * 100;
        a[1] = statusCode / 10;
        statusCode -= a[1] * 10;
        a[0] = statusCode;

        return a;
    }

    public void clearAllLogs() {
        mSQLOpenHelper.reset();
    }

    private static class SQLOpenHelper extends SQLiteOpenHelper {
        public SQLOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + LOG_TABLE_NAME + " ("
                       + KEY_ID          + " INTEGER PRIMARY KEY,"
                       + KEY_STATUS_CODE + " INTEGER,"
                       + KEY_CHARGE      + " INTEGER,"
                       + KEY_TIME        + " INTEGER"
                       + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE_NAME);
            onCreate(db);
        }

        public void reset() {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE_NAME);
            onCreate(db);
        }
    }
}
