package org.example.fuelmanagement.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.fuelmanagement.dao.DriverDAO;
import org.example.fuelmanagement.dao.TripDAO;
import org.example.fuelmanagement.dao.VehicleDAO;
import org.example.fuelmanagement.model.Driver;
import org.example.fuelmanagement.model.Trip;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.enums.Season;
import org.example.fuelmanagement.model.enums.TripStatus;
import org.example.fuelmanagement.model.enums.TripType;
import org.example.fuelmanagement.util.FuelCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class SpecialTripDialogController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SpecialTripDialogController.class);

    @FXML private CheckBox chkFactoryGround;
    @FXML private CheckBox chkIdleWork;
    @FXML private ComboBox<Vehicle> cmbVehicle;
    @FXML private ComboBox<Driver> cmbDriver;
    @FXML private VBox pnlFactoryGround;
    @FXML private TextField txtDistance;
    @FXML private VBox pnlIdleWork;
    @FXML private TextField txtIdleMinutes;
    @FXML private VBox pnlTotalTime;
    @FXML private TextField txtTotalTimeMinutes;
    @FXML private TextArea txtNotes;
    @FXML private TextArea txtFuelCalculation;
    @FXML private Button btnCalculateFuel;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private VehicleDAO vehicleDAO;
    private DriverDAO driverDAO;
    private TripDAO tripDAO;
    private Trip createdTrip;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.vehicleDAO = new VehicleDAO();
        this.driverDAO = new DriverDAO();
        this.tripDAO = new TripDAO();
        setupCheckBoxListeners();
        setupInputValidation();
        loadVehicles();
        loadDrivers();
        updatePanelVisibility();
    }

    private void setupCheckBoxListeners() {
        chkFactoryGround.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updatePanelVisibility();
            txtFuelCalculation.clear();
        });
        chkIdleWork.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updatePanelVisibility();
            txtFuelCalculation.clear();
        });
    }
    private void updatePanelVisibility() {
        boolean showFactory = chkFactoryGround.isSelected();
        boolean showIdle = chkIdleWork.isSelected();
        boolean showBoth = showFactory && showIdle;
        pnlFactoryGround.setVisible(showFactory);
        pnlFactoryGround.setManaged(showFactory);
        pnlIdleWork.setVisible(showIdle);
        pnlIdleWork.setManaged(showIdle);
        pnlTotalTime.setVisible(showBoth);
        pnlTotalTime.setManaged(showBoth);
    }

    private void setupInputValidation() {
        txtDistance.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                txtDistance.setText(oldVal);
            }
        });

        txtIdleMinutes.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtIdleMinutes.setText(oldVal);
            }
        });
        txtTotalTimeMinutes.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtTotalTimeMinutes.setText(oldVal);
            }
        });
    }

    private void loadVehicles() {
        try {
            List<Vehicle> vehicles = vehicleDAO.findAllActive();
            cmbVehicle.getItems().clear();
            cmbVehicle.getItems().addAll(vehicles);
        } catch (Exception e) {
            logger.error("Помилка завантаження автомобілів", e);
            showError("Не вдалося завантажити список автомобілів");
        }
    }

    private void loadDrivers() {
        try {
            List<Driver> drivers = driverDAO.findAllActive();
            cmbDriver.getItems().clear();
            cmbDriver.getItems().addAll(drivers);
        } catch (Exception e) {
            logger.error("Помилка завантаження водіїв", e);
            showError("Не вдалося завантажити список водіїв");
        }
    }

    @FXML
    private void handleCalculateFuel() {
        Vehicle vehicle = cmbVehicle.getValue();
        if (vehicle == null) {
            showError("Будь ласка, виберіть автомобіль");
            return;
        }
        if (!chkFactoryGround.isSelected() && !chkIdleWork.isSelected()) {
            showError("Будь ласка, виберіть хоча б один тип роботи");
            return;
        }

        boolean isSummer = Season.getCurrentSeason() == Season.SUMMER;
        BigDecimal consumption = BigDecimal.ZERO;
        StringBuilder report = new StringBuilder();

        try {
            boolean hasFactory = chkFactoryGround.isSelected();
            boolean hasIdle = chkIdleWork.isSelected();
            BigDecimal distance = BigDecimal.ZERO;
            int idleMinutes = 0;
            BigDecimal idleHours = BigDecimal.ZERO;
            BigDecimal factoryConsumption = BigDecimal.ZERO;
            BigDecimal idleConsumption = BigDecimal.ZERO;

            if (hasFactory) {
                String distanceStr = txtDistance.getText().trim();
                if (distanceStr.isEmpty()) {
                    showError("Будь ласка, введіть відстань для роботи на території");
                    return;
                }
                distance = new BigDecimal(distanceStr);
                factoryConsumption = FuelCalculator.calculateFactoryGroundConsumption(vehicle, distance, isSummer);
            }

            if (hasIdle) {
                String minutesStr = txtIdleMinutes.getText().trim();
                if (minutesStr.isEmpty()) {
                    showError("Будь ласка, введіть час роботи на холостому ході");
                    return;
                }
                idleMinutes = Integer.parseInt(minutesStr);
                idleHours = BigDecimal.valueOf(idleMinutes).divide(BigDecimal.valueOf(60), 4, java.math.RoundingMode.HALF_UP);
                idleConsumption = FuelCalculator.calculateIdleWorkConsumption(vehicle, idleHours, isSummer);
            }

            consumption = factoryConsumption.add(idleConsumption);

            if (hasFactory && hasIdle) {
                report.append(" Комбінована робота\n");
            } else if (hasFactory) {
                report.append("🏭 Робота на території заводу\n");
            } else {
                report.append("⏱ Робота на холостому ході\n");
            }
            report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            report.append(" Автомобіль: ").append(vehicle.getDisplayName()).append("\n");
            if (hasFactory) {
                report.append(" Відстань: ").append(distance).append(" км\n");
                report.append("   💧 Витрата (рух): ").append(factoryConsumption.setScale(2, java.math.RoundingMode.HALF_UP)).append(" л\n");
            }
            if (hasIdle) {
                report.append("⏰ Час на холостому ході: ").append(idleMinutes).append(" хв (").append(idleHours.setScale(2, java.math.RoundingMode.HALF_UP)).append(" год)\n");
                report.append("   💧 Витрата (холостий хід): ").append(idleConsumption.setScale(2, java.math.RoundingMode.HALF_UP)).append(" л\n");
            }
            report.append(" Поточний баланс: ").append(vehicle.getFuelBalance()).append(" л\n");
            report.append("\n💧 ЗАГАЛЬНА ВИТРАТА: ").append(consumption.setScale(2, java.math.RoundingMode.HALF_UP)).append(" л\n");

            if (FuelCalculator.isEnoughFuel(vehicle, consumption)) {
                BigDecimal remaining = vehicle.getFuelBalance().subtract(consumption);
                report.append("\n Палива достатньо\n");
                report.append(" Залишиться: ").append(remaining.setScale(2, java.math.RoundingMode.HALF_UP)).append(" л");
            } else {
                BigDecimal shortage = FuelCalculator.calculateFuelToAdd(vehicle, consumption);
                report.append("\n Недостатньо палива!\n");
                report.append(" Потрібно дозаправити: ").append(shortage.setScale(2, java.math.RoundingMode.HALF_UP)).append(" л");
            }

            txtFuelCalculation.setText(report.toString());

        } catch (NumberFormatException e) {
            showError("Невірний формат числа");
        } catch (Exception e) {
            logger.error("Помилка розрахунку витрат палива", e);
            showError("Помилка розрахунку: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            Trip trip = new Trip();
            Vehicle vehicle = cmbVehicle.getValue();
            Driver driver = cmbDriver.getValue();

            trip.setVehicleId(vehicle.getId());
            if (driver != null) {
                trip.setDriverId(driver.getId());
            }

            boolean isSummer = Season.getCurrentSeason() == Season.SUMMER;
            trip.setSeason(isSummer ? Season.SUMMER : Season.WINTER);
            LocalDateTime now = LocalDateTime.now();
            trip.setPlannedStartTime(now);
            trip.setActualStartTime(now);

            boolean hasFactory = chkFactoryGround.isSelected();
            boolean hasIdle = chkIdleWork.isSelected();
            BigDecimal distance = BigDecimal.ZERO;
            int idleMinutes = 0;
            BigDecimal idleHours = BigDecimal.ZERO;
            int totalMinutes = 0;

            if (hasFactory && hasIdle) {
                trip.setTripType(TripType.FACTORY_GROUND_WITH_IDLE);
                distance = new BigDecimal(txtDistance.getText().trim());
                idleMinutes = Integer.parseInt(txtIdleMinutes.getText().trim());
                idleHours = BigDecimal.valueOf(idleMinutes).divide(BigDecimal.valueOf(60), 4, java.math.RoundingMode.HALF_UP);
                if (!txtTotalTimeMinutes.getText().trim().isEmpty()) {
                    totalMinutes = Integer.parseInt(txtTotalTimeMinutes.getText().trim());
                } else {
                    totalMinutes = idleMinutes;
                }
                trip.setPlannedDistance(distance);
                trip.setPlannedCityKm(distance);
                trip.setPlannedHighwayKm(BigDecimal.ZERO);
                trip.setWaitingTime(totalMinutes);
                trip.setPlannedEndTime(now.plusMinutes(totalMinutes));
                trip.setStartAddress("Територія заводу + холостий хід");
                trip.setEndAddress("Територія заводу + холостий хід");
                BigDecimal consumption = FuelCalculator.calculateCombinedConsumption(vehicle, distance, idleHours, isSummer);
                trip.setPlannedFuelConsumption(consumption);
            } else if (hasFactory) {
                trip.setTripType(TripType.FACTORY_GROUND);
                distance = new BigDecimal(txtDistance.getText().trim());
                trip.setPlannedDistance(distance);
                trip.setPlannedCityKm(distance);
                trip.setPlannedHighwayKm(BigDecimal.ZERO);
                if (!txtTotalTimeMinutes.getText().trim().isEmpty()) {
                    totalMinutes = Integer.parseInt(txtTotalTimeMinutes.getText().trim());
                } else {
                    totalMinutes = 60; 
                }
                trip.setWaitingTime(totalMinutes);
                trip.setPlannedEndTime(now.plusMinutes(totalMinutes));
                trip.setStartAddress("Територія заводу");
                trip.setEndAddress("Територія заводу");
                BigDecimal consumption = FuelCalculator.calculateFactoryGroundConsumption(vehicle, distance, isSummer);
                trip.setPlannedFuelConsumption(consumption);
            } else if (hasIdle) {
                trip.setTripType(TripType.IDLE_WORK);
                idleMinutes = Integer.parseInt(txtIdleMinutes.getText().trim());
                idleHours = BigDecimal.valueOf(idleMinutes).divide(BigDecimal.valueOf(60), 4, java.math.RoundingMode.HALF_UP);
                totalMinutes = idleMinutes;
                trip.setWaitingTime(totalMinutes);
                trip.setPlannedEndTime(now.plusMinutes(totalMinutes));
                trip.setPlannedDistance(BigDecimal.ZERO);
                trip.setPlannedCityKm(BigDecimal.ZERO);
                trip.setPlannedHighwayKm(BigDecimal.ZERO);
                trip.setStartAddress("На місці");
                trip.setEndAddress("На місці");
                BigDecimal consumption = FuelCalculator.calculateIdleWorkConsumption(vehicle, idleHours, isSummer);
                trip.setPlannedFuelConsumption(consumption);
            }

            trip.setPurpose("Спеціальна робота: " + trip.getTripType().toString());
            if (txtNotes.getText() != null && !txtNotes.getText().trim().isEmpty()) {
                trip.setNotes(txtNotes.getText().trim());
            }

            trip.setRequesterName("Спеціальна робота");
            trip.setRequesterEmail("special@internal.local");
            trip.setRequesterPhone("");

            trip.setStatus(TripStatus.CREATED);
            trip.setCreatedAt(now);

            Trip savedTrip = tripDAO.create(trip);
            if (savedTrip != null) {
                trip.setId(savedTrip.getId());
                logger.info("Створено спеціальну поїздку #{} типу {}", savedTrip.getId(), trip.getTripType());
            } else {
                throw new Exception("Не вдалося зберегти поїздку в БД");
            }
            this.createdTrip = trip;
            closeDialog();

        } catch (Exception e) {
            logger.error("Помилка збереження поїздки", e);
            showError("Не вдалося створити поїздку: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private boolean validateInput() {
        if (cmbVehicle.getValue() == null) {
            showError("Будь ласка, виберіть автомобіль");
            return false;
        }

        if (!chkFactoryGround.isSelected() && !chkIdleWork.isSelected()) {
            showError("Будь ласка, виберіть хоча б один тип роботи");
            return false;
        }

        try {
            if (chkFactoryGround.isSelected()) {
                if (txtDistance.getText().trim().isEmpty()) {
                    showError("Будь ласка, введіть відстань");
                    return false;
                }
                BigDecimal distance = new BigDecimal(txtDistance.getText().trim());
                if (distance.compareTo(BigDecimal.ZERO) <= 0) {
                    showError("Відстань повинна бути більше 0");
                    return false;
                }
            }

            if (chkIdleWork.isSelected()) {
                if (txtIdleMinutes.getText().trim().isEmpty()) {
                    showError("Будь ласка, введіть час роботи на холостому ході");
                    return false;
                }
                int minutes = Integer.parseInt(txtIdleMinutes.getText().trim());
                if (minutes <= 0) {
                    showError("Час роботи повинен бути більше 0");
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            showError("Невірний формат числа");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Помилка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    public Trip getCreatedTrip() {
        return createdTrip;
    }
}
