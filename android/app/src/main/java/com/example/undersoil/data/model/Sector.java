package com.example.undersoil.data.model;

import java.util.UUID;

public class Sector {
    private String id;
    private String userId;
    private String name;
    private String cropNames;
    private boolean isSynced;

    public Sector() {
        this.id = UUID.randomUUID().toString();
        this.isSynced = false;
    }

    public Sector(String id, String userId, String name, String cropNames, boolean isSynced) {
        this.id = (id != null && !id.trim().isEmpty()) ? id : UUID.randomUUID().toString();
        this.userId = userId;
        this.name = name;
        this.cropNames = cropNames;
        this.isSynced = isSynced;
    }

    public Sector(String name, String cropNames, String userId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.cropNames = cropNames;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCropNames() {
        return cropNames;
    }

    public void setCropNames(String cropNames) {
        this.cropNames = cropNames;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }
}
