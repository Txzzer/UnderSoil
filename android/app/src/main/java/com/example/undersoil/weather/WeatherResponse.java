package com.example.undersoil.weather;

import com.google.gson.annotations.SerializedName;

// Open-Meteo forecast response; only the "current" block is requested.
public class WeatherResponse {

    @SerializedName("current")
    private Current current;

    public Current getCurrent() {
        return current;
    }

    public static class Current {
        @SerializedName("temperature_2m")
        private double temperature;

        @SerializedName("relative_humidity_2m")
        private int humidity;

        @SerializedName("weather_code")
        private int weatherCode;

        @SerializedName("is_day")
        private int isDay;

        public double getTemperature() {
            return temperature;
        }

        public int getHumidity() {
            return humidity;
        }

        public int getWeatherCode() {
            return weatherCode;
        }

        public boolean isDay() {
            return isDay == 1;
        }
    }
}
