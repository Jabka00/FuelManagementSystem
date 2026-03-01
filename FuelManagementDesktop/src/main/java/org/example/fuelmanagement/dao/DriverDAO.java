package org.example.fuelmanagement.dao;

import org.example.fuelmanagement.config.DatabaseConfig;
import org.example.fuelmanagement.model.Driver;
import org.example.fuelmanagement.model.enums.DriverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DriverDAO {
    private static final Logger logger = LoggerFactory.getLogger(DriverDAO.class);

    public List<Driver> findAllActive() {
        List<Driver> drivers = new ArrayList<>();
        String sql = "SELECT id, full_name, phone, telegram_chat_id, telegram_username, " +
                "license_number, license_expiry, status, hire_date, notes, created_at, updated_at " +
                "FROM drivers WHERE status = 'active' ORDER BY full_name";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                drivers.add(mapResultSetToDriver(rs));
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання списку водіїв: ", e);
        }

        return drivers;
    }

    public Driver findById(int id) {
        String sql = "SELECT id, full_name, phone, telegram_chat_id, telegram_username, " +
                "license_number, license_expiry, status, hire_date, notes, created_at, updated_at " +
                "FROM drivers WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDriver(rs);
                }
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання водія за ID {}: ", id, e);
        }

        return null;
    }

    public List<Driver> findAll() {
        List<Driver> drivers = new ArrayList<>();
        String sql = "SELECT id, full_name, phone, telegram_chat_id, telegram_username, " +
                "license_number, license_expiry, status, hire_date, notes, created_at, updated_at " +
                "FROM drivers ORDER BY full_name";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                drivers.add(mapResultSetToDriver(rs));
            }

        } catch (SQLException e) {
            logger.error("Помилка отримання списку всіх водіїв: ", e);
        }

        return drivers;
    }

    public boolean update(Driver driver) {
        String sql = "UPDATE drivers SET full_name = ?, phone = ?, telegram_chat_id = ?, " +
                "telegram_username = ?, license_number = ?, license_expiry = ?, status = ?, " +
                "hire_date = ?, notes = ?, updated_at = NOW() WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, driver.getFullName());
            stmt.setString(2, driver.getPhone());
            if (driver.getTelegramChatId() != null) {
                stmt.setLong(3, driver.getTelegramChatId());
            } else {
                stmt.setNull(3, Types.BIGINT);
            }
            stmt.setString(4, driver.getTelegramUsername());
            stmt.setString(5, driver.getLicenseNumber());
            if (driver.getLicenseExpiry() != null) {
                stmt.setDate(6, Date.valueOf(driver.getLicenseExpiry()));
            } else {
                stmt.setNull(6, Types.DATE);
            }
            stmt.setString(7, driver.getStatus().getValue());
            if (driver.getHireDate() != null) {
                stmt.setDate(8, Date.valueOf(driver.getHireDate()));
            } else {
                stmt.setNull(8, Types.DATE);
            }
            stmt.setString(9, driver.getNotes());
            stmt.setInt(10, driver.getId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                logger.info("Водій ID {} успішно оновлено", driver.getId());
                return true;
            }

        } catch (SQLException e) {
            logger.error("Помилка оновлення водія ID {}: ", driver.getId(), e);
        }

        return false;
    }

    private Driver mapResultSetToDriver(ResultSet rs) throws SQLException {
        Driver driver = new Driver();
        driver.setId(rs.getInt("id"));
        driver.setFullName(rs.getString("full_name"));
        driver.setPhone(rs.getString("phone"));

        Long chatId = rs.getLong("telegram_chat_id");
        if (!rs.wasNull()) {
            driver.setTelegramChatId(chatId);
        }

        driver.setTelegramUsername(rs.getString("telegram_username"));
        driver.setLicenseNumber(rs.getString("license_number"));

        Date licenseExpiry = rs.getDate("license_expiry");
        if (licenseExpiry != null) {
            driver.setLicenseExpiry(licenseExpiry.toLocalDate());
        }

        driver.setStatus(DriverStatus.fromString(rs.getString("status")));

        Date hireDate = rs.getDate("hire_date");
        if (hireDate != null) {
            driver.setHireDate(hireDate.toLocalDate());
        }

        driver.setNotes(rs.getString("notes"));
        driver.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            driver.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return driver;
    }
}