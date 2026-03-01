package org.example.fuelmanagement.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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

public class VehicleCreateDialogController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(VehicleCreateDialogController.class);

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
    private boolean vehicleCreated = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Ініціалізація контролера створення автомобіля");

        try {
            vehicleDAO = new VehicleDAO();

            setupUI();
            loadFuelTypes();

            logger.info("Контролер створення автомобіля успішно ініціалізовано");

        } catch (Exception e) {
            logger.error("Помилка ініціалізації контролера створення: ", e);
            showError("Помилка ініціалізації", "Не вдалося ініціалізувати контролер: " + e.getMessage());
        }
    }

    private void setupUI() {
        cmbStatus.setItems(FXCollections.observableArrayList(
                "Активний", "На ТО", "Неактивний", "Списаний"
        ));
        cmbStatus.setValue("Активний");

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
                    if (!fuelTypes.isEmpty()) {
                        cmbFuelType.setValue(fuelTypes.get(0));
                    }
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

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            Vehicle vehicle = new Vehicle();

            vehicle.setLicensePlate(txtLicensePlate.getText().trim());
            vehicle.setModel(txtModel.getText().trim());
            vehicle.setFuelTypeId(cmbFuelType.getValue().getId());

            String statusText = cmbStatus.getValue();
            VehicleStatus status = switch (statusText) {
                case "Активний" -> VehicleStatus.ACTIVE;
                case "На ТО" -> VehicleStatus.MAINTENANCE;
                case "Неактивний" -> VehicleStatus.INACTIVE;
                case "Списаний" -> VehicleStatus.RETIRED;
                default -> VehicleStatus.ACTIVE;
            };
            vehicle.setStatus(status);

            vehicle.setTankCapacity(new BigDecimal(txtTankCapacity.getText().trim()));
            vehicle.setCurrentOdometer(Integer.parseInt(txtOdometer.getText().trim()));

            vehicle.setCityRateSummer(new BigDecimal(txtCityRateSummer.getText().trim()).setScale(6, java.math.RoundingMode.HALF_UP));
            vehicle.setHighwayRateSummer(new BigDecimal(txtHighwayRateSummer.getText().trim()).setScale(6, java.math.RoundingMode.HALF_UP));
            vehicle.setCityRateWinter(new BigDecimal(txtCityRateWinter.getText().trim()).setScale(6, java.math.RoundingMode.HALF_UP));
            vehicle.setHighwayRateWinter(new BigDecimal(txtHighwayRateWinter.getText().trim()).setScale(6, java.math.RoundingMode.HALF_UP));

            vehicle.setFuelBalance(BigDecimal.ZERO);
            vehicle.setHasRefrigerator(chkHasRefrigerator.isSelected());

            vehicle.setNotes(txtNotes.getText().trim());

            Vehicle createdVehicle = vehicleDAO.create(vehicle);

            if (createdVehicle != null) {
                vehicleCreated = true;
                logger.info("Успішно створено автомобіль: {}", createdVehicle.getDisplayName());
                showInfo("Успіх", "Автомобіль '" + createdVehicle.getDisplayName() + "' успішно додано!");
                closeDialog();
            } else {
                showError("Помилка", "Не вдалося створити автомобіль. Спробуйте ще раз.");
            }

        } catch (NumberFormatException e) {
            logger.error("Помилка валідації числових даних: ", e);
            showError("Помилка введення", "Некоректні числові дані. Перевірте введені значення.");
        } catch (Exception e) {
            logger.error("Помилка створення автомобіля: ", e);
            showError("Помилка", "Не вдалося створити автомобіль:\n" + e.getMessage());
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (txtLicensePlate.getText() == null || txtLicensePlate.getText().trim().isEmpty()) {
            errors.append("• Введіть номерний знак\n");
        }

        if (txtModel.getText() == null || txtModel.getText().trim().isEmpty()) {
            errors.append("• Введіть модель автомобіля\n");
        }

        if (cmbFuelType.getValue() == null) {
            errors.append("• Оберіть тип палива\n");
        }

        if (!isValidDecimal(txtTankCapacity.getText())) {
            errors.append("• Введіть коректну ємність баку\n");
        }

        if (!isValidInteger(txtOdometer.getText())) {
            errors.append("• Введіть коректний пробіг\n");
        }

        if (!isValidDecimal(txtCityRateSummer.getText())) {
            errors.append("• Введіть коректну норму витрат по місту (літо) л/км\n");
        }
        if (!isValidDecimal(txtHighwayRateSummer.getText())) {
            errors.append("• Введіть коректну норму витрат на трасі (літо) л/км\n");
        }
        if (!isValidDecimal(txtCityRateWinter.getText())) {
            errors.append("• Введіть коректну норму витрат по місту (зима) л/км\n");
        }
        if (!isValidDecimal(txtHighwayRateWinter.getText())) {
            errors.append("• Введіть коректну норму витрат на трасі (зима) л/км\n");
        }

        if (errors.length() > 0) {
            showError("Помилка валідації", "Будь ласка, виправте наступні помилки:\n\n" + errors.toString());
            return false;
        }

        return true;
    }

    private boolean isValidDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            BigDecimal decimal = new BigDecimal(value.trim());
            return decimal.compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            int num = Integer.parseInt(value.trim());
            return num >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @FXML
    private void handleCancel() {
        logger.info("Скасовано створення автомобіля");
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    public boolean isVehicleCreated() {
        return vehicleCreated;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
