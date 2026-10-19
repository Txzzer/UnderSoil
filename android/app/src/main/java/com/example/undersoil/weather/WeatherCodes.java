package com.example.undersoil.weather;

import com.example.undersoil.R;

// Maps WMO weather interpretation codes (as returned by Open-Meteo) to a label and icon.
public final class WeatherCodes {

    private WeatherCodes() {
    }

    public static String label(int code, boolean isDay) {
        switch (code) {
            case 0:
                return isDay ? "Sunny" : "Clear night";
            case 1:
                return "Mostly clear";
            case 2:
                return "Partly cloudy";
            case 3:
                return "Overcast";
            case 45:
            case 48:
                return "Fog";
            case 51:
            case 53:
            case 55:
            case 56:
            case 57:
                return "Drizzle";
            case 61:
            case 63:
            case 65:
            case 66:
            case 67:
                return "Rain";
            case 71:
            case 73:
            case 75:
            case 77:
            case 85:
            case 86:
                return "Snow";
            case 80:
            case 81:
            case 82:
                return "Rain showers";
            case 95:
            case 96:
            case 99:
                return "Thunderstorm";
            default:
                return "Unknown";
        }
    }

    public static int iconRes(int code) {
        if (code == 0 || code == 1) return R.drawable.ic_sun;
        if (code >= 51 && code <= 67) return R.drawable.ic_drop;
        if (code >= 80 && code <= 82) return R.drawable.ic_drop;
        if (code >= 95) return R.drawable.ic_drop;
        return R.drawable.cloud;
    }

    // Vector icons are drawn in white and need a tint; the cloud bitmap does not (returns 0).
    public static int iconTintRes(int code) {
        int icon = iconRes(code);
        if (icon == R.drawable.ic_sun) return R.color.us_amber;
        if (icon == R.drawable.ic_drop) return R.color.us_aqua;
        return 0;
    }
}
