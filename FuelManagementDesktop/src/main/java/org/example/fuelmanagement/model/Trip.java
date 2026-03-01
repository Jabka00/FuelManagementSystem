package org.example.fuelmanagement.model;

import org.example.fuelmanagement.model.enums.Season;
import org.example.fuelmanagement.model.enums.TripStatus;
import org.example.fuelmanagement.model.enums.TripType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Trip {
    private int id;
    private String tripNumber;

    private int vehicleId;
    private Integer driverId;
    private int requesterId;

    private Vehicle vehicle;
    private Driver driver;
    private Requester requester;

    private String requesterName;
    private String requesterEmail;
    private String requesterPhone;

    private String startAddress;
    private String endAddress;
    private TripType tripType;

    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private int waitingTime;

    private BigDecimal plannedDistance;
    private BigDecimal plannedCityKm;
    private BigDecimal plannedHighwayKm;
    private BigDecimal plannedFuelConsumption;

    private Integer actualStartOdometer;
    private Integer actualEndOdometer;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;

    private BigDecimal actualDistance;
    private BigDecimal actualCityKm;
    private BigDecimal actualHighwayKm;
    private BigDecimal actualFuelConsumption;

    private BigDecimal fuelReceived;
    private BigDecimal fuelReceivedCoupons;
    private BigDecimal fuelReceivedMoney;

    private String purpose;
    private String powerOfAttorney;
    private boolean canDriverDeliver;

    private TripStatus status;

    private Season season;
    private int routeModificationsCount;
    private BigDecimal routeDeviationPercent;
    private BigDecimal refrigeratorUsagePercent;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    private List<Waypoint> waypoints;

    public Trip() {
        this.status = TripStatus.CREATED;
        this.tripType = TripType.ROUND_TRIP;
        this.createdAt = LocalDateTime.now();
        this.waitingTime = 0;
        this.routeModificationsCount = 0;
        this.canDriverDeliver = false;
        this.fuelReceived = BigDecimal.ZERO;
        this.fuelReceivedCoupons = BigDecimal.ZERO;
        this.fuelReceivedMoney = BigDecimal.ZERO;
        this.refrigeratorUsagePercent = BigDecimal.ZERO;
        this.waypoints = new ArrayList<>();
    }

    public boolean isAssigned() {
        return driverId != null && driverId > 0;
    }

    public boolean isCompleted() {
        return status.isCompleted();
    }

    public boolean isActive() {
        return status.isActive();
    }

    public BigDecimal getTotalDistance() {
        return actualDistance != null ? actualDistance : plannedDistance;
    }

    public BigDecimal getTotalFuelConsumption() {
        return actualFuelConsumption != null ? actualFuelConsumption : plannedFuelConsumption;
    }

    public String getRouteDescription() {
        StringBuilder route = new StringBuilder();
        route.append(startAddress);
        if (waypoints != null && !waypoints.isEmpty()) {
            for (Waypoint waypoint : waypoints) {
                route.append(" → ").append(waypoint.getAddress());
            }
        }
        route.append(" → ").append(endAddress);
        if (tripType == TripType.ROUND_TRIP) {
            route.append(" → ").append(startAddress);
        }
        return route.toString();
    }

    public String getStatusDescription() {
        StringBuilder desc = new StringBuilder(status.toString());
        if (isAssigned() && driver != null) {
            desc.append(" (").append(driver.getFullName()).append(")");
        }
        return desc.toString();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTripNumber() { return tripNumber; }
    public void setTripNumber(String tripNumber) { this.tripNumber = tripNumber; }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

    public Integer getDriverId() { return driverId; }
    public void setDriverId(Integer driverId) { this.driverId = driverId; }

    public int getRequesterId() { return requesterId; }
    public void setRequesterId(int requesterId) { this.requesterId = requesterId; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }

    public Requester getRequester() { return requester; }
    public void setRequester(Requester requester) { this.requester = requester; }

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

    public int getWaitingTime() { return waitingTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }

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

    public boolean isCanDriverDeliver() { return canDriverDeliver; }
    public void setCanDriverDeliver(boolean canDriverDeliver) { this.canDriverDeliver = canDriverDeliver; }

    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }

    public Season getSeason() { return season; }
    public void setSeason(Season season) { this.season = season; }

    public int getRouteModificationsCount() { return routeModificationsCount; }
    public void setRouteModificationsCount(int routeModificationsCount) { this.routeModificationsCount = routeModificationsCount; }

    public BigDecimal getRouteDeviationPercent() { return routeDeviationPercent; }
    public void setRouteDeviationPercent(BigDecimal routeDeviationPercent) { this.routeDeviationPercent = routeDeviationPercent; }

    public BigDecimal getRefrigeratorUsagePercent() { return refrigeratorUsagePercent; }
    public void setRefrigeratorUsagePercent(BigDecimal refrigeratorUsagePercent) { this.refrigeratorUsagePercent = refrigeratorUsagePercent; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<Waypoint> getWaypoints() { return waypoints; }
    public void setWaypoints(List<Waypoint> waypoints) { this.waypoints = waypoints; }

    public void addWaypoint(Waypoint waypoint) {
        if (waypoints == null) {
            waypoints = new ArrayList<>();
        }
        waypoint.setTripId(this.id);
        waypoint.setSequenceOrder(waypoints.size() + 1);
        waypoints.add(waypoint);
    }

    public boolean removeWaypoint(Waypoint waypoint) {
        if (waypoints != null) {
            boolean removed = waypoints.remove(waypoint);
            if (removed) {
                updateWaypointSequence();
            }
            return removed;
        }
        return false;
    }

    public void updateWaypointSequence() {
        if (waypoints != null) {
            for (int i = 0; i < waypoints.size(); i++) {
                waypoints.get(i).setSequenceOrder(i + 1);
            }
        }
    }

    public int getWaypointCount() {
        return waypoints != null ? waypoints.size() : 0;
    }

    public boolean hasWaypoints() {
        return waypoints != null && !waypoints.isEmpty();
    }

    public int getTotalStopTime() {
        if (waypoints == null || waypoints.isEmpty()) {
            return 0;
        }
        return waypoints.stream()
                .mapToInt(Waypoint::getEstimatedStopTime)
                .sum();
    }

    @Override
    public String toString() {
        return tripNumber + " - " + getRouteDescription();
    }
}