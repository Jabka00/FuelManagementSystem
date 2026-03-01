package org.example.fuelmanagement.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.example.fuelmanagement.model.Waypoint;
import org.example.fuelmanagement.service.GoogleMapsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class WaypointDialogController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(WaypointDialogController.class);

    @FXML private TextField txtWaypointAddress;
    @FXML private ComboBox<String> cmbWaypointDescription;
    @FXML private TextField txtStopTime;
    @FXML private TextArea txtWaypointNotes;
    @FXML private Button btnValidateAddress;
    @FXML private HBox validationStatusBox;
    @FXML private Label lblValidationIcon;
    @FXML private Label lblValidationStatus;
    @FXML private VBox advancedFieldsBox;
    @FXML private TextField txtLatitude;
    @FXML private TextField txtLongitude;
    @FXML private DatePicker dpPlannedArrival;
    @FXML private TextField txtPlannedArrivalTime;
    @FXML private DatePicker dpActualArrival;
    @FXML private TextField txtActualArrivalTime;
    @FXML private DatePicker dpActualDeparture;
    @FXML private TextField txtActualDepartureTime;
    @FXML private CheckBox chkShowAdvanced;
    @FXML private ComboBox<String> cmbTemplates;
    @FXML private Button btnSaveAsTemplate;
    @FXML private Button btnApplyTemplate;
    @FXML private Button btnCopyFromStart;
    @FXML private Button btnCopyFromEnd;
    @FXML private Button btnClearAll;
    @FXML private ComboBox<String> cmbQuickStopTime;

    private Waypoint waypoint;
    private GoogleMapsService googleMapsService;
    private ObservableList<Waypoint> existingWaypoints;
    private boolean addressValidated = false;
    private Stage dialogStage;
    private boolean saved = false;
    private String startAddress;
    private String endAddress;
    private static final ObservableList<WaypointTemplate> savedTemplates = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupValidation();
        setupDefaultValues();
        setupAdvancedFields();
        setupTemplates();
        setupQuickActions();
    }

    private void setupValidation() {
        txtWaypointAddress.textProperty().addListener((__, ___, newValue) -> {
            addressValidated = false;
            hideValidationStatus();
            if (newValue != null && newValue.trim().isEmpty()) {
                txtWaypointAddress.setStyle("-fx-border-color: #FF5722; -fx-border-width: 2;");
            } else {
                txtWaypointAddress.setStyle("-fx-border-color: #E0F0F2; -fx-border-width: 1;");
            }
        });

        txtStopTime.textProperty().addListener((__, ___, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                try {
                    int time = Integer.parseInt(newValue.trim());
                    if (time < 0 || time > 1440) {
                        txtStopTime.setStyle("-fx-border-color: #FF5722; -fx-border-width: 2;");
                    } else {
                        txtStopTime.setStyle("-fx-border-color: #E0F0F2; -fx-border-width: 1;");
                    }
                } catch (NumberFormatException e) {
                    txtStopTime.setStyle("-fx-border-color: #FF5722; -fx-border-width: 2;");
                }
            } else {
                txtStopTime.setStyle("-fx-border-color: #E0F0F2; -fx-border-width: 1;");
            }
        });
        if (txtLatitude != null) {
            txtLatitude.textProperty().addListener((__, ___, newValue) -> {
                if (newValue != null && !newValue.trim().isEmpty()) {
                    try {
                        double lat = Double.parseDouble(newValue.trim());
                        if (lat < -90 || lat > 90) {
                            txtLatitude.setStyle("-fx-border-color: #FF5722; -fx-border-width: 2;");
                        } else {
                            txtLatitude.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 1;");
                        }
                    } catch (NumberFormatException e) {
                        txtLatitude.setStyle("-fx-border-color: #FF5722; -fx-border-width: 2;");
                    }
                } else {
                    txtLatitude.setStyle("-fx-border-color: #E0F0F2; -fx-border-width: 1;");
                }
            });
        }
        if (txtLongitude != null) {
            txtLongitude.textProperty().addListener((__, ___, newValue) -> {
                if (newValue != null && !newValue.trim().isEmpty()) {
                    try {
                        double lon = Double.parseDouble(newValue.trim());
                        if (lon < -180 || lon > 180) {
                            txtLongitude.setStyle("-fx-border-color: #FF5722; -fx-border-width: 2;");
                        } else {
                            txtLongitude.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 1;");
                        }
                    } catch (NumberFormatException e) {
                        txtLongitude.setStyle("-fx-border-color: #FF5722; -fx-border-width: 2;");
                    }
                } else {
                    txtLongitude.setStyle("-fx-border-color: #E0F0F2; -fx-border-width: 1;");
                }
            });
        }
    }

    private void setupDefaultValues() {
        txtStopTime.setText("0");
        cmbWaypointDescription.setItems(FXCollections.observableArrayList(
            "Завантаження",
            "Розвантаження", 
            "Часткове розвантаження",
            "Часткове завантаження",
            "Зупинка для обіду",
            "Технічна зупинка",
            "Зупинка для відпочинку",
            "Зупинка для палива",
            "Зупинка для огляду",
            "Контрольна точка",
            "Митний пост",
            "Склад",
            "Офіс",
            "Клієнт",
            "Інше"
        ));
        cmbWaypointDescription.setEditable(true);
    }
    private void setupAdvancedFields() {
        if (advancedFieldsBox != null) {
            advancedFieldsBox.setVisible(false);
            advancedFieldsBox.setManaged(false);
        }
        if (chkShowAdvanced != null) {
            chkShowAdvanced.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (advancedFieldsBox != null) {
                    advancedFieldsBox.setVisible(newVal);
                    advancedFieldsBox.setManaged(newVal);
                    if (dialogStage != null) {
                        dialogStage.sizeToScene();
                    }
                }
            });
        }
    }
    private void setupTemplates() {
        if (cmbTemplates != null) {
            updateTemplatesList();
            cmbTemplates.setOnAction(e -> {
                String selectedName = cmbTemplates.getValue();
                if (selectedName != null && !selectedName.isEmpty()) {
                    WaypointTemplate template = findTemplateByName(selectedName);
                    if (template != null && btnApplyTemplate != null) {
                        btnApplyTemplate.setDisable(false);
                    }
                }
            });
        }
    }
    private void setupQuickActions() {
        if (cmbQuickStopTime != null) {
            cmbQuickStopTime.setItems(FXCollections.observableArrayList(
                "0 хв - Без зупинки",
                "5 хв - Коротка зупинка",
                "15 хв - Розвантаження/завантаження",
                "30 хв - Обід",
                "45 хв - Відпочинок",
                "60 хв - Тривала зупинка",
                "90 хв - Перерва водія",
                "120 хв - 2 години"
            ));
            cmbQuickStopTime.setOnAction(e -> {
                String selected = cmbQuickStopTime.getValue();
                if (selected != null) {
                    String[] parts = selected.split(" ");
                    if (parts.length > 0) {
                        txtStopTime.setText(parts[0]);
                    }
                }
            });
        }
        if (btnCopyFromStart != null) {
            btnCopyFromStart.setDisable(startAddress == null || startAddress.isEmpty());
        }
        if (btnCopyFromEnd != null) {
            btnCopyFromEnd.setDisable(endAddress == null || endAddress.isEmpty());
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setWaypoint(Waypoint waypoint) {
        this.waypoint = waypoint;
        this.saved = false;

        if (waypoint != null && waypoint.getAddress() != null) {
            txtWaypointAddress.setText(waypoint.getAddress());
            cmbWaypointDescription.setValue(waypoint.getDescription());
            txtStopTime.setText(String.valueOf(waypoint.getEstimatedStopTime()));
            if (waypoint.getNotes() != null) {
                txtWaypointNotes.setText(waypoint.getNotes());
            }
            if (txtLatitude != null && waypoint.getLatitude() != null) {
                txtLatitude.setText(waypoint.getLatitude().toString());
            }
            if (txtLongitude != null && waypoint.getLongitude() != null) {
                txtLongitude.setText(waypoint.getLongitude().toString());
            }
            if (waypoint.getPlannedArrivalTime() != null) {
                setDateTimeFields(dpPlannedArrival, txtPlannedArrivalTime, waypoint.getPlannedArrivalTime());
            }
            if (waypoint.getActualArrivalTime() != null) {
                setDateTimeFields(dpActualArrival, txtActualArrivalTime, waypoint.getActualArrivalTime());
            }
            if (waypoint.getActualDepartureTime() != null) {
                setDateTimeFields(dpActualDeparture, txtActualDepartureTime, waypoint.getActualDepartureTime());
            }
            if (chkShowAdvanced != null && hasAdvancedData(waypoint)) {
                chkShowAdvanced.setSelected(true);
            }
        }
    }
    private boolean hasAdvancedData(Waypoint wp) {
        return (wp.getLatitude() != null && wp.getLongitude() != null) ||
               wp.getPlannedArrivalTime() != null ||
               wp.getActualArrivalTime() != null ||
               wp.getActualDepartureTime() != null;
    }
    private void setDateTimeFields(DatePicker dp, TextField timeField, LocalDateTime dateTime) {
        if (dp != null && dateTime != null) {
            dp.setValue(dateTime.toLocalDate());
        }
        if (timeField != null && dateTime != null) {
            timeField.setText(dateTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }
    private LocalDateTime getDateTimeFromFields(DatePicker dp, TextField timeField) {
        if (dp == null || dp.getValue() == null) {
            return null;
        }
        LocalDate date = dp.getValue();
        LocalTime time = LocalTime.of(0, 0);
        if (timeField != null && timeField.getText() != null && !timeField.getText().trim().isEmpty()) {
            try {
                time = LocalTime.parse(timeField.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e) {
                logger.warn("Невірний формат часу: {}", timeField.getText());
            }
        }
        return LocalDateTime.of(date, time);
    }

    public void setWaypointForEdit(Waypoint waypoint) {
        setWaypoint(waypoint);
    }

    public Waypoint getWaypoint() {
        return waypoint;
    }

    public boolean isSaved() {
        return saved;
    }

    public boolean validateInput() {
        boolean isValid = true;

        if (txtWaypointAddress.getText() == null || txtWaypointAddress.getText().trim().isEmpty()) {
            showValidationError("Адреса проміжної точки є обов'язковою!");
            txtWaypointAddress.requestFocus();
            return false;
        }

        String stopTimeText = txtStopTime.getText();
        if (stopTimeText != null && !stopTimeText.trim().isEmpty()) {
            try {
                int time = Integer.parseInt(stopTimeText.trim());
                if (time < 0) {
                    showValidationError("Час зупинки не може бути від'ємним!");
                    txtStopTime.requestFocus();
                    return false;
                }
                if (time > 1440) {
                    showValidationError("Час зупинки не може перевищувати 24 години (1440 хвилин)!");
                    txtStopTime.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                showValidationError("Час зупинки повинен бути числом!");
                txtStopTime.requestFocus();
                return false;
            }
        }
        if (txtLatitude != null && !txtLatitude.getText().trim().isEmpty()) {
            try {
                double lat = Double.parseDouble(txtLatitude.getText().trim());
                if (lat < -90 || lat > 90) {
                    showValidationError("Широта повинна бути від -90 до 90!");
                    txtLatitude.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                showValidationError("Широта повинна бути числом!");
                txtLatitude.requestFocus();
                return false;
            }
        }
        if (txtLongitude != null && !txtLongitude.getText().trim().isEmpty()) {
            try {
                double lon = Double.parseDouble(txtLongitude.getText().trim());
                if (lon < -180 || lon > 180) {
                    showValidationError("Довгота повинна бути від -180 до 180!");
                    txtLongitude.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                showValidationError("Довгота повинна бути числом!");
                txtLongitude.requestFocus();
                return false;
            }
        }

        return isValid;
    }

    public void saveWaypoint() {
        if (!validateInput()) {
            return;
        }

        if (waypoint == null) {
            waypoint = new Waypoint();
        }

        waypoint.setAddress(txtWaypointAddress.getText().trim());
        String description = cmbWaypointDescription.getValue();
        if (description == null && cmbWaypointDescription.getEditor() != null) {
            description = cmbWaypointDescription.getEditor().getText();
        }
        waypoint.setDescription(description);
        String stopTimeText = txtStopTime.getText();
        int stopTime = 0;
        if (stopTimeText != null && !stopTimeText.trim().isEmpty()) {
            stopTime = Integer.parseInt(stopTimeText.trim());
        }
        waypoint.setEstimatedStopTime(stopTime);
        waypoint.setNotes(txtWaypointNotes.getText());
        if (txtLatitude != null && !txtLatitude.getText().trim().isEmpty()) {
            waypoint.setLatitude(new BigDecimal(txtLatitude.getText().trim()));
        }
        if (txtLongitude != null && !txtLongitude.getText().trim().isEmpty()) {
            waypoint.setLongitude(new BigDecimal(txtLongitude.getText().trim()));
        }
        waypoint.setPlannedArrivalTime(getDateTimeFromFields(dpPlannedArrival, txtPlannedArrivalTime));
        waypoint.setActualArrivalTime(getDateTimeFromFields(dpActualArrival, txtActualArrivalTime));
        waypoint.setActualDepartureTime(getDateTimeFromFields(dpActualDeparture, txtActualDepartureTime));

        saved = true;

        logger.info(" Збережено проміжну точку: {}", waypoint.getFullDescription());
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Помилка валідації");
        alert.setHeaderText("Некоректні дані");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleOk() {
        saveWaypoint();
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        waypoint = null;
        closeDialog();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            Stage stage = (Stage) txtWaypointAddress.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    public boolean hasChanges() {
        if (waypoint == null) {
            return !txtWaypointAddress.getText().trim().isEmpty() ||
                   cmbWaypointDescription.getValue() != null ||
                   !txtStopTime.getText().trim().equals("0") ||
                   !txtWaypointNotes.getText().trim().isEmpty();
        }

        return !txtWaypointAddress.getText().trim().equals(waypoint.getAddress()) ||
               !java.util.Objects.equals(cmbWaypointDescription.getValue(), waypoint.getDescription()) ||
               !txtStopTime.getText().trim().equals(String.valueOf(waypoint.getEstimatedStopTime())) ||
               !java.util.Objects.equals(txtWaypointNotes.getText(), waypoint.getNotes());
    }
    public void setGoogleMapsService(GoogleMapsService service) {
        this.googleMapsService = service;
        if (btnValidateAddress != null) {
            btnValidateAddress.setDisable(service == null || !service.isConfigured());
            if (service == null || !service.isConfigured()) {
                btnValidateAddress.setTooltip(new Tooltip("Google Maps API не налаштовано"));
            }
        }
    }
    public void setExistingWaypoints(ObservableList<Waypoint> waypoints) {
        this.existingWaypoints = waypoints;
    }
    public void setRouteAddresses(String startAddress, String endAddress) {
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        if (btnCopyFromStart != null) {
            btnCopyFromStart.setDisable(startAddress == null || startAddress.isEmpty());
        }
        if (btnCopyFromEnd != null) {
            btnCopyFromEnd.setDisable(endAddress == null || endAddress.isEmpty());
        }
    }
    @FXML
    private void handleValidateAddress() {
        String address = txtWaypointAddress.getText().trim();
        if (address.isEmpty()) {
            showValidationStatus(false, "Введіть адресу для перевірки", "");
            return;
        }
        if (googleMapsService == null || !googleMapsService.isConfigured()) {
            showValidationStatus(false, "Google Maps API не налаштовано", "");
            return;
        }
        if (isDuplicateAddress(address)) {
            showValidationStatus(false, "Ця адреса вже використовується в маршруті", "");
            addressValidated = false;
            return;
        }
        btnValidateAddress.setDisable(true);
        showValidationStatus(true, "Перевірка адреси...", "");
        Task<Boolean> validationTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return googleMapsService.validateAddress(address);
            }
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    boolean isValid = getValue();
                    addressValidated = isValid;
                    if (isValid) {
                        showValidationStatus(true, " Адреса знайдена та валідна", "");
                        txtWaypointAddress.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 2;");
                        logger.info("Адреса валідна: {}", address);
                    } else {
                        showValidationStatus(false, 
                            " Адреса не знайдена. Спробуйте формат: 'місто, вулиця, будинок' (наприклад: 'Київ, Хрещатик, 1')", 
                            "");
                        txtWaypointAddress.setStyle("-fx-border-color: #FF5722; -fx-border-width: 2;");
                        logger.warn("Адреса не валідна: {}", address);
                    }
                    btnValidateAddress.setDisable(false);
                });
            }
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showValidationStatus(false, " Помилка перевірки адреси. Спробуйте пізніше", "");
                    btnValidateAddress.setDisable(false);
                    logger.error("Помилка валідації адреси: ", getException());
                });
            }
        };
        new Thread(validationTask).start();
    }
    private boolean isDuplicateAddress(String address) {
        if (existingWaypoints == null || address == null) {
            return false;
        }
        String normalizedAddress = normalizeAddress(address);
        for (Waypoint existing : existingWaypoints) {
            if (waypoint != null && existing.getId() == waypoint.getId()) {
                continue;
            }
            if (existing.getAddress() != null) {
                String existingNormalized = normalizeAddress(existing.getAddress());
                if (existingNormalized.equals(normalizedAddress)) {
                    return true;
                }
            }
        }
        return false;
    }
    private String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        return address.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private void showValidationStatus(boolean isSuccess, String message, String icon) {
        if (validationStatusBox != null && lblValidationIcon != null && lblValidationStatus != null) {
            validationStatusBox.setVisible(true);
            validationStatusBox.setManaged(true);
            lblValidationIcon.setText(icon);
            lblValidationStatus.setText(message);
            if (isSuccess) {
                lblValidationStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
            } else {
                lblValidationStatus.setStyle("-fx-text-fill: #FF5722; -fx-font-size: 12px;");
            }
        }
    }
    private void hideValidationStatus() {
        if (validationStatusBox != null) {
            validationStatusBox.setVisible(false);
            validationStatusBox.setManaged(false);
        }
    }
    @FXML
    private void handleCopyFromStart() {
        if (startAddress != null && !startAddress.isEmpty()) {
            txtWaypointAddress.setText(startAddress);
            logger.info("Скопійовано адресу з початку маршруту: {}", startAddress);
        }
    }
    @FXML
    private void handleCopyFromEnd() {
        if (endAddress != null && !endAddress.isEmpty()) {
            txtWaypointAddress.setText(endAddress);
            logger.info("Скопійовано адресу з кінця маршруту: {}", endAddress);
        }
    }
    @FXML
    private void handleClearAll() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Підтвердження");
        confirmAlert.setHeaderText("Очистити всі поля?");
        confirmAlert.setContentText("Всі введені дані буде видалено. Продовжити?");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                txtWaypointAddress.clear();
                cmbWaypointDescription.setValue(null);
                txtStopTime.setText("0");
                txtWaypointNotes.clear();
                if (txtLatitude != null) txtLatitude.clear();
                if (txtLongitude != null) txtLongitude.clear();
                if (dpPlannedArrival != null) dpPlannedArrival.setValue(null);
                if (txtPlannedArrivalTime != null) txtPlannedArrivalTime.clear();
                if (dpActualArrival != null) dpActualArrival.setValue(null);
                if (txtActualArrivalTime != null) txtActualArrivalTime.clear();
                if (dpActualDeparture != null) dpActualDeparture.setValue(null);
                if (txtActualDepartureTime != null) txtActualDepartureTime.clear();
                hideValidationStatus();
                logger.info("Очищено всі поля форми");
            }
        });
    }
    @FXML
    private void handleSaveAsTemplate() {
        String address = txtWaypointAddress.getText().trim();
        String description = cmbWaypointDescription.getValue();
        if (address.isEmpty()) {
            showValidationError("Введіть адресу для збереження шаблону!");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(address.length() > 30 ? address.substring(0, 30) : address);
        dialog.setTitle("Зберегти шаблон");
        dialog.setHeaderText("Введіть назву для шаблону");
        dialog.setContentText("Назва:");
        dialog.showAndWait().ifPresent(name -> {
            if (name != null && !name.trim().isEmpty()) {
                WaypointTemplate template = new WaypointTemplate(
                    name.trim(),
                    address,
                    description,
                    Integer.parseInt(txtStopTime.getText().trim())
                );
                savedTemplates.removeIf(t -> t.getName().equals(name.trim()));
                savedTemplates.add(template);
                updateTemplatesList();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Успіх");
                alert.setHeaderText(null);
                alert.setContentText("Шаблон '" + name.trim() + "' успішно збережено!");
                alert.showAndWait();
                logger.info("Збережено шаблон: {}", name.trim());
            }
        });
    }
    @FXML
    private void handleApplyTemplate() {
        if (cmbTemplates == null) return;
        String selectedName = cmbTemplates.getValue();
        if (selectedName == null || selectedName.isEmpty()) {
            showValidationError("Оберіть шаблон для застосування!");
            return;
        }
        WaypointTemplate template = findTemplateByName(selectedName);
        if (template != null) {
            txtWaypointAddress.setText(template.getAddress());
            cmbWaypointDescription.setValue(template.getDescription());
            txtStopTime.setText(String.valueOf(template.getStopTime()));
            logger.info("Застосовано шаблон: {}", selectedName);
        }
    }
    @FXML
    private void handleDeleteTemplate() {
        if (cmbTemplates == null) return;
        String selectedName = cmbTemplates.getValue();
        if (selectedName == null || selectedName.isEmpty()) {
            showValidationError("Оберіть шаблон для видалення!");
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Підтвердження видалення");
        confirmAlert.setHeaderText("Видалити шаблон?");
        confirmAlert.setContentText("Ви впевнені, що хочете видалити шаблон '" + selectedName + "'?");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                savedTemplates.removeIf(t -> t.getName().equals(selectedName));
                updateTemplatesList();
                cmbTemplates.setValue(null);
                logger.info("Видалено шаблон: {}", selectedName);
            }
        });
    }
    private void updateTemplatesList() {
        if (cmbTemplates != null) {
            List<String> templateNames = new ArrayList<>();
            for (WaypointTemplate template : savedTemplates) {
                templateNames.add(template.getName());
            }
            cmbTemplates.setItems(FXCollections.observableArrayList(templateNames));
        }
    }
    private WaypointTemplate findTemplateByName(String name) {
        for (WaypointTemplate template : savedTemplates) {
            if (template.getName().equals(name)) {
                return template;
            }
        }
        return null;
    }
    private static class WaypointTemplate {
        private final String name;
        private final String address;
        private final String description;
        private final int stopTime;
        public WaypointTemplate(String name, String address, String description, int stopTime) {
            this.name = name;
            this.address = address;
            this.description = description;
            this.stopTime = stopTime;
        }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getDescription() { return description; }
        public int getStopTime() { return stopTime; }
    }
}
