package com.example.fuelmanagementapp.models;

import com.google.gson.annotations.SerializedName;
import com.example.fuelmanagementapp.utils.Constants;

public class Driver {
    private Integer id;

    @SerializedName("fullName")
    private String fullName;

    private String phone;

    @SerializedName("telegramChatId")
    private Long telegramChatId;

    @SerializedName("telegramUsername")
    private String telegramUsername;

    @SerializedName("licenseNumber")
    private String licenseNumber;

    @SerializedName("licenseExpiry")
    private String licenseExpiry;

    private String status;  

    @SerializedName("hireDate")
    private String hireDate;

    private String notes;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Driver() {}

    public Driver(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }

    public boolean isAvailable() {
        return "active".equalsIgnoreCase(status);
    }

    public String getDisplayName() {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Неизвестный водитель";
        }
        return fullName.trim() + (hasPhone() ? "(" + phone.trim() + ")" : "");
    }

    public String getStatusDisplayText() {
        if (status == null) return "Неизвестно";

        switch (status.toLowerCase()) {
            case "active": return "Активный";
            case "vacation": return "В отпуске";
            case "sick": return "На больничном";
            case "inactive": return "Неактивный";
            default: return status;
        }
    }

    public boolean hasValidLicense() {
        return licenseNumber != null && !licenseNumber.trim().isEmpty();
    }

    public boolean hasPhone() {
        return phone != null && !phone.trim().isEmpty();
    }

    public boolean hasTelegram() {
        return telegramChatId != null ||
                (telegramUsername != null && !telegramUsername.trim().isEmpty());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(Long telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public String getTelegramUsername() {
        return telegramUsername;
    }

    public void setTelegramUsername(String telegramUsername) {
        this.telegramUsername = telegramUsername;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getLicenseExpiry() {
        return licenseExpiry;
    }

    public void setLicenseExpiry(String licenseExpiry) {
        this.licenseExpiry = licenseExpiry;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Driver{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
