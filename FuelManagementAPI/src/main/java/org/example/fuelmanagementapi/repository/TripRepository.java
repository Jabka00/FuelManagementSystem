package org.example.fuelmanagementapi.repository;

import org.example.fuelmanagementapi.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Integer> {

    @Query(value = """
        SELECT t.id, t.trip_number, t.start_address, t.end_address, 
               t.status, t.planned_distance, t.actual_distance,
               t.planned_fuel_consumption, t.actual_fuel_consumption,
               t.planned_start_time, t.created_at, t.actual_start_time, 
               t.actual_end_time, v.license_plate, v.model, d.full_name,
               t.refrigerator_usage_percent, v.has_refrigerator,
               t.actual_start_odometer, t.actual_end_odometer, t.vehicle_id, t.driver_id
        FROM trips t
        LEFT JOIN vehicles v ON t.vehicle_id = v.id
        LEFT JOIN drivers d ON t.driver_id = d.id
        WHERE t.status IN ('assigned', 'started')
          AND t.actual_end_time IS NULL
        ORDER BY t.created_at DESC
        LIMIT 50
        """, nativeQuery = true)
    List<Object[]> findAllActiveTrips();

    @Query(value = """
        SELECT t.id, t.trip_number, t.start_address, t.end_address, 
               t.status, t.planned_distance, t.actual_distance,
               t.planned_fuel_consumption, t.actual_fuel_consumption,
               t.planned_start_time, t.created_at, t.actual_start_time, 
               t.actual_end_time, v.license_plate, v.model, d.full_name,
               t.refrigerator_usage_percent, v.has_refrigerator,
               t.actual_start_odometer, t.actual_end_odometer, t.vehicle_id, t.driver_id
        FROM trips t
        LEFT JOIN vehicles v ON t.vehicle_id = v.id
        LEFT JOIN drivers d ON t.driver_id = d.id
        WHERE t.driver_id = :driverId
          AND t.status IN ('assigned', 'started')
          AND t.actual_end_time IS NULL
        ORDER BY t.created_at DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findActiveTripsForDriver(@Param("driverId") Integer driverId);

    @Modifying
    @Transactional
    @Query("UPDATE Trip t SET t.actualStartTime = :startTime, t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.started, t.updatedAt = :updateTime WHERE t.id = :tripId AND t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.assigned")
    int startTrip(@Param("tripId") Integer tripId,
                  @Param("startTime") LocalDateTime startTime,
                  @Param("updateTime") LocalDateTime updateTime);

    @Modifying
    @Transactional
    @Query("UPDATE Trip t SET t.actualStartTime = :startTime, t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.started, t.updatedAt = :updateTime, t.actualStartOdometer = :startOdometer WHERE t.id = :tripId AND t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.assigned")
    int startTripWithOdometer(@Param("tripId") Integer tripId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("updateTime") LocalDateTime updateTime,
                              @Param("startOdometer") Integer startOdometer);

    @Modifying
    @Transactional
    @Query("UPDATE Trip t SET t.actualDistance = :actualDistance, t.actualEndTime = :endTime, t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.completed, t.updatedAt = :updateTime WHERE t.id = :tripId AND t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.started")
    int finishTrip(@Param("tripId") Integer tripId,
                   @Param("actualDistance") BigDecimal actualDistance,
                   @Param("endTime") LocalDateTime endTime,
                   @Param("updateTime") LocalDateTime updateTime);

    @Modifying
    @Transactional
    @Query("UPDATE Trip t SET t.actualDistance = :actualDistance, t.actualEndTime = :endTime, t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.completed, t.updatedAt = :updateTime, t.actualEndOdometer = :endOdometer, t.notes = :notes WHERE t.id = :tripId AND t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.started")
    int finishTripWithOdometer(@Param("tripId") Integer tripId,
                               @Param("actualDistance") BigDecimal actualDistance,
                               @Param("endTime") LocalDateTime endTime,
                               @Param("updateTime") LocalDateTime updateTime,
                               @Param("endOdometer") Integer endOdometer,
                               @Param("notes") String notes);

    @Modifying
    @Transactional
    @Query("UPDATE Trip t SET t.actualStartOdometer = :startOdometer, t.updatedAt = :updateTime WHERE t.id = :tripId AND t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.started")
    int updateStartOdometer(@Param("tripId") Integer tripId,
                            @Param("startOdometer") Integer startOdometer,
                            @Param("updateTime") LocalDateTime updateTime);

    List<Trip> findByStatusOrderByCreatedAtDesc(Trip.TripStatus status);

    @Query("SELECT t FROM Trip t WHERE t.driverId = :driverId AND t.actualStartTime BETWEEN :startDate AND :endDate ORDER BY t.actualStartTime DESC")
    List<Trip> findDriverTripsInPeriod(@Param("driverId") Integer driverId,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.actualDistance) FROM Trip t WHERE t.driverId = :driverId AND t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.completed")
    BigDecimal getTotalDistanceByDriver(@Param("driverId") Integer driverId);

    @Query("SELECT SUM(t.actualDistance) FROM Trip t WHERE t.vehicleId = :vehicleId AND t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.completed")
    BigDecimal getTotalDistanceByVehicle(@Param("vehicleId") Integer vehicleId);

    @Query("SELECT t FROM Trip t WHERE t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.started AND t.actualStartTime < :cutoffTime")
    List<Trip> findStuckTrips(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT t FROM Trip t WHERE t.status = org.example.fuelmanagementapi.entity.Trip$TripStatus.completed AND t.plannedDistance IS NOT NULL AND ABS(t.actualDistance - t.plannedDistance) > (t.plannedDistance * 0.1)")
    List<Trip> findTripsWithMileageDiscrepancies();
}