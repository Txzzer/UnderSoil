package com.example.undersoil.data;

public class Sector {
    private long id;
    private long userId;
    private String name;
    private String cropNames;

    public Sector() {
    }

    public Sector(long id, long userId, String name, String cropNames) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.cropNames = cropNames;
    }

    public Sector(String name, String cropNames, long userId) {
        this.name = name;
        this.cropNames = cropNames;
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
}
