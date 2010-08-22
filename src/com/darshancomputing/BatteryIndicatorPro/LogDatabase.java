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
    private static final int DATABASE_VERSION = 1;
    private static final String LOG_TABLE_NAME = "logs";
    private static final String KEY_ID = "_id";
    public  static final String KEY_STATUS  = "status";
    public  static final String KEY_PLUGGED = "plugged";
    public  static final String KEY_CHARGE  = "charge";
    public  static final String KEY_TIME    = "time";

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

    public void logStatus(int status, int plugged, int charge, long time) {
        mSQLOpenHelper.logStatus(status, plugged, charge, time);
    }

    private static class SQLOpenHelper extends SQLiteOpenHelper {
        public SQLOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + LOG_TABLE_NAME + " ("
                       + KEY_ID      + " INTEGER PRIMARY KEY,"
                       + KEY_STATUS  + " INTEGER,"
                       + KEY_PLUGGED + " INTEGER,"
                       + KEY_CHARGE  + " INTEGER,"
                       + KEY_TIME    + " INTEGER"
                       + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE_NAME);
            onCreate(db);
        }

        public void logStatus(int status, int plugged, int charge, long time) {
            getWritableDatabase().execSQL("INSERT INTO " + LOG_TABLE_NAME + " VALUES (NULL, "
                                           + status  + " ,"
                                           + plugged + " ,"
                                           + charge  + " ,"
                                           + time    + ")");
        }
    }
}
