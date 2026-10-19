package com.example.undersoil.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.undersoil.data.model.IotAlert;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IotAlertDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "iot_alerts.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_ALERTS = "iot_alerts";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DEVICE_ID = "device_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_ALERT_DESC = "alert_desc";
    public static final String COLUMN_DATE_TIME = "date_time";
    public static final String COLUMN_IS_SYNCED = "is_synced";

    private static IotAlertDatabaseHelper instance;

    public static synchronized IotAlertDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new IotAlertDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public IotAlertDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_ALERTS + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_DEVICE_ID + " TEXT, " +
                COLUMN_USER_ID + " TEXT, " +
                COLUMN_ALERT_DESC + " TEXT, " +
                COLUMN_DATE_TIME + " TEXT, " +
                COLUMN_IS_SYNCED + " INTEGER DEFAULT 0)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALERTS);
        onCreate(db);
    }

    public String insertAlert(IotAlert alert, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String alertId = (alert.getId() == null || alert.getId().trim().isEmpty()) ? UUID.randomUUID().toString() : alert.getId();

        values.put(COLUMN_ID, alertId);
        values.put(COLUMN_DEVICE_ID, alert.getDeviceId());
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_ALERT_DESC, alert.getAlertDesc());
        values.put(COLUMN_DATE_TIME, alert.getDateTime());
        values.put(COLUMN_IS_SYNCED, alert.isSynced() ? 1 : 0);

        long row = db.insert(TABLE_ALERTS, null, values);
        return (row != -1) ? alertId : null;
    }

    // Saves a server copy, unless the local row has changes that haven't been uploaded yet
    public void upsertFromServer(IotAlert alert) {
        if (alert.getId() == null || alert.getId().trim().isEmpty()) return;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, alert.getId());
        values.put(COLUMN_DEVICE_ID, alert.getDeviceId());
        values.put(COLUMN_USER_ID, alert.getUserId());
        values.put(COLUMN_ALERT_DESC, alert.getAlertDesc());
        values.put(COLUMN_DATE_TIME, alert.getDateTime());
        values.put(COLUMN_IS_SYNCED, 1);

        int updated = db.update(TABLE_ALERTS, values,
                COLUMN_ID + " = ? AND " + COLUMN_IS_SYNCED + " = 1", new String[]{alert.getId()});
        if (updated == 0) {
            db.insertWithOnConflict(TABLE_ALERTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public IotAlert getLatestAlert(String userId) {
        if (userId == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_ALERTS,
                null,
                COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null,
                null,
                COLUMN_ID + " DESC",
                "1"
        );

        IotAlert alert = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(COLUMN_ID);
                int devIdx = cursor.getColumnIndex(COLUMN_DEVICE_ID);
                int userIdx = cursor.getColumnIndex(COLUMN_USER_ID);
                int descIdx = cursor.getColumnIndex(COLUMN_ALERT_DESC);
                int dateIdx = cursor.getColumnIndex(COLUMN_DATE_TIME);
                int syncIdx = cursor.getColumnIndex(COLUMN_IS_SYNCED);

                alert = new IotAlert(
                        cursor.getString(idIdx),
                        cursor.getString(devIdx),
                        cursor.getString(userIdx),
                        cursor.getString(descIdx),
                        cursor.getString(dateIdx),
                        cursor.getInt(syncIdx) == 1
                );
            }
            cursor.close();
        }
        return alert;
    }

    /**
     * Newest alerts for a user. date_time is "yyyy-MM-dd HH:mm", which sorts correctly as text;
     * rowid breaks ties between alerts saved in the same minute.
     */
    public List<IotAlert> getRecentAlerts(String userId, int limit) {
        List<IotAlert> alerts = new ArrayList<>();
        if (userId == null) return alerts;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_ALERTS,
                null,
                COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null,
                null,
                COLUMN_DATE_TIME + " DESC, rowid DESC",
                String.valueOf(limit)
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(COLUMN_ID);
                int devIdx = cursor.getColumnIndex(COLUMN_DEVICE_ID);
                int userIdx = cursor.getColumnIndex(COLUMN_USER_ID);
                int descIdx = cursor.getColumnIndex(COLUMN_ALERT_DESC);
                int dateIdx = cursor.getColumnIndex(COLUMN_DATE_TIME);
                int syncIdx = cursor.getColumnIndex(COLUMN_IS_SYNCED);

                do {
                    alerts.add(new IotAlert(
                            cursor.getString(idIdx),
                            cursor.getString(devIdx),
                            cursor.getString(userIdx),
                            cursor.getString(descIdx),
                            cursor.getString(dateIdx),
                            cursor.getInt(syncIdx) == 1
                    ));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return alerts;
    }

    public List<IotAlert> getUnsyncedAlerts() {
        List<IotAlert> alerts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_ALERTS,
                null,
                COLUMN_IS_SYNCED + " = 0",
                null, null, null,
                COLUMN_ID + " ASC"
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex(COLUMN_ID);
                int devIdx = cursor.getColumnIndex(COLUMN_DEVICE_ID);
                int userIdx = cursor.getColumnIndex(COLUMN_USER_ID);
                int descIdx = cursor.getColumnIndex(COLUMN_ALERT_DESC);
                int dateIdx = cursor.getColumnIndex(COLUMN_DATE_TIME);
                int syncIdx = cursor.getColumnIndex(COLUMN_IS_SYNCED);

                do {
                    IotAlert alert = new IotAlert(
                            cursor.getString(idIdx),
                            cursor.getString(devIdx),
                            cursor.getString(userIdx),
                            cursor.getString(descIdx),
                            cursor.getString(dateIdx),
                            cursor.getInt(syncIdx) == 1
                    );
                    alerts.add(alert);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return alerts;
    }

    public void markAlertAsSynced(String alertId) {
        if (alertId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_SYNCED, 1);
        db.update(TABLE_ALERTS, values, COLUMN_ID + " = ?", new String[]{alertId});
    }
}
