package org.example.fuelmanagement.controller.helper;

import javafx.scene.control.*;

public class FormValidationHelper {

    private static final String ERROR_STYLE = 
        "-fx-border-color: #E53935; -fx-border-width: 2; -fx-border-radius: 6; " +
        "-fx-background-color: #FFEBEE; -fx-background-radius: 6;";
    private static final String WARNING_STYLE = 
        "-fx-border-color: #FFA726; -fx-border-width: 1.5; -fx-border-radius: 6; " +
        "-fx-background-color: #FFF3E0; -fx-background-radius: 6;";
    private static final String NORMAL_FIELD_STYLE = 
        "-fx-background-color: white; -fx-background-radius: 6; " +
        "-fx-border-color: #D0E0E2; -fx-border-radius: 6;";
    private static final String NORMAL_COMBOBOX_STYLE = 
        "-fx-background-color: white; -fx-background-radius: 6;";

    public static void markFieldAsError(TextField field, String tooltip) {
        field.setStyle(ERROR_STYLE);
        field.setTooltip(new Tooltip(tooltip));
    }

    public static void markFieldAsWarning(TextField field, String tooltip) {
        field.setStyle(WARNING_STYLE);
        field.setTooltip(new Tooltip(tooltip));
    }

    public static void clearFieldError(TextField field) {
        field.setStyle(NORMAL_FIELD_STYLE);
        field.setTooltip(null);
    }

    public static void markTextAreaAsWarning(TextArea area, String tooltip) {
        area.setStyle(WARNING_STYLE);
        area.setTooltip(new Tooltip(tooltip));
    }

    public static void clearTextAreaError(TextArea area) {
        area.setStyle(NORMAL_FIELD_STYLE);
        area.setTooltip(null);
    }

    public static void markComboBoxAsError(ComboBox<?> comboBox, String tooltip) {
        comboBox.setStyle(ERROR_STYLE);
        comboBox.setTooltip(new Tooltip(tooltip));
    }

    public static void markComboBoxAsWarning(ComboBox<?> comboBox, String tooltip) {
        comboBox.setStyle(WARNING_STYLE);
        comboBox.setTooltip(new Tooltip(tooltip));
    }

    public static void clearComboBoxError(ComboBox<?> comboBox) {
        comboBox.setStyle(NORMAL_COMBOBOX_STYLE);
        comboBox.setTooltip(null);
    }

    public static void markDatePickerAsWarning(DatePicker datePicker) {
        datePicker.setStyle(WARNING_STYLE);
        datePicker.setTooltip(new Tooltip("Рекомендовано вказати дату"));
    }

    public static void clearDatePickerError(DatePicker datePicker) {
        datePicker.setStyle(NORMAL_COMBOBOX_STYLE);
        datePicker.setTooltip(null);
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }

    public static String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        return address.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public static class ValidationResult {
        private final StringBuilder errors = new StringBuilder();
        private boolean hasErrors = false;

        public void addError(String error) {
            errors.append("• ").append(error).append("\n");
            hasErrors = true;
        }

        public boolean hasErrors() {
            return hasErrors;
        }

        public String getErrorMessage() {
            return errors.toString();
        }
    }
}
