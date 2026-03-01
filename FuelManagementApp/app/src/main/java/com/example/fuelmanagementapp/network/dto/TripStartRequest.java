package com.example.fuelmanagementapp.network.dto;

public class TripStartRequest {
    
    private Integer startOdometer;  
    private String notes;           

    public TripStartRequest() {}

    public TripStartRequest(Integer startOdometer) {
        this.startOdometer = startOdometer;
    }

    public TripStartRequest(Integer startOdometer, String notes) {
        this.startOdometer = startOdometer;
        this.notes = notes;
    }

    public boolean isValid() {
        return startOdometer != null && startOdometer > 0;
    }

    public Integer getStartOdometer() {
        return startOdometer;
    }

    public void setStartOdometer(Integer startOdometer) {
        this.startOdometer = startOdometer;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "TripStartRequest{" +
                "startOdometer=" + startOdometer +
                ", notes='" + notes + '\'' +
                '}';
    }
}
