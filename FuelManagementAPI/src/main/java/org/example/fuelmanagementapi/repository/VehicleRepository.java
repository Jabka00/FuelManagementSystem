package org.example.fuelmanagementapi.repository;

import org.example.fuelmanagementapi.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    @Query("SELECT v FROM Vehicle v WHERE v.status = org.example.fuelmanagementapi.entity.Vehicle$VehicleStatus.active ORDER BY v.licensePlate")
    List<Vehicle> findActiveVehicles();

    List<Vehicle> findByStatusOrderByLicensePlate(Vehicle.VehicleStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Vehicle v SET v.currentOdometer = :odometer, v.updatedAt = CURRENT_TIMESTAMP WHERE v.id = :vehicleId")
    int updateCurrentOdometer(@Param("vehicleId") Integer vehicleId, @Param("odometer") Integer odometer);

    @Modifying
    @Transactional
    @Query("UPDATE Vehicle v SET v.fuelBalance = v.fuelBalance - :fuelConsumed, v.updatedAt = CURRENT_TIMESTAMP WHERE v.id = :vehicleId AND v.fuelBalance >= :fuelConsumed")
    int reduceFuelBalance(@Param("vehicleId") Integer vehicleId, @Param("fuelConsumed") java.math.BigDecimal fuelConsumed);
 
    @Query("SELECT AVG(v.currentOdometer) FROM Vehicle v WHERE v.status = org.example.fuelmanagementapi.entity.Vehicle$VehicleStatus.active")
    Double getAverageOdometer();

    @Query("SELECT MAX(v.currentOdometer) FROM Vehicle v WHERE v.status = org.example.fuelmanagementapi.entity.Vehicle$VehicleStatus.active")
    Integer getMaxOdometer();

    @Query("SELECT MIN(v.currentOdometer) FROM Vehicle v WHERE v.status = org.example.fuelmanagementapi.entity.Vehicle$VehicleStatus.active")
    Integer getMinOdometer();
}