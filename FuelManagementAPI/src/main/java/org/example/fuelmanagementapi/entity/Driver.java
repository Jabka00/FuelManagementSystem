package org.example.fuelmanagementapi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DriverStatus status;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DriverStatus {
        active,      
        vacation,
        sick,
        inactive;

        public String getValue() {
            return this.name().toLowerCase();
        }

        public static DriverStatus fromString(String value) {
            if (value == null) return active;
            try {
                return DriverStatus.valueOf(value.toLowerCase());
            } catch (IllegalArgumentException e) {
                return active; 
            }
        }

        public boolean isActive() {
            return this == active;
        }

        @Override
        public String toString() {
            switch (this) {
                case active: return "Активний";
                case vacation: return "У відпустці";
                case sick: return "На лікарняному";
                case inactive: return "Неактивний";
                default: return name();
            }
        }
    }

    public Driver() {
        this.status = DriverStatus.active;
        this.createdAt = LocalDateTime.now();
    }

    public Driver(String fullName, String phone) {
        this();
        this.fullName = fullName;
        this.phone = phone;
    }

    public boolean isAvailable() {
        return status != null && status.isActive();
    }

    public String getDisplayName() {
        return fullName + (phone != null ? "(" + phone + ")" : "");
    }

    public String getStatusDisplayText() {
        if (status == null) return "Невідомо";

        switch (status) {
            case active: return "Активний";
            case vacation: return "У відпустці";
            case sick: return "На лікарняному";
            case inactive: return "Неактивний";
            default: return status.name();
        }
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

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
        return "Driver{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", status=" + status +
                '}';
    }
}
