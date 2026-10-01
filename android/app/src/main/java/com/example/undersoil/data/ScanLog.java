package com.example.undersoil.data;

import java.util.UUID;

public class ScanLog {
    private String id;
    private String userId;
    private String cropName;
    private String diagnosis;
    private float confidence;
    private String dateStr;
    private boolean isSynced;

    public ScanLog() {
        this.id = UUID.randomUUID().toString();
        this.isSynced = false;
    }

    public ScanLog(String id, String userId, String cropName, String diagnosis, float confidence, String dateStr, boolean isSynced) {
        this.id = (id != null && !id.trim().isEmpty()) ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.cropName = cropName;
        this.diagnosis = diagnosis;
        this.confidence = confidence;
        this.dateStr = dateStr;
        this.isSynced = isSynced;
    }

    public ScanLog(String cropName, String diagnosis, float confidence, String dateStr, String userId) {
        this.id = UUID.randomUUID().toString();
        this.cropName = cropName;
        this.diagnosis = diagnosis;
        this.confidence = confidence;
        this.dateStr = dateStr;
        this.userId = userId;
        this.isSynced = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public String getDateStr() {
        return dateStr;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }
}
