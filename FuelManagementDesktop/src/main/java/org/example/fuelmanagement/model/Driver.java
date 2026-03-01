package org.example.fuelmanagement.model;

import org.example.fuelmanagement.model.enums.DriverStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Driver {
    private int id;
    private String fullName;
    private String phone;
    private Long telegramChatId;
    private String telegramUsername;

    private String licenseNumber;
    private LocalDate licenseExpiry;

    private DriverStatus status;
    private LocalDate hireDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Driver() {
        this.status = DriverStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public Driver(String fullName, String phone) {
        this();
        this.fullName = fullName;
        this.phone = phone;
    }

    public boolean isAvailable() {
        return status == DriverStatus.ACTIVE;
    }

    public boolean hasTelegramAccount() {
        return telegramChatId != null;
    }

    public boolean isLicenseValid() {
        return licenseExpiry == null || licenseExpiry.isAfter(LocalDate.now());
    }

    public String getDisplayName() {
        return fullName + " (" + phone + ")";
    }

    public String getContactInfo() {
        StringBuilder contact = new StringBuilder();
        if (phone != null && !phone.trim().isEmpty()) {
            contact.append(" ").append(phone);
        }
        if (telegramUsername != null && !telegramUsername.trim().isEmpty()) {
            if (contact.length() > 0) contact.append(" | ");
            contact.append(" @").append(telegramUsername);
        }
        return contact.length() > 0 ? contact.toString() : "Контактів немає";
    }

    public String getLicenseInfo() {
        StringBuilder license = new StringBuilder();
        if (licenseNumber != null && !licenseNumber.trim().isEmpty()) {
            license.append("№ ").append(licenseNumber);
        }
        if (licenseExpiry != null) {
            if (license.length() > 0) license.append(" | ");
            license.append("до ").append(licenseExpiry.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        }
        return license.length() > 0 ? license.toString() : "Дані відсутні";
    }

    public boolean isLicenseExpiringSoon() {
        if (licenseExpiry == null) return false;
        return licenseExpiry.isBefore(LocalDate.now().plusMonths(1));
    }

    public String getLicenseStatus() {
        if (licenseExpiry == null) {
            return " Дата не указана";
        }

        if (licenseExpiry.isBefore(LocalDate.now())) {
            return " Просрочено";
        } else if (licenseExpiry.isBefore(LocalDate.now().plusMonths(1))) {
            return " Истекает скоро";
        } else {
            return " Действительно";
        }
    }

    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append(" ").append(fullName).append("\n");
        info.append(" ").append(getContactInfo()).append("\n");
        info.append(" ").append(getLicenseInfo()).append(" (").append(getLicenseStatus()).append(")\n");

        if (hireDate != null) {
            info.append(" Працює з ").append(hireDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        }

        return info.toString();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(Long telegramChatId) { this.telegramChatId = telegramChatId; }

    public String getTelegramUsername() { return telegramUsername; }
    public void setTelegramUsername(String telegramUsername) { this.telegramUsername = telegramUsername; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public LocalDate getLicenseExpiry() { return licenseExpiry; }
    public void setLicenseExpiry(LocalDate licenseExpiry) { this.licenseExpiry = licenseExpiry; }

    public DriverStatus getStatus() { return status; }
    public void setStatus(DriverStatus status) { this.status = status; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return getDisplayName();
    }
}