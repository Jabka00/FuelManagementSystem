package org.example.fuelmanagement.model.enums;

import java.time.LocalDateTime;

public enum Season {
    SUMMER("summer"),
    WINTER("winter");

    private final String value;

    Season(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Season fromString(String value) {
        if (value == null) return SUMMER;
        for (Season season : values()) {
            if (season.value.equals(value)) {
                return season;
            }
        }
        return SUMMER;
    }

    public static Season getCurrentSeason() {
        int month = LocalDateTime.now().getMonthValue();
        return (month >= 4 && month <= 10) ? SUMMER : WINTER;
    }

    @Override
    public String toString() {
        switch (this) {
            case SUMMER: return "Літо";
            case WINTER: return "Зима";
            default: return value;
        }
    }
}
