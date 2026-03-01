package org.example.fuelmanagement.model.enums;

public enum TripType {
    ONE_WAY("one_way"),
    ROUND_TRIP("round_trip"),
    FACTORY_GROUND("factory_ground"),
    IDLE_WORK("idle_work"),
    FACTORY_GROUND_WITH_IDLE("combined");

    private final String value;

    TripType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TripType fromString(String value) {
        if (value == null) return ROUND_TRIP;
        for (TripType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return ROUND_TRIP;
    }

    @Override
    public String toString() {
        switch (this) {
            case ONE_WAY: return "В один бік";
            case ROUND_TRIP: return "В обidві сторони";
            case FACTORY_GROUND: return "Робота на території";
            case IDLE_WORK: return "Робота на холостому ході";
            case FACTORY_GROUND_WITH_IDLE: return "Робота на території + холостий хід";
            default: return value;
        }
    }

    public boolean isSpecialType() {
        return this == FACTORY_GROUND || this == IDLE_WORK || this == FACTORY_GROUND_WITH_IDLE;
    }
}
