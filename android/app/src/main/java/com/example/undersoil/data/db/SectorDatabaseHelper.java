package com.example.undersoil.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.undersoil.data.model.Sector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SectorDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sectors.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_SECTORS = "sectors";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_CROP_NAMES = "crop_names";
    public static final String COLUMN_IS_SYNCED = "is_synced";

    private static SectorDatabaseHelper instance;

    public static synchronized SectorDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SectorDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public SectorDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_SECTORS + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_USER_ID + " TEXT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_CROP_NAMES + " TEXT, " +
                COLUMN_IS_SYNCED + " INTEGER DEFAULT 0)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SECTORS);
        onCreate(db);
    }

    public String insertSector(Sector sector, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String sectorId = (sector.getId() != null && !sector.getId().trim().isEmpty())
                ? sector.getId()
                : UUID.randomUUID().toString();

        values.put(COLUMN_ID, sectorId);
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_NAME, sector.getName());
        values.put(COLUMN_CROP_NAMES, sector.getCropNames());
        values.put(COLUMN_IS_SYNCED, sector.isSynced() ? 1 : 0);

        long row = db.insert(TABLE_SECTORS, null, values);
        return (row != -1) ? sectorId : null;
    }

    // Saves a server copy, unless the local row has changes that haven't been uploaded yet
    public void upsertFromServer(Sector sector) {
        if (sector.getId() == null || sector.getId().trim().isEmpty()) return;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, sector.getId());
        values.put(COLUMN_USER_ID, sector.getUserId());
        values.put(COLUMN_NAME, sector.getName());
        values.put(COLUMN_CROP_NAMES, sector.getCropNames());
        values.put(COLUMN_IS_SYNCED, 1);

        int updated = db.update(TABLE_SECTORS, values,
                COLUMN_ID + " = ? AND " + COLUMN_IS_SYNCED + " = 1", new String[]{sector.getId()});
        if (updated == 0) {
            db.insertWithOnConflict(TABLE_SECTORS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public List<Sector> getAllSectors(String userId) {
        List<Sector> sectors = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_SECTORS,
                null,
                COLUMN_USER_ID + " = ?",
                new String[]{userId},
                null,
                null,
                COLUMN_ID + " ASC"
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID);
                int userIdx = cursor.getColumnIndexOrThrow(COLUMN_USER_ID);
                int nameIdx = cursor.getColumnIndexOrThrow(COLUMN_NAME);
                int cropIdx = cursor.getColumnIndexOrThrow(COLUMN_CROP_NAMES);
                int syncIdx = cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED);

                do {
                    Sector sector = new Sector(
                            cursor.getString(idIdx),
                            cursor.getString(userIdx),
                            cursor.getString(nameIdx),
                            cursor.getString(cropIdx),
                            cursor.getInt(syncIdx) == 1
                    );
                    sectors.add(sector);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return sectors;
    }

    public List<Sector> getUnsyncedSectors() {
        List<Sector> sectors = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_SECTORS,
                null,
                COLUMN_IS_SYNCED + " = 0",
                null, null, null,
                COLUMN_ID + " ASC"
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID);
                int userIdx = cursor.getColumnIndexOrThrow(COLUMN_USER_ID);
                int nameIdx = cursor.getColumnIndexOrThrow(COLUMN_NAME);
                int cropIdx = cursor.getColumnIndexOrThrow(COLUMN_CROP_NAMES);
                int syncIdx = cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED);

                do {
                    Sector sector = new Sector(
                            cursor.getString(idIdx),
                            cursor.getString(userIdx),
                            cursor.getString(nameIdx),
                            cursor.getString(cropIdx),
                            cursor.getInt(syncIdx) == 1
                    );
                    sectors.add(sector);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return sectors;
    }

    public void markSectorAsSynced(String sectorId) {
        if (sectorId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_SYNCED, 1);
        db.update(TABLE_SECTORS, values, COLUMN_ID + " = ?", new String[]{sectorId});
    }

    public void deleteSector(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SECTORS, COLUMN_ID + " = ?", new String[]{id});
    }
}
