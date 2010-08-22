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

    public Cursor getAllLogs() {
        return mSQLOpenHelper.getReadableDatabase().query(LOG_TABLE_NAME,
                                                         null,
                                                         null, null,
                                                         null, null,
                                                         KEY_TIME);
    }

    private static class SQLOpenHelper extends SQLiteOpenHelper {
        private SQLiteDatabase db;

        public SQLOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            db = database;

            db.execSQL("CREATE TABLE " + LOG_TABLE_NAME + " ("
                    + KEY_ID      + " INTEGER PRIMARY KEY,"
                    + KEY_STATUS  + " INTEGER,"
                    + KEY_PLUGGED + " INTEGER,"
                    + KEY_CHARGE  + " INTEGER,"
                    + KEY_TIME    + " INTEGER"
                    + ");");

            db.execSQL("INSERT INTO " + LOG_TABLE_NAME + " VALUES (1, 2, 1, 57,  99999)");
            db.execSQL("INSERT INTO " + LOG_TABLE_NAME + " VALUES (2, 0, 0, 59, 109999)");
            db.execSQL("INSERT INTO " + LOG_TABLE_NAME + " VALUES (3, 2, 2, 57, 119999)");
            db.execSQL("INSERT INTO " + LOG_TABLE_NAME + " VALUES (4, 0, 0, 59, 129999)");
            db.execSQL("INSERT INTO " + LOG_TABLE_NAME + " VALUES (5, 2, 1, 57, 139999)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            database.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE_NAME);
            onCreate(database);
        }
    }
}
