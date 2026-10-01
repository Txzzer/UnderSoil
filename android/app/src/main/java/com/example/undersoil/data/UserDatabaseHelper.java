package com.example.undersoil.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UserDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "users.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD_HASH = "password_hash";
    public static final String COLUMN_ROLE = "role";

    private static UserDatabaseHelper instance;

    public static synchronized UserDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new UserDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public UserDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_EMAIL + " TEXT UNIQUE, " +
                COLUMN_PASSWORD_HASH + " TEXT, " +
                COLUMN_ROLE + " TEXT DEFAULT 'Farmer')";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public long registerUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, user.getName());
        values.put(COLUMN_EMAIL, user.getEmail().trim().toLowerCase());
        values.put(COLUMN_PASSWORD_HASH, user.getPasswordHash());
        values.put(COLUMN_ROLE, user.getRole() != null ? user.getRole() : "Farmer");

        return db.insert(TABLE_USERS, null, values);
    }

    public boolean checkEmailExists(String email) {
        if (email == null) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_ID},
                "LOWER(" + COLUMN_EMAIL + ") = LOWER(?)",
                new String[]{email.trim()},
                null, null, null
        );

        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    public User getAuthUser(String email, String passwordHash) {
        if (email == null || passwordHash == null) return null;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                null,
                "LOWER(" + COLUMN_EMAIL + ") = LOWER(?) AND " + COLUMN_PASSWORD_HASH + " = ?",
                new String[]{email.trim(), passwordHash},
                null, null, null
        );

        User user = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID);
                int nameIdx = cursor.getColumnIndexOrThrow(COLUMN_NAME);
                int emailIdx = cursor.getColumnIndexOrThrow(COLUMN_EMAIL);
                int passIdx = cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH);
                int roleIdx = cursor.getColumnIndexOrThrow(COLUMN_ROLE);

                user = new User(
                        cursor.getLong(idIdx),
                        cursor.getString(nameIdx),
                        cursor.getString(emailIdx),
                        cursor.getString(passIdx),
                        cursor.getString(roleIdx)
                );
            }
            cursor.close();
        }
        return user;
    }
}
