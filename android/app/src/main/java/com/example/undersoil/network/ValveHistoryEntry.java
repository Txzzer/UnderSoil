package com.example.undersoil.network;

import com.google.gson.annotations.SerializedName;

// One valve open/close action returned by GET api/v1/Valve/history
public class ValveHistoryEntry {
    @SerializedName(value = "valveIsOpen", alternate = {"ValveIsOpen", "valveOpen", "ValveOpen", "isValveOpen", "IsValveOpen", "isOpen", "IsOpen", "valve_is_open"})
    private boolean valveIsOpen;

    @SerializedName(value = "timestamp", alternate = {"Timestamp", "issuedAt", "IssuedAt", "time", "Time", "createdAt", "CreatedAt", "created_at"})
    private String timestamp;

    public ValveHistoryEntry() {
    }

    public boolean isValveIsOpen() {
        return valveIsOpen;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
