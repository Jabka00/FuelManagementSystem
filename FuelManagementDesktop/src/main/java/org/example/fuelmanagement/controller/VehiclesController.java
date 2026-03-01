package org.example.fuelmanagement.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.fuelmanagement.dao.VehicleDAO;
import org.example.fuelmanagement.dao.VehicleDAO.VehicleWithStats;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.enums.VehicleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class VehiclesController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(VehiclesController.class);

    @FXML private Button btnBack;
    @FXML private Button btnRefresh;
    @FXML private Button btnEdit;
    @FXML private Button btnAdd;
    @FXML private Button btnDelete;
    @FXML private Label lblTotalVehicles;
    @FXML private Label lblActiveVehicles;
    @FXML private Label lblMaintenanceVehicles;
    @FXML private Label lblTotalTrips;
    @FXML private Label lblInfo;

    @FXML private TableView<VehicleWithStats> tableVehicles;
    @FXML private TableColumn<VehicleWithStats, String> colLicensePlate;
    @FXML private TableColumn<VehicleWithStats, String> colModel;
    @FXML private TableColumn<VehicleWithStats, String> colFuelType;
    @FXML private TableColumn<VehicleWithStats, String> colFuelBalance;
    @FXML private TableColumn<VehicleWithStats, String> colTankCapacity;
    @FXML private TableColumn<VehicleWithStats, String> colOdometer;
    @FXML private TableColumn<VehicleWithStats, String> colStatus;
    @FXML private TableColumn<VehicleWithStats, Integer> colTripCount;
    @FXML private TableColumn<VehicleWithStats, String> colAvgConsumption;

    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ObservableList<VehicleWithStats> vehiclesList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Ініціалізація екрану автопарку");
        setupTableColumns();
        loadVehicles();
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
        tableVehicles.getSelectionModel().selectedItemProperty().addListener((_, _, newSelection) -> {
            boolean hasSelection = newSelection != null;
            btnEdit.setDisable(!hasSelection);
            btnDelete.setDisable(!hasSelection);
        });
    }

    private void setupTableColumns() {
        colLicensePlate.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getVehicle().getLicensePlate()));
        colLicensePlate.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        colModel.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getVehicle().getModel()));
        colModel.setStyle("-fx-alignment: CENTER-LEFT;");

        colFuelType.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getVehicle().getFuelTypeName()));
        colFuelType.setStyle("-fx-alignment: CENTER;");

        colFuelBalance.setCellValueFactory(data -> {
            Vehicle v = data.getValue().getVehicle();
            BigDecimal balance = v.getFuelBalance() != null ? v.getFuelBalance() : BigDecimal.ZERO;
            return new SimpleStringProperty(String.format("%.1f л", balance.doubleValue()));
        });
        colFuelBalance.setStyle("-fx-alignment: CENTER;");
        colFuelBalance.setCellFactory(_ -> new TableCell<VehicleWithStats, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    VehicleWithStats vws = getTableView().getItems().get(getIndex());
                    Vehicle v = vws.getVehicle();
                    if (getTableRow() != null && getTableRow().isSelected()) {
                        setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else if (v.isFuelCritical()) {
                        setStyle("-fx-text-fill: rgb(0, 0, 0); -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else if (v.needsRefueling()) {
                        setStyle("-fx-text-fill: rgb(0, 0, 0); -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: rgb(0, 0, 0); -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        colTankCapacity.setCellValueFactory(data -> {
            BigDecimal capacity = data.getValue().getVehicle().getTankCapacity();
            return new SimpleStringProperty(capacity != null ? 
                String.format("%.1f л", capacity.doubleValue()) : "—");
        });
        colTankCapacity.setStyle("-fx-alignment: CENTER;");

        colOdometer.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("%,d", data.getValue().getVehicle().getCurrentOdometer())));
        colOdometer.setStyle("-fx-alignment: CENTER;");

        colStatus.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getVehicle().getStatus().toString()));
        colStatus.setStyle("-fx-alignment: CENTER;");
        colStatus.setCellFactory(_ -> new TableCell<VehicleWithStats, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    VehicleWithStats vws = getTableView().getItems().get(getIndex());
                    VehicleStatus status = vws.getVehicle().getStatus();
                    if (getTableRow() != null && getTableRow().isSelected()) {
                        setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        switch (status) {
                            case ACTIVE:
                                setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold; -fx-alignment: CENTER;");
                                break;
                            case MAINTENANCE:
                                setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold; -fx-alignment: CENTER;");
                                break;
                            case INACTIVE:
                                setStyle("-fx-text-fill: #95A5A6; -fx-alignment: CENTER;");
                                break;
                            case RETIRED:
                                setStyle("-fx-text-fill: #E74C3C; -fx-alignment: CENTER;");
                                break;
                        }
                    }
                }
            }
        });

        colTripCount.setCellValueFactory(new PropertyValueFactory<>("tripCount"));
        colTripCount.setStyle("-fx-alignment: CENTER;");

        colAvgConsumption.setCellValueFactory(data -> {
            BigDecimal avgConsumption = data.getValue().getAverageConsumption();
            if (avgConsumption.compareTo(BigDecimal.ZERO) > 0) {
                return new SimpleStringProperty(
                    String.format("%.2f л/100км", avgConsumption.setScale(2, RoundingMode.HALF_UP).doubleValue()));
            }
            return new SimpleStringProperty("—");
        });
        colAvgConsumption.setStyle("-fx-alignment: CENTER;");

        tableVehicles.setItems(vehiclesList);
        tableVehicles.setRowFactory(_ -> new TableRow<VehicleWithStats>() {
            @Override
            protected void updateItem(VehicleWithStats item, boolean empty) {
                super.updateItem(item, empty);
                updateRowStyle();
            }
            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                updateRowStyle();
            }
            private void updateRowStyle() {
                if (isEmpty() || getItem() == null) {
                    setStyle("");
                } else if (isSelected()) {
                    setStyle("-fx-background-color: #238A90; " +
                            "-fx-text-fill: black;");
                } else {
                    if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: rgba(174, 233, 237, 0.15); " +
                                "-fx-text-fill: black;");
                    } else {
                        setStyle("-fx-background-color: white; " +
                                "-fx-text-fill: black;");
                    }
                }
            }
        });
    }

    private void loadVehicles() {
        try {
            logger.info("Завантаження даних автопарку");
            List<VehicleWithStats> vehicles = vehicleDAO.findAllWithStatistics();
            vehiclesList.clear();
            vehiclesList.addAll(vehicles);

            int totalVehicles = vehicles.size();
            long activeCount = vehicles.stream()
                .filter(v -> v.getVehicle().getStatus() == VehicleStatus.ACTIVE)
                .count();
            long maintenanceCount = vehicles.stream()
                .filter(v -> v.getVehicle().getStatus() == VehicleStatus.MAINTENANCE)
                .count();
            int totalTrips = vehicles.stream()
                .mapToInt(VehicleWithStats::getTripCount)
                .sum();

            lblTotalVehicles.setText(String.valueOf(totalVehicles));
            lblActiveVehicles.setText(String.valueOf(activeCount));
            lblMaintenanceVehicles.setText(String.valueOf(maintenanceCount));
            lblTotalTrips.setText(String.valueOf(totalTrips));

            lblInfo.setText(String.format("Завантажено %d автомобілів • Оновлено: %s", 
                totalVehicles, 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))));

            logger.info("Завантажено {} автомобілів (Активних: {}, На ТО: {}, Поїздок: {})", 
                totalVehicles, activeCount, maintenanceCount, totalTrips);

        } catch (Exception e) {
            logger.error("Помилка завантаження даних автопарку: ", e);
            showError("Помилка", "Не вдалося завантажити дані автопарку:\n" + e.getMessage());
            lblInfo.setText("Помилка завантаження даних");
        }
    }

    @FXML
    private void handleRefresh() {
        logger.info("Оновлення даних автопарку");
        loadVehicles();
    }

    @FXML
    private void handleAddVehicle() {
        try {
            logger.info("Відкриття діалогу додавання автомобіля");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/vehicle-create-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            VehicleCreateDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Додати новий автомобіль");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(btnBack.getScene().getWindow());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

            if (controller.isVehicleCreated()) {
                loadVehicles();
            }

        } catch (Exception e) {
            logger.error("Помилка відкриття діалогу додавання автомобіля: ", e);
            showError("Помилка", "Не вдалося відкрити діалог додавання автомобіля:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleEditSelected() {
        VehicleWithStats selectedVehicle = tableVehicles.getSelectionModel().getSelectedItem();
        if (selectedVehicle == null) {
            return;
        }
        handleEditVehicle(selectedVehicle.getVehicle());
    }

    @FXML
    private void handleDeleteSelected() {
        VehicleWithStats selectedVehicle = tableVehicles.getSelectionModel().getSelectedItem();
        if (selectedVehicle == null) {
            return;
        }

        Vehicle vehicle = selectedVehicle.getVehicle();
        int tripCount = selectedVehicle.getTripCount();
        Alert confirmDialog = new Alert(Alert.AlertType.WARNING);
        confirmDialog.setTitle("Підтвердження видалення");
        confirmDialog.setHeaderText("УВАГА! Видалення автомобіля");
        String contentText = "Ви впевнені, що хочете ПОВНІСТЮ видалити автомобіль?\n\n" +
            "Номер: " + vehicle.getLicensePlate() + "\n" +
            "Модель: " + vehicle.getModel() + "\n";
        if (tripCount > 0) {
            contentText += "Пов'язаних поїздок: " + tripCount + "\n\n";
        }
        contentText += "УВАГА: Автомобіль буде НАЗАВЖДИ видалено з бази даних!\n" +
            "Якщо у автомобіля є поїздки, вони ТАКОЖ будуть видалені!\n\n" +
            "Ця дія є НЕЗВОРОТНОЮ!";
        confirmDialog.setContentText(contentText);

        ButtonType btnYes = new ButtonType("Так, видалити", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(btnYes, btnNo);

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == btnYes) {
                deleteVehicle(vehicle);
            }
        });
    }

    private void handleEditVehicle(Vehicle vehicle) {
        try {
            logger.info("Відкриття редагування автомобіля: {}", vehicle.getDisplayName());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/vehicle-edit-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            VehicleEditDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Редагування автомобіля - " + vehicle.getDisplayName());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(btnBack.getScene().getWindow());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            dialogStage.setOnShown(_ -> controller.setVehicle(vehicle));

            dialogStage.showAndWait();

            loadVehicles();

        } catch (Exception e) {
            logger.error("Помилка відкриття редагування автомобіля: ", e);
            showError("Помилка", "Не вдалося відкрити редагування автомобіля:\n" + e.getMessage());
        }
    }

    private void deleteVehicle(Vehicle vehicle) {
        try {
            logger.info("Видалення автомобіля: {}", vehicle.getDisplayName());

            boolean success = vehicleDAO.delete(vehicle.getId());

            if (success) {
                logger.info("Автомобіль {} успішно видалено з бази даних", vehicle.getDisplayName());
                showInfo("Успішно видалено", "Автомобіль '" + vehicle.getDisplayName() + "' успішно видалено з бази даних!");
                loadVehicles();
            } else {
                showError("Помилка", "Не вдалося видалити автомобіль. Спробуйте ще раз.");
            }

        } catch (Exception e) {
            logger.error("Помилка видалення автомобіля: ", e);
            showError("Помилка", "Не вдалося видалити автомобіль:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleBackToMenu() {
        try {
            logger.info("Повернення до головного меню");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/menu.fxml"));
            Scene scene = new Scene(loader.load());

            Stage currentStage = (Stage) btnBack.getScene().getWindow();
            currentStage.setScene(scene);
            currentStage.setTitle("Головне меню - Система обліку палива");
            currentStage.setWidth(1200);
            currentStage.setHeight(800);
            currentStage.setMinWidth(1000);
            currentStage.setMinHeight(700);

        } catch (Exception e) {
            logger.error("Помилка повернення до меню: ", e);
            showError("Помилка", "Не вдалося повернутись до меню:\n" + e.getMessage());
        }
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
