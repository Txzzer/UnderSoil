package com.example.undersoil.data;

import java.util.UUID;

public class User {
    private String id;
    private String name;
    private String email;
    private String passwordHash;
    private String role;
    private boolean isSynced;

    public User() {
        this.id = UUID.randomUUID().toString();
        this.role = "Farmer";
        this.isSynced = false;
    }

    public User(String id, String name, String email, String passwordHash, String role, boolean isSynced) {
        this.id = (id != null && !id.trim().isEmpty()) ? id : UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = (role != null) ? role : "Farmer";
        this.isSynced = isSynced;
    }

    public User(String name, String email, String passwordHash, String role) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = (role != null) ? role : "Farmer";
        this.isSynced = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }
}
