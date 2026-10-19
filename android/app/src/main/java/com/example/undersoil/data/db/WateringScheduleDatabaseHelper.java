package com.example.undersoil.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.undersoil.data.model.WateringSchedule;

import java.util.ArrayList;
import java.util.List;

public class WateringScheduleDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "watering_schedules.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SCHEDULES = "watering_schedules";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_SECTOR_ID = "sector_id";
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_DURATION_MINUTES = "duration_minutes";
    public static final String COLUMN_EXPECTED_VOLUME = "expected_volume";
    public static final String COLUMN_ACTUAL_VOLUME = "actual_volume";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_IS_SYNCED = "is_synced";

    private static WateringScheduleDatabaseHelper instance;

    public static synchronized WateringScheduleDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new WateringScheduleDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public WateringScheduleDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_SCHEDULES + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_USER_ID + " TEXT, " +
                COLUMN_SECTOR_ID + " TEXT, " +
                COLUMN_START_TIME + " INTEGER, " +
                COLUMN_DURATION_MINUTES + " INTEGER, " +
                COLUMN_EXPECTED_VOLUME + " REAL, " +
                COLUMN_ACTUAL_VOLUME + " REAL DEFAULT 0, " +
                COLUMN_STATUS + " TEXT, " +
                COLUMN_IS_SYNCED + " INTEGER DEFAULT 0)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first schema. Add future migrations here as
        // "if (oldVersion < N) { ALTER TABLE ... }" so pending schedules are never dropped.
    }

    public boolean insertSchedule(WateringSchedule schedule) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, schedule.getId());
        values.put(COLUMN_USER_ID, schedule.getUserId());
        values.put(COLUMN_SECTOR_ID, schedule.getSectorId());
        values.put(COLUMN_START_TIME, schedule.getStartTime());
        values.put(COLUMN_DURATION_MINUTES, schedule.getDurationMinutes());
        values.put(COLUMN_EXPECTED_VOLUME, schedule.getExpectedVolume());
        values.put(COLUMN_ACTUAL_VOLUME, schedule.getActualVolume());
        values.put(COLUMN_STATUS, schedule.getStatus());
        values.put(COLUMN_IS_SYNCED, schedule.isSynced() ? 1 : 0);
        return db.insert(TABLE_SCHEDULES, null, values) != -1;
    }

    // Saves a server copy (which carries the server-owned status and actual volume),
    // unless the local row has changes that haven't been uploaded yet
    public void upsertFromServer(WateringSchedule schedule) {
        if (schedule.getId() == null || schedule.getId().trim().isEmpty()) return;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, schedule.getId());
        values.put(COLUMN_USER_ID, schedule.getUserId());
        values.put(COLUMN_SECTOR_ID, schedule.getSectorId());
        values.put(COLUMN_START_TIME, schedule.getStartTime());
        values.put(COLUMN_DURATION_MINUTES, schedule.getDurationMinutes());
        values.put(COLUMN_EXPECTED_VOLUME, schedule.getExpectedVolume());
        values.put(COLUMN_ACTUAL_VOLUME, schedule.getActualVolume());
        values.put(COLUMN_STATUS, schedule.getStatus());
        values.put(COLUMN_IS_SYNCED, 1);

        int updated = db.update(TABLE_SCHEDULES, values,
                COLUMN_ID + " = ? AND " + COLUMN_IS_SYNCED + " = 1", new String[]{schedule.getId()});
        if (updated == 0) {
            db.insertWithOnConflict(TABLE_SCHEDULES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public List<WateringSchedule> getUnsyncedSchedules() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_SCHEDULES,
                null,
                COLUMN_IS_SYNCED + " = 0",
                null, null, null,
                COLUMN_START_TIME + " ASC"
        );
        return readSchedules(cursor);
    }

    public void markScheduleAsSynced(String scheduleId) {
        if (scheduleId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_SYNCED, 1);
        db.update(TABLE_SCHEDULES, values, COLUMN_ID + " = ?", new String[]{scheduleId});
    }

    private List<WateringSchedule> readSchedules(Cursor cursor) {
        List<WateringSchedule> schedules = new ArrayList<>();
        if (cursor == null) return schedules;

        if (cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID);
            int userIdx = cursor.getColumnIndexOrThrow(COLUMN_USER_ID);
            int sectorIdx = cursor.getColumnIndexOrThrow(COLUMN_SECTOR_ID);
            int startIdx = cursor.getColumnIndexOrThrow(COLUMN_START_TIME);
            int durationIdx = cursor.getColumnIndexOrThrow(COLUMN_DURATION_MINUTES);
            int expectedIdx = cursor.getColumnIndexOrThrow(COLUMN_EXPECTED_VOLUME);
            int actualIdx = cursor.getColumnIndexOrThrow(COLUMN_ACTUAL_VOLUME);
            int statusIdx = cursor.getColumnIndexOrThrow(COLUMN_STATUS);
            int syncIdx = cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED);

            do {
                schedules.add(new WateringSchedule(
                        cursor.getString(idIdx),
                        cursor.getString(userIdx),
                        cursor.getString(sectorIdx),
                        cursor.getLong(startIdx),
                        cursor.getInt(durationIdx),
                        cursor.getDouble(expectedIdx),
                        cursor.getDouble(actualIdx),
                        cursor.getString(statusIdx),
                        cursor.getInt(syncIdx) == 1
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return schedules;
    }
}
