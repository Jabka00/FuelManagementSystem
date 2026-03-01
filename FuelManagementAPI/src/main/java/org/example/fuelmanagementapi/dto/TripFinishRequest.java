package org.example.fuelmanagementapi.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TripFinishRequest {

    @NotNull(message = "Спосіб введення відстані обов'язковий")
    private DistanceInputType inputType;

    @DecimalMin(value = "0.1", message = "Відстань має бути більше 0.1 км")
    @DecimalMax(value = "2000.0", message = "Відстань не може бути більше 2000 км")
    @Digits(integer = 6, fraction = 2, message = "Максимум 6 цифр до коми та 2 після")
    private BigDecimal actualDistance;

    @Min(value = 0, message = "Показник одометра не може бути від'ємним")
    @Max(value = 999999, message = "Показник одометра не може бути більше 999999")
    private Integer endOdometer;

    private String notes;
    private BigDecimal fuelConsumed;

    public enum DistanceInputType {
        MANUAL_DISTANCE,    
        ODOMETER_READING    
    }

    public TripFinishRequest() {}

    public TripFinishRequest(BigDecimal actualDistance) {
        this.actualDistance = actualDistance;
        this.inputType = DistanceInputType.MANUAL_DISTANCE;
    }

    public TripFinishRequest(Integer endOdometer) {
        this.endOdometer = endOdometer;
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
