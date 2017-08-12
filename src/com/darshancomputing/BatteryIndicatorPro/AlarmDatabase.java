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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

class AlarmDatabase {
    private static final String DATABASE_NAME    = "alarms.db";
    private static final int    DATABASE_VERSION = 6;
    private static final String ALARM_TABLE_NAME = "alarms";

    static final String KEY_ID           = "_id";
    static final String KEY_ENABLED      = "enabled";
    static final String KEY_TYPE         = "type";
    static final String KEY_THRESHOLD    = "threshold";
    static final String KEY_RINGTONE     = "ringtone";
    static final String KEY_AUDIO_STREAM = "audio_stream";
    static final String KEY_VIBRATE      = "vibrate";
    static final String KEY_LIGHTS       = "lights";

    private final SQLOpenHelper mSQLOpenHelper;
    private SQLiteDatabase rdb;
    private SQLiteDatabase wdb;

    AlarmDatabase(Context context) {
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

    Cursor getAllAlarms(Boolean reversed) {
        String order = "DESC";
        if (reversed) order = "ASC";

        openDBs();

        try {
            return rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " ORDER BY " + KEY_ID + " " + order, null);
        } catch (Exception e) {
            return null;
        }
    }

    Cursor getAlarm(int id) {
        openDBs();

        try {
            Cursor c = rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " WHERE "+ KEY_ID + "=" + id + " LIMIT 1", null);
            c.moveToFirst();
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    Boolean anyActiveAlarms() {
        openDBs();

        try {
            Cursor c = rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " WHERE ENABLED=1 LIMIT 1", null);
            Boolean b = (c.getCount() > 0);
            c.close();
            return b;
        } catch (Exception e) {
            return false;
        }
    }

    Cursor activeAlarmFull() {
        openDBs();

        try {
            Cursor c = rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " WHERE "+ KEY_TYPE + "='fully_charged' AND ENABLED=1 LIMIT 1", null);

            if (c.getCount() == 0) {
                c.close();
                return null;
            }

            c.moveToFirst();
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    Cursor activeAlarmChargeDrops(int current, int previous) {
        openDBs();

        try {
            Cursor c = rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " WHERE "+ KEY_TYPE +
                                    "='charge_drops' AND ENABLED=1 AND " +
                                    KEY_THRESHOLD + ">"  + current + " AND " +
                                    KEY_THRESHOLD + "<=" + previous +
                                    " LIMIT 1", null);

            if (c.getCount() == 0) {
                c.close();
                return null;
            }

            c.moveToFirst();
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    Cursor activeAlarmChargeRises(int current, int previous) {
        openDBs();

        try {
            Cursor c = rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " WHERE "+ KEY_TYPE +
                                    "='charge_rises' AND ENABLED=1 AND " +
                                    KEY_THRESHOLD + "<"  + current + " AND " +
                                    KEY_THRESHOLD + ">=" + previous +
                                    " LIMIT 1", null);

            if (c.getCount() == 0) {
                c.close();
                return null;
            }

            c.moveToFirst();
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    Cursor activeAlarmTempRises(int current, int previous) {
        openDBs();

        try {
            Cursor c = rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " WHERE "+ KEY_TYPE +
                                    "='temp_rises' AND ENABLED=1 AND " +
                                    KEY_THRESHOLD + "<"  + current + " AND " +
                                    KEY_THRESHOLD + ">=" + previous +
                                    " LIMIT 1", null);

            if (c.getCount() == 0) {
                c.close();
                return null;
            }

            c.moveToFirst();
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    Cursor activeAlarmFailure() {
        openDBs();

        try {
            Cursor c = rdb.rawQuery("SELECT * FROM " + ALARM_TABLE_NAME + " WHERE "+ KEY_TYPE + "='health_failure' AND ENABLED=1 LIMIT 1", null);

            if (c.getCount() == 0) {
                c.close();
                return null;
            }

            c.moveToFirst();
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    int addAlarm(Boolean enabled, String type, String threshold, String ringtone,
                        String audio_stream, Boolean vibrate, Boolean lights)
    {
        openDBs();

        try {
            ContentValues cv = new ContentValues();
            cv.put(KEY_ENABLED, enabled ? 1 : 0);
            cv.put(KEY_TYPE, type);
            cv.put(KEY_THRESHOLD, threshold);
            cv.put(KEY_RINGTONE, ringtone);
            cv.put(KEY_AUDIO_STREAM, audio_stream);
            cv.put(KEY_VIBRATE, vibrate ? 1 : 0);
            cv.put(KEY_LIGHTS, lights ? 1 : 0);
            return (int) wdb.insert(ALARM_TABLE_NAME, null, cv);
        } catch (Exception e) {
            return -1;
        }
    }

    int addAlarm() {
        return addAlarm(true, "fully_charged", "", android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString(),
                        "notification", false, true);
    }

    int setEnabled(int id, Boolean enabled) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_ENABLED, enabled ? 1 : 0);

        openDBs();

        try {
            return wdb.update(ALARM_TABLE_NAME, cv, KEY_ID + "=" + id, null);
        } catch (Exception e) {
            return -1;
        }
    }

    int setVibrate(int id, Boolean vibrate) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_VIBRATE, vibrate ? 1 : 0);

        openDBs();

        try {
            return wdb.update(ALARM_TABLE_NAME, cv, KEY_ID + "=" + id, null);
        } catch (Exception e) {
            return -1;
        }
    }

    int setLights(int id, Boolean lights) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_LIGHTS, lights ? 1 : 0);

        openDBs();

        try {
            return wdb.update(ALARM_TABLE_NAME, cv, KEY_ID + "=" + id, null);
        } catch (Exception e) {
            return -1;
        }
    }

