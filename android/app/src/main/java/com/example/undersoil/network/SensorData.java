package com.example.undersoil.network;

import com.google.gson.annotations.SerializedName;

public class SensorData {
    @SerializedName(value = "id", alternate = {"Id", "ID"})
    private long id;

    @SerializedName(value = "deviceId", alternate = {"DeviceId", "device_id", "DeviceID"})
    private String deviceId;

    @SerializedName(value = "flowRateLitersPerMin", alternate = {"FlowRateLitersPerMin", "flowRate", "FlowRate", "flow_rate_liters_per_min"})
    private double flowRateLitersPerMin;

    @SerializedName(value = "valveIsOpen", alternate = {"ValveIsOpen", "valveOpen", "ValveOpen", "isValveOpen", "IsValveOpen", "valve_is_open"})
    private boolean valveIsOpen;

    @SerializedName(value = "temperatureCelsius", alternate = {"TemperatureCelsius", "temperature", "Temperature", "temp", "Temp", "temperature_celsius"})
    private double temperatureCelsius;

    @SerializedName(value = "humidityPercentage", alternate = {"HumidityPercentage", "humidity", "Humidity", "humidity_percentage"})
    private double humidityPercentage;

    @SerializedName(value = "timestamp", alternate = {"Timestamp", "time", "Time", "created_at", "CreatedAt"})
    private String timestamp;

    // Computed by the backend from how long ago it last received a reading; null on older backends
    @SerializedName(value = "isOnline", alternate = {"IsOnline"})
    private Boolean isOnline;

    @SerializedName(value = "secondsSinceLastReading", alternate = {"SecondsSinceLastReading"})
    private Double secondsSinceLastReading;

    public SensorData() {
    }

    public long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public double getFlowRateLitersPerMin() {
        return flowRateLitersPerMin;
    }

    public boolean isValveIsOpen() {
        return valveIsOpen;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public double getHumidityPercentage() {
        return humidityPercentage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    /** False when the device has stopped reporting; treated as online if the backend doesn't send the field. */
    public boolean isOnline() {
        return isOnline == null || isOnline;
    }

    /** Seconds since the backend last received a reading, or null if unknown. */
    public Double getSecondsSinceLastReading() {
        return secondsSinceLastReading;
    }
}
