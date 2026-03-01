package org.example.fuelmanagement.model;

import java.time.LocalDateTime;

public class FuelType {
    private int id;
    private String name;
    private LocalDateTime createdAt;

    public FuelType() {}

    public FuelType(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public FuelType(int id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FuelType fuelType = (FuelType) obj;
        return id == fuelType.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}