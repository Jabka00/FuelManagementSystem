package org.example.fuelmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Waypoint {
    private int id;
    private int tripId;
    private int sequenceOrder; 
    private String address;
    private String description; 
    private BigDecimal latitude;
    private BigDecimal longitude;
    private int estimatedStopTime; 
    private LocalDateTime plannedArrivalTime;
    private LocalDateTime actualArrivalTime;
    private LocalDateTime actualDepartureTime;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Waypoint() {
        this.estimatedStopTime = 0;
        this.createdAt = LocalDateTime.now();
    }

    public Waypoint(int tripId, int sequenceOrder, String address, String description) {
        this();
        this.tripId = tripId;
        this.sequenceOrder = sequenceOrder;
        this.address = address;
        this.description = description;
    }

    public String getFullDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return String.format("%s (%s)", address, description);
        }
        return address;
    }

    public boolean isFirstPoint() {
        return sequenceOrder == 1;
    }

    public boolean isLastPoint() {
        return sequenceOrder > 0; 
    }

    public String getFormattedStopTime() {
        if (estimatedStopTime <= 0) {
            return "Без зупинки";
        }
        return String.format("%d хв", estimatedStopTime);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTripId() { return tripId; }
    public void setTripId(int tripId) { this.tripId = tripId; }

    public int getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(int sequenceOrder) { this.sequenceOrder = sequenceOrder; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public int getEstimatedStopTime() { return estimatedStopTime; }
    public void setEstimatedStopTime(int estimatedStopTime) { this.estimatedStopTime = estimatedStopTime; }

    public LocalDateTime getPlannedArrivalTime() { return plannedArrivalTime; }
    public void setPlannedArrivalTime(LocalDateTime plannedArrivalTime) { this.plannedArrivalTime = plannedArrivalTime; }

    public LocalDateTime getActualArrivalTime() { return actualArrivalTime; }
    public void setActualArrivalTime(LocalDateTime actualArrivalTime) { this.actualArrivalTime = actualArrivalTime; }

    public LocalDateTime getActualDepartureTime() { return actualDepartureTime; }
    public void setActualDepartureTime(LocalDateTime actualDepartureTime) { this.actualDepartureTime = actualDepartureTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("%d. %s", sequenceOrder, getFullDescription());
    }
}
