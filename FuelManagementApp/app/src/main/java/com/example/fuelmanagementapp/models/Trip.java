package com.example.fuelmanagementapp.models;

import com.google.gson.annotations.SerializedName;
import com.example.fuelmanagementapp.utils.Constants;
import java.math.BigDecimal;

public class Trip {
    private Integer id;

    @SerializedName("tripNumber")
    private String tripNumber;

    @SerializedName("driverId")
    private Integer driverId;

    @SerializedName("vehicleId")
    private Integer vehicleId;

    @SerializedName("requesterId")
    private Integer requesterId;

    @SerializedName("requesterName")
    private String requesterName;

    @SerializedName("requesterEmail")
    private String requesterEmail;

    @SerializedName("requesterPhone")
    private String requesterPhone;

    @SerializedName("startAddress")
    private String startAddress;

    @SerializedName("endAddress")
    private String endAddress;

    @SerializedName("tripType")
    private String tripType;

    @SerializedName("plannedStartTime")
    private String plannedStartTime;

    @SerializedName("plannedEndTime")
    private String plannedEndTime;

    @SerializedName("waitingTime")
    private Integer waitingTime;

    @SerializedName("plannedDistance")
    private BigDecimal plannedDistance;

    @SerializedName("plannedFuelConsumption")
    private BigDecimal plannedFuelConsumption;

    @SerializedName("actualStartTime")
    private String actualStartTime;

    @SerializedName("actualEndTime")
    private String actualEndTime;

    @SerializedName("actualDistance")
    private BigDecimal actualDistance;

    @SerializedName("actualFuelConsumption")
    private BigDecimal actualFuelConsumption;

    @SerializedName("fuelReceived")
    private BigDecimal fuelReceived;

    @SerializedName("actualStartOdometer")
    private Integer actualStartOdometer;

    @SerializedName("actualEndOdometer")
    private Integer actualEndOdometer;

    @SerializedName("currentOdometer")
    private Integer currentOdometer;

    private String purpose;
    private String status;
    private String notes;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("licensePlate")
    private String licensePlate;

    private String model;
    
    @SerializedName("driverFullName")
    private String driverFullName;  

    public Trip() {}

    public boolean isAssigned() {
        return driverId != null && driverId > 0;
    }

    public boolean isCompleted() {
        return Constants.TRIP_STATUS_COMPLETED.equals(status) ||
                Constants.TRIP_STATUS_CANCELLED.equals(status);
    }

    public boolean isActive() {
        return Constants.TRIP_STATUS_STARTED.equals(status) ||
                Constants.TRIP_STATUS_PAUSED.equals(status);
    }

    public boolean isStarted() {
        return Constants.TRIP_STATUS_STARTED.equals(status);
    }

    public boolean isNew() {
        return Constants.TRIP_STATUS_ASSIGNED.equals(status) ||
                Constants.TRIP_STATUS_CREATED.equals(status);
    }

    public boolean canBeStarted() {
        return Constants.TRIP_STATUS_ASSIGNED.equals(status);
    }

    public boolean canBeFinished() {
        return Constants.TRIP_STATUS_STARTED.equals(status);
    }

    public boolean hasStartOdometer() {
        return actualStartOdometer != null && actualStartOdometer > 0;
    }

    public boolean hasEndOdometer() {
        return actualEndOdometer != null && actualEndOdometer > 0;
    }

    public boolean hasCurrentOdometer() {
        return currentOdometer != null && currentOdometer > 0;
    }

    public BigDecimal getCalculatedDistance() {
        if (hasStartOdometer() && hasEndOdometer()) {
            return BigDecimal.valueOf(actualEndOdometer - actualStartOdometer);
        }
        return null;
    }

    public String getStatusDisplayText() {
        if (status == null) return "Неизвестно";

        switch (status.toLowerCase()) {
            case "created": return "Створенна";
            case "assigned": return "Назначена";
            case "started": return "В дорозі";
            case "paused": return "Призупинена";
            case "completed": return "Завершина";
            case "cancelled": return "Скасована";
            default: return status;
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
        return id != null ? "№" + id : "Без номера";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTripNumber() { return tripNumber; }
    public void setTripNumber(String tripNumber) { this.tripNumber = tripNumber; }

    public Integer getDriverId() { return driverId; }
    public void setDriverId(Integer driverId) { this.driverId = driverId; }

    public Integer getVehicleId() { return vehicleId; }
    public void setVehicleId(Integer vehicleId) { this.vehicleId = vehicleId; }

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

    public String getTripType() { return tripType; }
    public void setTripType(String tripType) { this.tripType = tripType; }

    public String getPlannedStartTime() { return plannedStartTime; }
    public void setPlannedStartTime(String plannedStartTime) { this.plannedStartTime = plannedStartTime; }

    public String getPlannedEndTime() { return plannedEndTime; }
    public void setPlannedEndTime(String plannedEndTime) { this.plannedEndTime = plannedEndTime; }

    public Integer getWaitingTime() { return waitingTime; }
    public void setWaitingTime(Integer waitingTime) { this.waitingTime = waitingTime; }

    public BigDecimal getPlannedDistance() { return plannedDistance; }
    public void setPlannedDistance(BigDecimal plannedDistance) { this.plannedDistance = plannedDistance; }

    public BigDecimal getPlannedFuelConsumption() { return plannedFuelConsumption; }
    public void setPlannedFuelConsumption(BigDecimal plannedFuelConsumption) { this.plannedFuelConsumption = plannedFuelConsumption; }

    public String getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(String actualStartTime) { this.actualStartTime = actualStartTime; }

    public String getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(String actualEndTime) { this.actualEndTime = actualEndTime; }

    public BigDecimal getActualDistance() { return actualDistance; }
    public void setActualDistance(BigDecimal actualDistance) { this.actualDistance = actualDistance; }

    public BigDecimal getActualFuelConsumption() { return actualFuelConsumption; }
    public void setActualFuelConsumption(BigDecimal actualFuelConsumption) { this.actualFuelConsumption = actualFuelConsumption; }

    public BigDecimal getFuelReceived() { return fuelReceived; }
    public void setFuelReceived(BigDecimal fuelReceived) { this.fuelReceived = fuelReceived; }

    public Integer getActualStartOdometer() { return actualStartOdometer; }
    public void setActualStartOdometer(Integer actualStartOdometer) { this.actualStartOdometer = actualStartOdometer; }

    public Integer getActualEndOdometer() { return actualEndOdometer; }
    public void setActualEndOdometer(Integer actualEndOdometer) { this.actualEndOdometer = actualEndOdometer; }

    public Integer getCurrentOdometer() { return currentOdometer; }
    public void setCurrentOdometer(Integer currentOdometer) { this.currentOdometer = currentOdometer; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public String getDriverFullName() { return driverFullName; }
    public void setDriverFullName(String driverFullName) { this.driverFullName = driverFullName; }
    
    public String getDriverDisplayName() {
        if (driverFullName != null && !driverFullName.trim().isEmpty()) {
            return driverFullName.trim();
        }
        return "Водій не вказаний";
    }

    @Override
    public String toString() {
        return "Trip{" +
                "id=" + id +
                ", tripNumber='" + tripNumber + '\'' +
                ", status='" + status + '\'' +
                ", startAddress='" + startAddress + '\'' +
                ", endAddress='" + endAddress + '\'' +
                '}';
    }
}
