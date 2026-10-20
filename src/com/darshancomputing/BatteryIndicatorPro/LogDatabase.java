/*
    Copyright (c) 2010-2026 Darshan Computing, LLC

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

class LogDatabase {
    private static final String DATABASE_NAME = "logs.db";
    private static final int DATABASE_VERSION = 5;
    private static final String LOG_TABLE_NAME = "logs";

    private static final String KEY_ID = "_id";
    static final String KEY_STATUS_CODE = "status";
    static final String KEY_CHARGE      = "charge";
    static final String KEY_TIME        = "time";
    static final String KEY_TEMPERATURE = "temperature";
    static final String KEY_VOLTAGE     = "voltage";
    static final String KEY_CHARGE_MAH  = "charge_mah"; // Fuel gauge charge counter; NULL if unsupported
    static final String KEY_CURRENT_MA  = "current_ma"; // Negative while discharging on most devices; NULL if unsupported

    // Custom values for logging things other than BatteryInfo; must be negative
    static final int STATUS_BOOT_COMPLETED = -1;

    // Values for status_age
    static final int STATUS_NEW = 0;
    static final int STATUS_OLD = 1;

    private final SQLOpenHelper mSQLOpenHelper;
    private SQLiteDatabase rdb;
    private SQLiteDatabase wdb;

    LogDatabase(Context context) {
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
                wdb = null;
            }
        }
    }

    public void close() {
        if (rdb != null)
            rdb.close();
        if (wdb != null)
            wdb.close();
    }

    // Level history for the chart, oldest first, as {time, percent, status} rows. Includes the last
    //  row before `since` (when there is one) so the line can start at the chart's left edge.
    java.util.List<long[]> getLevelHistory(long since) {
        java.util.List<long[]> points = new java.util.ArrayList<long[]>();
        String cols = "SELECT " + KEY_TIME + "," + KEY_CHARGE + "," + KEY_STATUS_CODE + " FROM " + LOG_TABLE_NAME
            + " WHERE " + KEY_STATUS_CODE + " >= 0 AND " + KEY_TIME;
        String[] args = {String.valueOf(since)};

        openDBs();

        try {
            Cursor c = rdb.rawQuery(cols + " < ? ORDER BY " + KEY_TIME + " DESC LIMIT 1", args);
            if (c.moveToFirst())
                points.add(new long[]{c.getLong(0), c.getInt(1), decodeStatus(c.getInt(2))[0]});
            c.close();

            c = rdb.rawQuery(cols + " >= ? ORDER BY " + KEY_TIME + " ASC", args);
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
                points.add(new long[]{c.getLong(0), c.getInt(1), decodeStatus(c.getInt(2))[0]});
            c.close();
        } catch (Exception e) {
            // No chart is better than a crash
        }

        return points;
    }

    Cursor getAllLogs(Boolean reversed) {
        String order = "DESC";
        if (reversed) order = "ASC";

        openDBs();

        try {
            return rdb.rawQuery("SELECT * FROM " + LOG_TABLE_NAME + " ORDER BY " + KEY_TIME + " " + order, null);
        } catch (Exception e) {
            return null;
        }
    }

    void logStatus(BatteryInfo info, long time, int status_age) {
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

            if (! duplicate) {
                String charge_mah = info.charge_counter_uah >= 0 ? String.valueOf(info.charge_counter_uah / 1000) : "NULL";
                Long current = CurrentHack.getCurrent();
                String current_ma = current != null ? String.valueOf(current) : "NULL";

                wdb.execSQL("INSERT INTO " + LOG_TABLE_NAME + " VALUES (NULL, "
                            + encodeStatus(info.status, info.plugged, status_age) + " ," + info.percent + " ," + time
                            + " ," + info.temperature + " ," + info.voltage
                            + " ," + charge_mah + " ," + current_ma + ")");
            }

            lastLog.close();
        } catch (Exception e) {
            // Maybe storage is full?  Okay to drop this log rather than crash.
        }
    }

    void logBoot() {
        openDBs();

        try {
            wdb.execSQL("INSERT INTO " + LOG_TABLE_NAME + " VALUES (NULL, "
                        + STATUS_BOOT_COMPLETED + ",NULL," + System.currentTimeMillis()
                        + ",NULL,NULL,NULL,NULL)");
        } catch (Exception e) {
            // Maybe storage is full?  Okay to drop this log rather than crash.
        }
    }

    void prune(int max_hours) {
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
    static int[] decodeStatus(int statusCode) {
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

    void clearAllLogs() {
        mSQLOpenHelper.reset();
    }

    private static class SQLOpenHelper extends SQLiteOpenHelper {
        SQLOpenHelper(Context context) {
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
                       + KEY_VOLTAGE     + " INTEGER,"
                       + KEY_CHARGE_MAH  + " INTEGER,"
                       + KEY_CURRENT_MA  + " INTEGER"
                       + ");");
            createIndex(db);
        }

        // Every query sorts by time, and months of logs make that slow without an index
        private void createIndex(SQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS " + LOG_TABLE_NAME + "_time_idx ON " + LOG_TABLE_NAME + " (" + KEY_TIME + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 3 || oldVersion == 4) {
                if (oldVersion == 3) {
                    db.execSQL("ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN " + KEY_TEMPERATURE + " INTEGER;");
                    db.execSQL("ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN " + KEY_VOLTAGE     + " INTEGER;");
                }

                db.execSQL("ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN " + KEY_CHARGE_MAH + " INTEGER;");
                db.execSQL("ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN " + KEY_CURRENT_MA + " INTEGER;");
                createIndex(db);
            } else {
                db.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE_NAME);
                onCreate(db);
            }
        }

        void reset() {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + LOG_TABLE_NAME);
            onCreate(db);
        }
    }
}
