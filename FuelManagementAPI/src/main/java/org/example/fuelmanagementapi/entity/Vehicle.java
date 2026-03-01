package org.example.fuelmanagementapi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "license_plate", unique = true)
    private String licensePlate;

    @Column(name = "model")
    private String model;

    @Column(name = "fuel_type_id")
    private Integer fuelTypeId;

    @Column(name = "city_rate_summer")
    private BigDecimal cityRateSummer;

    @Column(name = "highway_rate_summer")
    private BigDecimal highwayRateSummer;

    @Column(name = "city_rate_winter")
    private BigDecimal cityRateWinter;

    @Column(name = "highway_rate_winter")
    private BigDecimal highwayRateWinter;

    @Column(name = "current_odometer")
    private Integer currentOdometer;

    @Column(name = "fuel_balance")
    private BigDecimal fuelBalance;

    @Column(name = "tank_capacity")
    private BigDecimal tankCapacity;

    @Column(name = "has_refrigerator")
    private Boolean hasRefrigerator;

    @Enumerated(EnumType.STRING)
    private VehicleStatus status;

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum VehicleStatus {
        active,
        maintenance,
        inactive,
        retired;

        public String getValue() {
            return this.name();
        }

        public static VehicleStatus fromString(String value) {
            if (value == null) return active;
            try {
                return VehicleStatus.valueOf(value.toLowerCase());
            } catch (IllegalArgumentException e) {
                return active;
            }
        }

        public boolean isAvailable() {
            return this == active;
        }

        @Override
        public String toString() {
            switch (this) {
                case active: return "Активний";
                case maintenance: return "На ТО";
                case inactive: return "Неактивний";
                case retired: return "Списаний";
                default: return name();
            }
        }
    }

    public Vehicle() {
        this.status = VehicleStatus.active;
        this.createdAt = LocalDateTime.now();
        this.currentOdometer = 0;
        this.fuelBalance = BigDecimal.ZERO;
        this.hasRefrigerator = false;
    }

    public Vehicle(String licensePlate, String model) {
        this();
        this.licensePlate = licensePlate;
        this.model = model;
    }

    public boolean isAvailable() {
        return status != null && status.isAvailable();
    }

    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (licensePlate != null) {
            sb.append(licensePlate);
        }
        if (model != null) {
            sb.append("(").append(model).append(")");
        }
        return sb.toString();
    }

    public BigDecimal calculateFuelConsumption(BigDecimal cityKm, BigDecimal highwayKm, boolean isWinter) {
        if (cityKm == null) cityKm = BigDecimal.ZERO;
        if (highwayKm == null) highwayKm = BigDecimal.ZERO;

        BigDecimal cityRate = isWinter ? cityRateWinter : cityRateSummer;
        BigDecimal highwayRate = isWinter ? highwayRateWinter : highwayRateSummer;

        if (cityRate == null) cityRate = BigDecimal.ZERO;
        if (highwayRate == null) highwayRate = BigDecimal.ZERO;

        BigDecimal cityConsumption = cityKm.multiply(cityRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal highwayConsumption = highwayKm.multiply(highwayRate).setScale(2, RoundingMode.HALF_UP);

        return cityConsumption.add(highwayConsumption);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getFuelTypeId() { return fuelTypeId; }
    public void setFuelTypeId(Integer fuelTypeId) { this.fuelTypeId = fuelTypeId; }

    public BigDecimal getCityRateSummer() { return cityRateSummer; }
    public void setCityRateSummer(BigDecimal cityRateSummer) { this.cityRateSummer = cityRateSummer; }

    public BigDecimal getHighwayRateSummer() { return highwayRateSummer; }
    public void setHighwayRateSummer(BigDecimal highwayRateSummer) { this.highwayRateSummer = highwayRateSummer; }

    public BigDecimal getCityRateWinter() { return cityRateWinter; }
    public void setCityRateWinter(BigDecimal cityRateWinter) { this.cityRateWinter = cityRateWinter; }

    public BigDecimal getHighwayRateWinter() { return highwayRateWinter; }
    public void setHighwayRateWinter(BigDecimal highwayRateWinter) { this.highwayRateWinter = highwayRateWinter; }

    public Integer getCurrentOdometer() { return currentOdometer; }
    public void setCurrentOdometer(Integer currentOdometer) { this.currentOdometer = currentOdometer; }

    public BigDecimal getFuelBalance() { return fuelBalance; }
    public void setFuelBalance(BigDecimal fuelBalance) { this.fuelBalance = fuelBalance; }

    public BigDecimal getTankCapacity() { return tankCapacity; }
    public void setTankCapacity(BigDecimal tankCapacity) { this.tankCapacity = tankCapacity; }

    public Boolean getHasRefrigerator() { return hasRefrigerator; }
    public void setHasRefrigerator(Boolean hasRefrigerator) { this.hasRefrigerator = hasRefrigerator; }

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
        return "Vehicle{" +
                "id=" + id +
                ", licensePlate='" + licensePlate + '\'' +
                ", model='" + model + '\'' +
                ", currentOdometer=" + currentOdometer +
                ", status=" + status +
                '}';
    }
}
