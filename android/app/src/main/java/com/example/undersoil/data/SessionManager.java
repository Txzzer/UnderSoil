package com.example.undersoil.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.example.undersoil.auth.LoginActivity;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Instant;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SessionManager {

    public enum InvalidReason {
        NONE,
        NOT_LOGGED_IN,
        EXPIRED,
        CLOCK_ROLLBACK
    }

    private static final String PREF_NAME = "undersoil_farmer_session";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_EXPIRY_MILLIS = "token_expiry_millis";
    private static final String KEY_LAST_SEEN_TIME = "last_seen_time";
    private static final String KEY_INVALID_REASON = "last_invalid_reason";

    private static final String KEYSTORE_ALIAS = "undersoil_jwt_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;

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

    public void createLoginSession(String userId, String name, String email, String role, String token, String expiresAt) {
        long expiryMillis = parseExpiryMillis(expiresAt, token);
        long now = System.currentTimeMillis();

        String encryptedToken = encryptToken(token);

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId != null ? userId : "");
        editor.putString(KEY_USER_NAME, name != null ? name : "Farmer");
        editor.putString(KEY_USER_EMAIL, email != null ? email : "");
        editor.putString(KEY_USER_ROLE, role != null ? role : "Farmer");
        editor.putString(KEY_TOKEN, encryptedToken);
        editor.putLong(KEY_EXPIRY_MILLIS, expiryMillis);
        editor.putLong(KEY_LAST_SEEN_TIME, now);
        editor.putString(KEY_INVALID_REASON, InvalidReason.NONE.name());
        editor.apply();
    }

    public void createLoginSession(String userId, String name, String email, String role, String token) {
        createLoginSession(userId, name, email, role, token, null);
    }

    public void createLoginSession(String userId, String name, String email, String role) {
        createLoginSession(userId, name, email, role, "", null);
    }

    public InvalidReason getInvalidReason() {
        if (!pref.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return InvalidReason.NOT_LOGGED_IN;
        }

        long now = System.currentTimeMillis();
        long lastSeen = pref.getLong(KEY_LAST_SEEN_TIME, 0);

        // A clock set back more than 5 minutes could be used to dodge token expiry
        if (lastSeen > 0 && (lastSeen - now) > 5 * 60 * 1000) {
            return InvalidReason.CLOCK_ROLLBACK;
        }

        long expiryMillis = pref.getLong(KEY_EXPIRY_MILLIS, 0);
        if (expiryMillis > 0 && now >= expiryMillis) {
            return InvalidReason.EXPIRED;
        }

        String rawToken = pref.getString(KEY_TOKEN, null);
        if (rawToken == null || rawToken.isEmpty()) {
            return InvalidReason.NOT_LOGGED_IN;
        }

        return InvalidReason.NONE;
    }

    public boolean isLoggedIn() {
        InvalidReason reason = getInvalidReason();
        if (reason != InvalidReason.NONE) {
            setLastInvalidReason(reason);
            return false;
        }

        // Remember the latest time seen so a rolled-back clock can be spotted
        long now = System.currentTimeMillis();
        long lastSeen = pref.getLong(KEY_LAST_SEEN_TIME, 0);
        if (now > lastSeen) {
            editor.putLong(KEY_LAST_SEEN_TIME, now).apply();
        }

        return true;
    }

    public String getToken() {
        if (!isLoggedIn()) {
            return null;
        }
        String encryptedToken = pref.getString(KEY_TOKEN, null);
        if (encryptedToken == null || encryptedToken.isEmpty()) {
            return null;
        }
        return decryptToken(encryptedToken);
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

    public String getUserIdString() {
        try {
            String strVal = pref.getString(KEY_USER_ID, null);
            if (strVal != null && !strVal.isEmpty()) {
                return strVal;
            }
        } catch (ClassCastException ignored) {
            int intVal = pref.getInt(KEY_USER_ID, -1);
            if (intVal != -1) return String.valueOf(intVal);
        }
        return "";
    }

    public int getUserId() {
        try {
            String strVal = getUserIdString();
            return !strVal.isEmpty() ? Integer.parseInt(strVal) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public InvalidReason getLastInvalidReason() {
        String name = pref.getString(KEY_INVALID_REASON, InvalidReason.NONE.name());
        try {
            return InvalidReason.valueOf(name);
        } catch (Exception e) {
            return InvalidReason.NONE;
        }
    }

    public void setLastInvalidReason(InvalidReason reason) {
        editor.putString(KEY_INVALID_REASON, reason != null ? reason.name() : InvalidReason.NONE.name()).apply();
    }

    public void clearSessionOnly(InvalidReason reason) {
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_ROLE);
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_EXPIRY_MILLIS);
        editor.putString(KEY_INVALID_REASON, reason != null ? reason.name() : InvalidReason.EXPIRED.name());
        editor.apply();
    }

    public void logoutUser(Context context) {
        clearSessionOnly(InvalidReason.NOT_LOGGED_IN);

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private long parseExpiryMillis(String expiresAt, String token) {
        if (expiresAt != null && !expiresAt.trim().isEmpty()) {
            try {
                return Instant.parse(expiresAt.trim()).toEpochMilli();
            } catch (Exception ignored) {
            }
        }

        // Fallback: decode JWT exp claim
        if (token != null && !token.trim().isEmpty()) {
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payload = parts[1];
                    byte[] bytes = Base64.decode(payload, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
                    String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                    JSONObject jsonObject = new JSONObject(jsonStr);
                    if (jsonObject.has("exp")) {
                        long expSec = jsonObject.getLong("exp");
                        return expSec * 1000L;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Default: 7 days from now
        return System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEYSTORE_ALIAS, null);
            return entry.getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec parameterSpec = new KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();

        keyGenerator.init(parameterSpec);
        return keyGenerator.generateKey();
    }

    private String encryptToken(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        try {
            SecretKey secretKey = getOrCreateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            // Fallback if Keystore fails on device
            return plainText;
        }
    }

    private String decryptToken(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return null;
        try {
            byte[] combined = Base64.decode(encryptedText, Base64.DEFAULT);
            if (combined.length <= 12) return encryptedText;

            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);

            SecretKey secretKey = getOrCreateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback for unencrypted legacy stored token
            return encryptedText;
        }
    }
}
