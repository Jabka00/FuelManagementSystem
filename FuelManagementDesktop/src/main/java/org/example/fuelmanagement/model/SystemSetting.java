package org.example.fuelmanagement.model;

import java.time.LocalDateTime;

public class SystemSetting {
    private int id;
    private String settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public SystemSetting() {
        this.updatedAt = LocalDateTime.now();
    }

    public SystemSetting(String settingKey, String settingValue) {
        this();
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    public SystemSetting(String settingKey, String settingValue, String description) {
        this(settingKey, settingValue);
        this.description = description;
    }

    public boolean getBooleanValue() {
        return settingValue != null &&
                ("true".equalsIgnoreCase(settingValue) || "1".equals(settingValue));
    }

    public int getIntValue() {
        try {
            return settingValue != null ? Integer.parseInt(settingValue) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public double getDoubleValue() {
        try {
            return settingValue != null ? Double.parseDouble(settingValue) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public String getStringValue() {
        return settingValue != null ? settingValue : "";
    }

    public void setValue(boolean value) {
        this.settingValue = String.valueOf(value);
        this.updatedAt = LocalDateTime.now();
    }

    public void setValue(int value) {
        this.settingValue = String.valueOf(value);
        this.updatedAt = LocalDateTime.now();
    }

    public void setValue(double value) {
        this.settingValue = String.valueOf(value);
        this.updatedAt = LocalDateTime.now();
    }

    public void setValue(String value) {
        this.settingValue = value;
        this.updatedAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    @Override
    public String toString() {
        return settingKey + " = " + settingValue;
    }
}