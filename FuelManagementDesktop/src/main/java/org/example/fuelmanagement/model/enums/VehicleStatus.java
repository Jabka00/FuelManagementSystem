package org.example.fuelmanagement.model.enums;

public enum VehicleStatus {
    ACTIVE("active"),
    MAINTENANCE("maintenance"),
    INACTIVE("inactive"),
    RETIRED("retired");

    private final String value;

    VehicleStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static VehicleStatus fromString(String value) {
        if (value == null) return ACTIVE;
        for (VehicleStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return ACTIVE;
    }

    @Override
    public String toString() {
        switch (this) {
            case ACTIVE: return "Активний";
            case MAINTENANCE: return "На ТО";
            case INACTIVE: return "Неактивний";
            case RETIRED: return "Списаний";
            default: return value;
        }
    }
}
