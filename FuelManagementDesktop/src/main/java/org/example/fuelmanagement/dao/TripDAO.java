package org.example.fuelmanagement.dao;

import org.example.fuelmanagement.config.DatabaseConfig;
import org.example.fuelmanagement.model.Trip;
import org.example.fuelmanagement.model.Waypoint;
import org.example.fuelmanagement.model.enums.Season;
import org.example.fuelmanagement.model.enums.TripStatus;
import org.example.fuelmanagement.model.enums.TripType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TripDAO {
    private static final Logger logger = LoggerFactory.getLogger(TripDAO.class);

    public Trip create(Trip trip) {
        Connection conn = null;
        PreparedStatement stmtRequester = null;
        PreparedStatement stmtTrip = null;

        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            int requesterId = createOrFindRequester(conn, trip);

            String tripNumber = generateTripNumber();

            String sqlTrip = """
                INSERT INTO trips (
                    trip_number, vehicle_id, driver_id, requester_id,
                    requester_name, requester_email, requester_phone,
                    start_address, end_address, trip_type,
                    planned_start_time, planned_end_time, waiting_time,
                    planned_distance, planned_city_km, planned_highway_km, planned_fuel_consumption,
                    purpose, power_of_attorney, can_driver_deliver,
                    status, season, refrigerator_usage_percent, notes, created_at, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)
                """;

            stmtTrip = conn.prepareStatement(sqlTrip, Statement.RETURN_GENERATED_KEYS);

            stmtTrip.setString(1, tripNumber);
            stmtTrip.setInt(2, trip.getVehicleId());

            if (trip.getDriverId() != null) {
                stmtTrip.setInt(3, trip.getDriverId());
            } else {
                stmtTrip.setNull(3, Types.INTEGER);
            }

            stmtTrip.setInt(4, requesterId);
            stmtTrip.setString(5, truncateString(trip.getRequesterName(), 255));
            stmtTrip.setString(6, truncateString(trip.getRequesterEmail(), 255));
            stmtTrip.setString(7, truncateString(trip.getRequesterPhone(), 50));
            stmtTrip.setString(8, truncateString(trip.getStartAddress(), 500));
            stmtTrip.setString(9, truncateString(trip.getEndAddress(), 500));
            stmtTrip.setString(10, trip.getTripType().getValue());

            if (trip.getPlannedStartTime() != null) {
                stmtTrip.setTimestamp(11, Timestamp.valueOf(trip.getPlannedStartTime()));
            } else {
                stmtTrip.setNull(11, Types.TIMESTAMP);
            }

            if (trip.getPlannedEndTime() != null) {
                stmtTrip.setTimestamp(12, Timestamp.valueOf(trip.getPlannedEndTime()));
            } else {
                stmtTrip.setNull(12, Types.TIMESTAMP);
            }

            stmtTrip.setInt(13, trip.getWaitingTime());

            setBigDecimalOrNull(stmtTrip, 14, trip.getPlannedDistance());
            setBigDecimalOrNull(stmtTrip, 15, trip.getPlannedCityKm());
            setBigDecimalOrNull(stmtTrip, 16, trip.getPlannedHighwayKm());
            setBigDecimalOrNull(stmtTrip, 17, trip.getPlannedFuelConsumption());

            stmtTrip.setString(18, truncateString(trip.getPurpose(), 65535));
            stmtTrip.setString(19, truncateString(trip.getPowerOfAttorney(), 65535));
            stmtTrip.setBoolean(20, trip.isCanDriverDeliver());
            stmtTrip.setString(21, trip.getStatus().getValue());

            Season season = trip.getSeason() != null ? trip.getSeason() : Season.getCurrentSeason();
            stmtTrip.setString(22, season.getValue());

            BigDecimal refrigeratorPercent = trip.getRefrigeratorUsagePercent();
            setBigDecimalOrNull(stmtTrip, 23, refrigeratorPercent);
            logger.info("💾 Зберігаємо refrigerator_usage_percent: {}", refrigeratorPercent);

            stmtTrip.setString(24, truncateString(trip.getNotes(), 65535));
            stmtTrip.setString(25, trip.getCreatedBy() != null ? trip.getCreatedBy() : "system");

            int rowsInserted = stmtTrip.executeUpdate();

            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmtTrip.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        trip.setId(generatedKeys.getInt(1));
                        trip.setTripNumber(tripNumber);
                        trip.setRequesterId(requesterId);
                        trip.setSeason(season);
                        trip.setCreatedAt(LocalDateTime.now());

                        if (trip.hasWaypoints()) {
                            WaypointDAO waypointDAO = new WaypointDAO();
                            for (Waypoint waypoint : trip.getWaypoints()) {
                                waypoint.setTripId(trip.getId());
                                waypointDAO.create(conn, waypoint);
                            }
                            logger.info("   Проміжні точки: {} шт.", trip.getWaypointCount());
                        }

                        conn.commit();

                        logger.info("Створено поїздку: {} (ID: {})", tripNumber, trip.getId());
                        logger.info("   Маршрут: {} → {}", trip.getStartAddress(), trip.getEndAddress());
                        logger.info("   Замовник: {} (ID: {})", trip.getRequesterName(), requesterId);

                        return trip;
                    }
                }
            }

            conn.rollback();
            logger.error(" Не вдалося створити поїздку");
            return null;

        } catch (SQLException e) {
            logger.error(" Помилка створення поїздки: ", e);
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                logger.error("Помилка rollback: ", rollbackEx);
            }
            return null;
        } finally {
            try {
                if (stmtTrip != null) stmtTrip.close();
                if (stmtRequester != null) stmtRequester.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error("Помилка закриття ресурсів: ", e);
            }
        }
    }

    private int createOrFindRequester(Connection conn, Trip trip) throws SQLException {

        String sqlFind = "SELECT id FROM requesters WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sqlFind)) {
            stmt.setString(1, trip.getRequesterEmail());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int existingId = rs.getInt("id");
                    logger.info(" Знайдено існуючого замовника: {} (ID: {})",
                            trip.getRequesterName(), existingId);
                    return existingId;
                }
            }
        }

        String sqlCreate = """
            INSERT INTO requesters (full_name, email, phone, created_at)
            VALUES (?, ?, ?, NOW())
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sqlCreate, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, truncateString(trip.getRequesterName(), 255));
            stmt.setString(2, truncateString(trip.getRequesterEmail(), 255));
            stmt.setString(3, truncateString(trip.getRequesterPhone(), 50));

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newId = generatedKeys.getInt(1);
                        logger.info("➕ Створено нового замовника: {} (ID: {})",
                                trip.getRequesterName(), newId);
                        return newId;
                    }
                }
            }
        }

        throw new SQLException("Не вдалося створити замовника");
    }

    private String generateTripNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePrefix = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try (Connection conn = DatabaseConfig.getConnection()) {

            String sql = "SELECT COUNT(*) + 1 as next_number FROM trips WHERE trip_number LIKE ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, datePrefix + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int nextNumber = rs.getInt("next_number");
                        return String.format("%s-%03d", datePrefix, nextNumber);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка генерації номера поїздки: ", e);
        }

        return datePrefix + "-" + now.format(DateTimeFormatter.ofPattern("HHmmss"));
    }

    public Trip findById(int id) {
        String sql = """
            SELECT t.*, 
                   r.full_name as req_full_name, r.email as req_email, 
                   r.phone as req_phone, r.department as req_department,
                   v.license_plate, v.model as vehicle_model,
                   d.full_name as driver_name, d.phone as driver_phone
            FROM trips t
            LEFT JOIN requesters r ON t.requester_id = r.id
            LEFT JOIN vehicles v ON t.vehicle_id = v.id
            LEFT JOIN drivers d ON t.driver_id = d.id
            WHERE t.id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Trip trip = mapResultSetToTrip(rs);
                    loadWaypointsForTrip(trip);
                    return trip;
                }
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання поїздки за ID {}: ", id, e);
        }

        return null;
    }

    public List<Trip> findAll() {
        return findAll(null, null, false);
    }

    public List<Trip> findAll(TripStatus status, LocalDateTime fromDate) {
        return findAll(status, fromDate, false);
    }

    public List<Trip> findAll(TripStatus status, LocalDateTime fromDate, boolean includeDeleted) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.*, 
                   r.full_name as req_full_name, r.email as req_email,
                   v.license_plate, v.model as vehicle_model,
                   d.full_name as driver_name
            FROM trips t
            LEFT JOIN requesters r ON t.requester_id = r.id
            LEFT JOIN vehicles v ON t.vehicle_id = v.id
            LEFT JOIN drivers d ON t.driver_id = d.id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (!includeDeleted) {
            sql.append(" AND t.status != ?");
            params.add(TripStatus.DELETED.getValue());
        }

        if (status != null) {
            sql.append(" AND t.status = ?");
            params.add(status.getValue());
        }

        if (fromDate != null) {
            sql.append(" AND t.created_at >= ?");
            params.add(Timestamp.valueOf(fromDate));
        }

        sql.append(" ORDER BY t.created_at DESC");

        List<Trip> trips = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Trip trip = mapResultSetToTrip(rs);
                    loadWaypointsForTrip(trip);
                    trips.add(trip);
                }
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання списку поїздок: ", e);
        }

        return trips;
    }

    public boolean updateStatus(int tripId, TripStatus newStatus) {
        String sql = "UPDATE trips SET status = ?, updated_at = NOW() WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String statusValue = newStatus.getValue();
            if (statusValue.length() > 10) {
                logger.error(" Статус '{}' занадто довгий ({} символів). Максимум 10 символів.", 
                    statusValue, statusValue.length());
                return false;
            }

            stmt.setString(1, statusValue);
            stmt.setInt(2, tripId);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                logger.info(" Оновлено статус поїздки ID {}: {}", tripId, newStatus);
                return true;
            }

        } catch (SQLException e) {
            logger.error("Помилка оновлення статусу поїздки ID {}: ", tripId, e);
            if (e.getMessage().contains("Data truncated")) {
                logger.error(" Помилка обрізання даних. Можливо, колонка 'status' занадто коротка для значення '{}'", 
                    newStatus.getValue());
                logger.error(" Рішення: Виконайте SQL команду: ALTER TABLE trips MODIFY COLUMN status VARCHAR(20);");
            }
        }

        return false;
    }

    public boolean softDelete(int tripId) {
        return updateStatus(tripId, TripStatus.DELETED);
    }

    public boolean restore(int tripId) {
        return updateStatus(tripId, TripStatus.CREATED);
    }

    public boolean assignDriver(int tripId, int driverId) {
        String sql = """
            UPDATE trips 
            SET driver_id = ?, status = ?, updated_at = NOW() 
            WHERE id = ? AND status IN ('created', 'assigned')
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, driverId);
            stmt.setString(2, TripStatus.ASSIGNED.getValue());
            stmt.setInt(3, tripId);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                logger.info("Призначено водія {} на поїздку ID {}", driverId, tripId);
                return true;
            }

        } catch (SQLException e) {
            logger.error("Помилка призначення водія на поїздку ID {}: ", tripId, e);
        }

        return false;
    }

    private Trip mapResultSetToTrip(ResultSet rs) throws SQLException {
        Trip trip = new Trip();

        trip.setId(rs.getInt("id"));
        trip.setTripNumber(rs.getString("trip_number"));
        trip.setVehicleId(rs.getInt("vehicle_id"));

        Integer driverId = rs.getInt("driver_id");
        if (!rs.wasNull()) {
            trip.setDriverId(driverId);
        }

        trip.setRequesterId(rs.getInt("requester_id"));

        trip.setRequesterName(rs.getString("requester_name"));
        trip.setRequesterEmail(rs.getString("requester_email"));
        trip.setRequesterPhone(rs.getString("requester_phone"));

        trip.setStartAddress(rs.getString("start_address"));
        trip.setEndAddress(rs.getString("end_address"));
        trip.setTripType(TripType.fromString(rs.getString("trip_type")));

        Timestamp plannedStartTime = rs.getTimestamp("planned_start_time");
        if (plannedStartTime != null) {
            trip.setPlannedStartTime(plannedStartTime.toLocalDateTime());
        }

        Timestamp plannedEndTime = rs.getTimestamp("planned_end_time");
        if (plannedEndTime != null) {
            trip.setPlannedEndTime(plannedEndTime.toLocalDateTime());
        }

        trip.setWaitingTime(rs.getInt("waiting_time"));

        trip.setPlannedDistance(rs.getBigDecimal("planned_distance"));
        trip.setPlannedCityKm(rs.getBigDecimal("planned_city_km"));
        trip.setPlannedHighwayKm(rs.getBigDecimal("planned_highway_km"));
        trip.setPlannedFuelConsumption(rs.getBigDecimal("planned_fuel_consumption"));

        int actualStartOdometer = rs.getInt("actual_start_odometer");
        if (!rs.wasNull()) {
            trip.setActualStartOdometer(actualStartOdometer);
        }
        int actualEndOdometer = rs.getInt("actual_end_odometer");
        if (!rs.wasNull()) {
            trip.setActualEndOdometer(actualEndOdometer);
        }

        Timestamp actualStartTime = rs.getTimestamp("actual_start_time");
        if (actualStartTime != null) {
            trip.setActualStartTime(actualStartTime.toLocalDateTime());
        }
        Timestamp actualEndTime = rs.getTimestamp("actual_end_time");
        if (actualEndTime != null) {
            trip.setActualEndTime(actualEndTime.toLocalDateTime());
        }

        trip.setActualDistance(rs.getBigDecimal("actual_distance"));
        trip.setActualCityKm(rs.getBigDecimal("actual_city_km"));
        trip.setActualHighwayKm(rs.getBigDecimal("actual_highway_km"));
        trip.setActualFuelConsumption(rs.getBigDecimal("actual_fuel_consumption"));

        trip.setFuelReceived(rs.getBigDecimal("fuel_received"));
        trip.setFuelReceivedCoupons(rs.getBigDecimal("fuel_received_coupons"));
        trip.setFuelReceivedMoney(rs.getBigDecimal("fuel_received_money"));

        trip.setPurpose(rs.getString("purpose"));
        trip.setPowerOfAttorney(rs.getString("power_of_attorney"));
        trip.setCanDriverDeliver(rs.getBoolean("can_driver_deliver"));

        trip.setStatus(TripStatus.fromString(rs.getString("status")));
        trip.setSeason(Season.fromString(rs.getString("season")));
        trip.setRouteDeviationPercent(rs.getBigDecimal("route_deviation_percent"));
        trip.setRefrigeratorUsagePercent(rs.getBigDecimal("refrigerator_usage_percent"));
        trip.setNotes(rs.getString("notes"));

        trip.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            trip.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        trip.setCreatedBy(rs.getString("created_by"));

        return trip;
    }

    private void setBigDecimalOrNull(PreparedStatement stmt, int parameterIndex, BigDecimal value) throws SQLException {
        if (value != null) {
            stmt.setBigDecimal(parameterIndex, value);
        } else {
            stmt.setNull(parameterIndex, Types.DECIMAL);
        }
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }

        String truncated = str.substring(0, maxLength - 3) + "...";
        logger.warn(" Рядок обрізано з {} до {} символів: '{}'",
                str.length(), maxLength,
                str.length() > 100 ? str.substring(0, 100) + "..." : str);
        return truncated;
    }
    public List<Trip> findAllWithDetails() {
        String sql = """
            SELECT t.*, 
                   r.full_name as req_full_name, r.email as req_email, 
                   r.phone as req_phone, r.department as req_department,
                   v.license_plate, v.model as vehicle_model, v.fuel_balance,
                   d.full_name as driver_name, d.phone as driver_phone,
                   d.license_number as driver_license
            FROM trips t
            LEFT JOIN requesters r ON t.requester_id = r.id
            LEFT JOIN vehicles v ON t.vehicle_id = v.id
            LEFT JOIN drivers d ON t.driver_id = d.id
            WHERE t.status != ?
            ORDER BY t.created_at DESC
            """;

        List<Trip> trips = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, TripStatus.DELETED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Trip trip = mapResultSetToTripWithDetails(rs);
                    trips.add(trip);
                }
            }

            logger.info("Завантажено {} поїздок з деталями", trips.size());

        } catch (SQLException e) {
            logger.error("Помилка отримання поїздок з деталями: ", e);
        }

        return trips;
    }

    public TripStatistics getTripStatistics() {
        String sql = """
            SELECT 
                COUNT(*) as total_trips,
                COUNT(CASE WHEN status = 'completed' THEN 1 END) as completed_trips,
                COUNT(CASE WHEN status = 'created' THEN 1 END) as created_trips,
                COUNT(CASE WHEN status = 'assigned' THEN 1 END) as assigned_trips,
                COUNT(CASE WHEN status = 'started' THEN 1 END) as started_trips,
                COUNT(CASE WHEN status = 'cancelled' THEN 1 END) as cancelled_trips,
                COALESCE(SUM(planned_fuel_consumption), 0) as total_planned_fuel,
                COALESCE(SUM(actual_fuel_consumption), 0) as total_actual_fuel,
                COALESCE(SUM(planned_distance), 0) as total_planned_distance,
                COALESCE(SUM(actual_distance), 0) as total_actual_distance
            FROM trips
            WHERE status != ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, TripStatus.DELETED.getValue());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                TripStatistics stats = new TripStatistics();
                stats.setTotalTrips(rs.getInt("total_trips"));
                stats.setCompletedTrips(rs.getInt("completed_trips"));
                stats.setCreatedTrips(rs.getInt("created_trips"));
                stats.setAssignedTrips(rs.getInt("assigned_trips"));
                stats.setStartedTrips(rs.getInt("started_trips"));
                stats.setCancelledTrips(rs.getInt("cancelled_trips"));
                stats.setTotalPlannedFuel(rs.getBigDecimal("total_planned_fuel"));
                stats.setTotalActualFuel(rs.getBigDecimal("total_actual_fuel"));
                stats.setTotalPlannedDistance(rs.getBigDecimal("total_planned_distance"));
                stats.setTotalActualDistance(rs.getBigDecimal("total_actual_distance"));
                return stats;
                }
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання статистики поїздок: ", e);
        }

        return new TripStatistics();
    }

    private Trip mapResultSetToTripWithDetails(ResultSet rs) throws SQLException {
        Trip trip = mapResultSetToTrip(rs);

        return trip;
    }

    public static class TripStatistics {
        private int totalTrips;
        private int completedTrips;
        private int createdTrips;
        private int assignedTrips;
        private int startedTrips;
        private int cancelledTrips;
        private BigDecimal totalPlannedFuel = BigDecimal.ZERO;
        private BigDecimal totalActualFuel = BigDecimal.ZERO;
        private BigDecimal totalPlannedDistance = BigDecimal.ZERO;
        private BigDecimal totalActualDistance = BigDecimal.ZERO;

        public int getTotalTrips() { return totalTrips; }
        public void setTotalTrips(int totalTrips) { this.totalTrips = totalTrips; }

        public int getCompletedTrips() { return completedTrips; }
        public void setCompletedTrips(int completedTrips) { this.completedTrips = completedTrips; }

        public int getCreatedTrips() { return createdTrips; }
        public void setCreatedTrips(int createdTrips) { this.createdTrips = createdTrips; }

        public int getAssignedTrips() { return assignedTrips; }
        public void setAssignedTrips(int assignedTrips) { this.assignedTrips = assignedTrips; }

        public int getStartedTrips() { return startedTrips; }
        public void setStartedTrips(int startedTrips) { this.startedTrips = startedTrips; }

        public int getCancelledTrips() { return cancelledTrips; }
        public void setCancelledTrips(int cancelledTrips) { this.cancelledTrips = cancelledTrips; }

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

        public BigDecimal getEffectiveFuelConsumption() {
            return totalActualFuel.compareTo(BigDecimal.ZERO) > 0 ? totalActualFuel : totalPlannedFuel;
        }

        public BigDecimal getEffectiveDistance() {
            return totalActualDistance.compareTo(BigDecimal.ZERO) > 0 ? totalActualDistance : totalPlannedDistance;
        }
    }

    private void loadWaypointsForTrip(Trip trip) {
        if (trip == null || trip.getId() <= 0) {
            return;
        }

        try {
            WaypointDAO waypointDAO = new WaypointDAO();
            List<Waypoint> waypoints = waypointDAO.findByTripId(trip.getId());
            trip.setWaypoints(waypoints);
        } catch (Exception e) {
            logger.error("Помилка завантаження проміжних точок для поїздки ID {}: ", trip.getId(), e);
        }
    }

    public boolean updateVehicle(int tripId, int newVehicleId, BigDecimal newFuelConsumption) {
        String sql = """
            UPDATE trips 
            SET vehicle_id = ?, planned_fuel_consumption = ?, updated_at = NOW()
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newVehicleId);
            setBigDecimalOrNull(stmt, 2, newFuelConsumption);
            stmt.setInt(3, tripId);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                logger.info(" Оновлено автомобіль для поїздки ID {}: новий vehicle_id={}, нові витрати={} л", 
                    tripId, newVehicleId, newFuelConsumption);
                return true;
            }

        } catch (SQLException e) {
            logger.error("Помилка оновлення автомобіля для поїздки ID {}: ", tripId, e);
        }

        return false;
    }

    public boolean updateRoute(Trip trip) {
        String sql = """
            UPDATE trips 
            SET start_address = ?, end_address = ?, 
                planned_distance = ?, planned_city_km = ?, planned_highway_km = ?,
                planned_fuel_consumption = ?,
                route_modifications_count = route_modifications_count + 1,
                updated_at = NOW()
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, truncateString(trip.getStartAddress(), 500));
            stmt.setString(2, truncateString(trip.getEndAddress(), 500));
            setBigDecimalOrNull(stmt, 3, trip.getPlannedDistance());
            setBigDecimalOrNull(stmt, 4, trip.getPlannedCityKm());
            setBigDecimalOrNull(stmt, 5, trip.getPlannedHighwayKm());
            setBigDecimalOrNull(stmt, 6, trip.getPlannedFuelConsumption());
            stmt.setInt(7, trip.getId());

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                logger.info(" Оновлено маршрут поїздки ID {}: {} → {}", 
                    trip.getId(), trip.getStartAddress(), trip.getEndAddress());
                logger.info("   Відстань: {} км, Паливо: {} л", 
                    trip.getPlannedDistance(), trip.getPlannedFuelConsumption());
                return true;
            }

        } catch (SQLException e) {
            logger.error("Помилка оновлення маршруту поїздки ID {}: ", trip.getId(), e);
        }

        return false;
    }

    public boolean existsSimilarTrip(String requesterEmail, String startAddress, String endAddress, 
                                     LocalDateTime plannedStartTime) {
        String normalizedEmail = normalizeForComparison(requesterEmail);
        String normalizedStartAddr = normalizeForComparison(startAddress);
        String normalizedEndAddr = normalizeForComparison(endAddress);
        if (normalizedEmail.isEmpty()) {
            logger.debug("Email порожній, пропускаємо перевірку");
            return false;
        }
        String sql = """
            SELECT start_address, end_address, planned_start_time FROM trips
            WHERE LOWER(requester_email) = ?
            AND status != 'del'
            """;
        if (plannedStartTime != null) {
            sql += " AND planned_start_time BETWEEN ? AND ?";
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizedEmail);
            if (plannedStartTime != null) {
                LocalDateTime timeBefore = plannedStartTime.minusHours(48);
                LocalDateTime timeAfter = plannedStartTime.plusHours(48);
                stmt.setTimestamp(2, Timestamp.valueOf(timeBefore));
                stmt.setTimestamp(3, Timestamp.valueOf(timeAfter));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String dbStartAddr = normalizeForComparison(rs.getString("start_address"));
                    String dbEndAddr = normalizeForComparison(rs.getString("end_address"));
                    boolean startMatch = addressesMatch(normalizedStartAddr, dbStartAddr);
                    boolean endMatch = addressesMatch(normalizedEndAddr, dbEndAddr);
                    if (startMatch && endMatch) {
                        logger.debug("Знайдено схожу поїздку для email={}, start={}, end={}", 
                                normalizedEmail, normalizedStartAddr, normalizedEndAddr);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка перевірки існування схожої поїздки: ", e);
        }
        return false;
    }
    private boolean addressesMatch(String addr1, String addr2) {
        if (addr1 == null || addr2 == null || addr1.isEmpty() || addr2.isEmpty()) {
            return false;
        }
        if (addr1.equals(addr2)) {
            return true;
        }
        if (addr1.contains(addr2) || addr2.contains(addr1)) {
            return true;
        }
        int minLen = Math.min(addr1.length(), addr2.length());
        if (minLen >= 10) {
            String prefix1 = addr1.substring(0, Math.min(15, minLen));
            String prefix2 = addr2.substring(0, Math.min(15, minLen));
            if (prefix1.equals(prefix2)) {
                return true;
            }
        }
        return false;
    }
    private String normalizeForComparison(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public static class PagedResult {
        private final List<Trip> trips;
        private final int totalCount;
        private final int page;
        private final int pageSize;

        public PagedResult(List<Trip> trips, int totalCount, int page, int pageSize) {
            this.trips = trips;
            this.totalCount = totalCount;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<Trip> getTrips() { return trips; }
        public int getTotalCount() { return totalCount; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { 
            return totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / pageSize); 
        }
        public boolean hasNext() { return page < getTotalPages(); }
        public boolean hasPrevious() { return page > 1; }
    }

    public PagedResult findPaged(int page, int pageSize, TripStatus status, 
                                  LocalDateTime fromDate, String searchText, 
                                  boolean onlyAssigned, boolean includeDeleted) {
        StringBuilder whereClause = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (!includeDeleted && status != TripStatus.DELETED) {
            whereClause.append(" AND t.status != ?");
            params.add(TripStatus.DELETED.getValue());
        }

        if (status != null) {
            whereClause.append(" AND t.status = ?");
            params.add(status.getValue());
        }

        if (fromDate != null) {
            whereClause.append(" AND t.created_at >= ?");
            params.add(Timestamp.valueOf(fromDate));
        }

        if (onlyAssigned) {
            whereClause.append(" AND t.driver_id IS NOT NULL");
        }

        if (searchText != null && !searchText.trim().isEmpty()) {
            String searchPattern = "%" + searchText.trim().toLowerCase() + "%";
            whereClause.append(" AND (LOWER(t.trip_number) LIKE ? OR LOWER(t.requester_name) LIKE ? ");
            whereClause.append("OR LOWER(t.start_address) LIKE ? OR LOWER(t.end_address) LIKE ?)");
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        String countSql = "SELECT COUNT(*) as total FROM trips t " + whereClause;
        String dataSql = """
            SELECT t.*, 
                   r.full_name as req_full_name, r.email as req_email,
                   v.license_plate, v.model as vehicle_model,
                   d.full_name as driver_name
            FROM trips t
            LEFT JOIN requesters r ON t.requester_id = r.id
            LEFT JOIN vehicles v ON t.vehicle_id = v.id
            LEFT JOIN drivers d ON t.driver_id = d.id
            """ + whereClause + " ORDER BY t.created_at DESC LIMIT ? OFFSET ?";

        int totalCount = 0;
        List<Trip> trips = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) {
                    countStmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        totalCount = rs.getInt("total");
                    }
                }
            }

            int offset = (page - 1) * pageSize;
            try (PreparedStatement dataStmt = conn.prepareStatement(dataSql)) {
                int paramIndex = 1;
                for (Object param : params) {
                    dataStmt.setObject(paramIndex++, param);
                }
                dataStmt.setInt(paramIndex++, pageSize);
                dataStmt.setInt(paramIndex, offset);

                try (ResultSet rs = dataStmt.executeQuery()) {
                    while (rs.next()) {
                        Trip trip = mapResultSetToTrip(rs);
                        loadWaypointsForTrip(trip);
                        trips.add(trip);
                    }
                }
            }

            logger.debug("Пагінація: сторінка {}/{}, записів {}, всього {}", 
                page, (int) Math.ceil((double) totalCount / pageSize), trips.size(), totalCount);

        } catch (SQLException e) {
            logger.error("Помилка отримання пагінованого списку поїздок: ", e);
        }

        return new PagedResult(trips, totalCount, page, pageSize);
    }
}