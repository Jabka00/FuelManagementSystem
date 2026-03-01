package org.example.fuelmanagement.model;

import java.time.LocalDateTime;

public class Requester {
    private int id;
    private String fullName;
    private String email;
    private String phone;
    private String department;
    private String position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Requester() {
        this.createdAt = LocalDateTime.now();
    }

    public Requester(String fullName, String email) {
        this();
        this.fullName = fullName;
        this.email = email;
    }

    public Requester(String fullName, String email, String phone, String department) {
        this(fullName, email);
        this.phone = phone;
        this.department = department;
    }

    public String getDisplayName() {
        StringBuilder display = new StringBuilder(fullName);
        if (department != null && !department.isEmpty()) {
            display.append(" (").append(department).append(")");
        }
        return display.toString();
    }

    public String getContactInfo() {
        StringBuilder contact = new StringBuilder();
        if (email != null && !email.isEmpty()) {
            contact.append("Email: ").append(email);
        }
        if (phone != null && !phone.isEmpty()) {
            if (contact.length() > 0) contact.append(", ");
            contact.append("Тел: ").append(phone);
        }
        return contact.toString();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return getDisplayName();
    }
}