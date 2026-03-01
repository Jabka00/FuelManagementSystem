package com.example.fuelmanagementapp.models;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class Vehicle {
    private Integer id;

    @SerializedName("licensePlate")
    private String licensePlate;

    private String model;

    @SerializedName("fuelTypeId")
    private Integer fuelTypeId;

    @SerializedName("currentOdometer")
    private Integer currentOdometer;

    @SerializedName("fuelBalance")
    private BigDecimal fuelBalance;

    @SerializedName("tankCapacity")
    private BigDecimal tankCapacity;

    @SerializedName("hasRefrigerator")
    private Boolean hasRefrigerator;

    private String status;
    private String notes;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Vehicle() {}

    public Vehicle(String licensePlate, String model) {
        this.licensePlate = licensePlate;
        this.model = model;
    }

    public boolean isAvailable() {
        return "active".equalsIgnoreCase(status);
    }

    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (licensePlate != null && !licensePlate.trim().isEmpty()) {
            sb.append(licensePlate.trim());
        }
        if (model != null && !model.trim().isEmpty()) {
            sb.append("(").append(model.trim()).append(")");
        }
        return sb.length() > 0 ? sb.toString() : "Автомобіль не вказано";
    }

    public String getStatusDisplayText() {
        if (status == null) return "Невідомо";

        switch (status.toLowerCase()) {
            case "active": return "Активний";
            case "maintenance": return "На ТО";
            case "inactive": return "Неактивний";
            case "retired": return "Списаний";
            default: return status;
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getFuelTypeId() {
        return fuelTypeId;
    }

    public void setFuelTypeId(Integer fuelTypeId) {
        this.fuelTypeId = fuelTypeId;
    }

    public Integer getCurrentOdometer() {
        return currentOdometer;
    }

    public void setCurrentOdometer(Integer currentOdometer) {
        this.currentOdometer = currentOdometer;
    }

    public BigDecimal getFuelBalance() {
        return fuelBalance;
    }

    public void setFuelBalance(BigDecimal fuelBalance) {
        this.fuelBalance = fuelBalance;
    }

    public BigDecimal getTankCapacity() {
        return tankCapacity;
    }

    public void setTankCapacity(BigDecimal tankCapacity) {
        this.tankCapacity = tankCapacity;
    }

    public Boolean getHasRefrigerator() {
        return hasRefrigerator;
    }

    public void setHasRefrigerator(Boolean hasRefrigerator) {
        this.hasRefrigerator = hasRefrigerator;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id=" + id +
                ", licensePlate='" + licensePlate + '\'' +
                ", model='" + model + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
