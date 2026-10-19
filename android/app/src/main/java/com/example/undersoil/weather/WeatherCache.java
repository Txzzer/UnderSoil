package com.example.undersoil.weather;

import android.content.Context;
import android.content.SharedPreferences;

// Last successful location + weather, so Home can show conditions instantly and offline.
public class WeatherCache {

    private static final String PREF_NAME = "undersoil_weather_cache";
    private static final String KEY_PLACE = "place";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_HUMIDITY = "humidity";
    private static final String KEY_CODE = "weather_code";
    private static final String KEY_IS_DAY = "is_day";
    private static final String KEY_FETCHED_AT = "fetched_at";

    private final SharedPreferences pref;

    public WeatherCache(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void save(String place, WeatherResponse.Current current) {
        pref.edit()
                .putString(KEY_PLACE, place)
                .putFloat(KEY_TEMPERATURE, (float) current.getTemperature())
                .putInt(KEY_HUMIDITY, current.getHumidity())
                .putInt(KEY_CODE, current.getWeatherCode())
                .putBoolean(KEY_IS_DAY, current.isDay())
                .putLong(KEY_FETCHED_AT, System.currentTimeMillis())
                .apply();
    }

    public boolean hasData() {
        return pref.contains(KEY_FETCHED_AT);
    }

    public boolean isOlderThan(long maxAgeMillis) {
        long fetchedAt = pref.getLong(KEY_FETCHED_AT, 0);
        long age = System.currentTimeMillis() - fetchedAt;
        // A negative age means the clock moved backwards; treat that as stale too
        return fetchedAt == 0 || age < 0 || age > maxAgeMillis;
    }

    public String getPlace() {
        return pref.getString(KEY_PLACE, "");
    }

    public float getTemperature() {
        return pref.getFloat(KEY_TEMPERATURE, 0f);
    }

    public int getHumidity() {
        return pref.getInt(KEY_HUMIDITY, 0);
    }

    public int getWeatherCode() {
        return pref.getInt(KEY_CODE, -1);
    }

    public boolean isDay() {
        return pref.getBoolean(KEY_IS_DAY, true);
    }
}
