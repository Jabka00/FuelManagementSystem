package org.example.fuelmanagement.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TelegramSession {
    private int id;
    private long chatId;
    private Integer driverId;
    private Map<String, Object> sessionData;
    private LocalDateTime lastActivity;

    public TelegramSession() {
        this.sessionData = new HashMap<>();
        this.lastActivity = LocalDateTime.now();
    }

    public TelegramSession(long chatId) {
        this();
        this.chatId = chatId;
    }

    public TelegramSession(long chatId, Integer driverId) {
        this(chatId);
        this.driverId = driverId;
    }

    public void setSessionValue(String key, Object value) {
        if (sessionData == null) {
            sessionData = new HashMap<>();
        }
        sessionData.put(key, value);
        updateActivity();
    }

    public Object getSessionValue(String key) {
        return sessionData != null ? sessionData.get(key) : null;
    }

    public String getSessionString(String key) {
        Object value = getSessionValue(key);
        return value != null ? value.toString() : null;
    }

    public Integer getSessionInt(String key) {
        Object value = getSessionValue(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Boolean getSessionBoolean(String key) {
        Object value = getSessionValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    public void removeSessionValue(String key) {
        if (sessionData != null) {
            sessionData.remove(key);
            updateActivity();
        }
    }

    public void clearSession() {
        if (sessionData != null) {
            sessionData.clear();
            updateActivity();
        }
    }

    public boolean hasSessionValue(String key) {
        return sessionData != null && sessionData.containsKey(key);
    }

    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public boolean isExpired(int timeoutMinutes) {
        return lastActivity.isBefore(LocalDateTime.now().minusMinutes(timeoutMinutes));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getChatId() { return chatId; }
    public void setChatId(long chatId) { this.chatId = chatId; }

    public Integer getDriverId() { return driverId; }
    public void setDriverId(Integer driverId) { this.driverId = driverId; }

    public Map<String, Object> getSessionData() { return sessionData; }
    public void setSessionData(Map<String, Object> sessionData) { this.sessionData = sessionData; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    @Override
    public String toString() {
        return "TelegramSession{chatId=" + chatId + ", driverId=" + driverId + "}";
    }
}