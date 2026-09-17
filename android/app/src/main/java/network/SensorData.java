package network;

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
}
