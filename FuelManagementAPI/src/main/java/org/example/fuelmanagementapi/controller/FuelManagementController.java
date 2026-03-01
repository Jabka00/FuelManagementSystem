package org.example.fuelmanagementapi.controller;

import org.example.fuelmanagementapi.dto.ApiResponse;
import org.example.fuelmanagementapi.dto.TripCreateRequest;
import org.example.fuelmanagementapi.dto.TripFinishRequest;
import org.example.fuelmanagementapi.dto.TripStartRequest;
import org.example.fuelmanagementapi.entity.Driver;
import org.example.fuelmanagementapi.entity.Trip;
import org.example.fuelmanagementapi.entity.Vehicle;
import org.example.fuelmanagementapi.service.FuelManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FuelManagementController {

    private static final Logger logger = LoggerFactory.getLogger(FuelManagementController.class);

    private final FuelManagementService fuelService;

    public FuelManagementController(FuelManagementService fuelService) {
        this.fuelService = fuelService;
    }

    @GetMapping("/drivers")
    public ResponseEntity<ApiResponse<List<Driver>>> getActiveDrivers() {
        logger.info("Запит списку активних водіїв");

        try {
            List<Driver> drivers = fuelService.getActiveDrivers();

            if (drivers.isEmpty()) {
                logger.warn("Список водіїв порожній");
                return ResponseEntity.ok(ApiResponse.success("Активних водіїв не знайдено", drivers));
            }

            logger.info("Знайдено {} активних водіїв", drivers.size());
            return ResponseEntity.ok(ApiResponse.success(drivers));

        } catch (Exception e) {
            logger.error("Помилка при отриманні списку водіїв: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Внутрішня помилка сервера"));
        }
    }

    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponse<List<Vehicle>>> getActiveVehicles() {
        logger.info("Запит списку активних автомобілів");

        try {
            List<Vehicle> vehicles = fuelService.getActiveVehicles();

            if (vehicles.isEmpty()) {
                logger.warn("Список автомобілів порожній");
                return ResponseEntity.ok(ApiResponse.success("Активних автомобілів не знайдено", vehicles));
            }

            logger.info("Знайдено {} активних автомобілів", vehicles.size());
            return ResponseEntity.ok(ApiResponse.success(vehicles));

        } catch (Exception e) {
            logger.error("Помилка при отриманні списку автомобілів: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Внутрішня помилка сервера"));
        }
    }

    @PostMapping("/trips/create")
    public ResponseEntity<ApiResponse<Trip>> createTrip(@Valid @RequestBody TripCreateRequest request) {
        logger.info("Створення поїздки: автомобіль {}, водій {}, пробіг {} км", 
                request.getVehicleId(), request.getDriverId(), request.getOdometer());

        if (!request.isValid()) {
            logger.warn("Некоректні дані для створення поїздки: {}", request);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Некоректні дані. Перевірте всі поля."));
        }

        try {
            Trip trip = fuelService.createTrip(
                    request.getVehicleId(),
                    request.getDriverId(),
                    request.getOdometer()
            );

            logger.info("Поїздку {} успішно створено", trip.getId());
            return ResponseEntity.ok(ApiResponse.success("Поїздку створено", trip));

        } catch (IllegalArgumentException e) {
            logger.warn("Помилка валідації: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Помилка створення поїздки: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Внутрішня помилка при створенні поїздки: " + e.getMessage()));
        }
    }

    @GetMapping("/trips/active/all")
    public ResponseEntity<ApiResponse<List<Trip>>> getAllActiveTrips() {
        logger.info("Запит всіх активних поїздок (пост охорони)");

        try {
            List<Trip> trips = fuelService.getAllActiveTrips();

            if (trips.isEmpty()) {
                logger.info("Активних поїздок не знайдено");
            } else {
                logger.info("Знайдено {} активних поїздок", trips.size());
            }

            return ResponseEntity.ok(ApiResponse.success(trips));

        } catch (Exception e) {
            logger.error("Помилка при отриманні активних поїздок: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Помилка при завантаженні поїздок"));
        }
    }

    @GetMapping("/trips/{driverId}")
    public ResponseEntity<ApiResponse<List<Trip>>> getDriverTrips(
            @PathVariable @NotNull @Min(1) Integer driverId) {

        logger.info("Запит поїздок для водія {}", driverId);

        try {
            List<Trip> trips = fuelService.getDriverTrips(driverId);

            if (trips.isEmpty()) {
                logger.info("У водія {} немає активних поїздок", driverId);
            } else {
                logger.info("Знайдено {} поїздок для водія {}", trips.size(), driverId);
            }

            return ResponseEntity.ok(ApiResponse.success(trips));

        } catch (Exception e) {
            logger.error("Помилка при отриманні поїздок водія {}: {}", driverId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Помилка при завантаженні поїздок"));
        }
    }

    @PostMapping("/trips/{tripId}/start")
    public ResponseEntity<ApiResponse<Trip>> startTrip(
            @PathVariable @NotNull @Min(1) Integer tripId,
            @RequestBody @NotNull @Valid TripStartRequest request) {

        logger.info("Спроба розпочати поїздку {} (охорона)", tripId);

        if (request == null || request.getStartOdometer() == null || request.getStartOdometer() <= 0) {
            logger.warn("Спроба стартувати поїздку {} без показників одометра заблокована", tripId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Показники одометра обов'язкові для старту поїздки"));
        }

        try {
            logger.info("Фіксація виїзду з показниками одометра: {} км", request.getStartOdometer());
            boolean success = fuelService.startTripWithOdometer(tripId, request.getStartOdometer());

            if (success) {
                Optional<Trip> updatedTrip = fuelService.getTripById(tripId);
                if (updatedTrip.isPresent()) {
                    logger.info("Поїздку {} успішно розпочато", tripId);
                    return ResponseEntity.ok(
                            ApiResponse.success("Виїзд зафіксовано", updatedTrip.get())
                    );
                }
            }

            logger.warn("Не вдалося розпочати поїздку {}", tripId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Неможливо розпочати поїздку. Перевірте статус."));

        } catch (Exception e) {
            logger.error("Помилка при старті поїздки {}: {}", tripId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Внутрішня помилка при старті поїздки"));
        }
    }

    @PostMapping("/trips/{tripId}/finish")
    public ResponseEntity<ApiResponse<String>> finishTrip(
            @PathVariable @NotNull @Min(1) Integer tripId,
            @Valid @RequestBody TripFinishRequest request) {

        logger.info("Спроба завершити поїздку {} з типом вводу: {}", tripId, request.getInputType());

        if (!request.isValid()) {
            logger.warn("Некоректні дані для завершення поїздки {}: {}", tripId, request);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Некоректні дані. Вкажіть або відстань, або показники одометра."));
        }

        if (request.getInputType() == TripFinishRequest.DistanceInputType.MANUAL_DISTANCE) {
            if (request.getActualDistance().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Пробіг повинен бути більше 0"));
            }
            if (request.getActualDistance().compareTo(BigDecimal.valueOf(2000)) > 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Пробіг не може бути більше 2000 км"));
            }
        }

        if (request.getInputType() == TripFinishRequest.DistanceInputType.ODOMETER_READING) {
            if (request.getEndOdometer() == null || request.getEndOdometer() <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Показники одометра повинні бути більше 0"));
            }
        }

        try {
            boolean success = fuelService.finishTrip(tripId, request);

            if (success) {
                String message = request.getInputType() == TripFinishRequest.DistanceInputType.ODOMETER_READING ?
                        "Поїздку завершено. Пробіг розраховано за одометром." :
                        "Поїздку завершено із зазначеним пробігом.";

                logger.info("Поїздку {} успішно завершено", tripId);
                return ResponseEntity.ok(ApiResponse.success(message));
            }

            logger.warn("Не вдалося завершити поїздку {}", tripId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Неможливо завершити поїздку. Перевірте статус і дані."));

        } catch (Exception e) {
            logger.error("Помилка при завершенні поїздки {}: {}", tripId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Внутрішня помилка при завершенні поїздки: " + e.getMessage()));
        }
    }

    @PostMapping("/trips/{tripId}/finish-manual")
    public ResponseEntity<ApiResponse<String>> finishTripManual(
            @PathVariable @NotNull @Min(1) Integer tripId,
            @RequestParam @NotNull BigDecimal actualDistance) {

        logger.info("Завершення поїздки {} ручним введенням: {} км", tripId, actualDistance);

        TripFinishRequest request = new TripFinishRequest(actualDistance);
        return finishTrip(tripId, request);
    }

    @GetMapping("/trips/info/{tripId}")
    public ResponseEntity<ApiResponse<Trip>> getTripInfo(
            @PathVariable @NotNull @Min(1) Integer tripId) {

        logger.info("Запит інформації про поїздку {}", tripId);

        try {
            Optional<Trip> trip = fuelService.getTripById(tripId);

            if (trip.isPresent()) {
                logger.info("Інформацію про поїздку {} знайдено", tripId);
                return ResponseEntity.ok(ApiResponse.success(trip.get()));
            }

            logger.warn("Поїздку {} не знайдено", tripId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Помилка при отриманні інформації про поїздку {}: {}", tripId, e.getMessage(), e);

            if (e.getMessage().contains("No enum constant") && e.getMessage().contains("Season")) {
                logger.error("Виявлено проблему з enum Season в БД для поїздки {}", tripId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Помилка даних: некоректне значення сезону в базі даних"));
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Помилка при отриманні інформації про поїздку"));
        }
    }

    @GetMapping("/trips/{tripId}/odometer")
    public ResponseEntity<ApiResponse<Integer>> getCurrentOdometer(
            @PathVariable @NotNull @Min(1) Integer tripId) {

        logger.info("Запит поточного пробігу для поїздки {}", tripId);

        try {
            Optional<Integer> odometer = fuelService.getCurrentOdometerForTrip(tripId);

            if (odometer.isPresent()) {
                logger.info("Поточний пробіг для поїздки {}: {} км", tripId, odometer.get());
                return ResponseEntity.ok(ApiResponse.success("Поточний пробіг отримано", odometer.get()));
            }

            logger.warn("Не вдалося отримати пробіг для поїздки {}", tripId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Помилка при отриманні пробігу для поїздки {}: {}", tripId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Помилка при отриманні пробігу"));
        }
    }

    @PutMapping("/trips/{tripId}/odometer/start")
    public ResponseEntity<ApiResponse<String>> updateTripStartOdometer(
            @PathVariable @NotNull @Min(1) Integer tripId,
            @RequestParam @NotNull @Min(1) Integer startOdometer) {

        logger.info("Запит на оновлення початкового пробігу поїздки {}: {} км", tripId, startOdometer);

        try {
            boolean success = fuelService.updateTripStartOdometer(tripId, startOdometer);

            if (success) {
                logger.info("Початковий пробіг поїздки {} успішно оновлено: {} км", tripId, startOdometer);
                return ResponseEntity.ok(ApiResponse.success(
                        String.format("Початковий пробіг поїздки оновлено: %d км", startOdometer)));
            }

            logger.warn("Не вдалося оновити початковий пробіг поїздки {}", tripId);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Не вдалося оновити початковий пробіг. Перевірте статус поїздки та значення пробігу."));

        } catch (Exception e) {
            logger.error("Помилка при оновленні початкового пробігу поїздки {}: {}", tripId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Внутрішня помилка при оновленні пробігу: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        logger.debug("Health check запит");
        return ResponseEntity.ok(ApiResponse.success("API працює нормально", "OK"));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationErrors(
            jakarta.validation.ConstraintViolationException e) {

        logger.warn("Помилка валідації: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Помилка валідації даних: " + e.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiResponse<String>> handleDataAccessErrors(
            org.springframework.dao.InvalidDataAccessApiUsageException e) {

        logger.error("Помилка доступу до даних: {}", e.getMessage());

        if (e.getMessage().contains("No enum constant") && e.getMessage().contains("Season")) {
            logger.error("Виявлено проблему з enum Season в БД");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Помилка даних: некоректне значення сезону в базі даних. Зверніться до адміністратора."));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Помилка при роботі з даними"));
    }
}
