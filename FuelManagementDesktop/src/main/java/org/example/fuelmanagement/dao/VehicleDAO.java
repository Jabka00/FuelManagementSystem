package org.example.fuelmanagement.dao;

import org.example.fuelmanagement.config.DatabaseConfig;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.enums.VehicleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {
    private static final Logger logger = LoggerFactory.getLogger(VehicleDAO.class);

    public List<Vehicle> findAllActive() {
        List<Vehicle> vehicles = new ArrayList<>();
        String sql = """
            SELECT v.id, v.license_plate, v.model, v.fuel_type_id, ft.name as fuel_type_name,
                   v.city_rate_summer, v.highway_rate_summer, 
                   v.city_rate_winter, v.highway_rate_winter,
                   v.idle_rate_summer, v.idle_rate_winter,
                   v.current_odometer, v.fuel_balance, v.tank_capacity, 
                   v.has_refrigerator, v.status, v.notes, v.created_at, v.updated_at
            FROM vehicles v
            LEFT JOIN fuel_types ft ON v.fuel_type_id = ft.id
            WHERE v.status = 'active'
            ORDER BY v.model, v.license_plate
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Vehicle vehicle = mapResultSetToVehicle(rs);
                vehicles.add(vehicle);
            }

            logger.info("Завантажено {} активних автомобілів", vehicles.size());

        } catch (SQLException e) {
            logger.error("Помилка отримання списку автомобілів: ", e);
        }

        return vehicles;
    }

    public Vehicle findById(int id) {
        String sql = """
            SELECT v.id, v.license_plate, v.model, v.fuel_type_id, ft.name as fuel_type_name,
                   v.city_rate_summer, v.highway_rate_summer, 
                   v.city_rate_winter, v.highway_rate_winter,
                   v.idle_rate_summer, v.idle_rate_winter,
                   v.current_odometer, v.fuel_balance, v.tank_capacity, 
                   v.has_refrigerator, v.status, v.notes, v.created_at, v.updated_at
            FROM vehicles v
            LEFT JOIN fuel_types ft ON v.fuel_type_id = ft.id
            WHERE v.id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVehicle(rs);
                }
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання автомобіля за ID {}: ", id, e);
        }

        return null;
    }

    public boolean createTestVehicleIfNotExists() {
        String testLicensePlate = "TEST-REFR-1.7";
        String checkSql = "SELECT id FROM vehicles WHERE license_plate = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, testLicensePlate);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    logger.info("Тестова машина {} вже існує", testLicensePlate);
                    return true;
                }
            }
            int fuelTypeId = 1; 
            String fuelTypeSql = "SELECT id FROM fuel_types ORDER BY id LIMIT 1";
            try (PreparedStatement fuelTypeStmt = conn.prepareStatement(fuelTypeSql);
                 ResultSet fuelTypeRs = fuelTypeStmt.executeQuery()) {
                if (fuelTypeRs.next()) {
                    fuelTypeId = fuelTypeRs.getInt("id");
                }
            } catch (SQLException e) {
                logger.warn("Не вдалося знайти тип палива, використовуємо ID=1: ", e);
            }
            Vehicle testVehicle = new Vehicle();
            testVehicle.setLicensePlate(testLicensePlate);
            testVehicle.setModel("Тестова машина з холодильником");
            testVehicle.setFuelTypeId(fuelTypeId);
            testVehicle.setCityRateSummer(new BigDecimal("0.15")); 
            testVehicle.setHighwayRateSummer(new BigDecimal("0.12")); 
            testVehicle.setCityRateWinter(new BigDecimal("0.17")); 
            testVehicle.setHighwayRateWinter(new BigDecimal("0.14")); 
            testVehicle.setIdleRateSummer(new BigDecimal("2.0")); 
            testVehicle.setIdleRateWinter(new BigDecimal("2.5")); 
            testVehicle.setCurrentOdometer(0);
            testVehicle.setFuelBalance(BigDecimal.ZERO);
            testVehicle.setTankCapacity(new BigDecimal("100")); 
            testVehicle.setHasRefrigerator(true);
            testVehicle.setStatus(VehicleStatus.ACTIVE);
            testVehicle.setNotes("Тестова машина з фіксованим розходом холодильника 1.7 л замість 15%");
            Vehicle created = create(testVehicle);
            if (created != null) {
                logger.info(" Створено тестову машину {} з фіксованим розходом холодильника 1.7 л", testLicensePlate);
                return true;
            } else {
                logger.error(" Не вдалося створити тестову машину {}", testLicensePlate);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Помилка створення тестової машини {}: ", testLicensePlate, e);
            return false;
        }
    }

    public boolean updateFuelBalance(int vehicleId, BigDecimal newBalance) {
        String sql = "UPDATE vehicles SET fuel_balance = ?, updated_at = NOW() WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, newBalance);
            stmt.setInt(2, vehicleId);

            int rowsUpdated = stmt.executeUpdate();
            logger.info("Оновлено баланс палива для автомобіля ID {}: {} л", vehicleId, newBalance);

            return rowsUpdated > 0;

        } catch (SQLException e) {
            logger.error("Помилка оновлення балансу палива для автомобіля ID {}: ", vehicleId, e);
            return false;
        }
    }

    public boolean updateOdometer(int vehicleId, int newOdometer) {
        String sql = "UPDATE vehicles SET current_odometer = ?, updated_at = NOW() WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newOdometer);
            stmt.setInt(2, vehicleId);

            int rowsUpdated = stmt.executeUpdate();
            logger.info("Оновлено одометр для автомобіля ID {}: {} км", vehicleId, newOdometer);

            return rowsUpdated > 0;

        } catch (SQLException e) {
            logger.error("Помилка оновлення одометра для автомобіля ID {}: ", vehicleId, e);
            return false;
        }
    }

    public Vehicle create(Vehicle vehicle) {
        String sql = """
            INSERT INTO vehicles (license_plate, model, fuel_type_id, 
                                city_rate_summer, highway_rate_summer,
                                city_rate_winter, highway_rate_winter,
                                idle_rate_summer, idle_rate_winter,
                                current_odometer, fuel_balance, tank_capacity,
                                has_refrigerator, status, notes, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, vehicle.getLicensePlate());
            stmt.setString(2, vehicle.getModel());
            stmt.setInt(3, vehicle.getFuelTypeId());
            logger.info("Збереження норм витрат - Місто (літо): {}, Траса (літо): {}, Місто (зима): {}, Траса (зима): {}", 
                vehicle.getCityRateSummer(), vehicle.getHighwayRateSummer(), 
                vehicle.getCityRateWinter(), vehicle.getHighwayRateWinter());
            stmt.setBigDecimal(4, vehicle.getCityRateSummer());
            stmt.setBigDecimal(5, vehicle.getHighwayRateSummer());
            stmt.setBigDecimal(6, vehicle.getCityRateWinter());
            stmt.setBigDecimal(7, vehicle.getHighwayRateWinter());
            stmt.setBigDecimal(8, vehicle.getIdleRateSummer() != null ? vehicle.getIdleRateSummer() : BigDecimal.ZERO);
            stmt.setBigDecimal(9, vehicle.getIdleRateWinter() != null ? vehicle.getIdleRateWinter() : BigDecimal.ZERO);
            stmt.setInt(10, vehicle.getCurrentOdometer());
            stmt.setBigDecimal(11, vehicle.getFuelBalance());
            stmt.setBigDecimal(12, vehicle.getTankCapacity());
            stmt.setBoolean(13, vehicle.hasRefrigerator());
            stmt.setString(14, vehicle.getStatus().getValue());
            stmt.setString(15, vehicle.getNotes());

            int rowsInserted = stmt.executeUpdate();

            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        vehicle.setId(generatedKeys.getInt(1));
                        logger.info("Створено новий автомобіль: {} (ID: {})", vehicle.getDisplayName(), vehicle.getId());
                        return vehicle;
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Помилка створення автомобіля: ", e);
        }

        return null;
    }

    public boolean updateFuelRates(int vehicleId,
                                   BigDecimal cityRateSummer, BigDecimal highwayRateSummer,
                                   BigDecimal cityRateWinter, BigDecimal highwayRateWinter) {
        String sql = """
            UPDATE vehicles 
            SET city_rate_summer = ?, highway_rate_summer = ?,
                city_rate_winter = ?, highway_rate_winter = ?,
                updated_at = NOW()
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, cityRateSummer);
            stmt.setBigDecimal(2, highwayRateSummer);
            stmt.setBigDecimal(3, cityRateWinter);
            stmt.setBigDecimal(4, highwayRateWinter);
            stmt.setInt(5, vehicleId);

            int rowsUpdated = stmt.executeUpdate();
            logger.info("Оновлено норми витрат для автомобіля ID {}", vehicleId);

            return rowsUpdated > 0;

        } catch (SQLException e) {
            logger.error("Помилка оновлення норм витрат для автомобіля ID {}: ", vehicleId, e);
            return false;
        }
    }

    public boolean delete(int vehicleId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String deleteTripsSql = "DELETE FROM trips WHERE vehicle_id = ?";
                try (PreparedStatement deleteTripsStmt = conn.prepareStatement(deleteTripsSql)) {
                    deleteTripsStmt.setInt(1, vehicleId);
                    int tripsDeleted = deleteTripsStmt.executeUpdate();
                    logger.info("Видалено {} поїздок для автомобіля ID {}", tripsDeleted, vehicleId);
                }
                String deleteVehicleSql = "DELETE FROM vehicles WHERE id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteVehicleSql)) {
                    deleteStmt.setInt(1, vehicleId);
                    int rowsDeleted = deleteStmt.executeUpdate();
                    if (rowsDeleted > 0) {
                        conn.commit();
                        logger.info("Видалено автомобіль ID {} з бази даних", vehicleId);
                        return true;
                    } else {
                        conn.rollback();
                        logger.warn("Автомобіль ID {} не знайдено для видалення", vehicleId);
                        return false;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Помилка видалення автомобіля ID {}: ", vehicleId, e);
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            logger.error("Помилка підключення до БД при видаленні автомобіля ID {}: ", vehicleId, e);
            return false;
        }
    }

    private Vehicle mapResultSetToVehicle(ResultSet rs) throws SQLException {
        Vehicle vehicle = new Vehicle();

        vehicle.setId(rs.getInt("id"));
        vehicle.setLicensePlate(rs.getString("license_plate"));
        vehicle.setModel(rs.getString("model"));
        vehicle.setFuelTypeId(rs.getInt("fuel_type_id"));
        vehicle.setFuelTypeName(rs.getString("fuel_type_name"));

        BigDecimal cityRateSummer = rs.getBigDecimal("city_rate_summer");
        BigDecimal highwayRateSummer = rs.getBigDecimal("highway_rate_summer");
        BigDecimal cityRateWinter = rs.getBigDecimal("city_rate_winter");
        BigDecimal highwayRateWinter = rs.getBigDecimal("highway_rate_winter");
        BigDecimal idleRateSummer = rs.getBigDecimal("idle_rate_summer");
        BigDecimal idleRateWinter = rs.getBigDecimal("idle_rate_winter");
        logger.debug("Читання норм витрат з БД - Місто (літо): {}, Траса (літо): {}, Місто (зима): {}, Траса (зима): {}", 
            cityRateSummer, highwayRateSummer, cityRateWinter, highwayRateWinter);
        vehicle.setCityRateSummer(cityRateSummer);
        vehicle.setHighwayRateSummer(highwayRateSummer);
        vehicle.setCityRateWinter(cityRateWinter);
        vehicle.setHighwayRateWinter(highwayRateWinter);
        vehicle.setIdleRateSummer(idleRateSummer);
        vehicle.setIdleRateWinter(idleRateWinter);

        vehicle.setCurrentOdometer(rs.getInt("current_odometer"));
        vehicle.setFuelBalance(rs.getBigDecimal("fuel_balance"));
        vehicle.setTankCapacity(rs.getBigDecimal("tank_capacity"));
        vehicle.setHasRefrigerator(rs.getBoolean("has_refrigerator"));

        vehicle.setStatus(VehicleStatus.fromString(rs.getString("status")));
        vehicle.setNotes(rs.getString("notes"));
        vehicle.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            vehicle.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return vehicle;
    }

    public VehicleStatistics getVehicleStatistics(int vehicleId) {
        String sql = """
            SELECT 
                v.id,
                v.license_plate,
                v.model,
                v.fuel_balance,
                v.tank_capacity,
                v.current_odometer,
                COUNT(t.id) as total_trips,
                COUNT(CASE WHEN t.status = 'completed' THEN 1 END) as completed_trips,
                COALESCE(SUM(t.planned_fuel_consumption), 0) as total_planned_fuel,
                COALESCE(SUM(t.actual_fuel_consumption), 0) as total_actual_fuel,
                COALESCE(SUM(t.planned_distance), 0) as total_planned_distance,
                COALESCE(SUM(t.actual_distance), 0) as total_actual_distance,
                MAX(t.created_at) as last_trip_date
            FROM vehicles v
            LEFT JOIN trips t ON v.id = t.vehicle_id
            WHERE v.id = ?
            GROUP BY v.id
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, vehicleId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    VehicleStatistics stats = new VehicleStatistics();
                    stats.setVehicleId(rs.getInt("id"));
                    stats.setLicensePlate(rs.getString("license_plate"));
                    stats.setModel(rs.getString("model"));
                    stats.setFuelBalance(rs.getBigDecimal("fuel_balance"));
                    stats.setTankCapacity(rs.getBigDecimal("tank_capacity"));
                    stats.setCurrentOdometer(rs.getInt("current_odometer"));
                    stats.setTotalTrips(rs.getInt("total_trips"));
                    stats.setCompletedTrips(rs.getInt("completed_trips"));
                    stats.setTotalPlannedFuel(rs.getBigDecimal("total_planned_fuel"));
                    stats.setTotalActualFuel(rs.getBigDecimal("total_actual_fuel"));
                    stats.setTotalPlannedDistance(rs.getBigDecimal("total_planned_distance"));
                    stats.setTotalActualDistance(rs.getBigDecimal("total_actual_distance"));

                    Timestamp lastTripDate = rs.getTimestamp("last_trip_date");
                    if (lastTripDate != null) {
                        stats.setLastTripDate(lastTripDate.toLocalDateTime());
                    }

                    return stats;
                }
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання статистики автомобіля ID {}: ", vehicleId, e);
        }

        return null;
    }

    public List<VehicleWithStats> findAllWithStatistics() {
        String sql = """
            SELECT 
                v.id, v.license_plate, v.model, v.fuel_type_id, ft.name as fuel_type_name,
                v.city_rate_summer, v.highway_rate_summer, 
                v.city_rate_winter, v.highway_rate_winter,
                v.idle_rate_summer, v.idle_rate_winter,
                v.current_odometer, v.fuel_balance, v.tank_capacity, 
                v.has_refrigerator, v.status, v.notes, v.created_at, v.updated_at,
                COUNT(t.id) as trip_count,
                COALESCE(SUM(CASE WHEN t.status = 'completed' THEN t.actual_fuel_consumption 
                                  ELSE t.planned_fuel_consumption END), 0) as total_fuel_used,
                COALESCE(SUM(CASE WHEN t.status = 'completed' THEN t.actual_distance 
                                  ELSE t.planned_distance END), 0) as total_distance,
                MAX(t.created_at) as last_trip_date
            FROM vehicles v
            LEFT JOIN fuel_types ft ON v.fuel_type_id = ft.id
            LEFT JOIN trips t ON v.id = t.vehicle_id
            WHERE v.status IN ('active', 'maintenance')
            GROUP BY v.id
            ORDER BY v.model, v.license_plate
            """;

        List<VehicleWithStats> vehicles = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                VehicleWithStats vehicleWithStats = new VehicleWithStats();

                Vehicle vehicle = mapResultSetToVehicle(rs);
                vehicleWithStats.setVehicle(vehicle);

                vehicleWithStats.setTripCount(rs.getInt("trip_count"));
                vehicleWithStats.setTotalFuelUsed(rs.getBigDecimal("total_fuel_used"));
                vehicleWithStats.setTotalDistance(rs.getBigDecimal("total_distance"));

                Timestamp lastTripDate = rs.getTimestamp("last_trip_date");
                if (lastTripDate != null) {
                    vehicleWithStats.setLastTripDate(lastTripDate.toLocalDateTime());
                }

                vehicles.add(vehicleWithStats);
            }

            logger.info("Завантажено {} автомобілів з статистикою", vehicles.size());

        } catch (SQLException e) {
            logger.error("Помилка отримання автомобілів з статистикою: ", e);
        }

        return vehicles;
    }

    public List<VehicleFuelEfficiency> getTopFuelEfficient(int limit) {
        String sql = """
            SELECT 
                v.id, v.license_plate, v.model,
                COALESCE(SUM(CASE WHEN t.status = 'completed' THEN t.actual_fuel_consumption 
                                  ELSE t.planned_fuel_consumption END), 0) as total_fuel,
                COALESCE(SUM(CASE WHEN t.status = 'completed' THEN t.actual_distance 
                                  ELSE t.planned_distance END), 0) as total_distance,
                COUNT(t.id) as trip_count
            FROM vehicles v
            LEFT JOIN trips t ON v.id = t.vehicle_id
            WHERE v.status = 'active' AND t.id IS NOT NULL
            GROUP BY v.id
            HAVING total_distance > 0
            ORDER BY (total_fuel / total_distance) ASC
            LIMIT ?
            """;

        List<VehicleFuelEfficiency> efficiencyList = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    VehicleFuelEfficiency efficiency = new VehicleFuelEfficiency();
                    efficiency.setVehicleId(rs.getInt("id"));
                    efficiency.setLicensePlate(rs.getString("license_plate"));
                    efficiency.setModel(rs.getString("model"));
                    efficiency.setTotalFuel(rs.getBigDecimal("total_fuel"));
                    efficiency.setTotalDistance(rs.getBigDecimal("total_distance"));
                    efficiency.setTripCount(rs.getInt("trip_count"));

                    BigDecimal efficiency100km = efficiency.getTotalFuel()
                            .divide(efficiency.getTotalDistance(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    efficiency.setConsumptionPer100km(efficiency100km);

                    efficiencyList.add(efficiency);
                }
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання рейтингу ефективності: ", e);
        }

        return efficiencyList;
    }

    public static class VehicleStatistics {
        private int vehicleId;
        private String licensePlate;
        private String model;
        private BigDecimal fuelBalance;
        private BigDecimal tankCapacity;
        private int currentOdometer;
        private int totalTrips;
        private int completedTrips;
        private BigDecimal totalPlannedFuel = BigDecimal.ZERO;
        private BigDecimal totalActualFuel = BigDecimal.ZERO;
        private BigDecimal totalPlannedDistance = BigDecimal.ZERO;
        private BigDecimal totalActualDistance = BigDecimal.ZERO;
        private LocalDateTime lastTripDate;

        public int getVehicleId() { return vehicleId; }
        public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

        public String getLicensePlate() { return licensePlate; }
        public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public BigDecimal getFuelBalance() { return fuelBalance; }
        public void setFuelBalance(BigDecimal fuelBalance) { this.fuelBalance = fuelBalance; }

        public BigDecimal getTankCapacity() { return tankCapacity; }
        public void setTankCapacity(BigDecimal tankCapacity) { this.tankCapacity = tankCapacity; }

        public int getCurrentOdometer() { return currentOdometer; }
        public void setCurrentOdometer(int currentOdometer) { this.currentOdometer = currentOdometer; }

        public int getTotalTrips() { return totalTrips; }
        public void setTotalTrips(int totalTrips) { this.totalTrips = totalTrips; }

        public int getCompletedTrips() { return completedTrips; }
        public void setCompletedTrips(int completedTrips) { this.completedTrips = completedTrips; }

        public BigDecimal getTotalPlannedFuel() { return totalPlannedFuel; }
        public void setTotalPlannedFuel(BigDecimal totalPlannedFuel) {
            this.totalPlannedFuel = totalPlannedFuel != null ? totalPlannedFuel : BigDecimal.ZERO;
        }

        public BigDecimal getTotalActualFuel() { return totalActualFuel; }
        public void setTotalActualFuel(BigDecimal totalActualFuel) {
            this.totalActualFuel = totalActualFuel != null ? totalActualFuel : BigDecimal.ZERO;
        }

        public BigDecimal getTotalPlannedDistance() { return totalPlannedDistance; }
        public void setTotalPlannedDistance(BigDecimal totalPlannedDistance) {
            this.totalPlannedDistance = totalPlannedDistance != null ? totalPlannedDistance : BigDecimal.ZERO;
        }

        public BigDecimal getTotalActualDistance() { return totalActualDistance; }
        public void setTotalActualDistance(BigDecimal totalActualDistance) {
            this.totalActualDistance = totalActualDistance != null ? totalActualDistance : BigDecimal.ZERO;
        }

        public LocalDateTime getLastTripDate() { return lastTripDate; }
        public void setLastTripDate(LocalDateTime lastTripDate) { this.lastTripDate = lastTripDate; }

        public BigDecimal getEffectiveFuel() {
            return totalActualFuel.compareTo(BigDecimal.ZERO) > 0 ? totalActualFuel : totalPlannedFuel;
        }

        public BigDecimal getEffectiveDistance() {
            return totalActualDistance.compareTo(BigDecimal.ZERO) > 0 ? totalActualDistance : totalPlannedDistance;
        }

        public BigDecimal getAverageConsumption() {
            BigDecimal distance = getEffectiveDistance();
            if (distance.compareTo(BigDecimal.ZERO) > 0) {
                return getEffectiveFuel().divide(distance, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            return BigDecimal.ZERO;
        }
    }

    public static class VehicleWithStats {
        private Vehicle vehicle;
        private int tripCount;
        private BigDecimal totalFuelUsed = BigDecimal.ZERO;
        private BigDecimal totalDistance = BigDecimal.ZERO;
        private LocalDateTime lastTripDate;

        public Vehicle getVehicle() { return vehicle; }
        public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

        public int getTripCount() { return tripCount; }
        public void setTripCount(int tripCount) { this.tripCount = tripCount; }

        public BigDecimal getTotalFuelUsed() { return totalFuelUsed; }
        public void setTotalFuelUsed(BigDecimal totalFuelUsed) {
            this.totalFuelUsed = totalFuelUsed != null ? totalFuelUsed : BigDecimal.ZERO;
        }

        public BigDecimal getTotalDistance() { return totalDistance; }
        public void setTotalDistance(BigDecimal totalDistance) {
            this.totalDistance = totalDistance != null ? totalDistance : BigDecimal.ZERO;
        }

        public LocalDateTime getLastTripDate() { return lastTripDate; }
        public void setLastTripDate(LocalDateTime lastTripDate) { this.lastTripDate = lastTripDate; }

        public BigDecimal getAverageConsumption() {
            if (totalDistance.compareTo(BigDecimal.ZERO) > 0) {
                return totalFuelUsed.divide(totalDistance, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            return BigDecimal.ZERO;
        }
    }

    public static class VehicleFuelEfficiency {
        private int vehicleId;
        private String licensePlate;
        private String model;
        private BigDecimal totalFuel = BigDecimal.ZERO;
        private BigDecimal totalDistance = BigDecimal.ZERO;
        private BigDecimal consumptionPer100km = BigDecimal.ZERO;
        private int tripCount;

        public int getVehicleId() { return vehicleId; }
        public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }

        public String getLicensePlate() { return licensePlate; }
        public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public BigDecimal getTotalFuel() { return totalFuel; }
        public void setTotalFuel(BigDecimal totalFuel) {
            this.totalFuel = totalFuel != null ? totalFuel : BigDecimal.ZERO;
        }

        public BigDecimal getTotalDistance() { return totalDistance; }
        public void setTotalDistance(BigDecimal totalDistance) {
            this.totalDistance = totalDistance != null ? totalDistance : BigDecimal.ZERO;
        }

        public BigDecimal getConsumptionPer100km() { return consumptionPer100km; }
        public void setConsumptionPer100km(BigDecimal consumptionPer100km) {
            this.consumptionPer100km = consumptionPer100km != null ? consumptionPer100km : BigDecimal.ZERO;
        }

        public int getTripCount() { return tripCount; }
        public void setTripCount(int tripCount) { this.tripCount = tripCount; }

        public String getDisplayName() {
            return licensePlate + (model != null ? " (" + model + ")" : "");
        }

        public String getEfficiencyDescription() {
            return String.format("%.2f л/100км (%d поїздок, %.1f км)",
                    consumptionPer100km.doubleValue(),
                    tripCount,
                    totalDistance.doubleValue());
        }
    }
}