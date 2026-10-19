package com.example.undersoil.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.undersoil.data.model.ScanLog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScanLogDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "scan_logs.db";
    private static final int DATABASE_VERSION = 4;

    public static final int MAX_NOTE_LENGTH = 50;

    public static final String TABLE_LOGS = "scan_logs";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_CROP_NAME = "crop_name";
    public static final String COLUMN_DIAGNOSIS = "diagnosis";
    public static final String COLUMN_CONFIDENCE = "confidence";
    public static final String COLUMN_DATE = "date_str";
    public static final String COLUMN_IS_SYNCED = "is_synced";
    public static final String COLUMN_NOTES = "notes";
    public static final String COLUMN_IMAGE_PATH = "image_path";

    // Dates are stored as "yyyy-MM-dd HH:mm", so they sort chronologically as text;
    // rowid breaks ties between scans taken within the same minute.
    private static final String NEWEST_FIRST = COLUMN_DATE + " DESC, rowid DESC";

    private static ScanLogDatabaseHelper instance;

    public static synchronized ScanLogDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ScanLogDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public ScanLogDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_LOGS + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_USER_ID + " TEXT, " +
                COLUMN_CROP_NAME + " TEXT, " +
                COLUMN_DIAGNOSIS + " TEXT, " +
                COLUMN_CONFIDENCE + " REAL, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_IS_SYNCED + " INTEGER DEFAULT 0, " +
                COLUMN_NOTES + " TEXT, " +
                COLUMN_IMAGE_PATH + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Pre-v3 schemas are unknown/incompatible: rebuild as before
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
            onCreate(db);
            return;
        }
        if (oldVersion < 4) {
            // v3 -> v4: add columns in place so existing (possibly unsynced) logs are kept
            db.execSQL("ALTER TABLE " + TABLE_LOGS + " ADD COLUMN " + COLUMN_NOTES + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_LOGS + " ADD COLUMN " + COLUMN_IMAGE_PATH + " TEXT");
        }
    }

    public String insertLog(ScanLog log, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String logId = (log.getId() != null && !log.getId().trim().isEmpty())
                ? log.getId()
                : UUID.randomUUID().toString();

        values.put(COLUMN_ID, logId);
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_CROP_NAME, log.getCropName());
        values.put(COLUMN_DIAGNOSIS, log.getDiagnosis());
        values.put(COLUMN_CONFIDENCE, log.getConfidence());
        values.put(COLUMN_DATE, log.getDateStr());
        values.put(COLUMN_IS_SYNCED, log.isSynced() ? 1 : 0);
        values.put(COLUMN_NOTES, log.getNotes());
        values.put(COLUMN_IMAGE_PATH, log.getImagePath());

        long row = db.insert(TABLE_LOGS, null, values);
        return (row != -1) ? logId : null;
    }

    public List<ScanLog> getLogsByCropName(String cropName, String userId) {
        if (cropName == null || cropName.trim().isEmpty()) {
            return new ArrayList<>();
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_LOGS,
                null,
                "LOWER(" + COLUMN_CROP_NAME + ") = LOWER(?) AND " + COLUMN_USER_ID + " = ?",
                new String[]{cropName.trim(), userId},
                null,
                null,
                NEWEST_FIRST
        );
        return readLogs(cursor);
    }

    // Saves a server copy, unless the local row has changes that haven't been uploaded yet.
    // The server has no photo, so an existing row keeps its local image path.
    public void upsertFromServer(ScanLog log) {
        if (log.getId() == null || log.getId().trim().isEmpty()) return;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, log.getId());
        values.put(COLUMN_USER_ID, log.getUserId());
        values.put(COLUMN_CROP_NAME, log.getCropName());
        values.put(COLUMN_DIAGNOSIS, log.getDiagnosis());
        values.put(COLUMN_CONFIDENCE, log.getConfidence());
        values.put(COLUMN_DATE, log.getDateStr());
        values.put(COLUMN_NOTES, log.getNotes());
        values.put(COLUMN_IS_SYNCED, 1);

        int updated = db.update(TABLE_LOGS, values,
                COLUMN_ID + " = ? AND " + COLUMN_IS_SYNCED + " = 1", new String[]{log.getId()});
        if (updated == 0) {
            db.insertWithOnConflict(TABLE_LOGS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public long getLogCount(String userId) {
        return DatabaseUtils.queryNumEntries(getReadableDatabase(), TABLE_LOGS,
                COLUMN_USER_ID + " = ?", new String[]{userId});
    }

    // Returns the most recent scan for the crop, or null if it has never been scanned.
    public ScanLog getLatestLogByCropName(String cropName, String userId) {
        if (cropName == null || cropName.trim().isEmpty()) {
            return null;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_LOGS,
                null,
                "LOWER(" + COLUMN_CROP_NAME + ") = LOWER(?) AND " + COLUMN_USER_ID + " = ?",
                new String[]{cropName.trim(), userId},
                null,
                null,
                NEWEST_FIRST,
                "1"
        );
        List<ScanLog> logs = readLogs(cursor);
        return logs.isEmpty() ? null : logs.get(0);
    }

    public ScanLog getLogById(String logId) {
        if (logId == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_LOGS,
                null,
                COLUMN_ID + " = ?",
                new String[]{logId},
                null, null, null
        );
        List<ScanLog> logs = readLogs(cursor);
        return logs.isEmpty() ? null : logs.get(0);
    }

    public List<ScanLog> getUnsyncedLogs() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_LOGS,
                null,
                COLUMN_IS_SYNCED + " = 0",
                null, null, null,
                COLUMN_ID + " ASC"
        );
        return readLogs(cursor);
    }

     // Updates a log's note and flags it unsynced so SyncWorker re-uploads the edit.
     // Notes longer than {@link #MAX_NOTE_LENGTH} are truncated.
     // @return true if a row was updated
    public boolean updateLogNote(String logId, String note) {
        if (logId == null) return false;

        String trimmed = (note != null) ? note.trim() : "";
        if (trimmed.length() > MAX_NOTE_LENGTH) {
            trimmed = trimmed.substring(0, MAX_NOTE_LENGTH);
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTES, trimmed);
        values.put(COLUMN_IS_SYNCED, 0);
        return db.update(TABLE_LOGS, values, COLUMN_ID + " = ?", new String[]{logId}) > 0;
    }

    public void markLogAsSynced(String logId) {
        if (logId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_SYNCED, 1);
        db.update(TABLE_LOGS, values, COLUMN_ID + " = ?", new String[]{logId});
    }

    private List<ScanLog> readLogs(Cursor cursor) {
        List<ScanLog> logs = new ArrayList<>();
        if (cursor == null) return logs;

        if (cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID);
            int userIdx = cursor.getColumnIndexOrThrow(COLUMN_USER_ID);
            int cropIdx = cursor.getColumnIndexOrThrow(COLUMN_CROP_NAME);
            int diagIdx = cursor.getColumnIndexOrThrow(COLUMN_DIAGNOSIS);
            int confIdx = cursor.getColumnIndexOrThrow(COLUMN_CONFIDENCE);
            int dateIdx = cursor.getColumnIndexOrThrow(COLUMN_DATE);
            int syncIdx = cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED);
            int notesIdx = cursor.getColumnIndexOrThrow(COLUMN_NOTES);
            int imageIdx = cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH);

            do {
                logs.add(new ScanLog(
                        cursor.getString(idIdx),
                        cursor.getString(userIdx),
                        cursor.getString(cropIdx),
                        cursor.getString(diagIdx),
                        cursor.getFloat(confIdx),
                        cursor.getString(dateIdx),
                        cursor.getInt(syncIdx) == 1,
                        cursor.getString(notesIdx),
                        cursor.getString(imageIdx)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return logs;
    }
}
