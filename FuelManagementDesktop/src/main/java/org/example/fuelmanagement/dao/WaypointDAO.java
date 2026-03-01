package org.example.fuelmanagement.dao;

import org.example.fuelmanagement.config.DatabaseConfig;
import org.example.fuelmanagement.model.Waypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WaypointDAO {
    private static final Logger logger = LoggerFactory.getLogger(WaypointDAO.class);

    public Waypoint create(Waypoint waypoint) {
        String sql = """
            INSERT INTO waypoints (
                trip_id, sequence_order, address, description,
                latitude, longitude, estimated_stop_time,
                planned_arrival_time, notes, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, waypoint.getTripId());
            stmt.setInt(2, waypoint.getSequenceOrder());
            stmt.setString(3, truncateString(waypoint.getAddress(), 500));
            stmt.setString(4, truncateString(waypoint.getDescription(), 255));

            setBigDecimalOrNull(stmt, 5, waypoint.getLatitude());
            setBigDecimalOrNull(stmt, 6, waypoint.getLongitude());
            stmt.setInt(7, waypoint.getEstimatedStopTime());

            if (waypoint.getPlannedArrivalTime() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(waypoint.getPlannedArrivalTime()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }

            stmt.setString(9, truncateString(waypoint.getNotes(), 1000));

            int rowsInserted = stmt.executeUpdate();

            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        waypoint.setId(generatedKeys.getInt(1));
                        waypoint.setCreatedAt(LocalDateTime.now());
                        logger.info(" Створено проміжну точку: {} (ID: {})", waypoint.getFullDescription(), waypoint.getId());
                        return waypoint;
                    }
                }
            }

        } catch (SQLException e) {
            logger.error(" Помилка створення проміжної точки: ", e);
        }

        return null;
    }

    public Waypoint create(Connection conn, Waypoint waypoint) throws SQLException {
        String sql = """
            INSERT INTO waypoints (
                trip_id, sequence_order, address, description,
                latitude, longitude, estimated_stop_time,
                planned_arrival_time, notes, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, waypoint.getTripId());
            stmt.setInt(2, waypoint.getSequenceOrder());
            stmt.setString(3, truncateString(waypoint.getAddress(), 500));
            stmt.setString(4, truncateString(waypoint.getDescription(), 255));

            setBigDecimalOrNull(stmt, 5, waypoint.getLatitude());
            setBigDecimalOrNull(stmt, 6, waypoint.getLongitude());
            stmt.setInt(7, waypoint.getEstimatedStopTime());

            if (waypoint.getPlannedArrivalTime() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(waypoint.getPlannedArrivalTime()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }

            stmt.setString(9, truncateString(waypoint.getNotes(), 1000));

            int rowsInserted = stmt.executeUpdate();

            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        waypoint.setId(generatedKeys.getInt(1));
                        waypoint.setCreatedAt(LocalDateTime.now());
                        logger.info("Створено проміжну точку: {} (ID: {})", waypoint.getFullDescription(), waypoint.getId());
                        return waypoint;
                    }
                }
            }

        } catch (SQLException e) {
            logger.error(" Помилка створення проміжної точки в транзакції: ", e);
            throw e; 
        }

        return null;
    }

    public List<Waypoint> findByTripId(int tripId) {
        String sql = """
            SELECT * FROM waypoints 
            WHERE trip_id = ? 
            ORDER BY sequence_order ASC
            """;

        List<Waypoint> waypoints = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tripId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    waypoints.add(mapResultSetToWaypoint(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання проміжних точок для поїздки {}: ", tripId, e);
        }

        return waypoints;
    }

    public boolean update(Waypoint waypoint) {
        String sql = """
            UPDATE waypoints SET
                sequence_order = ?, address = ?, description = ?,
                latitude = ?, longitude = ?, estimated_stop_time = ?,
                planned_arrival_time = ?, actual_arrival_time = ?,
                actual_departure_time = ?, notes = ?, updated_at = NOW()
            WHERE id = ?
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, waypoint.getSequenceOrder());
            stmt.setString(2, truncateString(waypoint.getAddress(), 500));
            stmt.setString(3, truncateString(waypoint.getDescription(), 255));

            setBigDecimalOrNull(stmt, 4, waypoint.getLatitude());
            setBigDecimalOrNull(stmt, 5, waypoint.getLongitude());
            stmt.setInt(6, waypoint.getEstimatedStopTime());

            if (waypoint.getPlannedArrivalTime() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(waypoint.getPlannedArrivalTime()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }

            if (waypoint.getActualArrivalTime() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(waypoint.getActualArrivalTime()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }

            if (waypoint.getActualDepartureTime() != null) {
                stmt.setTimestamp(9, Timestamp.valueOf(waypoint.getActualDepartureTime()));
            } else {
                stmt.setNull(9, Types.TIMESTAMP);
            }

            stmt.setString(10, truncateString(waypoint.getNotes(), 1000));
            stmt.setInt(11, waypoint.getId());

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                waypoint.setUpdatedAt(LocalDateTime.now());
                logger.info("Оновлено проміжну точку: {} (ID: {})", waypoint.getFullDescription(), waypoint.getId());
                return true;
            }

        } catch (SQLException e) {
            logger.error("Помилка оновлення проміжної точки ID {}: ", waypoint.getId(), e);
        }

        return false;
    }

    public boolean delete(int waypointId) {
        String sql = "DELETE FROM waypoints WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, waypointId);
            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted > 0) {
                logger.info(" Видалено проміжну точку ID: {}", waypointId);
                return true;
            }

        } catch (SQLException e) {
            logger.error(" Помилка видалення проміжної точки ID {}: ", waypointId, e);
        }

        return false;
    }

    public boolean deleteByTripId(int tripId) {
        String sql = "DELETE FROM waypoints WHERE trip_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, tripId);
            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted > 0) {
                logger.info(" Видалено {} проміжних точок для поїздки ID: {}", rowsDeleted, tripId);
                return true;
            }

        } catch (SQLException e) {
            logger.error(" Помилка видалення проміжних точок для поїздки ID {}: ", tripId, e);
        }

        return false;
    }

    public boolean updateSequenceOrder(int tripId, List<Waypoint> waypoints) {
        String sql = "UPDATE waypoints SET sequence_order = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Waypoint waypoint : waypoints) {
                    stmt.setInt(1, waypoint.getSequenceOrder());
                    stmt.setInt(2, waypoint.getId());
                    stmt.addBatch();
                }

                int[] results = stmt.executeBatch();
                conn.commit();

                logger.info(" Оновлено порядок {} проміжних точок для поїздки ID: {}", results.length, tripId);
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            logger.error(" Помилка оновлення порядку проміжних точок для поїздки ID {}: ", tripId, e);
        }

        return false;
    }

    private Waypoint mapResultSetToWaypoint(ResultSet rs) throws SQLException {
        Waypoint waypoint = new Waypoint();

        waypoint.setId(rs.getInt("id"));
        waypoint.setTripId(rs.getInt("trip_id"));
        waypoint.setSequenceOrder(rs.getInt("sequence_order"));
        waypoint.setAddress(rs.getString("address"));
        waypoint.setDescription(rs.getString("description"));

        BigDecimal latitude = rs.getBigDecimal("latitude");
        if (!rs.wasNull()) {
            waypoint.setLatitude(latitude);
        }

        BigDecimal longitude = rs.getBigDecimal("longitude");
        if (!rs.wasNull()) {
            waypoint.setLongitude(longitude);
        }

        waypoint.setEstimatedStopTime(rs.getInt("estimated_stop_time"));

        Timestamp plannedArrivalTime = rs.getTimestamp("planned_arrival_time");
        if (plannedArrivalTime != null) {
            waypoint.setPlannedArrivalTime(plannedArrivalTime.toLocalDateTime());
        }

        Timestamp actualArrivalTime = rs.getTimestamp("actual_arrival_time");
        if (actualArrivalTime != null) {
            waypoint.setActualArrivalTime(actualArrivalTime.toLocalDateTime());
        }

        Timestamp actualDepartureTime = rs.getTimestamp("actual_departure_time");
        if (actualDepartureTime != null) {
            waypoint.setActualDepartureTime(actualDepartureTime.toLocalDateTime());
        }

        waypoint.setNotes(rs.getString("notes"));
        waypoint.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            waypoint.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return waypoint;
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
        logger.warn("Рядок обрізано з {} до {} символів: '{}'",
                str.length(), maxLength,
                str.length() > 100 ? str.substring(0, 100) + "..." : str);
        return truncated;
    }
}
