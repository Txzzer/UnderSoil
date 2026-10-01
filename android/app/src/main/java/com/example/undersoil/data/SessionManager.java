package com.example.undersoil.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.undersoil.Log_in;

public class SessionManager {

    private static final String PREF_NAME = "undersoil_farmer_session";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_TOKEN = "jwt_token";

    private static SessionManager instance;
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    private SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // UPDATED: userId is now an int, and we added the token parameter
    public void createLoginSession(int userId, String name, String email, String role, String token) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role != null ? role : "Farmer");

        // Save the JWT Token from the backend
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public void createLoginSession(String userId, String name, String email, String role) {
        int idInt = -1;
        try {
            if (userId != null) idInt = Integer.parseInt(userId);
        } catch (NumberFormatException ignored) {}

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, idInt);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role != null ? role : "Farmer");
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "Farmer");
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "Farmer");
    }

    // Safely retrieves user ID whether stored as int or String in SharedPreferences
    public int getUserId() {
        try {
            return pref.getInt(KEY_USER_ID, -1);
        } catch (ClassCastException e) {
            try {
                String strVal = pref.getString(KEY_USER_ID, "-1");
                return Integer.parseInt(strVal);
            } catch (Exception ignored) {
                return -1;
            }
        }
    }

    public String getUserIdString() {
        try {
            String strVal = pref.getString(KEY_USER_ID, null);
            if (strVal != null && !strVal.isEmpty()) {
                return strVal;
            }
        } catch (ClassCastException ignored) {
        }
        int intId = getUserId();
        return intId != -1 ? String.valueOf(intId) : "";
    }

    // NEW: Method to retrieve the token so you can attach it to future network calls
    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public void logoutUser(Context context) {
        editor.clear();
        editor.apply();

        Intent intent = new Intent(context, Log_in.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}