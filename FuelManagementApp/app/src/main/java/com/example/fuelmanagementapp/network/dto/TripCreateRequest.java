package com.example.fuelmanagementapp.network.dto;

import com.google.gson.annotations.SerializedName;

public class TripCreateRequest {
    @SerializedName("vehicleId")
    private Integer vehicleId;

    @SerializedName("driverId")
    private Integer driverId;

    @SerializedName("odometer")
    private Integer odometer;

    @SerializedName("notes")
    private String notes;

    public TripCreateRequest() {}

    public TripCreateRequest(Integer vehicleId, Integer driverId, Integer odometer) {
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.odometer = odometer;
    }

    public TripCreateRequest(Integer vehicleId, Integer driverId, Integer odometer, String notes) {
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.odometer = odometer;
        this.notes = notes;
    }

    public boolean isValid() {
        return vehicleId != null && vehicleId > 0 &&
               driverId != null && driverId > 0 &&
               odometer != null && odometer > 0;
    }

    public Integer getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Integer vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Integer getDriverId() {
        return driverId;
    }

    public void setDriverId(Integer driverId) {
        this.driverId = driverId;
    }

    public Integer getOdometer() {
        return odometer;
    }

    public void setOdometer(Integer odometer) {
        this.odometer = odometer;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "TripCreateRequest{" +
                "vehicleId=" + vehicleId +
                ", driverId=" + driverId +
                ", odometer=" + odometer +
                ", notes='" + notes + '\'' +
                '}';
    }
}
