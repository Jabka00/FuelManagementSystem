package org.example.fuelmanagement.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.fuelmanagement.dao.VehicleDAO;
import org.example.fuelmanagement.model.FuelType;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.enums.VehicleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class VehicleEditDialogController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(VehicleEditDialogController.class);

    @FXML private ComboBox<Vehicle> cmbVehicle;
    @FXML private VBox editPanel;

    @FXML private TextField txtLicensePlate;
    @FXML private TextField txtModel;
    @FXML private ComboBox<FuelType> cmbFuelType;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private TextField txtTankCapacity;
    @FXML private TextField txtOdometer;
    @FXML private TextField txtCityRateSummer;
    @FXML private TextField txtHighwayRateSummer;
    @FXML private TextField txtCityRateWinter;
    @FXML private TextField txtHighwayRateWinter;
    @FXML private TextArea txtNotes;
    @FXML private CheckBox chkHasRefrigerator;

    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblStatus;

    private VehicleDAO vehicleDAO;
    private List<Vehicle> allVehicles;
    private Vehicle selectedVehicle;
    private boolean changesMade = false;
    private Vehicle vehicleToLoad; 
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Ініціалізація контролера редагування автомобілів");

        try {
            vehicleDAO = new VehicleDAO();

            setupUI();
            loadVehicles();
            loadFuelTypes();

            logger.info("Контролер редагування успішно ініціалізовано");

        } catch (Exception e) {
            logger.error("Помилка ініціалізації контролера редагування: ", e);
            showError("Помилка ініціалізації", "Не вдалося ініціалізувати контролер: " + e.getMessage());
        }
    }

    private void setupUI() {
        cmbStatus.setItems(FXCollections.observableArrayList(
                "Активний", "На ТО", "Неактивний", "Списаний"
        ));

        cmbVehicle.setCellFactory(_ -> new ListCell<Vehicle>() {
            @Override
            protected void updateItem(Vehicle vehicle, boolean empty) {
                super.updateItem(vehicle, empty);
                if (empty || vehicle == null) {
                    setText(null);
                } else {
                    setText(vehicle.getDisplayName());
                }
            }
        });

        cmbVehicle.setButtonCell(new ListCell<Vehicle>() {
            @Override
            protected void updateItem(Vehicle vehicle, boolean empty) {
                super.updateItem(vehicle, empty);
                if (empty || vehicle == null) {
                    setText(null);
                } else {
                    setText(vehicle.getDisplayName());
                }
            }
        });

        cmbVehicle.setOnAction(_ -> {
            Vehicle selected = cmbVehicle.getValue();
            if (selected != null) {
                loadVehicleData(selected);
            }
        });

        cmbFuelType.setCellFactory(_ -> new ListCell<FuelType>() {
            @Override
            protected void updateItem(FuelType fuelType, boolean empty) {
                super.updateItem(fuelType, empty);
                if (empty || fuelType == null) {
                    setText(null);
                } else {
                    setText(fuelType.getName());
                }
            }
        });

        cmbFuelType.setButtonCell(new ListCell<FuelType>() {
            @Override
            protected void updateItem(FuelType fuelType, boolean empty) {
                super.updateItem(fuelType, empty);
                if (empty || fuelType == null) {
                    setText(null);
                } else {
                    setText(fuelType.getName());
                }
            }
        });

        txtLicensePlate.textProperty().addListener((_, _, _) -> onFieldChanged());
        txtModel.textProperty().addListener((_, _, _) -> onFieldChanged());
        cmbFuelType.valueProperty().addListener((_, _, _) -> onFieldChanged());
        cmbStatus.valueProperty().addListener((_, _, _) -> onFieldChanged());
        txtTankCapacity.textProperty().addListener((_, _, _) -> onFieldChanged());
        txtOdometer.textProperty().addListener((_, _, _) -> onFieldChanged());
        txtCityRateSummer.textProperty().addListener((_, _, _) -> onFieldChanged());
        txtHighwayRateSummer.textProperty().addListener((_, _, _) -> onFieldChanged());
        txtCityRateWinter.textProperty().addListener((_, _, _) -> onFieldChanged());
        txtHighwayRateWinter.textProperty().addListener((_, _, _) -> onFieldChanged());
        txtNotes.textProperty().addListener((_, _, _) -> onFieldChanged());
        chkHasRefrigerator.selectedProperty().addListener((_, _, _) -> onFieldChanged());
    }

    private void onFieldChanged() {
        changesMade = true;
        btnSave.setDisable(false);
    }

    private void loadVehicles() {
        Task<List<Vehicle>> loadTask = new Task<List<Vehicle>>() {
            @Override
            protected List<Vehicle> call() throws Exception {
                return vehicleDAO.findAllActive();
            }

            @Override
            protected void succeeded() {
                allVehicles = getValue();
                if (allVehicles != null) {
                    cmbVehicle.setItems(FXCollections.observableArrayList(allVehicles));
                    logger.info("Завантажено {} автомобілів", allVehicles.size());
                    if (vehicleToLoad != null) {
                        for (Vehicle v : allVehicles) {
                            if (v.getId() == vehicleToLoad.getId()) {
                                cmbVehicle.setValue(v);
                                loadVehicleData(v);
                                vehicleToLoad = null;
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            protected void failed() {
                logger.error("Помилка завантаження автомобілів: ", getException());
                showError("Помилка", "Не вдалося завантажити список автомобілів: " + getException().getMessage());
            }
        };

        new Thread(loadTask).start();
    }

    private void loadFuelTypes() {
        Task<List<FuelType>> loadTask = new Task<List<FuelType>>() {
            @Override
            protected List<FuelType> call() throws Exception {
                List<FuelType> fuelTypes = new ArrayList<>();
                String sql = "SELECT id, name FROM fuel_types ORDER BY name";

                try (Connection conn = org.example.fuelmanagement.config.DatabaseConfig.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        FuelType fuelType = new FuelType();
                        fuelType.setId(rs.getInt("id"));
                        fuelType.setName(rs.getString("name"));
                        fuelTypes.add(fuelType);
                    }
                }

                return fuelTypes;
            }

            @Override
            protected void succeeded() {
                List<FuelType> fuelTypes = getValue();
                if (fuelTypes != null) {
                    cmbFuelType.setItems(FXCollections.observableArrayList(fuelTypes));
                    logger.info("Завантажено {} типів палива", fuelTypes.size());
                }
            }

            @Override
            protected void failed() {
                logger.error("Помилка завантаження типів палива: ", getException());
                showError("Помилка", "Не вдалося завантажити типи палива: " + getException().getMessage());
            }
        };

        new Thread(loadTask).start();
    }

    private void loadVehicleData(Vehicle vehicle) {
        selectedVehicle = vehicle;
        changesMade = false;

        txtLicensePlate.setText(vehicle.getLicensePlate());
        txtModel.setText(vehicle.getModel());

        for (FuelType fuelType : cmbFuelType.getItems()) {
            if (fuelType.getId() == vehicle.getFuelTypeId()) {
                cmbFuelType.setValue(fuelType);
                break;
            }
        }

        cmbStatus.setValue(vehicle.getStatus().toString());

        txtTankCapacity.setText(vehicle.getTankCapacity() != null ? 
                vehicle.getTankCapacity().toString() : "");
        txtOdometer.setText(String.valueOf(vehicle.getCurrentOdometer()));

        txtCityRateSummer.setText(vehicle.getCityRateSummer() != null ? 
                vehicle.getCityRateSummer().toString() : "");
        txtHighwayRateSummer.setText(vehicle.getHighwayRateSummer() != null ? 
                vehicle.getHighwayRateSummer().toString() : "");
        txtCityRateWinter.setText(vehicle.getCityRateWinter() != null ? 
                vehicle.getCityRateWinter().toString() : "");
        txtHighwayRateWinter.setText(vehicle.getHighwayRateWinter() != null ? 
                vehicle.getHighwayRateWinter().toString() : "");

        txtNotes.setText(vehicle.getNotes() != null ? vehicle.getNotes() : "");
        chkHasRefrigerator.setSelected(vehicle.hasRefrigerator());

        editPanel.setVisible(true);
        editPanel.setManaged(true);
    }

    public void setVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        vehicleToLoad = vehicle;
        if (cmbVehicle.getItems() != null && !cmbVehicle.getItems().isEmpty()) {
            for (Vehicle v : cmbVehicle.getItems()) {
                if (v.getId() == vehicle.getId()) {
                    cmbVehicle.setValue(v);
                    loadVehicleData(v);
                    vehicleToLoad = null;
                    break;
                }
            }
        }
    }

    @FXML
    private void handleSave() {
        if (selectedVehicle == null) {
            showError("Помилка", "Оберіть автомобіль для редагування");
            return;
        }

        if (!validateInput()) {
            return;
        }

        btnSave.setDisable(true);
        btnCancel.setDisable(true);
        lblStatus.setText("Збереження...");
        lblStatus.setStyle("-fx-text-fill: #238A90;");

        Task<Boolean> saveTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    String licensePlate = txtLicensePlate.getText().trim();
                    String model = txtModel.getText().trim();
                    FuelType fuelType = cmbFuelType.getValue();
                    String statusStr = cmbStatus.getValue();

                    VehicleStatus status = switch (statusStr) {
                        case "Активний" -> VehicleStatus.ACTIVE;
                        case "На ТО" -> VehicleStatus.MAINTENANCE;
                        case "Неактивний" -> VehicleStatus.INACTIVE;
                        case "Списаний" -> VehicleStatus.RETIRED;
                        default -> VehicleStatus.ACTIVE;
                    };

                    BigDecimal tankCapacity = new BigDecimal(txtTankCapacity.getText().trim());
                    int odometer = Integer.parseInt(txtOdometer.getText().trim());

                    BigDecimal cityRateSummer = new BigDecimal(txtCityRateSummer.getText().trim()).setScale(6, java.math.RoundingMode.HALF_UP);
                    BigDecimal highwayRateSummer = new BigDecimal(txtHighwayRateSummer.getText().trim()).setScale(6, java.math.RoundingMode.HALF_UP);
                    BigDecimal cityRateWinter = new BigDecimal(txtCityRateWinter.getText().trim()).setScale(6, java.math.RoundingMode.HALF_UP);
                    BigDecimal highwayRateWinter = new BigDecimal(txtHighwayRateWinter.getText().trim()).setScale(6, java.math.RoundingMode.HALF_UP);

                    String notes = txtNotes.getText().trim();

                    String sql = """
                        UPDATE vehicles 
                        SET license_plate = ?, model = ?, fuel_type_id = ?, status = ?,
                            tank_capacity = ?, current_odometer = ?,
                            city_rate_summer = ?, highway_rate_summer = ?,
                            city_rate_winter = ?, highway_rate_winter = ?,
                            has_refrigerator = ?, notes = ?, updated_at = NOW()
                        WHERE id = ?
                        """;

                    try (Connection conn = org.example.fuelmanagement.config.DatabaseConfig.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(sql)) {

                        stmt.setString(1, licensePlate);
                        stmt.setString(2, model);
                        stmt.setInt(3, fuelType.getId());
                        stmt.setString(4, status.getValue());
                        stmt.setBigDecimal(5, tankCapacity);
                        stmt.setInt(6, odometer);
                        stmt.setBigDecimal(7, cityRateSummer);
                        stmt.setBigDecimal(8, highwayRateSummer);
                        stmt.setBigDecimal(9, cityRateWinter);
                        stmt.setBigDecimal(10, highwayRateWinter);
                        stmt.setBoolean(11, chkHasRefrigerator.isSelected());
                        stmt.setString(12, notes.isEmpty() ? null : notes);
                        stmt.setInt(13, selectedVehicle.getId());

                        int rowsUpdated = stmt.executeUpdate();

                        if (rowsUpdated > 0) {
                            logger.info("Оновлено автомобіль ID {}: {}", selectedVehicle.getId(), licensePlate);
                            return true;
                        }
                    }

                    return false;

                } catch (Exception e) {
                    logger.error("Помилка збереження автомобіля: ", e);
                    throw e;
                }
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    if (getValue()) {
                        lblStatus.setText(" Збережено успішно!");
                        lblStatus.setStyle("-fx-text-fill: #34C759; -fx-font-weight: bold;");

                        showInfo("Успіх", "Дані автомобіля успішно оновлено!");

                        changesMade = false;
                        btnSave.setDisable(true);
                        btnCancel.setDisable(false);

                        loadVehicles();
                    } else {
                        lblStatus.setText(" Помилка збереження");
                        lblStatus.setStyle("-fx-text-fill: #FF3B30;");
                        btnSave.setDisable(false);
                        btnCancel.setDisable(false);
                        showError("Помилка", "Не вдалося оновити дані автомобіля");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    lblStatus.setText(" Помилка збереження");
                    lblStatus.setStyle("-fx-text-fill: #FF3B30;");
                    btnSave.setDisable(false);
                    btnCancel.setDisable(false);
                    logger.error("Помилка збереження: ", getException());
                    showError("Помилка", "Не вдалося зберегти: " + getException().getMessage());
                });
            }
        };

        new Thread(saveTask).start();
    }

    @FXML
    private void handleCancel() {
        if (changesMade) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Підтвердження");
            alert.setHeaderText("Незбережені зміни");
            alert.setContentText("У вас є незбережені зміни. Ви впевнені, що хочете закрити вікно?");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private boolean validateInput() {
        if (txtLicensePlate.getText().trim().isEmpty()) {
            showError("Помилка валідації", "Введіть номерний знак");
            txtLicensePlate.requestFocus();
            return false;
        }

        if (txtModel.getText().trim().isEmpty()) {
            showError("Помилка валідації", "Введіть модель автомобіля");
            txtModel.requestFocus();
            return false;
        }

        if (cmbFuelType.getValue() == null) {
            showError("Помилка валідації", "Оберіть тип палива");
            cmbFuelType.requestFocus();
            return false;
        }

        if (cmbStatus.getValue() == null) {
            showError("Помилка валідації", "Оберіть статус");
            cmbStatus.requestFocus();
            return false;
        }

        try {
            BigDecimal tankCapacity = new BigDecimal(txtTankCapacity.getText().trim());
            if (tankCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Помилка валідації", "Ємність бака повинна бути більше 0");
                txtTankCapacity.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Помилка валідації", "Невірний формат ємності бака. Введіть число (наприклад: 50.0)");
            txtTankCapacity.requestFocus();
            return false;
        }

        try {
            int odometer = Integer.parseInt(txtOdometer.getText().trim());
            if (odometer < 0) {
                showError("Помилка валідації", "Одометр не може бути від'ємним");
                txtOdometer.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Помилка валідації", "Невірний формат одометра. Введіть ціле число (наприклад: 100000)");
            txtOdometer.requestFocus();
            return false;
        }

        try {
            BigDecimal cityRateSummer = new BigDecimal(txtCityRateSummer.getText().trim());
            if (cityRateSummer.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Помилка валідації", "Норма витрат (місто, літо) л/км повинна бути більше 0");
                txtCityRateSummer.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Помилка валідації", "Невірний формат норми витрат (місто, літо) л/км");
            txtCityRateSummer.requestFocus();
            return false;
        }

        try {
            BigDecimal highwayRateSummer = new BigDecimal(txtHighwayRateSummer.getText().trim());
            if (highwayRateSummer.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Помилка валідації", "Норма витрат (траса, літо) л/км повинна бути більше 0");
                txtHighwayRateSummer.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Помилка валідації", "Невірний формат норми витрат (траса, літо) л/км");
            txtHighwayRateSummer.requestFocus();
            return false;
        }

        try {
            BigDecimal cityRateWinter = new BigDecimal(txtCityRateWinter.getText().trim());
            if (cityRateWinter.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Помилка валідації", "Норма витрат (місто, зима) л/км повинна бути більше 0");
                txtCityRateWinter.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Помилка валідації", "Невірний формат норми витрат (місто, зима) л/км");
            txtCityRateWinter.requestFocus();
            return false;
        }

        try {
            BigDecimal highwayRateWinter = new BigDecimal(txtHighwayRateWinter.getText().trim());
            if (highwayRateWinter.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Помилка валідації", "Норма витрат (траса, зима) л/км повинна бути більше 0");
                txtHighwayRateWinter.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Помилка валідації", "Невірний формат норми витрат (траса, зима) л/км");
            txtHighwayRateWinter.requestFocus();
            return false;
        }

        return true;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
