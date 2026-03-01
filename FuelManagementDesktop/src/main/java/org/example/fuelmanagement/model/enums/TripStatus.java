package org.example.fuelmanagement.model.enums;

public enum TripStatus {
    CREATED("created"),
    ASSIGNED("assigned"),
    STARTED("started"),
    PAUSED("paused"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    DELETED("del");

    private final String value;

    TripStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TripStatus fromString(String value) {
        if (value == null) return CREATED;
        for (TripStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return CREATED;
    }

    @Override
    public String toString() {
        switch (this) {
            case CREATED: return "Створено";
            case ASSIGNED: return "Призначено";
            case STARTED: return "Розпочато";
            case PAUSED: return "Призупинено";
            case COMPLETED: return "Завершено";
            case CANCELLED: return "Скасовано";
            case DELETED: return "Видалено";
            default: return value;
        }
    }

    public boolean isActive() {
        return this == STARTED || this == PAUSED;
    }

    public boolean isCompleted() {
        return this == COMPLETED || this == CANCELLED || this == DELETED;
    }
}
