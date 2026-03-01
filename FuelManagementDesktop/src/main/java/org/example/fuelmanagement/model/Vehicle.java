package org.example.fuelmanagement.model;

import org.example.fuelmanagement.model.enums.VehicleStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class Vehicle {
    private int id;
    private String licensePlate;
    private String model;
    private int fuelTypeId;
    private String fuelTypeName;

    private BigDecimal cityRateSummer;
    private BigDecimal highwayRateSummer;
    private BigDecimal cityRateWinter;
    private BigDecimal highwayRateWinter;
    private BigDecimal idleRateSummer;
    private BigDecimal idleRateWinter;

    private int currentOdometer;
    private BigDecimal fuelBalance;
    private BigDecimal tankCapacity;
    private boolean hasRefrigerator;

    private VehicleStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Vehicle() {
        this.status = VehicleStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.fuelBalance = BigDecimal.ZERO;
    }

    public Vehicle(String licensePlate, String model, int fuelTypeId) {
        this();
        this.licensePlate = licensePlate;
        this.model = model;
        this.fuelTypeId = fuelTypeId;
    }

    public boolean hasEnoughFuel(BigDecimal requiredFuel) {
        if (fuelBalance == null || requiredFuel == null) {
            return false;
        }
        return fuelBalance.compareTo(requiredFuel) >= 0;
    }

    public BigDecimal getFuelShortage(BigDecimal requiredFuel) {
        if (fuelBalance == null || requiredFuel == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal shortage = requiredFuel.subtract(fuelBalance);
        return shortage.compareTo(BigDecimal.ZERO) > 0 ?
                shortage.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public boolean canRefill(BigDecimal amount) {
        if (tankCapacity == null || fuelBalance == null || amount == null) {
            return false;
        }
        return fuelBalance.add(amount).compareTo(tankCapacity) <= 0;
    }

    public BigDecimal getAvailableCapacity() {
        if (tankCapacity == null || fuelBalance == null) {
            return BigDecimal.ZERO;
        }
        return tankCapacity.subtract(fuelBalance);
    }

    public String getCityRateDisplay(boolean isSummer) {
        BigDecimal rate = isSummer ? cityRateSummer : cityRateWinter;
        if (rate == null) return "не задано";

        BigDecimal per100km = rate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        return per100km + " л/100км";
    }

    public String getHighwayRateDisplay(boolean isSummer) {
        BigDecimal rate = isSummer ? highwayRateSummer : highwayRateWinter;
        if (rate == null) return "не задано";

        BigDecimal per100km = rate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        return per100km + " л/100км";
    }

    public String getDisplayName() {
        StringBuilder display = new StringBuilder();
        display.append(licensePlate);
        if (model != null && !model.isEmpty()) {
            display.append(" (").append(model).append(")");
        }
        return display.toString();
    }

    public String getFullDisplayName() {
        StringBuilder display = new StringBuilder(getDisplayName());
        if (fuelTypeName != null) {
            display.append(" - ").append(fuelTypeName);
        }
        display.append(" [").append(fuelBalance).append("л/").append(tankCapacity).append("л]");
        return display.toString();
    }

    public String getStatusDisplayName() {
        return getDisplayName() + " (" + status.toString() + ")";
    }

    public boolean isAvailable() {
        return status == VehicleStatus.ACTIVE;
    }

    public boolean isOnMaintenance() {
        return status == VehicleStatus.MAINTENANCE;
    }

    public boolean needsRefueling() {
        if (fuelBalance == null || tankCapacity == null) {
            return false;
        }

        BigDecimal threshold = tankCapacity.multiply(BigDecimal.valueOf(0.25));
        return fuelBalance.compareTo(threshold) < 0;
    }

    public boolean isFuelCritical() {
        if (fuelBalance == null || tankCapacity == null) {
            return false;
        }

        BigDecimal threshold = tankCapacity.multiply(BigDecimal.valueOf(0.10));
        return fuelBalance.compareTo(threshold) < 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getFuelTypeId() { return fuelTypeId; }
    public void setFuelTypeId(int fuelTypeId) { this.fuelTypeId = fuelTypeId; }

    public String getFuelTypeName() { return fuelTypeName; }
    public void setFuelTypeName(String fuelTypeName) { this.fuelTypeName = fuelTypeName; }

    public BigDecimal getCityRateSummer() { return cityRateSummer; }
    public void setCityRateSummer(BigDecimal cityRateSummer) { this.cityRateSummer = cityRateSummer; }

    public BigDecimal getHighwayRateSummer() { return highwayRateSummer; }
    public void setHighwayRateSummer(BigDecimal highwayRateSummer) { this.highwayRateSummer = highwayRateSummer; }

    public BigDecimal getCityRateWinter() { return cityRateWinter; }
    public void setCityRateWinter(BigDecimal cityRateWinter) { this.cityRateWinter = cityRateWinter; }

    public BigDecimal getHighwayRateWinter() { return highwayRateWinter; }
    public void setHighwayRateWinter(BigDecimal highwayRateWinter) { this.highwayRateWinter = highwayRateWinter; }

    public BigDecimal getIdleRateSummer() { return idleRateSummer; }
    public void setIdleRateSummer(BigDecimal idleRateSummer) { this.idleRateSummer = idleRateSummer; }

    public BigDecimal getIdleRateWinter() { return idleRateWinter; }
    public void setIdleRateWinter(BigDecimal idleRateWinter) { this.idleRateWinter = idleRateWinter; }

    public int getCurrentOdometer() { return currentOdometer; }
    public void setCurrentOdometer(int currentOdometer) { this.currentOdometer = currentOdometer; }

    public BigDecimal getFuelBalance() { return fuelBalance; }
    public void setFuelBalance(BigDecimal fuelBalance) { this.fuelBalance = fuelBalance; }

    public BigDecimal getTankCapacity() { return tankCapacity; }
    public void setTankCapacity(BigDecimal tankCapacity) { this.tankCapacity = tankCapacity; }

    public boolean hasRefrigerator() { return hasRefrigerator; }
    public void setHasRefrigerator(boolean hasRefrigerator) { this.hasRefrigerator = hasRefrigerator; }

    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public String getFuelRateInfo(boolean isSummer) {
        BigDecimal cityRate = isSummer ? cityRateSummer : cityRateWinter;
        BigDecimal highwayRate = isSummer ? highwayRateSummer : highwayRateWinter;

        return String.format("Місто: %.2f л/км, Траса: %.2f л/км (%s)",
                cityRate.doubleValue(),
                highwayRate.doubleValue(),
                isSummer ? "літо" : "зима");
    }

    public String getFuelBalanceStatus() {
        if (fuelBalance == null) {
            return "Невідомо";
        }

        if (fuelBalance.compareTo(BigDecimal.valueOf(10)) < 0) {
            return " Низький рівень";
        } else if (fuelBalance.compareTo(BigDecimal.valueOf(5)) < 0) {
            return "🔴 Критично низький";
        } else {
            return " Достатньо";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vehicle vehicle = (Vehicle) obj;
        return id == vehicle.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}