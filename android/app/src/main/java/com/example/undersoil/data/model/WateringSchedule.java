package com.example.undersoil.data.model;

import java.util.UUID;

public class WateringSchedule {

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_COMPLETED = "Completed";

    /** Placeholder flow rate until it's measured per device. */
    public static final double FLOW_RATE_LITERS_PER_MIN = 2.5;

    private String id;
    private String userId;
    private String sectorId;
    /** Start time as UTC epoch milliseconds. */
    private long startTime;
    private int durationMinutes;
    private double expectedVolume;
    private double actualVolume;
    private String status;
    private boolean isSynced;

    public WateringSchedule(String userId, String sectorId, long startTime, int durationMinutes) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.sectorId = sectorId;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.expectedVolume = durationMinutes * FLOW_RATE_LITERS_PER_MIN;
        this.actualVolume = 0;
        this.status = STATUS_PENDING;
        this.isSynced = false;
    }

    public WateringSchedule(String id, String userId, String sectorId, long startTime, int durationMinutes,
                            double expectedVolume, double actualVolume, String status, boolean isSynced) {
        this.id = (id != null && !id.trim().isEmpty()) ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.sectorId = sectorId;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.expectedVolume = expectedVolume;
        this.actualVolume = actualVolume;
        this.status = status;
        this.isSynced = isSynced;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getSectorId() {
        return sectorId;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public double getExpectedVolume() {
        return expectedVolume;
    }

    public double getActualVolume() {
        return actualVolume;
    }

    public String getStatus() {
        return status;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }
}
