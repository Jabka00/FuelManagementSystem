package org.example.fuelmanagement.controller.helper;

import javafx.collections.ObservableList;
import org.example.fuelmanagement.model.Driver;
import org.example.fuelmanagement.model.RouteInfo;
import org.example.fuelmanagement.model.Trip;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.Waypoint;
import org.example.fuelmanagement.model.enums.Season;
import org.example.fuelmanagement.model.enums.TripStatus;
import org.example.fuelmanagement.model.enums.TripType;
import org.example.fuelmanagement.util.FuelCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TripFormBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TripFormBuilder.class);

    private String requesterName;
    private String requesterEmail;
    private String requesterPhone;
    private String startAddress;
    private String endAddress;
    private String purpose;
    private String powerOfAttorney;
    private boolean canDriverDeliver;
    private TripType tripType;
    private Vehicle vehicle;
    private Driver driver;
    private LocalDate startDate;
    private Integer startHour;
    private Integer startMinute;
    private LocalDate endDate;
    private Integer endHour;
    private Integer endMinute;
    private BigDecimal refrigeratorPercent = BigDecimal.ZERO;
    private RouteInfo routeInfo;
    private ObservableList<Waypoint> waypoints;

    public TripFormBuilder requesterName(String requesterName) {
        this.requesterName = requesterName;
        return this;
    }

    public TripFormBuilder requesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
        return this;
    }

    public TripFormBuilder requesterPhone(String requesterPhone) {
        this.requesterPhone = requesterPhone;
        return this;
    }

    public TripFormBuilder startAddress(String startAddress) {
        this.startAddress = startAddress;
        return this;
    }

    public TripFormBuilder endAddress(String endAddress) {
        this.endAddress = endAddress;
        return this;
    }

    public TripFormBuilder purpose(String purpose) {
        this.purpose = purpose;
        return this;
    }

    public TripFormBuilder powerOfAttorney(String powerOfAttorney) {
        this.powerOfAttorney = powerOfAttorney;
        return this;
    }

    public TripFormBuilder canDriverDeliver(boolean canDriverDeliver) {
        this.canDriverDeliver = canDriverDeliver;
        return this;
    }

    public TripFormBuilder tripType(TripType tripType) {
        this.tripType = tripType;
        return this;
    }

    public TripFormBuilder tripTypeFromString(String tripTypeStr) {
        this.tripType = "В обидві сторони".equals(tripTypeStr) ? TripType.ROUND_TRIP : TripType.ONE_WAY;
        return this;
    }

    public TripFormBuilder vehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
        return this;
    }

    public TripFormBuilder driver(Driver driver) {
        this.driver = driver;
        return this;
    }

    public TripFormBuilder startDateTime(LocalDate date, Integer hour, Integer minute) {
        this.startDate = date;
        this.startHour = hour;
        this.startMinute = minute;
        return this;
    }

    public TripFormBuilder endDateTime(LocalDate date, Integer hour, Integer minute) {
        this.endDate = date;
        this.endHour = hour;
        this.endMinute = minute;
        return this;
    }

    public TripFormBuilder refrigeratorPercent(BigDecimal percent) {
        this.refrigeratorPercent = percent != null ? percent : BigDecimal.ZERO;
        return this;
    }

    public TripFormBuilder routeInfo(RouteInfo routeInfo) {
        this.routeInfo = routeInfo;
        return this;
    }

    public TripFormBuilder waypoints(ObservableList<Waypoint> waypoints) {
        this.waypoints = waypoints;
        return this;
    }

    public Trip build() {
        Trip trip = new Trip();

        trip.setRequesterName(trim(requesterName));
        trip.setRequesterEmail(trim(requesterEmail));
        trip.setRequesterPhone(trim(requesterPhone));

        trip.setStartAddress(trim(startAddress));
        trip.setEndAddress(trim(endAddress));
        trip.setPurpose(trim(purpose));
        trip.setPowerOfAttorney(trim(powerOfAttorney));
        trip.setCanDriverDeliver(canDriverDeliver);
        trip.setTripType(tripType);

        if (vehicle != null) {
            trip.setVehicleId(vehicle.getId());
        }

        if (driver != null) {
            trip.setDriverId(driver.getId());
            trip.setStatus(TripStatus.ASSIGNED);
        } else {
            trip.setStatus(TripStatus.CREATED);
        }

        if (waypoints != null && !waypoints.isEmpty()) {
            for (Waypoint waypoint : waypoints) {
                trip.addWaypoint(waypoint);
            }
        }

        setPlannedTimes(trip);

        trip.setRefrigeratorUsagePercent(refrigeratorPercent);
        if (vehicle != null && vehicle.hasRefrigerator()) {
            logger.info("Зберігаємо відсоток холодильника: {}%", refrigeratorPercent);
        }

        if (routeInfo != null && !routeInfo.isEmpty() && vehicle != null) {
            calculateAndSetRouteData(trip);
        }

        trip.setSeason(Season.getCurrentSeason());
        trip.setCreatedBy("system");

        return trip;
    }

    private void setPlannedTimes(Trip trip) {
        try {
            if (startDate != null && startHour != null && startMinute != null) {
                LocalTime startTime = LocalTime.of(startHour, startMinute);
                trip.setPlannedStartTime(LocalDateTime.of(startDate, startTime));
            }

            if (endDate != null && endHour != null && endMinute != null) {
                LocalTime endTime = LocalTime.of(endHour, endMinute);
                trip.setPlannedEndTime(LocalDateTime.of(endDate, endTime));
            }
        } catch (Exception e) {
            logger.warn("Помилка встановлення часу: {}", e.getMessage());
        }
    }

    private void calculateAndSetRouteData(Trip trip) {
        boolean isSummer = Season.getCurrentSeason() == Season.SUMMER;
        boolean isAlreadyCombined = tripType == TripType.ROUND_TRIP && waypoints != null && !waypoints.isEmpty();

        BigDecimal totalDistance = routeInfo.getTotalDistance();
        BigDecimal cityDistance = routeInfo.getCityDistance();
        BigDecimal highwayDistance = routeInfo.getHighwayDistance();
        BigDecimal plannedFuel;

        if (isAlreadyCombined) {
            TripType calculationType = TripType.ONE_WAY;
            plannedFuel = FuelCalculator.calculateFuelConsumptionForTrip(
                    vehicle, routeInfo, calculationType, isSummer, refrigeratorPercent);
        } else if (tripType == TripType.ROUND_TRIP) {
            plannedFuel = FuelCalculator.calculatePlannedFuelConsumption(
                    vehicle, routeInfo, isSummer, refrigeratorPercent);
            plannedFuel = plannedFuel.multiply(BigDecimal.valueOf(2));
            totalDistance = totalDistance.multiply(BigDecimal.valueOf(2));
            cityDistance = cityDistance.multiply(BigDecimal.valueOf(2));
            highwayDistance = highwayDistance.multiply(BigDecimal.valueOf(2));
        } else {
            plannedFuel = FuelCalculator.calculatePlannedFuelConsumption(
                    vehicle, routeInfo, isSummer, refrigeratorPercent);
        }

        trip.setPlannedFuelConsumption(plannedFuel);
        trip.setPlannedDistance(totalDistance);
        trip.setPlannedCityKm(cityDistance);
        trip.setPlannedHighwayKm(highwayDistance);

        logger.info("Збережено: відстань = {} км, витрата = {} л",
                totalDistance.setScale(2, RoundingMode.HALF_UP),
                plannedFuel.setScale(2, RoundingMode.HALF_UP));
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }

    public static String createSuccessMessage(
            Trip trip,
            Vehicle vehicle,
            Driver driver,
            ObservableList<Waypoint> waypoints,
            RouteInfo routeInfo
    ) {
        StringBuilder message = new StringBuilder();
        message.append("Поїздку успішно створено!\n\n");
        message.append("Номер поїздки: ").append(trip.getTripNumber()).append("\n");
        message.append("Замовник: ").append(trip.getRequesterName()).append("\n");
        message.append("Маршрут: ").append(trip.getRouteDescription()).append("\n");

        if (waypoints != null && !waypoints.isEmpty()) {
            message.append("Проміжні точки (").append(waypoints.size()).append("):\n");
            for (Waypoint wp : waypoints) {
                message.append("   • ").append(wp.getFullDescription()).append("\n");
            }
        }

        if (vehicle != null) {
            message.append("Автомобіль: ").append(vehicle.getDisplayName()).append("\n");
        }

        if (driver != null) {
            message.append("Водій: ").append(driver.getFullName()).append("\n");
            message.append("Статус: Призначено\n");
        } else {
            message.append("Статус: Створено (водій не призначений)\n");
        }

        if (trip.getPlannedDistance() != null && routeInfo != null) {
            message.append("Дистанція: ").append(routeInfo.getFormattedDistance()).append("\n");
        }

        if (trip.getPlannedFuelConsumption() != null && vehicle != null) {
            message.append("Планові витрати: ").append(trip.getPlannedFuelConsumption()).append(" л\n");

            if (vehicle.hasEnoughFuel(trip.getPlannedFuelConsumption())) {
                message.append("Палива достатньо для поїздки\n");
            } else {
                BigDecimal shortage = vehicle.getFuelShortage(trip.getPlannedFuelConsumption());
                message.append("Потрібно дозаправити: ").append(shortage).append(" л\n");
            }
        }

        return message.toString();
    }
}
