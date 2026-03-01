package org.example.fuelmanagement.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

public class RouteInfo {
    private BigDecimal totalDistance;
    private BigDecimal cityDistance;
    private BigDecimal highwayDistance;
    private Duration estimatedDuration;
    private String routeDescription;

    public RouteInfo() {
        this.totalDistance = BigDecimal.ZERO;
        this.cityDistance = BigDecimal.ZERO;
        this.highwayDistance = BigDecimal.ZERO;
    }

    public RouteInfo(BigDecimal totalDistance, Duration estimatedDuration) {
        this();
        this.totalDistance = totalDistance;
        this.estimatedDuration = estimatedDuration;
    }

    public BigDecimal getCityPercentage() {
        if (totalDistance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return cityDistance.divide(totalDistance, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    public BigDecimal getHighwayPercentage() {
        if (totalDistance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return highwayDistance.divide(totalDistance, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    public String getFormattedDistance() {
        if (totalDistance == null || totalDistance.compareTo(BigDecimal.ZERO) == 0) {
            return "0 км";
        }
        return String.format("%.1f км", totalDistance.doubleValue());
    }

    public String getFormattedDuration() {
        if (estimatedDuration == null) {
            return "Невідомо";
        }
        long hours = estimatedDuration.toHours();
        long minutes = estimatedDuration.toMinutes() % 60;

        if (hours > 0) {
            return String.format("%d год %d хв", hours, minutes);
        } else {
            return String.format("%d хв", minutes);
        }
    }

    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append(" Загальна дистанція: ").append(getFormattedDistance()).append("\n");
        info.append(" Міські дороги: ").append(String.format("%.1f км (%.1f%%)",
                cityDistance.doubleValue(), getCityPercentage().doubleValue())).append("\n");
        info.append(" Траса: ").append(String.format("%.1f км (%.1f%%)",
                highwayDistance.doubleValue(), getHighwayPercentage().doubleValue())).append("\n");
        info.append("⏱ Час в дорозі: ").append(getFormattedDuration());
        return info.toString();
    }

    public boolean isEmpty() {
        return totalDistance == null || totalDistance.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal getTotalDistance() { return totalDistance; }
    public void setTotalDistance(BigDecimal totalDistance) {
        this.totalDistance = totalDistance;

        if (cityDistance != null && highwayDistance != null) {
            this.totalDistance = cityDistance.add(highwayDistance);
        }
    }

    public BigDecimal getCityDistance() { return cityDistance; }
    public void setCityDistance(BigDecimal cityDistance) {
        this.cityDistance = cityDistance != null ? cityDistance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getHighwayDistance() { return highwayDistance; }
    public void setHighwayDistance(BigDecimal highwayDistance) {
        this.highwayDistance = highwayDistance != null ? highwayDistance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public Duration getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(Duration estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public String getRouteDescription() { return routeDescription; }
    public void setRouteDescription(String routeDescription) { this.routeDescription = routeDescription; }

    @Override
    public String toString() {
        return getFormattedDistance() + " (" + getFormattedDuration() + ")";
    }
}