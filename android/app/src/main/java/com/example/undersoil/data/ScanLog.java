package com.example.undersoil.data;

public class ScanLog {
    private long id;
    private long userId;
    private String cropName;
    private String diagnosis;
    private float confidence;
    private String dateStr;

    public ScanLog() {
    }

    public ScanLog(long id, long userId, String cropName, String diagnosis, float confidence, String dateStr) {
        this.id = id;
        this.userId = userId;
        this.cropName = cropName;
        this.diagnosis = diagnosis;
        this.confidence = confidence;
        this.dateStr = dateStr;
    }

    public ScanLog(String cropName, String diagnosis, float confidence, String dateStr, long userId) {
        this.cropName = cropName;
        this.diagnosis = diagnosis;
        this.confidence = confidence;
        this.dateStr = dateStr;
        this.userId = userId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
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
}
