package com.example.undersoil.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class SectorDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sectors.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SECTORS = "sectors";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_CROP_NAMES = "crop_names";

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
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_CROP_NAMES + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SECTORS);
        onCreate(db);
    }

    public long insertSector(Sector sector) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, sector.getName());
        values.put(COLUMN_CROP_NAMES, sector.getCropNames());
        return db.insert(TABLE_SECTORS, null, values);
    }

    public List<Sector> getAllSectors() {
        List<Sector> sectors = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_SECTORS,
                null,
                null,
                null,
                null,
                null,
                COLUMN_ID + " ASC"
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID);
                int nameIdx = cursor.getColumnIndexOrThrow(COLUMN_NAME);
                int cropIdx = cursor.getColumnIndexOrThrow(COLUMN_CROP_NAMES);

                do {
                    Sector sector = new Sector(
                            cursor.getLong(idIdx),
                            cursor.getString(nameIdx),
                            cursor.getString(cropIdx)
                    );
                    sectors.add(sector);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return sectors;
    }

    public void deleteSector(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SECTORS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
