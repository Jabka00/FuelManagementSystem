package com.example.fuelmanagementapp.network.dto;

import java.math.BigDecimal;

public class TripFinishRequest {

    public enum DistanceInputType {
        MANUAL_DISTANCE,    
        ODOMETER_READING    
    }

    private DistanceInputType inputType;
    private BigDecimal actualDistance;  
    private Integer endOdometer;        
    private String notes;
    private BigDecimal fuelConsumed;

    public TripFinishRequest() {}

    public TripFinishRequest(BigDecimal actualDistance) {
        this.actualDistance = actualDistance;
        this.inputType = DistanceInputType.MANUAL_DISTANCE;
    }

    public TripFinishRequest(Integer endOdometer) {
        this.endOdometer = endOdometer;
        this.inputType = DistanceInputType.ODOMETER_READING;
    }

    public TripFinishRequest(BigDecimal actualDistance, String notes) {
        this.actualDistance = actualDistance;
        this.notes = notes;
        this.inputType = DistanceInputType.MANUAL_DISTANCE;
    }

    public TripFinishRequest(Integer endOdometer, String notes) {
        this.endOdometer = endOdometer;
        this.notes = notes;
        this.inputType = DistanceInputType.ODOMETER_READING;
    }

    public boolean isValid() {
        if (inputType == DistanceInputType.MANUAL_DISTANCE) {
            return actualDistance != null && actualDistance.compareTo(BigDecimal.ZERO) > 0;
        } else if (inputType == DistanceInputType.ODOMETER_READING) {
            return endOdometer != null && endOdometer > 0;
        }
        return false;
    }

    public DistanceInputType getInputType() {
        return inputType;
    }

    public void setInputType(DistanceInputType inputType) {
        this.inputType = inputType;
    }

    public BigDecimal getActualDistance() {
        return actualDistance;
    }

    public void setActualDistance(BigDecimal actualDistance) {
        this.actualDistance = actualDistance;
        if (actualDistance != null) {
            this.inputType = DistanceInputType.MANUAL_DISTANCE;
        }
    }

    public Integer getEndOdometer() {
        return endOdometer;
    }

    public void setEndOdometer(Integer endOdometer) {
        this.endOdometer = endOdometer;
        if (endOdometer != null) {
            this.inputType = DistanceInputType.ODOMETER_READING;
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getFuelConsumed() {
        return fuelConsumed;
    }

    public void setFuelConsumed(BigDecimal fuelConsumed) {
        this.fuelConsumed = fuelConsumed;
    }

    @Override
    public String toString() {
        return "TripFinishRequest{" +
                "inputType=" + inputType +
                ", actualDistance=" + actualDistance +
                ", endOdometer=" + endOdometer +
                ", notes='" + notes + '\'' +
                ", fuelConsumed=" + fuelConsumed +
                '}';
    }
}