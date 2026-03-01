package org.example.fuelmanagement.controller.helper;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.example.fuelmanagement.model.RouteInfo;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.Waypoint;
import org.example.fuelmanagement.model.enums.Season;
import org.example.fuelmanagement.model.enums.TripType;
import org.example.fuelmanagement.service.GoogleMapsService;
import org.example.fuelmanagement.util.FuelCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RouteCalculationHelper {
    private static final Logger logger = LoggerFactory.getLogger(RouteCalculationHelper.class);

    private final GoogleMapsService googleMapsService;

    public RouteCalculationHelper(GoogleMapsService googleMapsService) {
        this.googleMapsService = googleMapsService;
    }

    public boolean isConfigured() {
        return googleMapsService != null && googleMapsService.isConfigured();
    }

    public void calculateRoute(
            String startAddr,
            String endAddr,
            ObservableList<Waypoint> waypoints,
            TripType tripType,
            Consumer<RouteInfo> onSuccess,
            Consumer<Throwable> onError
    ) {
        List<String> waypointAddresses = waypoints.stream()
                .map(Waypoint::getAddress)
                .collect(Collectors.toList());

        if (!waypointAddresses.isEmpty() && tripType == TripType.ROUND_TRIP) {
            calculateRoundTripWithWaypoints(startAddr, endAddr, waypointAddresses, waypoints, onSuccess, onError);
        } else if (!waypointAddresses.isEmpty()) {
            googleMapsService.calculateRouteWithWaypointsAsync(startAddr, endAddr, waypointAddresses)
                    .thenAccept(routeInfo -> Platform.runLater(() -> onSuccess.accept(routeInfo)))
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> onError.accept(throwable));
                        return null;
                    });
        } else {
            googleMapsService.calculateRouteAsync(startAddr, endAddr)
                    .thenAccept(routeInfo -> Platform.runLater(() -> onSuccess.accept(routeInfo)))
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> onError.accept(throwable));
                        return null;
                    });
        }
    }

    private void calculateRoundTripWithWaypoints(
            String startAddr,
            String endAddr,
            List<String> waypointAddresses,
            ObservableList<Waypoint> waypoints,
            Consumer<RouteInfo> onSuccess,
            Consumer<Throwable> onError
    ) {
        googleMapsService.calculateRouteWithWaypointsAsync(startAddr, endAddr, waypointAddresses)
                .thenCompose(forwardRoute -> 
                    googleMapsService.calculateRouteAsync(endAddr, startAddr)
                            .thenApply(returnRoute -> combineRoutes(forwardRoute, returnRoute, startAddr, endAddr, waypoints))
                )
                .thenAccept(combinedRoute -> Platform.runLater(() -> onSuccess.accept(combinedRoute)))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> onError.accept(throwable));
                    return null;
                });
    }

    public RouteInfo combineRoutes(
            RouteInfo forwardRoute,
            RouteInfo returnRoute,
            String startAddr,
            String endAddr,
            ObservableList<Waypoint> waypoints
    ) {
        if (forwardRoute == null || returnRoute == null || forwardRoute.isEmpty() || returnRoute.isEmpty()) {
            return forwardRoute != null ? forwardRoute : new RouteInfo();
        }

        BigDecimal totalDistance = forwardRoute.getTotalDistance().add(returnRoute.getTotalDistance());
        BigDecimal cityDistance = forwardRoute.getCityDistance().add(returnRoute.getCityDistance());
        BigDecimal highwayDistance = forwardRoute.getHighwayDistance().add(returnRoute.getHighwayDistance());
        Duration totalDuration = forwardRoute.getEstimatedDuration().plus(returnRoute.getEstimatedDuration());

        RouteInfo combined = new RouteInfo(totalDistance, totalDuration);
        combined.setCityDistance(cityDistance);
        combined.setHighwayDistance(highwayDistance);

        StringBuilder detailedRouteDescription = new StringBuilder();
        detailedRouteDescription.append(startAddr);
        if (waypoints != null) {
            for (Waypoint wp : waypoints) {
                detailedRouteDescription.append(" → ").append(wp.getAddress());
            }
        }
        detailedRouteDescription.append(" → ").append(endAddr);
        detailedRouteDescription.append(" → ").append(startAddr);

        combined.setRouteDescription(String.format("Туди-назад: %s (%s, час: %s)",
                detailedRouteDescription.toString(),
                combined.getFormattedDistance(),
                combined.getFormattedDuration()));

        logger.info("Об'єднано маршрути: прямий {} км + зворотний {} км = загалом {} км",
                forwardRoute.getTotalDistance(), returnRoute.getTotalDistance(), totalDistance);

        return combined;
    }

    public String createFuelReport(
            RouteInfo routeInfo,
            Vehicle vehicle,
            TripType tripType,
            BigDecimal refrigeratorPercent,
            ObservableList<Waypoint> waypoints,
            String startAddress,
            String endAddress
    ) {
        if (routeInfo.isEmpty()) {
            return "Не вдалося розрахувати маршрут\n" + routeInfo.getRouteDescription();
        }

        boolean isSummer = Season.getCurrentSeason() == Season.SUMMER;
        boolean isAlreadyCombined = tripType == TripType.ROUND_TRIP && !waypoints.isEmpty();
        TripType calculationType = isAlreadyCombined ? TripType.ONE_WAY : tripType;

        StringBuilder fuelReport = new StringBuilder();
        fuelReport.append(FuelCalculator.createFuelReport(vehicle, routeInfo, calculationType, isSummer, refrigeratorPercent));

        if (!waypoints.isEmpty()) {
            fuelReport.append("\n══════════════════════════════════════\n");
            fuelReport.append("ПРОМІЖНІ ТОЧКИ МАРШРУТУ:\n");
            for (int i = 0; i < waypoints.size(); i++) {
                Waypoint wp = waypoints.get(i);
                fuelReport.append(String.format("%d. %s", i + 1, wp.getFullDescription()));
                if (wp.getEstimatedStopTime() > 0) {
                    fuelReport.append(String.format(" (зупинка %s)", wp.getFormattedStopTime()));
                }
                fuelReport.append("\n");
            }

            fuelReport.append("\nСТРУКТУРА МАРШРУТУ:\n");
            if (tripType == TripType.ROUND_TRIP) {
                fuelReport.append("   Туди: ").append(startAddress);
                for (Waypoint wp : waypoints) {
                    fuelReport.append(" → ").append(wp.getAddress());
                }
                fuelReport.append(" → ").append(endAddress).append("\n");
                fuelReport.append("   Назад: ").append(endAddress)
                        .append(" → ").append(startAddress)
                        .append(" (без проміжних точок)\n");
                fuelReport.append("\nПроміжні точки відвідуються тільки один раз - на шляху туди");
            } else {
                fuelReport.append("   ").append(startAddress);
                for (Waypoint wp : waypoints) {
                    fuelReport.append(" → ").append(wp.getAddress());
                }
                fuelReport.append(" → ").append(endAddress).append("\n");
                fuelReport.append("\nМаршрут в один бік з усіма проміжними точками");
            }
        }

        return fuelReport.toString();
    }

    public String createLogMessage(
            RouteInfo routeInfo,
            Vehicle vehicle,
            TripType tripType,
            BigDecimal refrigeratorPercent,
            ObservableList<Waypoint> waypoints
    ) {
        boolean isSummer = Season.getCurrentSeason() == Season.SUMMER;
        boolean isAlreadyCombined = tripType == TripType.ROUND_TRIP && !waypoints.isEmpty();
        TripType calculationType = isAlreadyCombined ? TripType.ONE_WAY : tripType;

        BigDecimal baseFuelConsumption = FuelCalculator.calculateFuelConsumptionForTrip(
                vehicle, routeInfo, calculationType, isSummer);
        BigDecimal finalFuelConsumption = FuelCalculator.applyRefrigeratorMultiplier(
                baseFuelConsumption, refrigeratorPercent, vehicle.hasRefrigerator(), vehicle);

        String waypointInfo = !waypoints.isEmpty() ? " (з " + waypoints.size() + " проміжними точками)" : "";
        BigDecimal totalDistance = routeInfo.getTotalDistance();
        String distanceInfo;
        if (tripType == TripType.ROUND_TRIP && !isAlreadyCombined) {
            BigDecimal roundTripDistance = totalDistance.multiply(BigDecimal.valueOf(2));
            distanceInfo = String.format("%.1f км (%.1f км × 2)",
                    roundTripDistance.doubleValue(),
                    totalDistance.doubleValue());
        } else {
            distanceInfo = String.format("%.1f км", totalDistance.doubleValue());
        }

        String refrigeratorInfo = "";
        if (vehicle.hasRefrigerator() && refrigeratorPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal additionalFuel = finalFuelConsumption.subtract(baseFuelConsumption);
            refrigeratorInfo = String.format(" | Холодильник: +%.2f л (%s%%)",
                    additionalFuel.doubleValue(),
                    refrigeratorPercent.setScale(0, RoundingMode.HALF_UP));
        }

        return String.format("Маршрут%s: %s | Паливо: %.2f л%s\n",
                waypointInfo, distanceInfo, finalFuelConsumption.doubleValue(), refrigeratorInfo);
    }
}