    public Boolean toggleEnabled(int id) {
        openDBs();

        try {
            Cursor c = rdb.query(ALARM_TABLE_NAME, new String[] {KEY_ENABLED}, KEY_ID + "=" + id, null, null, null, null, null);
            c.moveToFirst();
            Boolean newEnabled = !(c.getInt(0) == 1);
            c.close();

            setEnabled(id, newEnabled);

            return newEnabled;
        } catch (Exception e) {
            return false;
        }
    }

    int setType(int id, String type) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_TYPE, type);

        openDBs();

        try {
            return wdb.update(ALARM_TABLE_NAME, cv, KEY_ID + "=" + id, null);
        } catch (Exception e) {
            return -1;
        }
    }

    int setThreshold(int id, String threshold) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_THRESHOLD, threshold);

        openDBs();

        try {
            return wdb.update(ALARM_TABLE_NAME, cv, KEY_ID + "=" + id, null);
        } catch (Exception e) {
            return -1;
        }
    }

    int setRingtone(int id, String ringtone) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_RINGTONE, ringtone);

        openDBs();

        try {
            return wdb.update(ALARM_TABLE_NAME, cv, KEY_ID + "=" + id, null);
        } catch (Exception e) {
            return -1;
        }
    }

    int setAudioStream(int id, String stream) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_AUDIO_STREAM, stream);

        openDBs();

        try {
            return wdb.update(ALARM_TABLE_NAME, cv, KEY_ID + "=" + id, null);
        } catch (Exception e) {
            return -1;
        }
    }

    void deleteAlarm(int id) {
        openDBs();

        try {
            wdb.delete(ALARM_TABLE_NAME, KEY_ID + "=" + id, null);
        } catch (Exception e) {
        }
    }

    public void deleteAllAlarms() {
        mSQLOpenHelper.reset();
    }

    private static class SQLOpenHelper extends SQLiteOpenHelper {
        SQLOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE "    + ALARM_TABLE_NAME + " ("
                       + KEY_ID           + " INTEGER PRIMARY KEY,"
                       + KEY_ENABLED      + " INTEGER,"
                       + KEY_TYPE         + " STRING,"
                       + KEY_THRESHOLD    + " STRING,"
                       + KEY_RINGTONE     + " STRING,"
                       + KEY_VIBRATE      + " INTEGER,"
                       + KEY_LIGHTS       + " INTEGER,"
                       + KEY_AUDIO_STREAM + " STRING"
                       + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 5 && newVersion == 6) {
                db.execSQL("ALTER TABLE " + ALARM_TABLE_NAME + " ADD COLUMN " + KEY_AUDIO_STREAM + " STRING;");
            } else {
                db.execSQL("DROP TABLE IF EXISTS " + ALARM_TABLE_NAME);
                onCreate(db);
            }
        }

        void reset() {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + ALARM_TABLE_NAME);
            onCreate(db);
        }
    }
}
