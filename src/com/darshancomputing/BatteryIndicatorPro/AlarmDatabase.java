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

public class AlarmDatabase {
    private static final String DATABASE_NAME    = "alarms.db";
    private static final int    DATABASE_VERSION = 1;
    private static final String ALARM_TABLE_NAME = "alarms";

    public static final String KEY_ID        = "_id";
    public static final String KEY_TYPE      = "type";
    public static final String KEY_THRESHOLD = "threshold";
    public static final String KEY_ENABLED   = "enabled";

    /* I'm not sure if this is considered safe practice -- do I really need to use Cursor.getColumnIndexOrThrow()? */
    public static final int INDEX_ID        = 0;
    public static final int INDEX_TYPE      = 1;
    public static final int INDEX_THRESHOLD = 2;
    public static final int INDEX_ENABLED   = 3;

    private final SQLOpenHelper mSQLOpenHelper;
    private SQLiteDatabase rdb;
    private SQLiteDatabase wdb;

    public AlarmDatabase(Context context) {
        mSQLOpenHelper = new SQLOpenHelper(context);
        rdb = mSQLOpenHelper.getReadableDatabase();
        wdb = mSQLOpenHelper.getWritableDatabase();
    }

    public void close() {
        rdb.close();
        wdb.close();
    }

    public Cursor getAllAlarms(Boolean reversed) {
        String order = "DESC";
        if (reversed) order = "ASC";

        return rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " ORDER BY " + KEY_ID + " " + order, null);
    }

    public Cursor getAlarm(int id) {
        Cursor c = rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " WHERE "+ KEY_ID + "=" + id + " LIMIT 1", null);
        c.moveToFirst();
        return c;
    }

    public void addAlarm(int type, int threshold, Boolean enabled) {
        wdb.execSQL("INSERT INTO " + ALARM_TABLE_NAME + " VALUES (NULL, "
                    + type + " ," + threshold + " ," + (enabled ? 1 : 0) + ")");
    }

    public int addAlarm() {
        addAlarm(0, 0, true);

        Cursor c = rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " WHERE " + KEY_ID + "= last_insert_rowid()", null);
        c.moveToFirst();
        int i = c.getInt(INDEX_ID);
        c.close();

        return i;
    }

    public void setEnabledness(int id, Boolean enabled) {
        wdb.execSQL("UPDATE " + ALARM_TABLE_NAME + " SET " + KEY_ENABLED + "=" +
                    (enabled ? 1 : 0) + " WHERE " + KEY_ID + "=" + id);
    }

    public Boolean toggle(int id) {
        Cursor c = rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " WHERE " + KEY_ID + "=" + id, null);
        c.moveToFirst();
        Boolean newEnabled = !(c.getInt(INDEX_ENABLED) == 1);
        c.close();

        setEnabledness(id, newEnabled);

        return newEnabled;
    }

    public void setType(int id, int type) {
        wdb.execSQL("UPDATE " + ALARM_TABLE_NAME + " SET " + KEY_TYPE + "=" +
                    type + " WHERE " + KEY_ID + "=" + id);
    }

    public void deleteAlarm(int id) {
        wdb.execSQL("DELETE FROM " + ALARM_TABLE_NAME + " WHERE _id = " + id);
    }

    public void clearAllAlarms() {
        mSQLOpenHelper.reset();
    }

    private static class SQLOpenHelper extends SQLiteOpenHelper {
        public SQLOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + ALARM_TABLE_NAME + " ("
                       + KEY_ID        + " INTEGER PRIMARY KEY,"
                       + KEY_TYPE      + " INTEGER,"
                       + KEY_THRESHOLD + " INTEGER,"
                       + KEY_ENABLED   + " INTEGER"
                       + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (false) {
            } else {
                db.execSQL("DROP TABLE IF EXISTS " + ALARM_TABLE_NAME);
                onCreate(db);
            }
        }

        public void reset() {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + ALARM_TABLE_NAME);
            onCreate(db);
        }
    }
}
