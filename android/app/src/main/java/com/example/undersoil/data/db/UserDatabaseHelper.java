package com.example.undersoil.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.undersoil.data.model.User;

import java.util.UUID;

public class UserDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "users.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD_HASH = "password_hash";
    public static final String COLUMN_ROLE = "role";
    public static final String COLUMN_IS_SYNCED = "is_synced";

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
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_EMAIL + " TEXT UNIQUE, " +
                COLUMN_PASSWORD_HASH + " TEXT, " +
                COLUMN_ROLE + " TEXT DEFAULT 'Farmer', " +
                COLUMN_IS_SYNCED + " INTEGER DEFAULT 0)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public String registerUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String userId = (user.getId() != null && !user.getId().trim().isEmpty())
                ? user.getId()
                : UUID.randomUUID().toString();

        values.put(COLUMN_ID, userId);
        values.put(COLUMN_NAME, user.getName());
        values.put(COLUMN_EMAIL, user.getEmail().trim().toLowerCase());
        values.put(COLUMN_PASSWORD_HASH, user.getPasswordHash());
        values.put(COLUMN_ROLE, user.getRole() != null ? user.getRole() : "Farmer");
        values.put(COLUMN_IS_SYNCED, user.isSynced() ? 1 : 0);

        long row = db.insert(TABLE_USERS, null, values);
        return (row != -1) ? userId : null;
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
                int idIdx = cursor.getColumnIndex(COLUMN_ID);
                int nameIdx = cursor.getColumnIndex(COLUMN_NAME);
                int emailIdx = cursor.getColumnIndex(COLUMN_EMAIL);
                int passIdx = cursor.getColumnIndex(COLUMN_PASSWORD_HASH);
                int roleIdx = cursor.getColumnIndex(COLUMN_ROLE);
                int syncIdx = cursor.getColumnIndex(COLUMN_IS_SYNCED);

                String id = (idIdx != -1) ? cursor.getString(idIdx) : UUID.randomUUID().toString();
                String name = (nameIdx != -1) ? cursor.getString(nameIdx) : "Farmer";
                String userEmail = (emailIdx != -1) ? cursor.getString(emailIdx) : email;
                String pass = (passIdx != -1) ? cursor.getString(passIdx) : passwordHash;
                String role = (roleIdx != -1) ? cursor.getString(roleIdx) : "Farmer";
                boolean isSynced = (syncIdx != -1) && (cursor.getInt(syncIdx) == 1);

                user = new User(id, name, userEmail, pass, role, isSynced);
            }
            cursor.close();
        }
        return user;
    }
}
