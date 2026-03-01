package org.example.fuelmanagementapi.repository;

import org.example.fuelmanagementapi.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Integer> {

    @Query("SELECT d FROM Driver d WHERE d.status = org.example.fuelmanagementapi.entity.Driver$DriverStatus.active ORDER BY d.fullName")
    List<Driver> findActiveDrivers();

    List<Driver> findByStatusOrderByFullName(Driver.DriverStatus status);

    List<Driver> findByStatus(Driver.DriverStatus status);

    Driver findByPhone(String phone);
}