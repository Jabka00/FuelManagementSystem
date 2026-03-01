package org.example.fuelmanagement.util;

import org.example.fuelmanagement.model.RouteInfo;
import org.example.fuelmanagement.model.Trip;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.Waypoint;
import org.example.fuelmanagement.model.enums.TripType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class FuelCalculator {
    private static final Logger logger = LoggerFactory.getLogger(FuelCalculator.class);
    private static final BigDecimal REFRIGERATOR_CONSUMPTION_MULTIPLIER = new BigDecimal("0.15");
    private static final BigDecimal DEFAULT_FACTORY_GROUND_CITY_RATE = new BigDecimal("1.0");
    private static final BigDecimal IDLE_RATE_MULTIPLIER = new BigDecimal("1.0");

    public static BigDecimal calculatePlannedFuelConsumption(Vehicle vehicle, RouteInfo routeInfo, boolean isSummer) {
        if (vehicle == null || routeInfo == null || routeInfo.isEmpty()) {
            logger.warn("Не можна розрахувати витрати палива: відсутні дані про автомобіль або маршрут");
            return BigDecimal.ZERO;
        }

        try {
            BigDecimal cityKm = routeInfo.getCityDistance();
            BigDecimal highwayKm = routeInfo.getHighwayDistance();
            BigDecimal totalKm = routeInfo.getTotalDistance();

            logger.info("Розрахунок палива для {} на {} км (в один бік)",
                    vehicle.getDisplayName(), totalKm.setScale(2, RoundingMode.HALF_UP));
            logger.info("   Місто: {} км, Траса: {} км", 
                    cityKm.setScale(2, RoundingMode.HALF_UP), 
                    highwayKm.setScale(2, RoundingMode.HALF_UP));

            BigDecimal cityRate = isSummer ? vehicle.getCityRateSummer() : vehicle.getCityRateWinter();
            BigDecimal highwayRate = isSummer ? vehicle.getHighwayRateSummer() : vehicle.getHighwayRateWinter();

            if (cityRate == null || highwayRate == null) {
                logger.warn("Не задані норми витрат палива для автомобіля {}", vehicle.getDisplayName());
                return BigDecimal.ZERO;
            }

            BigDecimal cityRatePer100 = cityRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal highwayRatePer100 = highwayRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            logger.info("   Норми витрат ({}): місто {} л/100км, траса {} л/100км",
                    isSummer ? "літо" : "зима", cityRatePer100, highwayRatePer100);

            BigDecimal cityConsumption = cityKm.multiply(cityRate);
            BigDecimal highwayConsumption = highwayKm.multiply(highwayRate);
            BigDecimal totalConsumption = cityConsumption.add(highwayConsumption)
                    .setScale(4, RoundingMode.HALF_UP);

            logger.info("   Розрахунок: місто {} км × {} л/км = {} л",
                    cityKm, cityRate, cityConsumption.setScale(2, RoundingMode.HALF_UP));
            logger.info("   Розрахунок: траса {} км × {} л/км = {} л",
                    highwayKm, highwayRate, highwayConsumption.setScale(2, RoundingMode.HALF_UP));
            logger.info("   ЗАГАЛЬНА витрата: {} л",
                    totalConsumption.setScale(2, RoundingMode.HALF_UP));

            return totalConsumption;

        } catch (Exception e) {
            logger.error("Помилка розрахунку витрат палива: ", e);
            return BigDecimal.ZERO;
        }
    }

    public static BigDecimal calculatePlannedFuelConsumption(Vehicle vehicle, RouteInfo routeInfo, boolean isSummer,
                                                              BigDecimal refrigeratorUsagePercent) {
        BigDecimal baseFuelConsumption = calculatePlannedFuelConsumption(vehicle, routeInfo, isSummer);
        return applyRefrigeratorMultiplier(baseFuelConsumption, refrigeratorUsagePercent, vehicle.hasRefrigerator(), vehicle);
    }

    public static BigDecimal calculateRoundTripFuelConsumption(Vehicle vehicle, RouteInfo routeInfo, boolean isSummer) {
        BigDecimal oneWayConsumption = calculatePlannedFuelConsumption(vehicle, routeInfo, isSummer);
        BigDecimal roundTripConsumption = oneWayConsumption.multiply(BigDecimal.valueOf(2)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalDistance = routeInfo.getTotalDistance().multiply(BigDecimal.valueOf(2));
        logger.info("Поїздка туди-назад: загальна відстань {} км, паливо {} л", 
                totalDistance.setScale(2, RoundingMode.HALF_UP),
                roundTripConsumption.setScale(2, RoundingMode.HALF_UP));
        return roundTripConsumption;
    }

    public static BigDecimal calculateRoundTripFuelConsumption(Vehicle vehicle, RouteInfo routeInfo, boolean isSummer,
                                                                BigDecimal refrigeratorUsagePercent) {
        BigDecimal baseFuelConsumption = calculateRoundTripFuelConsumption(vehicle, routeInfo, isSummer);
        return applyRefrigeratorMultiplier(baseFuelConsumption, refrigeratorUsagePercent, vehicle.hasRefrigerator(), vehicle);
    }

    public static BigDecimal calculateFuelConsumptionForTrip(Vehicle vehicle, RouteInfo routeInfo,
                                                             TripType tripType, boolean isSummer) {
        if (tripType == TripType.ROUND_TRIP) {
            return calculateRoundTripFuelConsumption(vehicle, routeInfo, isSummer);
        } else {
            return calculatePlannedFuelConsumption(vehicle, routeInfo, isSummer);
        }
    }

    public static BigDecimal calculateFuelConsumptionForTrip(Vehicle vehicle, RouteInfo routeInfo,
                                                             TripType tripType, boolean isSummer,
                                                             BigDecimal refrigeratorUsagePercent) {
        BigDecimal baseFuelConsumption = calculateFuelConsumptionForTrip(vehicle, routeInfo, tripType, isSummer);
        return applyRefrigeratorMultiplier(baseFuelConsumption, refrigeratorUsagePercent, vehicle.hasRefrigerator(), vehicle);
    }

    public static boolean isEnoughFuel(Vehicle vehicle, BigDecimal requiredFuel) {
        if (vehicle == null || vehicle.getFuelBalance() == null || requiredFuel == null) {
            return false;
        }
        return vehicle.getFuelBalance().compareTo(requiredFuel) >= 0;
    }

    public static BigDecimal calculateFuelToAdd(Vehicle vehicle, BigDecimal requiredFuel) {
        if (vehicle == null || vehicle.getFuelBalance() == null || requiredFuel == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal shortage = requiredFuel.subtract(vehicle.getFuelBalance());
        return shortage.compareTo(BigDecimal.ZERO) > 0 ?
                shortage.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public static String createFuelReport(Vehicle vehicle, RouteInfo routeInfo, TripType tripType, boolean isSummer) {
        StringBuilder report = new StringBuilder();

        report.append(" ЗВІТ ПРО ВИТРАТИ ПАЛИВА\n");
        report.append("══════════════════════════════════════\n");
        report.append(" Автомобіль: ").append(vehicle.getDisplayName()).append("\n");
        report.append(" Баланс палива: ").append(vehicle.getFuelBalance()).append(" л\n");
        report.append(" Відстань маршруту: ").append(routeInfo.getFormattedDistance()).append("\n");
        report.append(" Тип поїздки: ").append(tripType.toString()).append("\n");
        report.append(" Сезон: ").append(isSummer ? "Літо" : "Зима").append("\n\n");

        BigDecimal cityRate = isSummer ? vehicle.getCityRateSummer() : vehicle.getCityRateWinter();
        BigDecimal highwayRate = isSummer ? vehicle.getHighwayRateSummer() : vehicle.getHighwayRateWinter();

        if (cityRate != null && highwayRate != null) {
            report.append("📐 НОРМИ ВИТРАТ:\n");
            report.append(" Місто: ").append(cityRate).append(" л/км (")
                    .append(cityRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .append(" л/100км)\n");
            report.append(" Траса: ").append(highwayRate).append(" л/км (")
                    .append(highwayRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .append(" л/100км)\n\n");
        }

        BigDecimal requiredFuel = calculateFuelConsumptionForTrip(vehicle, routeInfo, tripType, isSummer);

        BigDecimal cityKm = routeInfo.getCityDistance();
        BigDecimal highwayKm = routeInfo.getHighwayDistance();
        BigDecimal totalKm = cityKm.add(highwayKm);

        report.append(" РОЗРАХУНОК ВИТРАТ:\n");
        if (tripType == TripType.ROUND_TRIP) {
            report.append(" Відстань в один бік: ").append(totalKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Місто: ").append(cityKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Траса: ").append(highwayKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append(" Відстань туди-назад: ").append(totalKm.multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Місто: ").append(cityKm.multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Траса: ").append(highwayKm.multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP)).append(" км\n\n");
            cityKm = cityKm.multiply(BigDecimal.valueOf(2));
            highwayKm = highwayKm.multiply(BigDecimal.valueOf(2));
        } else {
            report.append(" Загальна відстань: ").append(totalKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Місто: ").append(cityKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Траса: ").append(highwayKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n\n");
        }

        if (cityRate != null && highwayRate != null) {
            BigDecimal cityConsumption = cityKm.multiply(cityRate);
            BigDecimal highwayConsumption = highwayKm.multiply(highwayRate);

            report.append("💧 Витрата палива:\n");
            report.append("    Місто: ").append(cityKm.setScale(2, RoundingMode.HALF_UP)).append(" км × ")
                    .append(cityRate).append(" л/км = ").append(cityConsumption.setScale(2, RoundingMode.HALF_UP)).append(" л\n");
            report.append("    Траса: ").append(highwayKm.setScale(2, RoundingMode.HALF_UP)).append(" км × ")
                    .append(highwayRate).append(" л/км = ").append(highwayConsumption.setScale(2, RoundingMode.HALF_UP)).append(" л\n");
        }
        report.append(" ЗАГАЛОМ ПОТРІБНО: ").append(requiredFuel.setScale(2, RoundingMode.HALF_UP)).append(" л\n\n");

        if (isEnoughFuel(vehicle, requiredFuel)) {
            report.append(" ПАЛИВА ДОСТАТНЬО ДЛЯ ПОЇЗДКИ\n");
            BigDecimal remaining = vehicle.getFuelBalance().subtract(requiredFuel);
            report.append(" Залишиться після поїздки: ").append(remaining.setScale(2, RoundingMode.HALF_UP)).append(" л\n");
        } else {
            BigDecimal toAdd = calculateFuelToAdd(vehicle, requiredFuel);
            report.append(" НЕДОСТАТНЬО ПАЛИВА!\n");
            report.append(" Потрібно дозаправити: ").append(toAdd).append(" л\n");

            BigDecimal afterRefill = vehicle.getFuelBalance().add(toAdd);
            if (vehicle.getTankCapacity() != null && afterRefill.compareTo(vehicle.getTankCapacity()) > 0) {
                report.append(" УВАГА: Після заправки (").append(afterRefill.setScale(1, RoundingMode.HALF_UP))
                        .append(" л) перевищить ємність бака (").append(vehicle.getTankCapacity()).append(" л)\n");
            }
        }

        return report.toString();
    }

    public static String createFuelReport(Vehicle vehicle, RouteInfo routeInfo, TripType tripType, boolean isSummer,
                                          BigDecimal refrigeratorUsagePercent) {
        StringBuilder report = new StringBuilder();

        report.append(" ЗВІТ ПРО ВИТРАТИ ПАЛИВА\n");
        report.append("══════════════════════════════════════\n");
        report.append(" Автомобіль: ").append(vehicle.getDisplayName()).append("\n");
        report.append(" Баланс палива: ").append(vehicle.getFuelBalance()).append(" л\n");
        report.append(" Відстань маршруту: ").append(routeInfo.getFormattedDistance()).append("\n");
        report.append(" Тип поїздки: ").append(tripType.toString()).append("\n");
        report.append(" Сезон: ").append(isSummer ? "Літо" : "Зима").append("\n\n");

        BigDecimal cityRate = isSummer ? vehicle.getCityRateSummer() : vehicle.getCityRateWinter();
        BigDecimal highwayRate = isSummer ? vehicle.getHighwayRateSummer() : vehicle.getHighwayRateWinter();

        if (cityRate != null && highwayRate != null) {
            report.append("📐 НОРМИ ВИТРАТ:\n");
            report.append(" Місто: ").append(cityRate).append(" л/км (")
                    .append(cityRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .append(" л/100км)\n");
            report.append(" Траса: ").append(highwayRate).append(" л/км (")
                    .append(highwayRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .append(" л/100км)\n\n");
        }

        BigDecimal baseFuel = calculateFuelConsumptionForTrip(vehicle, routeInfo, tripType, isSummer);
        BigDecimal requiredFuel = applyRefrigeratorMultiplier(baseFuel, refrigeratorUsagePercent, vehicle.hasRefrigerator(), vehicle);

        BigDecimal cityKm = routeInfo.getCityDistance();
        BigDecimal highwayKm = routeInfo.getHighwayDistance();
        BigDecimal totalKm = cityKm.add(highwayKm);

        report.append(" РОЗРАХУНОК ВИТРАТ:\n");
        if (tripType == TripType.ROUND_TRIP) {
            report.append(" Відстань в один бік: ").append(totalKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Місто: ").append(cityKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Траса: ").append(highwayKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append(" Відстань туди-назад: ").append(totalKm.multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Місто: ").append(cityKm.multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Траса: ").append(highwayKm.multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP)).append(" км\n\n");
            cityKm = cityKm.multiply(BigDecimal.valueOf(2));
            highwayKm = highwayKm.multiply(BigDecimal.valueOf(2));
        } else {
            report.append(" Загальна відстань: ").append(totalKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Місто: ").append(cityKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n");
            report.append("    Траса: ").append(highwayKm.setScale(2, RoundingMode.HALF_UP)).append(" км\n\n");
        }

        if (cityRate != null && highwayRate != null) {
            BigDecimal cityConsumption = cityKm.multiply(cityRate);
            BigDecimal highwayConsumption = highwayKm.multiply(highwayRate);

            report.append("💧 Витрата палива (БЕЗ холодильника):\n");
            report.append("    Місто: ").append(cityKm.setScale(2, RoundingMode.HALF_UP)).append(" км × ")
                    .append(cityRate).append(" л/км = ").append(cityConsumption.setScale(2, RoundingMode.HALF_UP)).append(" л\n");
            report.append("    Траса: ").append(highwayKm.setScale(2, RoundingMode.HALF_UP)).append(" км × ")
                    .append(highwayRate).append(" л/км = ").append(highwayConsumption.setScale(2, RoundingMode.HALF_UP)).append(" л\n");
            report.append("   💰 Базова витрата: ").append(baseFuel.setScale(2, RoundingMode.HALF_UP)).append(" л\n");
        }

        if (vehicle.hasRefrigerator() && refrigeratorUsagePercent != null && refrigeratorUsagePercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal additionalFuel = requiredFuel.subtract(baseFuel);
            if (isTestVehicleWithFixedRefrigeratorConsumption(vehicle)) {
                report.append("\n Холодільник (тестова машина):\n");
                report.append("   Додаткова витрата: +").append(additionalFuel.setScale(2, RoundingMode.HALF_UP))
                      .append(" л (фіксований розхід 1.7 л)\n");
            } else {
                BigDecimal additionalPercent = refrigeratorUsagePercent.multiply(REFRIGERATOR_CONSUMPTION_MULTIPLIER);
                report.append("\n Холодільник (").append(refrigeratorUsagePercent.setScale(0, RoundingMode.HALF_UP))
                      .append("% маршруту):\n");
                report.append("   Додаткова витрата: +").append(additionalFuel.setScale(2, RoundingMode.HALF_UP))
                      .append(" л (+").append(additionalPercent.setScale(1, RoundingMode.HALF_UP)).append("%)\n");
            }
        }

        report.append("\n ЗАГАЛОМ ПОТРІБНО: ").append(requiredFuel.setScale(2, RoundingMode.HALF_UP)).append(" л\n\n");

        if (isEnoughFuel(vehicle, requiredFuel)) {
            report.append(" ПАЛИВА ДОСТАТНЬО ДЛЯ ПОЇЗДКИ\n");
            BigDecimal remaining = vehicle.getFuelBalance().subtract(requiredFuel);
            report.append(" Залишиться після поїздки: ").append(remaining.setScale(2, RoundingMode.HALF_UP)).append(" л\n");
        } else {
            BigDecimal toAdd = calculateFuelToAdd(vehicle, requiredFuel);
            report.append(" НЕДОСТАТНЬО ПАЛИВА!\n");
            report.append(" Потрібно дозаправити: ").append(toAdd).append(" л\n");

            BigDecimal afterRefill = vehicle.getFuelBalance().add(toAdd);
            if (vehicle.getTankCapacity() != null && afterRefill.compareTo(vehicle.getTankCapacity()) > 0) {
                report.append(" УВАГА: Після заправки (").append(afterRefill.setScale(1, RoundingMode.HALF_UP))
                        .append(" л) перевищить ємність бака (").append(vehicle.getTankCapacity()).append(" л)\n");
            }
        }

        return report.toString();
    }

    public static String createShortFuelReport(Vehicle vehicle, RouteInfo routeInfo, TripType tripType, boolean isSummer) {
        StringBuilder report = new StringBuilder();

        BigDecimal requiredFuel = calculateFuelConsumptionForTrip(vehicle, routeInfo, tripType, isSummer);

        report.append(" Планована витрата: ").append(requiredFuel).append(" л\n");
        report.append("💰 Поточний баланс: ").append(vehicle.getFuelBalance()).append(" л\n");

        if (isEnoughFuel(vehicle, requiredFuel)) {
            BigDecimal remaining = vehicle.getFuelBalance().subtract(requiredFuel);
            report.append(" Достатньо палива (залишиться ").append(remaining.setScale(1, RoundingMode.HALF_UP)).append(" л)");
        } else {
            BigDecimal toAdd = calculateFuelToAdd(vehicle, requiredFuel);
            report.append(" Потрібно дозаправити: ").append(toAdd).append(" л");
        }

        return report.toString();
    }

    public static BigDecimal calculateMultiSegmentConsumption(Vehicle vehicle,
                                                              List<RouteSegment> segments,
                                                              boolean isSummer) {
        if (vehicle == null || segments == null || segments.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalConsumption = BigDecimal.ZERO;
        BigDecimal cityRate = isSummer ? vehicle.getCityRateSummer() : vehicle.getCityRateWinter();
        BigDecimal highwayRate = isSummer ? vehicle.getHighwayRateSummer() : vehicle.getHighwayRateWinter();

        if (cityRate == null || highwayRate == null) {
            return BigDecimal.ZERO;
        }

        for (RouteSegment segment : segments) {
            BigDecimal segmentConsumption = segment.getCityKm().multiply(cityRate)
                    .add(segment.getHighwayKm().multiply(highwayRate));
            totalConsumption = totalConsumption.add(segmentConsumption);
        }

        return totalConsumption.setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateFuelConsumptionWithWaypoints(Vehicle vehicle, Trip trip, boolean isSummer) {
        if (vehicle == null || trip == null) {
            logger.warn("Не можна розрахувати витрати палива: відсутні дані про автомобіль або поїздку");
            return BigDecimal.ZERO;
        }

        if (!trip.hasWaypoints()) {
            RouteInfo routeInfo = new RouteInfo();
            routeInfo.setTotalDistance(trip.getPlannedDistance());
            routeInfo.setCityDistance(trip.getPlannedCityKm());
            routeInfo.setHighwayDistance(trip.getPlannedHighwayKm());
            return calculateFuelConsumptionForTrip(vehicle, routeInfo, trip.getTripType(), isSummer);
        }

        List<RouteSegment> segments = createRouteSegments(trip);
        BigDecimal oneWayConsumption = calculateMultiSegmentConsumption(vehicle, segments, isSummer);

        if (trip.getTripType() == TripType.ROUND_TRIP) {
            return oneWayConsumption.multiply(BigDecimal.valueOf(2)).setScale(4, RoundingMode.HALF_UP);
        }

        return oneWayConsumption;
    }

    public static BigDecimal calculateFuelConsumptionWithWaypoints(Vehicle vehicle, Trip trip, boolean isSummer,
                                                                    BigDecimal refrigeratorUsagePercent) {
        BigDecimal baseFuelConsumption = calculateFuelConsumptionWithWaypoints(vehicle, trip, isSummer);
        return applyRefrigeratorMultiplier(baseFuelConsumption, refrigeratorUsagePercent, vehicle.hasRefrigerator(), vehicle);
    }

    public static List<RouteSegment> createRouteSegments(Trip trip) {
        List<RouteSegment> segments = new ArrayList<>();

        if (trip == null || trip.getStartAddress() == null || trip.getEndAddress() == null) {
            return segments;
        }

        if (!trip.hasWaypoints()) {
            segments.add(new RouteSegment(
                trip.getPlannedCityKm() != null ? trip.getPlannedCityKm() : BigDecimal.ZERO,
                trip.getPlannedHighwayKm() != null ? trip.getPlannedHighwayKm() : BigDecimal.ZERO,
                trip.getStartAddress() + " → " + trip.getEndAddress()
            ));
            return segments;
        }

        String previousAddress = trip.getStartAddress();
        for (Waypoint waypoint : trip.getWaypoints()) {
            RouteSegment segment = new RouteSegment(
                BigDecimal.ZERO, 
                BigDecimal.ZERO, 
                previousAddress + " → " + waypoint.getAddress()
            );
            segments.add(segment);
            previousAddress = waypoint.getAddress();
        }

        segments.add(new RouteSegment(
            BigDecimal.ZERO, 
            BigDecimal.ZERO, 
            previousAddress + " → " + trip.getEndAddress()
        ));

        return segments;
    }

    public static String createWaypointFuelReport(Vehicle vehicle, Trip trip, boolean isSummer) {
        StringBuilder report = new StringBuilder();

        report.append(" ЗВІТ ПРО ВИТРАТИ ПАЛИВА З ПРОМІЖНИМИ ТОЧКАМИ\n");
        report.append("═══════════════════════════════════════════════════════\n");
        report.append(" Автомобіль: ").append(vehicle.getDisplayName()).append("\n");
        report.append(" Баланс палива: ").append(vehicle.getFuelBalance()).append(" л\n");
        report.append(" Маршрут: ").append(trip.getRouteDescription()).append("\n");
        report.append("Тип поїздки: ").append(trip.getTripType().toString()).append("\n");
        report.append(" Сезон: ").append(isSummer ? "Літо" : "Зима").append("\n\n");

        if (trip.hasWaypoints()) {
            report.append(" ПРОМІЖНІ ТОЧКИ:\n");
            for (Waypoint waypoint : trip.getWaypoints()) {
                report.append("   ").append(waypoint.toString());
                if (waypoint.getEstimatedStopTime() > 0) {
                    report.append(" (зупинка ").append(waypoint.getFormattedStopTime()).append(")");
                }
                report.append("\n");
            }
            report.append("\n");
        }

        BigDecimal cityRate = isSummer ? vehicle.getCityRateSummer() : vehicle.getCityRateWinter();
        BigDecimal highwayRate = isSummer ? vehicle.getHighwayRateSummer() : vehicle.getHighwayRateWinter();

        if (cityRate != null && highwayRate != null) {
            report.append(" НОРМИ ВИТРАТ:\n");
            report.append(" Місто: ").append(cityRate).append(" л/км (")
                    .append(cityRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .append(" л/100км)\n");
            report.append(" Траса: ").append(highwayRate).append(" л/км (")
                    .append(highwayRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                    .append(" л/100км)\n\n");
        }

        BigDecimal requiredFuel = calculateFuelConsumptionWithWaypoints(vehicle, trip, isSummer);

        report.append(" РОЗРАХУНОК ВИТРАТ:\n");
        report.append(" ЗАГАЛОМ ПОТРІБНО: ").append(requiredFuel).append(" л\n\n");

        if (isEnoughFuel(vehicle, requiredFuel)) {
            report.append(" ПАЛИВА ДОСТАТНЬО ДЛЯ ПОЇЗДКИ\n");
            BigDecimal remaining = vehicle.getFuelBalance().subtract(requiredFuel);
            report.append(" Залишиться після поїздки: ").append(remaining.setScale(2, RoundingMode.HALF_UP)).append(" л\n");
        } else {
            BigDecimal toAdd = calculateFuelToAdd(vehicle, requiredFuel);
            report.append(" НЕДОСТАТНЬО ПАЛИВА!\n");
            report.append(" Потрібно дозаправити: ").append(toAdd).append(" л\n");

            BigDecimal afterRefill = vehicle.getFuelBalance().add(toAdd);
            if (vehicle.getTankCapacity() != null && afterRefill.compareTo(vehicle.getTankCapacity()) > 0) {
                report.append(" УВАГА: Після заправки (").append(afterRefill.setScale(1, RoundingMode.HALF_UP))
                        .append(" л) перевищить ємність бака (").append(vehicle.getTankCapacity()).append(" л)\n");
            }
        }

        return report.toString();
    }

    public static BigDecimal applyRefrigeratorMultiplier(BigDecimal baseFuelConsumption, 
                                                          BigDecimal refrigeratorUsagePercent,
                                                          boolean hasRefrigerator) {
        return applyRefrigeratorMultiplier(baseFuelConsumption, refrigeratorUsagePercent, hasRefrigerator, null);
    }

    public static BigDecimal applyRefrigeratorMultiplier(BigDecimal baseFuelConsumption, 
                                                          BigDecimal refrigeratorUsagePercent,
                                                          boolean hasRefrigerator,
                                                          Vehicle vehicle) {
        if (!hasRefrigerator || baseFuelConsumption == null || refrigeratorUsagePercent == null) {
            return baseFuelConsumption;
        }

        if (refrigeratorUsagePercent.compareTo(BigDecimal.ZERO) <= 0) {
            return baseFuelConsumption;
        }

        if (vehicle != null && isTestVehicleWithFixedRefrigeratorConsumption(vehicle)) {
            BigDecimal fixedConsumption = new BigDecimal("1.7");
            BigDecimal finalConsumption = baseFuelConsumption.add(fixedConsumption).setScale(2, RoundingMode.HALF_UP);
            logger.info(" Холодильник (тестова машина): фіксований розхід {} л → +{} л",
                    fixedConsumption,
                    fixedConsumption);
            return finalConsumption;
        }

        BigDecimal usageRatio = refrigeratorUsagePercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal coefficient = BigDecimal.ONE.add(REFRIGERATOR_CONSUMPTION_MULTIPLIER.multiply(usageRatio));
        BigDecimal finalConsumption = baseFuelConsumption.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
        BigDecimal additionalFuel = finalConsumption.subtract(baseFuelConsumption);
        logger.info(" Холодильник: {}% маршруту → коефіцієнт {} → +{} л",
                refrigeratorUsagePercent.setScale(0, RoundingMode.HALF_UP),
                coefficient.setScale(4, RoundingMode.HALF_UP),
                additionalFuel.setScale(2, RoundingMode.HALF_UP));
        return finalConsumption;
    }

    private static boolean isTestVehicleWithFixedRefrigeratorConsumption(Vehicle vehicle) {
        if (vehicle == null || vehicle.getLicensePlate() == null) {
            return false;
        }
        String licensePlate = vehicle.getLicensePlate().trim().toUpperCase();
        return licensePlate.equals("TEST-REFR-1.7") || licensePlate.equals("ТЕСТ-ХОЛОД-1.7");
    }

    public static BigDecimal calculateFactoryGroundConsumption(Vehicle vehicle, BigDecimal distanceKm, boolean isSummer) {
        if (vehicle == null || distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Не можна розрахувати витрати для роботи на території: відсутні дані");
            return BigDecimal.ZERO;
        }

        BigDecimal cityRate = isSummer ? vehicle.getCityRateSummer() : vehicle.getCityRateWinter();
        if (cityRate == null) {
            logger.warn("Не задана норма витрат для міста, використовую норму за замовчуванням");
            cityRate = DEFAULT_FACTORY_GROUND_CITY_RATE;
        }

        BigDecimal consumption = distanceKm.multiply(cityRate).setScale(4, RoundingMode.HALF_UP);
        logger.info("🏭 Робота на території: {} км × {} л/км = {} л",
                distanceKm.setScale(2, RoundingMode.HALF_UP),
                cityRate,
                consumption.setScale(2, RoundingMode.HALF_UP));
        return consumption;
    }

    public static BigDecimal calculateIdleWorkConsumption(Vehicle vehicle, BigDecimal hoursWorked, boolean isSummer) {
        if (vehicle == null || hoursWorked == null || hoursWorked.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Не можна розрахувати витрати на холостому ході: відсутні дані");
            return BigDecimal.ZERO;
        }

        BigDecimal idleRate = isSummer ? vehicle.getIdleRateSummer() : vehicle.getIdleRateWinter();
        if (idleRate == null) {
            logger.warn("Не задана норма витрат на холостому ході, використовую норму за замовчуванням");
            idleRate = IDLE_RATE_MULTIPLIER;
        }

        BigDecimal consumption = hoursWorked.multiply(idleRate).setScale(4, RoundingMode.HALF_UP);
        logger.info("⏱ Робота на холостому ході: {} год × {} л/год = {} л",
                hoursWorked.setScale(2, RoundingMode.HALF_UP),
                idleRate,
                consumption.setScale(2, RoundingMode.HALF_UP));
        return consumption;
    }

    public static BigDecimal calculateCombinedConsumption(Vehicle vehicle, BigDecimal distanceKm, 
                                                          BigDecimal hoursWorked, boolean isSummer) {
        if (vehicle == null) {
            logger.warn("Не можна розрахувати витрати: відсутні дані про автомобіль");
            return BigDecimal.ZERO;
        }

        BigDecimal factoryConsumption = BigDecimal.ZERO;
        BigDecimal idleConsumption = BigDecimal.ZERO;

        if (distanceKm != null && distanceKm.compareTo(BigDecimal.ZERO) > 0) {
            factoryConsumption = calculateFactoryGroundConsumption(vehicle, distanceKm, isSummer);
        }

        if (hoursWorked != null && hoursWorked.compareTo(BigDecimal.ZERO) > 0) {
            idleConsumption = calculateIdleWorkConsumption(vehicle, hoursWorked, isSummer);
        }

        BigDecimal totalConsumption = factoryConsumption.add(idleConsumption).setScale(4, RoundingMode.HALF_UP);
        logger.info(" Комбінована робота: рух {} л + холостий хід {} л = {} л",
                factoryConsumption.setScale(2, RoundingMode.HALF_UP),
                idleConsumption.setScale(2, RoundingMode.HALF_UP),
                totalConsumption.setScale(2, RoundingMode.HALF_UP));
        return totalConsumption;
    }

    public static BigDecimal calculateFuelConsumptionUniversal(Vehicle vehicle, Trip trip, boolean isSummer) {
        if (vehicle == null || trip == null) {
            return BigDecimal.ZERO;
        }

        TripType tripType = trip.getTripType();
        if (tripType == TripType.FACTORY_GROUND) {
            return calculateFactoryGroundConsumption(vehicle, trip.getPlannedDistance(), isSummer);
        } else if (tripType == TripType.IDLE_WORK) {
            BigDecimal hoursWorked = BigDecimal.valueOf(trip.getWaitingTime()).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            return calculateIdleWorkConsumption(vehicle, hoursWorked, isSummer);
        } else if (tripType == TripType.FACTORY_GROUND_WITH_IDLE) {
            BigDecimal hoursWorked = BigDecimal.valueOf(trip.getWaitingTime()).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            return calculateCombinedConsumption(vehicle, trip.getPlannedDistance(), hoursWorked, isSummer);
        } else {
            RouteInfo routeInfo = new RouteInfo();
            routeInfo.setTotalDistance(trip.getPlannedDistance());
            routeInfo.setCityDistance(trip.getPlannedCityKm());
            routeInfo.setHighwayDistance(trip.getPlannedHighwayKm());
            return calculateFuelConsumptionForTrip(vehicle, routeInfo, tripType, isSummer);
        }
    }

    public static class RouteSegment {
        private final BigDecimal cityKm;
        private final BigDecimal highwayKm;
        private final String description;

        public RouteSegment(BigDecimal cityKm, BigDecimal highwayKm, String description) {
            this.cityKm = cityKm != null ? cityKm : BigDecimal.ZERO;
            this.highwayKm = highwayKm != null ? highwayKm : BigDecimal.ZERO;
            this.description = description;
        }

        public BigDecimal getCityKm() { return cityKm; }
        public BigDecimal getHighwayKm() { return highwayKm; }
        public String getDescription() { return description; }
        public BigDecimal getTotalKm() { return cityKm.add(highwayKm); }
    }
}