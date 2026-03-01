package org.example.fuelmanagementapi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "trip_number")
    private String tripNumber;

    @Column(name = "vehicle_id")
    private Integer vehicleId;

    @Column(name = "driver_id")
    private Integer driverId;

    @Column(name = "requester_id")
    private Integer requesterId;

    @Column(name = "requester_name")
    private String requesterName;

    @Column(name = "requester_email")
    private String requesterEmail;

    @Column(name = "requester_phone")
    private String requesterPhone;

    @Column(name = "start_address")
    private String startAddress;

    @Column(name = "end_address")
    private String endAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type")
    private TripType tripType;

    @Column(name = "planned_start_time", nullable = true)
    private LocalDateTime plannedStartTime;

    @Column(name = "planned_end_time", nullable = true)
    private LocalDateTime plannedEndTime;

    @Column(name = "waiting_time")
    private Integer waitingTime;

    @Column(name = "planned_distance")
    private BigDecimal plannedDistance;

    @Column(name = "planned_city_km")
    private BigDecimal plannedCityKm;

    @Column(name = "planned_highway_km")
    private BigDecimal plannedHighwayKm;

    @Column(name = "planned_fuel_consumption")
    private BigDecimal plannedFuelConsumption;

    @Column(name = "actual_start_odometer")
    private Integer actualStartOdometer;

    @Column(name = "actual_end_odometer")
    private Integer actualEndOdometer;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(name = "actual_distance")
    private BigDecimal actualDistance;

    @Column(name = "actual_city_km")
    private BigDecimal actualCityKm;

    @Column(name = "actual_highway_km")
    private BigDecimal actualHighwayKm;

    @Column(name = "actual_fuel_consumption")
    private BigDecimal actualFuelConsumption;

    @Column(name = "fuel_received")
    private BigDecimal fuelReceived;

    @Column(name = "fuel_received_coupons")
    private BigDecimal fuelReceivedCoupons;

    @Column(name = "fuel_received_money")
    private BigDecimal fuelReceivedMoney;

    private String purpose;

    @Column(name = "power_of_attorney")
    private String powerOfAttorney;

    @Column(name = "can_driver_deliver")
    private Boolean canDriverDeliver;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    @Enumerated(EnumType.STRING)
    private Season season;

    @Column(name = "route_modifications_count")
    private Integer routeModificationsCount;

    @Column(name = "route_deviation_percent")
    private BigDecimal routeDeviationPercent;

    @Column(name = "refrigerator_usage_percent")
    private BigDecimal refrigeratorUsagePercent;

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Transient
    private String driverFullName;  
    @Column(name = "created_by")
    private String createdBy;

    @Transient
    private String licensePlate;

    @Transient
    private String model;

    @Transient
    private Boolean vehicleHasRefrigerator;

    public enum Season {
        summer, winter;

        public static Season fromString(String value) {
            if (value == null) return summer;
            try {
                return Season.valueOf(value.toLowerCase());
            } catch (IllegalArgumentException e) {
                return summer;
            }
        }

        public static Season getCurrentSeason() {
            int month = LocalDateTime.now().getMonthValue();
            return (month >= 4 && month <= 10) ? summer : winter;
        }
    }

    public enum TripType {
        one_way, round_trip;

        public static TripType fromString(String value) {
            if (value == null) return round_trip;
            try {
                return TripType.valueOf(value.toLowerCase());
            } catch (IllegalArgumentException e) {
                return round_trip;
            }
        }
    }

    public enum TripStatus {
        created,    
        assigned,   
        started,    
        paused,     
        completed,  
        cancelled;  

        public String getValue() {
            return this.name();
        }

        public static TripStatus fromString(String value) {
            if (value == null) return created;

            try {
                return TripStatus.valueOf(value.toLowerCase());
            } catch (IllegalArgumentException e) {
                return created;
            }
        }

        public boolean isActive() {
            return this == started || this == paused;
        }

        public boolean isCompleted() {
            return this == completed || this == cancelled;
        }

        public boolean canBeStarted() {
            return this == assigned;
        }

        public boolean canBeFinished() {
            return this == started;
        }
    }

    public Trip() {
        this.status = TripStatus.created;
        this.tripType = TripType.round_trip;
        this.season = Season.summer;
        this.createdAt = LocalDateTime.now();
        this.canDriverDeliver = false;
        this.routeModificationsCount = 0;
        this.refrigeratorUsagePercent = BigDecimal.ZERO;
    }

    public boolean isAssigned() {
        return driverId != null && driverId > 0;
    }

    public boolean isCompleted() {
        return status != null && status.isCompleted();
    }

    public boolean isActive() {
        return status != null && status.isActive();
    }

    public boolean isStarted() {
        return status == TripStatus.started;
    }

    public boolean isNew() {
        return status == TripStatus.assigned || status == TripStatus.created;
    }

    public boolean canBeStarted() {
        return status != null && status.canBeStarted();
    }

    public boolean canBeFinished() {
        return status != null && status.canBeFinished();
    }

    public boolean hasStartOdometer() {
        return actualStartOdometer != null && actualStartOdometer > 0;
    }

    public boolean hasEndOdometer() {
        return actualEndOdometer != null && actualEndOdometer > 0;
    }

    public BigDecimal getCalculatedDistance() {
        if (hasStartOdometer() && hasEndOdometer()) {
            return BigDecimal.valueOf(actualEndOdometer - actualStartOdometer);
        }
        return null;
    }

    public String getStatusDisplayText() {
        if (status == null) return "Невідомо";

        switch (status) {
            case created: return "Створено";
            case assigned: return "Призначено";
            case started: return "В дорозі";
            case paused: return "Призупинено";
            case completed: return "Завершено";
            case cancelled: return "Скасовано";
            default: return status.getValue();
        }
    }

    public String getVehicleInfo() {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            return "Автомобіль не вказано";
        }

        StringBuilder vehicleInfo = new StringBuilder(licensePlate.trim());
        if (model != null && !model.trim().isEmpty()) {
            vehicleInfo.append("(").append(model.trim()).append(")");
        }

        return vehicleInfo.toString();
    }

    public String getRouteInfo() {
        if (startAddress == null && endAddress == null) {
            return "Маршрут не вказано";
        }

        String start = (startAddress != null && !startAddress.trim().isEmpty())
                ? startAddress.trim() : "Не вказано";
        String end = (endAddress != null && !endAddress.trim().isEmpty())
                ? endAddress.trim() : "Не вказано";

        return start + "→ " + end;
    }

    public boolean hasActualData() {
        return actualStartTime != null || actualEndTime != null ||
                (actualDistance != null && actualDistance.compareTo(BigDecimal.ZERO) > 0);
    }

    public String getTripDisplayNumber() {
        if (tripNumber != null && !tripNumber.trim().isEmpty()) {
            return tripNumber.trim();
        }
        return id != null ? "№" + id : "Без номеру";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTripNumber() { return tripNumber; }
    public void setTripNumber(String tripNumber) { this.tripNumber = tripNumber; }

    public Integer getVehicleId() { return vehicleId; }
    public void setVehicleId(Integer vehicleId) { this.vehicleId = vehicleId; }

    public Integer getDriverId() { return driverId; }
    public void setDriverId(Integer driverId) { this.driverId = driverId; }

    public Integer getRequesterId() { return requesterId; }
    public void setRequesterId(Integer requesterId) { this.requesterId = requesterId; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }

    public String getRequesterPhone() { return requesterPhone; }
    public void setRequesterPhone(String requesterPhone) { this.requesterPhone = requesterPhone; }

    public String getStartAddress() { return startAddress; }
    public void setStartAddress(String startAddress) { this.startAddress = startAddress; }

    public String getEndAddress() { return endAddress; }
    public void setEndAddress(String endAddress) { this.endAddress = endAddress; }

    public TripType getTripType() { return tripType; }
    public void setTripType(TripType tripType) { this.tripType = tripType; }

    public LocalDateTime getPlannedStartTime() { return plannedStartTime; }
    public void setPlannedStartTime(LocalDateTime plannedStartTime) { this.plannedStartTime = plannedStartTime; }

    public LocalDateTime getPlannedEndTime() { return plannedEndTime; }
    public void setPlannedEndTime(LocalDateTime plannedEndTime) { this.plannedEndTime = plannedEndTime; }

    public Integer getWaitingTime() { return waitingTime; }
    public void setWaitingTime(Integer waitingTime) { this.waitingTime = waitingTime; }

    public BigDecimal getPlannedDistance() { return plannedDistance; }
    public void setPlannedDistance(BigDecimal plannedDistance) { this.plannedDistance = plannedDistance; }

    public BigDecimal getPlannedCityKm() { return plannedCityKm; }
    public void setPlannedCityKm(BigDecimal plannedCityKm) { this.plannedCityKm = plannedCityKm; }

    public BigDecimal getPlannedHighwayKm() { return plannedHighwayKm; }
    public void setPlannedHighwayKm(BigDecimal plannedHighwayKm) { this.plannedHighwayKm = plannedHighwayKm; }

    public BigDecimal getPlannedFuelConsumption() { return plannedFuelConsumption; }
    public void setPlannedFuelConsumption(BigDecimal plannedFuelConsumption) { this.plannedFuelConsumption = plannedFuelConsumption; }

    public Integer getActualStartOdometer() { return actualStartOdometer; }
    public void setActualStartOdometer(Integer actualStartOdometer) { this.actualStartOdometer = actualStartOdometer; }

    public Integer getActualEndOdometer() { return actualEndOdometer; }
    public void setActualEndOdometer(Integer actualEndOdometer) { this.actualEndOdometer = actualEndOdometer; }

    public LocalDateTime getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(LocalDateTime actualStartTime) { this.actualStartTime = actualStartTime; }

    public LocalDateTime getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(LocalDateTime actualEndTime) { this.actualEndTime = actualEndTime; }

    public BigDecimal getActualDistance() { return actualDistance; }
    public void setActualDistance(BigDecimal actualDistance) { this.actualDistance = actualDistance; }

    public BigDecimal getActualCityKm() { return actualCityKm; }
    public void setActualCityKm(BigDecimal actualCityKm) { this.actualCityKm = actualCityKm; }

    public BigDecimal getActualHighwayKm() { return actualHighwayKm; }
    public void setActualHighwayKm(BigDecimal actualHighwayKm) { this.actualHighwayKm = actualHighwayKm; }

    public BigDecimal getActualFuelConsumption() { return actualFuelConsumption; }
    public void setActualFuelConsumption(BigDecimal actualFuelConsumption) { this.actualFuelConsumption = actualFuelConsumption; }

    public BigDecimal getFuelReceived() { return fuelReceived; }
    public void setFuelReceived(BigDecimal fuelReceived) { this.fuelReceived = fuelReceived; }

    public BigDecimal getFuelReceivedCoupons() { return fuelReceivedCoupons; }
    public void setFuelReceivedCoupons(BigDecimal fuelReceivedCoupons) { this.fuelReceivedCoupons = fuelReceivedCoupons; }

    public BigDecimal getFuelReceivedMoney() { return fuelReceivedMoney; }
    public void setFuelReceivedMoney(BigDecimal fuelReceivedMoney) { this.fuelReceivedMoney = fuelReceivedMoney; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getPowerOfAttorney() { return powerOfAttorney; }
    public void setPowerOfAttorney(String powerOfAttorney) { this.powerOfAttorney = powerOfAttorney; }

    public Boolean getCanDriverDeliver() { return canDriverDeliver; }
    public void setCanDriverDeliver(Boolean canDriverDeliver) { this.canDriverDeliver = canDriverDeliver; }

    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }

    public Season getSeason() { return season; }
    public void setSeason(Season season) { this.season = season; }

    public Integer getRouteModificationsCount() { return routeModificationsCount; }
    public void setRouteModificationsCount(Integer routeModificationsCount) { this.routeModificationsCount = routeModificationsCount; }

    public BigDecimal getRouteDeviationPercent() { return routeDeviationPercent; }
    public void setRouteDeviationPercent(BigDecimal routeDeviationPercent) { this.routeDeviationPercent = routeDeviationPercent; }

    public BigDecimal getRefrigeratorUsagePercent() { return refrigeratorUsagePercent; }
    public void setRefrigeratorUsagePercent(BigDecimal refrigeratorUsagePercent) { this.refrigeratorUsagePercent = refrigeratorUsagePercent; }

    public Boolean getVehicleHasRefrigerator() { return vehicleHasRefrigerator; }
    public void setVehicleHasRefrigerator(Boolean vehicleHasRefrigerator) { this.vehicleHasRefrigerator = vehicleHasRefrigerator; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public String getDriverFullName() { return driverFullName; }
    public void setDriverFullName(String driverFullName) { this.driverFullName = driverFullName; }

    @Override
    public String toString() {
        return "Trip{" +
                "id=" + id +
                ", tripNumber='" + tripNumber + '\'' +
                ", status=" + status +
                ", startAddress='" + startAddress + '\'' +
                ", endAddress='" + endAddress + '\'' +
                '}';
    }
}
