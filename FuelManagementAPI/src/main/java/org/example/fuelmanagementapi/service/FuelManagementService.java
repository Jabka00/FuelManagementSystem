package org.example.fuelmanagementapi.service;

import org.example.fuelmanagementapi.dto.TripFinishRequest;
import org.example.fuelmanagementapi.entity.Driver;
import org.example.fuelmanagementapi.entity.Trip;
import org.example.fuelmanagementapi.entity.Vehicle;
import org.example.fuelmanagementapi.repository.DriverRepository;
import org.example.fuelmanagementapi.repository.TripRepository;
import org.example.fuelmanagementapi.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FuelManagementService {

    private static final Logger logger = LoggerFactory.getLogger(FuelManagementService.class);
    
    private static final BigDecimal REFRIGERATOR_CONSUMPTION_MULTIPLIER = new BigDecimal("0.15");

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Vehicle> getActiveVehicles() {
        try {
            List<Vehicle> vehicles = vehicleRepository.findActiveVehicles();
            logger.info("Завантажено {} активних машин", vehicles.size());
            return vehicles;
        } catch (Exception e) {
            logger.error("Помилка отримання машин: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Driver> getActiveDrivers() {
        try {
            List<Driver> drivers = driverRepository.findActiveDrivers();
            logger.info("Завантажено {} активних водіїв", drivers.size());

            for (Driver driver : drivers) {
                logger.debug("Driver: id={}, name={}, status={}",
                        driver.getId(), driver.getFullName(), driver.getStatus());
            }

            return drivers;
        } catch (Exception e) {
            logger.error("Помилка отримання водіїв: {}", e.getMessage(), e);

            try {
                List<Driver> allDrivers = driverRepository.findAll();
                List<Driver> activeDrivers = new ArrayList<>();

                for (Driver driver : allDrivers) {
                    if (driver.getStatus() == Driver.DriverStatus.active) {
                        activeDrivers.add(driver);
                    }
                }

                return activeDrivers;

            } catch (Exception fallbackEx) {
                return new ArrayList<>();
            }
        }
    }

    public List<Trip> getAllActiveTrips() {
        try {
            List<Object[]> results = tripRepository.findAllActiveTrips();
            List<Trip> trips = new ArrayList<>();

            for (Object[] row : results) {
                try {
                    Trip trip = mapRowToTrip(row);
                    trips.add(trip);
                } catch (Exception rowEx) {
                    logger.error("Помилка обробки рядка результату: {}", rowEx.getMessage(), rowEx);
                    
                }
            }

            logger.info("Завантажено {} активних поїздок", trips.size());
            return trips;
        } catch (Exception e) {
            logger.error("Помилка отримання активних поїздок: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<Trip> getDriverTrips(Integer driverId) {
        try {
            if (driverId == null || driverId <= 0) {
                logger.warn("Некоректний ID водія: {}", driverId);
                return new ArrayList<>();
            }

            List<Object[]> results = tripRepository.findActiveTripsForDriver(driverId);
            List<Trip> trips = new ArrayList<>();

            for (Object[] row : results) {
                try {
                    Trip trip = mapRowToTrip(row);
                    trips.add(trip);
                } catch (Exception rowEx) {
                    logger.error("Помилка обробки рядка результату: {}", rowEx.getMessage(), rowEx);
                }
            }

            logger.info("Завантажено {} активних поїздок для водія {}", trips.size(), driverId);
            return trips;
        } catch (Exception e) {
            logger.error("Помилка отримання поїздок: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private Trip mapRowToTrip(Object[] row) {
        Trip trip = new Trip();
        
        trip.setId(row[0] != null ? ((Number) row[0]).intValue() : null);
        trip.setTripNumber(row[1] != null ? (String) row[1] : null);
        trip.setStartAddress(row[2] != null ? (String) row[2] : null);
        trip.setEndAddress(row[3] != null ? (String) row[3] : null);

        String statusStr = row[4] != null ? (String) row[4] : null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                trip.setStatus(Trip.TripStatus.fromString(statusStr));
            } catch (Exception e) {
                logger.warn("Невідомий статус поїздки: '{}', встановлюємо assigned", statusStr);
                trip.setStatus(Trip.TripStatus.assigned);
            }
        } else {
            logger.debug("Статус не вказано для поїздки, встановлюємо assigned");
            trip.setStatus(Trip.TripStatus.assigned);
        }

        trip.setPlannedDistance(row[5] != null ? (BigDecimal) row[5] : null);
        trip.setActualDistance(row[6] != null ? (BigDecimal) row[6] : null);
        trip.setPlannedFuelConsumption(row[7] != null ? (BigDecimal) row[7] : null);
        trip.setActualFuelConsumption(row[8] != null ? (BigDecimal) row[8] : null);
        trip.setPlannedStartTime(row[9] != null ? ((java.sql.Timestamp) row[9]).toLocalDateTime() : null);
        trip.setCreatedAt(row[10] != null ? ((java.sql.Timestamp) row[10]).toLocalDateTime() : null);
        trip.setActualStartTime(row[11] != null ? ((java.sql.Timestamp) row[11]).toLocalDateTime() : null);
        trip.setActualEndTime(row[12] != null ? ((java.sql.Timestamp) row[12]).toLocalDateTime() : null);
        trip.setLicensePlate(row[13] != null ? (String) row[13] : null);
        trip.setModel(row[14] != null ? (String) row[14] : null);
        
        if (row.length > 15 && row[15] != null) {
            trip.setDriverFullName((String) row[15]);
        }
        
        if (row.length > 16 && row[16] != null) {
            trip.setRefrigeratorUsagePercent((BigDecimal) row[16]);
        }
        if (row.length > 17 && row[17] != null) {
            trip.setVehicleHasRefrigerator((Boolean) row[17]);
        }
        
        if (row.length > 18 && row[18] != null) {
            Integer startOdometer = ((Number) row[18]).intValue();
            trip.setActualStartOdometer(startOdometer);
            logger.debug("Поїздка {}: actualStartOdometer = {} км", trip.getId(), startOdometer);
        } else {
            logger.debug("Поїздка {}: actualStartOdometer не вказано (row.length={})", trip.getId(), row.length);
        }
        if (row.length > 19 && row[19] != null) {
            Integer endOdometer = ((Number) row[19]).intValue();
            trip.setActualEndOdometer(endOdometer);
            logger.debug("Поїздка {}: actualEndOdometer = {} км", trip.getId(), endOdometer);
        }
        if (row.length > 20 && row[20] != null) {
            trip.setVehicleId(((Number) row[20]).intValue());
        }
        if (row.length > 21 && row[21] != null) {
            trip.setDriverId(((Number) row[21]).intValue());
        }

        logger.debug("Поїздку {} успішно оброблено: статус={}, пробіг={} км", 
                trip.getId(), trip.getStatus(), trip.getActualStartOdometer());
        return trip;
    }

    @Transactional
    public boolean startTrip(Integer tripId) {
        try {
            if (tripId == null || tripId <= 0) {
                logger.error("Некоректний ID поїздки: {}", tripId);
                return false;
            }

            Optional<Trip> tripOpt = tripRepository.findById(tripId);
            if (!tripOpt.isPresent()) {
                logger.warn("Поїздку {} не знайдено", tripId);
                return false;
            }

            Trip trip = tripOpt.get();

            if (!trip.canBeStarted()) {
                logger.warn("Поїздку {} не можна розпочати, поточний статус: {}", tripId, trip.getStatus());
                return false;
            }

            Integer currentOdometer = null;
            if (trip.getVehicleId() != null && trip.getVehicleId() > 0) {
                try {
                    Optional<Vehicle> vehicleOpt = vehicleRepository.findById(trip.getVehicleId());
                    if (vehicleOpt.isPresent()) {
                        currentOdometer = vehicleOpt.get().getCurrentOdometer();
                        if (currentOdometer != null) {
                            logger.info("Поточний пробіг автомобіля {}: {} км",
                                    vehicleOpt.get().getLicensePlate(), currentOdometer);
                        } else {
                            logger.warn("Пробіг не вказано для автомобіля {}", 
                                    vehicleOpt.get().getLicensePlate());
                        }
                    } else {
                        logger.warn("Автомобіль з ID {} не знайдено", trip.getVehicleId());
                    }
                } catch (Exception ex) {
                    logger.error("Помилка отримання пробігу автомобіля: {}", ex.getMessage());
                }
            } else {
                logger.warn("Автомобіль не призначено для поїздки {}", tripId);
            }

            LocalDateTime now = LocalDateTime.now();
            int updated = tripRepository.startTripWithOdometer(tripId, now, now, currentOdometer);

            if (updated > 0) {
                logger.info("Розпочато поїздку {} з показником одометра: {} км", tripId, 
                        currentOdometer != null ? currentOdometer : "не вказано");
                return true;
            } else {
                logger.warn("Поїздку {} не вдалося розпочати (можливо, статус змінився)", tripId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Помилка початку поїздки {}: {}", tripId, e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public boolean startTripWithOdometer(Integer tripId, Integer startOdometer) {
        try {
            if (tripId == null || tripId <= 0) {
                logger.error("Некоректний ID поїздки: {}", tripId);
                return false;
            }

            if (startOdometer == null || startOdometer <= 0) {
                logger.error("Некоректний показник одометра: {}", startOdometer);
                return false;
            }

            Optional<Trip> tripOpt = tripRepository.findById(tripId);
            if (!tripOpt.isPresent()) {
                logger.warn("Поїздку {} не знайдено", tripId);
                return false;
            }

            Trip trip = tripOpt.get();

            if (!trip.canBeStarted()) {
                logger.warn("Поїздку {} не можна розпочати, поточний статус: {}", tripId, trip.getStatus());
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            int updated = tripRepository.startTripWithOdometer(tripId, now, now, startOdometer);

            if (updated > 0) {
                logger.info("Розпочато поїздку {} з показником одометра: {} км (охорона)", tripId, startOdometer);
                return true;
            } else {
                logger.warn("Поїздку {} не вдалося розпочати (можливо, статус змінився)", tripId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Помилка початку поїздки {}: {}", tripId, e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public boolean finishTrip(Integer tripId, TripFinishRequest request) {
        try {
            if (tripId == null || tripId <= 0) {
                logger.error("Некоректний ID поїздки: {}", tripId);
                return false;
            }

            if (request == null) {
                logger.error("Запит на завершення поїздки порожній");
                return false;
            }

            Optional<Trip> tripOpt = tripRepository.findById(tripId);
            if (!tripOpt.isPresent()) {
                logger.warn("Поїздку {} не знайдено", tripId);
                return false;
            }

            Trip trip = tripOpt.get();

            if (!trip.canBeFinished()) {
                logger.warn("Поїздку {} не можна завершити, поточний статус: {}", tripId, trip.getStatus());
                return false;
            }

            BigDecimal actualDistance;
            Integer endOdometer = null;

            if (request.getInputType() == TripFinishRequest.DistanceInputType.ODOMETER_READING) {
                endOdometer = request.getEndOdometer();

                if (endOdometer == null || endOdometer <= 0) {
                    logger.error("Некоректний кінцевий показник одометра: {}", endOdometer);
                    return false;
                }

                if (trip.getActualStartOdometer() == null) {
                    logger.error("Відсутній початковий показник одометра для поїздки {}", tripId);
                    return false;
                }

                if (endOdometer <= trip.getActualStartOdometer()) {
                    logger.error("Кінцевий показник одометра ({}) не може бути меншим або рівним початковому ({})",
                            endOdometer, trip.getActualStartOdometer());
                    return false;
                }

                actualDistance = BigDecimal.valueOf(endOdometer - trip.getActualStartOdometer());

                if (trip.getVehicleId() != null && trip.getVehicleId() > 0) {
                    try {
                        int updatedVehicle = vehicleRepository.updateCurrentOdometer(trip.getVehicleId(), endOdometer);
                        if (updatedVehicle > 0) {
                            logger.info("Оновлено пробіг автомобіля ID {}: {} км", trip.getVehicleId(), endOdometer);
                        } else {
                            logger.warn("Не вдалося оновити пробіг автомобіля ID {}", trip.getVehicleId());
                        }
                    } catch (Exception ex) {
                        logger.error("Помилка оновлення пробігу автомобіля: {}", ex.getMessage());
                    }
                }

                logger.info("Розраховано відстань за одометром: {} - {} = {} км",
                        endOdometer, trip.getActualStartOdometer(), actualDistance);

            } else {
                actualDistance = request.getActualDistance();
                
                if (actualDistance == null || actualDistance.compareTo(BigDecimal.ZERO) <= 0) {
                    logger.error("Некоректна відстань: {}", actualDistance);
                    return false;
                }
                
                logger.info("Введено відстань вручну: {} км", actualDistance);
            }

            BigDecimal actualCityKm = BigDecimal.ZERO;
            BigDecimal actualHighwayKm = BigDecimal.ZERO;
            BigDecimal routeDeviationPercent = BigDecimal.ZERO;
            
            if (trip.getPlannedDistance() != null && trip.getPlannedDistance().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal deviation = actualDistance.subtract(trip.getPlannedDistance());
                routeDeviationPercent = deviation
                    .divide(trip.getPlannedDistance(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
                
                BigDecimal plannedCityKm = trip.getPlannedCityKm() != null ? trip.getPlannedCityKm() : BigDecimal.ZERO;
                BigDecimal plannedHighwayKm = trip.getPlannedHighwayKm() != null ? trip.getPlannedHighwayKm() : BigDecimal.ZERO;
                
                if (plannedCityKm.compareTo(BigDecimal.ZERO) > 0 || plannedHighwayKm.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal cityProportion = plannedCityKm.divide(trip.getPlannedDistance(), 4, RoundingMode.HALF_UP);
                    BigDecimal highwayProportion = plannedHighwayKm.divide(trip.getPlannedDistance(), 4, RoundingMode.HALF_UP);
                    
                    actualCityKm = actualDistance.multiply(cityProportion).setScale(2, RoundingMode.HALF_UP);
                    actualHighwayKm = actualDistance.multiply(highwayProportion).setScale(2, RoundingMode.HALF_UP);
                    
                    logger.info("Пропорційний розподіл: {}% міста ({} км), {}% траси ({} км), відхилення: {}%",
                        cityProportion.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP),
                        actualCityKm,
                        highwayProportion.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP),
                        actualHighwayKm,
                        routeDeviationPercent);
                } else {
                    logger.warn("Планові км по місту/трасі не вказані, неможливо розрахувати пропорції");
                }
            } else {
                logger.warn("Планова відстань не вказана, неможливо розрахувати відхилення");
            }
            
            BigDecimal actualFuelConsumption = BigDecimal.ZERO;
            if (trip.getVehicleId() != null && trip.getVehicleId() > 0) {
                try {
                    Optional<Vehicle> vehicleOpt = vehicleRepository.findById(trip.getVehicleId());
                    if (vehicleOpt.isPresent()) {
                        Vehicle vehicle = vehicleOpt.get();
                        boolean isWinter = trip.getSeason() == Trip.Season.winter;
                        
                        BigDecimal baseFuelConsumption = vehicle.calculateFuelConsumption(actualCityKm, actualHighwayKm, isWinter);
                        
                        actualFuelConsumption = applyRefrigeratorMultiplier(
                            baseFuelConsumption, 
                            trip.getRefrigeratorUsagePercent(), 
                            vehicle.getHasRefrigerator()
                        );
                        
                        if (vehicle.getHasRefrigerator() != null && vehicle.getHasRefrigerator() && 
                            trip.getRefrigeratorUsagePercent() != null && 
                            trip.getRefrigeratorUsagePercent().compareTo(BigDecimal.ZERO) > 0) {
                            logger.info("Розраховано фактичні витрати палива: базові {} л + холодільник {}% = {} л", 
                                baseFuelConsumption, trip.getRefrigeratorUsagePercent(), actualFuelConsumption);
                        } else {
                            logger.info("Розраховано фактичні витрати палива: {} л", actualFuelConsumption);
                        }
                    } else {
                        logger.warn("Автомобіль не знайдено для розрахунку витрат палива");
                    }
                } catch (Exception ex) {
                    logger.error("Помилка розрахунку витрат палива: {}", ex.getMessage());
                }
            }
            
            trip.setActualCityKm(actualCityKm);
            trip.setActualHighwayKm(actualHighwayKm);
            trip.setActualFuelConsumption(actualFuelConsumption);
            trip.setRouteDeviationPercent(routeDeviationPercent);
            trip.setActualDistance(actualDistance);
            trip.setActualEndOdometer(endOdometer);
            trip.setActualEndTime(LocalDateTime.now());
            trip.setStatus(Trip.TripStatus.completed);
            
            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                trip.setNotes(request.getNotes());
            }
            
            Trip savedTrip = tripRepository.save(trip);

            if (savedTrip != null) {
                
                if (trip.getVehicleId() != null && trip.getVehicleId() > 0 && 
                    actualFuelConsumption != null && actualFuelConsumption.compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        int updatedFuel = vehicleRepository.reduceFuelBalance(trip.getVehicleId(), actualFuelConsumption);
                        if (updatedFuel > 0) {
                            logger.info("Оновлено баланс палива автомобіля ID {}: віднято {} л", 
                                    trip.getVehicleId(), actualFuelConsumption);
                        } else {
                            logger.warn("Не вдалося оновити баланс палива автомобіля ID {} (можливо недостатньо палива)", 
                                    trip.getVehicleId());
                        }
                    } catch (Exception ex) {
                        logger.error("Помилка оновлення балансу палива автомобіля ID {}: {}", 
                                trip.getVehicleId(), ex.getMessage());
                    }
                }
                
                logger.info("Завершено поїздку {}: {} км (місто: {} км, траса: {} км), відхилення: {}%, витрати палива: {} л", 
                    tripId, actualDistance, actualCityKm, actualHighwayKm, routeDeviationPercent, actualFuelConsumption);
                return true;
            } else {
                logger.warn("Поїздку {} не вдалося завершити", tripId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Помилка завершення поїздки {}: {}", tripId, e.getMessage(), e);
            return false;
        }
    }

    public boolean finishTrip(Integer tripId, BigDecimal actualDistance) {
        TripFinishRequest request = new TripFinishRequest(actualDistance);
        return finishTrip(tripId, request);
    }

    public Optional<Trip> getTripById(Integer tripId) {
        try {
            return tripRepository.findById(tripId);
        } catch (Exception e) {
            logger.error("Помилка отримання поїздки: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<Integer> getCurrentOdometerForTrip(Integer tripId) {
        try {
            Optional<Trip> tripOpt = tripRepository.findById(tripId);
            if (!tripOpt.isPresent()) {
                return Optional.empty();
            }

            Trip trip = tripOpt.get();
            if (trip.getVehicleId() == null) {
                return Optional.empty();
            }

            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(trip.getVehicleId());
            if (vehicleOpt.isPresent()) {
                return Optional.ofNullable(vehicleOpt.get().getCurrentOdometer());
            }

            return Optional.empty();
        } catch (Exception e) {
            logger.error("Помилка отримання пробігу автомобіля: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Transactional
    public boolean updateTripStartOdometer(Integer tripId, Integer newStartOdometer) {
        try {
            if (tripId == null || tripId <= 0) {
                logger.error("Некоректний ID поїздки: {}", tripId);
                return false;
            }

            if (newStartOdometer == null || newStartOdometer <= 0) {
                logger.error("Некоректний показник одометра: {}", newStartOdometer);
                return false;
            }

            Optional<Trip> tripOpt = tripRepository.findById(tripId);
            if (!tripOpt.isPresent()) {
                logger.warn("Поїздку {} не знайдено", tripId);
                return false;
            }

            Trip trip = tripOpt.get();

            if (trip.getStatus() != Trip.TripStatus.started) {
                logger.warn("Поїздку {} не можна оновити, поточний статус: {}", tripId, trip.getStatus());
                return false;
            }

            if (trip.getVehicleId() != null && trip.getVehicleId() > 0) {
                Optional<Vehicle> vehicleOpt = vehicleRepository.findById(trip.getVehicleId());
                if (vehicleOpt.isPresent()) {
                    Vehicle vehicle = vehicleOpt.get();
                    if (vehicle.getCurrentOdometer() != null && newStartOdometer > vehicle.getCurrentOdometer()) {
                        logger.warn("Новий початковий пробіг ({}) більший за поточний пробіг машини ({})", 
                                newStartOdometer, vehicle.getCurrentOdometer());
                        return false;
                    }
                }
            }

            if (trip.getActualEndOdometer() != null && newStartOdometer >= trip.getActualEndOdometer()) {
                logger.warn("Новий початковий пробіг ({}) повинен бути меншим за кінцевий ({})", 
                        newStartOdometer, trip.getActualEndOdometer());
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            int updated = tripRepository.updateStartOdometer(tripId, newStartOdometer, now);

            if (updated > 0) {
                logger.info("Оновлено початковий пробіг поїздки {}: {} км (було: {} км)", 
                        tripId, newStartOdometer, trip.getActualStartOdometer());
                return true;
            } else {
                logger.warn("Не вдалося оновити початковий пробіг поїздки {}", tripId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Помилка оновлення початкового пробігу поїздки {}: {}", tripId, e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public Trip createTrip(Integer vehicleId, Integer driverId, Integer odometer) {
        try {
            if (vehicleId == null || vehicleId <= 0) {
                logger.error("Некоректний ID автомобіля: {}", vehicleId);
                throw new IllegalArgumentException("ID автомобіля обов'язковий");
            }

            if (driverId == null || driverId <= 0) {
                logger.error("Некоректний ID водія: {}", driverId);
                throw new IllegalArgumentException("ID водія обов'язковий");
            }

            if (odometer == null || odometer <= 0) {
                logger.error("Некоректний пробіг: {}", odometer);
                throw new IllegalArgumentException("Пробіг обов'язковий");
            }

            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (!vehicleOpt.isPresent()) {
                logger.error("Автомобіль з ID {} не знайдено", vehicleId);
                throw new IllegalArgumentException("Автомобіль не знайдено");
            }

            Optional<Driver> driverOpt = driverRepository.findById(driverId);
            if (!driverOpt.isPresent()) {
                logger.error("Водій з ID {} не знайдено", driverId);
                throw new IllegalArgumentException("Водій не знайдено");
            }

            Vehicle vehicle = vehicleOpt.get();
            Driver driver = driverOpt.get();

            if (!vehicle.isAvailable()) {
                logger.error("Автомобіль {} недоступний (статус: {})", vehicle.getLicensePlate(), vehicle.getStatus());
                throw new IllegalArgumentException("Автомобіль недоступний");
            }

            if (!driver.isAvailable()) {
                logger.error("Водій {} недоступний (статус: {})", driver.getFullName(), driver.getStatus());
                throw new IllegalArgumentException("Водій недоступний");
            }

            Integer requesterId = getOrCreateDefaultRequester();
            
            LocalDateTime now = LocalDateTime.now();
            
            Trip trip = new Trip();
            trip.setVehicleId(vehicleId);
            trip.setDriverId(driverId);
            trip.setRequesterId(requesterId);
            trip.setRequesterName("Охорона");
            trip.setRequesterEmail("security@company.com");
            trip.setActualStartOdometer(odometer);
            trip.setActualStartTime(now);
            trip.setStatus(Trip.TripStatus.started);
            trip.setCreatedAt(now);
            trip.setUpdatedAt(now);
            trip.setPlannedStartTime(now);
            trip.setPlannedEndTime(now.plusHours(8));
            
            Trip.Season currentSeason = Trip.Season.getCurrentSeason();
            trip.setSeason(currentSeason);
            logger.info("Встановлено сезон для поїздки: {} (місяць: {})", 
                    currentSeason == Trip.Season.summer ? "літо" : "зима", now.getMonthValue());
            
            if (vehicle.getCurrentOdometer() != null && odometer > vehicle.getCurrentOdometer()) {
                vehicleRepository.updateCurrentOdometer(vehicleId, odometer);
                logger.info("Оновлено пробіг автомобіля {}: {} км", vehicle.getLicensePlate(), odometer);
            }

            Trip savedTrip = tripRepository.save(trip);
            logger.info("Створено поїздку ID {}: автомобіль {}, водій {}, пробіг {} км", 
                    savedTrip.getId(), vehicle.getLicensePlate(), driver.getFullName(), odometer);

            return savedTrip;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Помилка створення поїздки: {}", e.getMessage(), e);
            throw new RuntimeException("Помилка створення поїздки: " + e.getMessage(), e);
        }
    }

    private Integer getOrCreateDefaultRequester() {
        try {
            String findSql = "SELECT id FROM requesters WHERE email = ? LIMIT 1";
            List<Integer> ids = jdbcTemplate.query(findSql, 
                (rs, rowNum) -> rs.getInt("id"), 
                "security@company.com");
            
            if (!ids.isEmpty()) {
                Integer existingId = ids.get(0);
                logger.info("Знайдено дефолтного замовника (Охорона) з ID {}", existingId);
                return existingId;
            }
            
            String createSql = "INSERT INTO requesters (full_name, email, phone, created_at) VALUES (?, ?, ?, NOW())";
            jdbcTemplate.update(createSql, "Охорона", "security@company.com", "");
            
            List<Integer> newIds = jdbcTemplate.query(findSql, 
                (rs, rowNum) -> rs.getInt("id"), 
                "security@company.com");
            
            if (!newIds.isEmpty()) {
                Integer newId = newIds.get(0);
                logger.info("Створено дефолтного замовника (Охорона) з ID {}", newId);
                return newId;
            }
            
            logger.warn("Не вдалося створити дефолтного замовника, використовую ID 1");
            return 1;
        } catch (Exception e) {
            logger.error("Помилка створення/пошуку дефолтного замовника: {}", e.getMessage(), e);
            logger.warn("Використовую ID 1 як запасний варіант");
            return 1;
        }
    }

    private BigDecimal applyRefrigeratorMultiplier(BigDecimal baseFuelConsumption, 
                                                    BigDecimal refrigeratorUsagePercent, 
                                                    Boolean hasRefrigerator) {
        if (baseFuelConsumption == null || baseFuelConsumption.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        if (hasRefrigerator == null || !hasRefrigerator || 
            refrigeratorUsagePercent == null || 
            refrigeratorUsagePercent.compareTo(BigDecimal.ZERO) == 0) {
            return baseFuelConsumption;
        }
        
        BigDecimal percent = refrigeratorUsagePercent;
        if (percent.compareTo(BigDecimal.ZERO) < 0) {
            percent = BigDecimal.ZERO;
        }
        if (percent.compareTo(BigDecimal.valueOf(100)) > 0) {
            percent = BigDecimal.valueOf(100);
        }
        
        BigDecimal multiplierEffect = REFRIGERATOR_CONSUMPTION_MULTIPLIER
            .multiply(percent)
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        
        BigDecimal coefficient = BigDecimal.ONE.add(multiplierEffect);
        
        return baseFuelConsumption.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
    }
}
