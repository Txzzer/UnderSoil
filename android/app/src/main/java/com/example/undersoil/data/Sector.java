package com.example.undersoil.data;

public class Sector {
    private long id;
    private String name;
    private String cropNames;

    public Sector() {
    }

    public Sector(long id, String name, String cropNames) {
        this.id = id;
        this.name = name;
        this.cropNames = cropNames;
    }

    public Sector(String name, String cropNames) {
        this.name = name;
        this.cropNames = cropNames;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
