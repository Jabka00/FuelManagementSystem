package org.example.fuelmanagementapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class TripCreateRequest {
    
    @NotNull(message = "ID автомобіля обов'язковий")
    @Min(value = 1, message = "ID автомобіля має бути більше 0")
    private Integer vehicleId;
    
    @NotNull(message = "ID водія обов'язковий")
    @Min(value = 1, message = "ID водія має бути більше 0")
    private Integer driverId;
    
    @NotNull(message = "Пробіг обов'язковий")
    @Min(value = 1, message = "Пробіг має бути більше 0")
    private Integer odometer;
    
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
