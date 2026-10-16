package com.example.undersoil.data.model;

import java.util.UUID;

public class IotAlert {
    private String id;
    private String deviceId;
    private String userId;
    private String alertDesc;
    private String dateTime;
    private boolean isSynced;

    public IotAlert() {
        this.id = UUID.randomUUID().toString();
        this.isSynced = false;
    }

    public IotAlert(String id, String deviceId, String userId, String alertDesc, String dateTime, boolean isSynced) {
        this.id = (id != null && !id.trim().isEmpty()) ? id : UUID.randomUUID().toString();
        this.deviceId = deviceId;
        this.userId = userId;
        this.alertDesc = alertDesc;
        this.dateTime = dateTime;
        this.isSynced = isSynced;
    }

    public IotAlert(String deviceId, String userId, String alertDesc, String dateTime) {
        this.id = UUID.randomUUID().toString();
        this.deviceId = deviceId;
        this.userId = userId;
        this.alertDesc = alertDesc;
        this.dateTime = dateTime;
        this.isSynced = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAlertDesc() {
        return alertDesc;
    }

    public void setAlertDesc(String alertDesc) {
        this.alertDesc = alertDesc;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }
}
