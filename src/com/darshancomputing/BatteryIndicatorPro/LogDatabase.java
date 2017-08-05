/*
    Copyright (c) 2010-2017 Darshan-Josiah Barber

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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class LogDatabase {
    private static final String DATABASE_NAME = "logs.db";
    private static final int DATABASE_VERSION = 4;
    private static final String LOG_TABLE_NAME = "logs";

    private static final String KEY_ID = "_id";
    public  static final String KEY_STATUS_CODE = "status";
    public  static final String KEY_CHARGE      = "charge";
    public  static final String KEY_TIME        = "time";
    public  static final String KEY_TEMPERATURE = "temperature";
    public  static final String KEY_VOLTAGE     = "voltage";

    // Custom values for logging things other than BatteryInfo; must be negative
    public static final int STATUS_BOOT_COMPLETED = -1;

    // Values for status_age
    public static final int STATUS_NEW = 0;
    public static final int STATUS_OLD = 1;

    private final SQLOpenHelper mSQLOpenHelper;
    private SQLiteDatabase rdb;
    private SQLiteDatabase wdb;

    public LogDatabase(Context context) {
        mSQLOpenHelper = new SQLOpenHelper(context);

        openDBs();
    }

    private void openDBs(){
        if (rdb == null || !rdb.isOpen()) {
            try {
                rdb = mSQLOpenHelper.getReadableDatabase();
            } catch (SQLiteException e) {
                rdb = null;
            }
        }

        if (wdb == null || !wdb.isOpen()) {
            try {
                wdb = mSQLOpenHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                rdb = null;
            }
        }
    }

    public void close() {
        if (rdb != null)
            rdb.close();
        if (wdb != null)
            wdb.close();
    }

    public Cursor getAllLogs(Boolean reversed) {
        String order = "DESC";
        if (reversed) order = "ASC";

        openDBs();

        try {
            return rdb.rawQuery("SELECT * FROM " + LOG_TABLE_NAME + " ORDER BY " + KEY_TIME + " " + order, null);
        } catch (Exception e) {
            return null;
        }
    }

    public void logStatus(BatteryInfo info, long time, int status_age) {
        Boolean duplicate = false;

        openDBs();

        try {
            Cursor lastLog = rdb.rawQuery("SELECT * FROM " + LOG_TABLE_NAME
                                          + " ORDER BY " + KEY_TIME + " DESC LIMIT 1", null);

            if (lastLog.moveToFirst()){
                int statusCode = lastLog.getInt(lastLog.getColumnIndexOrThrow(KEY_STATUS_CODE));
                int lastCharge = lastLog.getInt(lastLog.getColumnIndexOrThrow(KEY_CHARGE));
                int[] a = decodeStatus(statusCode);
                int lastStatus  = a[0];
                int lastPlugged = a[1];

                if (info.percent == lastCharge && info.status == lastStatus && info.plugged == lastPlugged)
                    duplicate = true;
            }

            if (! duplicate)
                wdb.execSQL("INSERT INTO " + LOG_TABLE_NAME + " VALUES (NULL, "
                            + encodeStatus(info.status, info.plugged, status_age) + " ," + info.percent + " ," + time
                            + " ," + info.temperature + " ," + info.voltage + ")");

            lastLog.close();
        } catch (Exception e) {
            // Maybe storage is full?  Okay to drop this log rather than crash.
        }
    }

    public void logBoot() {
        openDBs();

        try {
            wdb.execSQL("INSERT INTO " + LOG_TABLE_NAME + " VALUES (NULL, "
                        + STATUS_BOOT_COMPLETED + ",NULL," + System.currentTimeMillis()
                        + ",NULL,NULL)");
        } catch (Exception e) {
            // Maybe storage is full?  Okay to drop this log rather than crash.
        }
    }

    public void prune(int max_hours) {
        long currentTM = System.currentTimeMillis();
        long oldest_log = currentTM - ((long) max_hours * 60 * 60 * 1000);

        openDBs();

        try {
            wdb.execSQL("DELETE FROM " + LOG_TABLE_NAME + " WHERE " + KEY_TIME + " < " + oldest_log);
        } catch (Exception e) {
            // Maybe storage is full?  Okay to just return rather than crash.
        }
    }

    /* My cursor adapter was getting a bit complicated since it could only see one datum at a time, and
       how I want to present the data depends on several interrelated factors.  Storing all three of
       these items together simplifies things. */
    private static int encodeStatus(int status, int plugged, int status_age) {
        return status + (plugged * 10) + (status_age * 100);
    }

    /* Returns [status, plugged, status_age] */
    public static int[] decodeStatus(int statusCode) {
        if (statusCode < 0) // Negative status for custom statuses like STATUS_BOOT_COMPLETED
            return new int[]{statusCode, 0, 0};

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
                       + KEY_TIME        + " INTEGER,"
                       + KEY_TEMPERATURE + " INTEGER,"
                       + KEY_VOLTAGE     + " INTEGER"
                       + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 3 && newVersion == 4) {
                db.execSQL("ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN " + KEY_TEMPERATURE + " INTEGER;");
                db.execSQL("ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN " + KEY_VOLTAGE     + " INTEGER;");
            } else {
                db.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE_NAME);
                onCreate(db);
            }
        }

        public void reset() {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE_NAME);
            onCreate(db);
        }
    }
}
