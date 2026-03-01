package org.example.fuelmanagement.model.enums;

public enum DriverStatus {
    ACTIVE("active"),
    VACATION("vacation"),
    SICK("sick"),
    INACTIVE("inactive");

    private final String value;

    DriverStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DriverStatus fromString(String value) {
        if (value == null) return ACTIVE;
        for (DriverStatus status : values()) {
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
            case VACATION: return "У відпустці";
            case SICK: return "На лікарняному";
            case INACTIVE: return "Неактивний";
            default: return value;
        }
    }
}
