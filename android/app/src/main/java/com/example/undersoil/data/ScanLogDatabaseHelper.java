package com.example.undersoil.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ScanLogDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "scan_logs.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_LOGS = "scan_logs";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_CROP_NAME = "crop_name";
    public static final String COLUMN_DIAGNOSIS = "diagnosis";
    public static final String COLUMN_CONFIDENCE = "confidence";
    public static final String COLUMN_DATE = "date_str";

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
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_CROP_NAME + " TEXT, " +
                COLUMN_DIAGNOSIS + " TEXT, " +
                COLUMN_CONFIDENCE + " REAL, " +
                COLUMN_DATE + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        onCreate(db);
    }

    public long insertLog(ScanLog log, long userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_CROP_NAME, log.getCropName());
        values.put(COLUMN_DIAGNOSIS, log.getDiagnosis());
        values.put(COLUMN_CONFIDENCE, log.getConfidence());
        values.put(COLUMN_DATE, log.getDateStr());
        return db.insert(TABLE_LOGS, null, values);
    }

    public List<ScanLog> getLogsByCropName(String cropName, long userId) {
        List<ScanLog> logs = new ArrayList<>();
        if (cropName == null || cropName.trim().isEmpty()) {
            return logs;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_LOGS,
                null,
                "LOWER(" + COLUMN_CROP_NAME + ") = LOWER(?) AND " + COLUMN_USER_ID + " = ?",
                new String[]{cropName.trim(), String.valueOf(userId)},
                null,
                null,
                COLUMN_ID + " DESC"
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID);
                int userIdx = cursor.getColumnIndexOrThrow(COLUMN_USER_ID);
                int cropIdx = cursor.getColumnIndexOrThrow(COLUMN_CROP_NAME);
                int diagIdx = cursor.getColumnIndexOrThrow(COLUMN_DIAGNOSIS);
                int confIdx = cursor.getColumnIndexOrThrow(COLUMN_CONFIDENCE);
                int dateIdx = cursor.getColumnIndexOrThrow(COLUMN_DATE);

                do {
                    ScanLog log = new ScanLog(
                            cursor.getLong(idIdx),
                            cursor.getLong(userIdx),
                            cursor.getString(cropIdx),
                            cursor.getString(diagIdx),
                            cursor.getFloat(confIdx),
                            cursor.getString(dateIdx)
                    );
                    logs.add(log);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return logs;
    }
}
