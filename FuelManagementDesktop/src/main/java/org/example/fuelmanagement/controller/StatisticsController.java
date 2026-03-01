package org.example.fuelmanagement.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.fuelmanagement.dao.TripDAO;
import org.example.fuelmanagement.dao.VehicleDAO;
import org.example.fuelmanagement.model.Trip;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.enums.TripStatus;
import org.example.fuelmanagement.model.enums.VehicleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class StatisticsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private Button btnBackToMenu;
    @FXML private Button btnRefresh;
    @FXML private Button btnEditVehicle;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private ComboBox<String> cmbFuelTypeFilter;
    @FXML private CheckBox chkShowOnlyWithTrips;
    @FXML private ListView<Vehicle> listVehicles;

    @FXML private Label lblTotalTrips;
    @FXML private Label lblTotalFuel;
    @FXML private Label lblTotalDistance;
    @FXML private Label lblAverageConsumption;
    @FXML private Label lblActiveVehicles;

    @FXML private Label lblSelectedVehicle;
    @FXML private VBox vehicleInfoPanel;
    @FXML private VBox tripStatsPanel;
    @FXML private VBox tripHistoryPanel;
    @FXML private VBox selectedTripPanel;

    @FXML private Label lblVehicleNumber;
    @FXML private Label lblVehicleModel;
    @FXML private Label lblVehicleFuelType;
    @FXML private Label lblVehicleStatus;
    @FXML private Label lblVehicleFuelBalance;
    @FXML private Label lblVehicleTankCapacity;

    @FXML private TextField txtFuelAmount;
    @FXML private Button btnRefuel;
    @FXML private Button btnQuickRefuel10;
    @FXML private Button btnQuickRefuel20;
    @FXML private Button btnQuickRefuel50;
    @FXML private Button btnFillTank;
    @FXML private Label lblRefuelStatus;

    @FXML private Label lblVehicleTotalTrips;
    @FXML private Label lblVehicleCompletedTrips;
    @FXML private Label lblVehicleTotalFuel;
    @FXML private Label lblVehicleTotalDistance;

    @FXML private ComboBox<String> cmbTripStatusFilter;
    @FXML private TableView<Trip> tblVehicleTrips;
    @FXML private TableColumn<Trip, String> colTripNumber;
    @FXML private TableColumn<Trip, String> colTripStatus;
    @FXML private TableColumn<Trip, String> colTripRoute;
    @FXML private TableColumn<Trip, String> colTripDate;
    @FXML private TableColumn<Trip, String> colTripFuel;
    @FXML private TableColumn<Trip, String> colTripDistance;
    @FXML private Label lblTripStatusBadge;
    @FXML private Label lblDetailTripNumber;
    @FXML private Label lblDetailTripType;
    @FXML private Label lblDetailCreatedAt;
    @FXML private Label lblDetailUpdatedAt;
    @FXML private Label lblDetailRequesterName;
    @FXML private Label lblDetailRequesterEmail;
    @FXML private Label lblDetailRequesterPhone;
    @FXML private Label lblDetailStartAddress;
    @FXML private VBox waypointsBox;
    @FXML private Label lblDetailEndAddress;
    @FXML private VBox plannedMetricsPanel;
    @FXML private Label lblDetailPlannedStart;
    @FXML private Label lblDetailPlannedEnd;
    @FXML private Label lblDetailPlannedDistance;
    @FXML private Label lblDetailPlannedFuel;
    @FXML private VBox actualMetricsPanel;
    @FXML private Label lblDetailActualStart;
    @FXML private Label lblDetailActualEnd;
    @FXML private Label lblDetailActualDistance;
    @FXML private Label lblDetailActualFuel;
    @FXML private VBox additionalInfoPanel;
    @FXML private VBox purposeBox;
    @FXML private Label lblDetailPurpose;
    @FXML private VBox attorneyBox;
    @FXML private Label lblDetailPowerOfAttorney;
    @FXML private HBox deliveryBox;
    @FXML private Label lblDetailCanDeliver;
    @FXML private VBox notesBox;
    @FXML private TextArea txtDetailNotes;

    @FXML private Label lblStatus;
    @FXML private Label lblLastUpdate;
    @FXML private ProgressIndicator progressLoading;

    private VehicleDAO vehicleDAO;
    private TripDAO tripDAO;
    private ObservableList<Vehicle> allVehicles;
    private ObservableList<Vehicle> filteredVehicles;
    private Map<Integer, List<Trip>> vehicleTripsMap;
    private TripDAO.TripStatistics globalStatistics;
    private Vehicle selectedVehicle;
    private ObservableList<Trip> vehicleTrips;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Ініціалізація контролера статистики");

        try {

            vehicleDAO = new VehicleDAO();
            tripDAO = new TripDAO();

            setupUI();

            loadStatisticsData();

            logger.info("Контролер статистики успішно ініціалізовано");

        } catch (Exception e) {
            logger.error("Помилка ініціалізації контролера статистики: ", e);
            showError("Помилка ініціалізації", "Не вдалося ініціалізувати контролер: " + e.getMessage());
        }
    }

    private void setupUI() {

        cmbStatusFilter.setItems(FXCollections.observableArrayList(
                "Всі статуси", "Активні", "На ТО", "Неактивні"
        ));
        cmbStatusFilter.setValue("Всі статуси");

        cmbFuelTypeFilter.setItems(FXCollections.observableArrayList(
                "Всі типи палива", "Бензин", "Дизель", "Газ"
        ));
        cmbFuelTypeFilter.setValue("Всі типи палива");

        cmbTripStatusFilter.setItems(FXCollections.observableArrayList(
                "Всі статуси", "Створено", "Призначено", "Розпочато", "Завершено", "Скасовано", "Видалено"
        ));
        cmbTripStatusFilter.setValue("Всі статуси");

        allVehicles = FXCollections.observableArrayList();
        filteredVehicles = FXCollections.observableArrayList();
        listVehicles.setItems(filteredVehicles);

        vehicleTrips = FXCollections.observableArrayList();
        tblVehicleTrips.setItems(vehicleTrips);
        setupTripTable();

        listVehicles.setCellFactory(_ -> new ListCell<Vehicle>() {
            @Override
            protected void updateItem(Vehicle vehicle, boolean empty) {
                super.updateItem(vehicle, empty);
                if (empty || vehicle == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {

                    List<Trip> trips = vehicleTripsMap != null ? vehicleTripsMap.get(vehicle.getId()) : null;
                    int tripCount = trips != null ? trips.size() : 0;

                    StringBuilder text = new StringBuilder();
                    text.append(" ").append(vehicle.getDisplayName());
                    text.append("\n💧 ").append(vehicle.getFuelBalance()).append(" л");
                    text.append(" |  ").append(tripCount).append(" поїздок");

                    if (vehicle.needsRefueling()) {
                        text.append(" ");
                    }

                    setText(text.toString());

                    String style = "-fx-padding: 8; -fx-background-radius: 8;";
                    if (vehicle.getStatus() == VehicleStatus.ACTIVE) {
                        style += " -fx-background-color: #E8F5E8;";
                    } else if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
                        style += " -fx-background-color: #FFF3CD;";
                    } else {
                        style += " -fx-background-color: #F8D7DA;";
                    }
                    setStyle(style);
                }
            }
        });

        listVehicles.getSelectionModel().selectedItemProperty().addListener(
                (_, _, newSelection) -> {
                    if (newSelection != null) {
                        showVehicleDetails(newSelection);
                    } else {
                        hideVehicleDetails();
                    }
                });

        tblVehicleTrips.getSelectionModel().selectedItemProperty().addListener(
                (_, _, newSelection) -> {
                    if (newSelection != null) {
                        showTripDetails(newSelection);
                    } else {
                        hideTripDetails();
                    }
                });

        cmbStatusFilter.setOnAction(_ -> applyFilters());
        cmbFuelTypeFilter.setOnAction(_ -> applyFilters());
        chkShowOnlyWithTrips.setOnAction(_ -> applyFilters());
        cmbTripStatusFilter.setOnAction(_ -> applyTripFilters());

        progressLoading.setVisible(false);

        hideVehicleDetails();
    }

    private void setupTripTable() {

        colTripNumber.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTripNumber()));

        colTripStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus().toString()));

        colTripRoute.setCellValueFactory(cellData -> {
            Trip trip = cellData.getValue();
            return new SimpleStringProperty(trip.getRouteDescription());
        });

        colTripDate.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCreatedAt().format(DATE_FORMATTER)));

        colTripFuel.setCellValueFactory(cellData -> {
            BigDecimal fuel = cellData.getValue().getTotalFuelConsumption();
            String fuelStr = fuel != null ? String.format("%.1f л", fuel.doubleValue()) : "—";
            return new SimpleStringProperty(fuelStr);
        });

        colTripDistance.setCellValueFactory(cellData -> {
            BigDecimal distance = cellData.getValue().getTotalDistance();
            String distanceStr = distance != null ? String.format("%.0f", distance.doubleValue()) : "—";
            return new SimpleStringProperty(distanceStr);
        });
    }

    private void loadStatisticsData() {
        lblStatus.setText("Завантаження даних...");
        progressLoading.setVisible(true);
        btnRefresh.setDisable(true);

        Task<Void> loadDataTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    List<Vehicle> vehicles = vehicleDAO.findAllActive();
                    if (vehicles == null) {
                        vehicles = new ArrayList<>();
                    }

                    TripDAO.TripStatistics stats = tripDAO.getTripStatistics();
                    if (stats == null) {
                        stats = new TripDAO.TripStatistics();
                    }

                    List<Trip> allTrips = tripDAO.findAll();
                    if (allTrips == null) {
                        allTrips = new ArrayList<>();
                    }
                    final List<Vehicle> finalVehicles = vehicles;
                    final TripDAO.TripStatistics finalStats = stats;
                    final Map<Integer, List<Trip>> tripsMap = allTrips.stream()
                            .collect(Collectors.groupingBy(Trip::getVehicleId));

                    Platform.runLater(() -> {
                        allVehicles.setAll(finalVehicles);
                        globalStatistics = finalStats;
                        vehicleTripsMap = tripsMap;

                        applyFilters();
                        updateGlobalStatistics();

                        progressLoading.setVisible(false);
                        btnRefresh.setDisable(false);
                        lblStatus.setText("Дані завантажено успішно");
                        lblLastUpdate.setText("Оновлено: " + LocalDateTime.now().format(DATE_FORMATTER));

                        logger.info("Завантажено статистику: {} автомобілів, {} поїздок",
                                finalVehicles.size(), finalStats.getTotalTrips());
                    });

                    return null;
                } catch (Exception e) {
                    logger.error("Помилка завантаження даних статистики: ", e);
                    throw e;
                }
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressLoading.setVisible(false);
                    btnRefresh.setDisable(false);
                    lblStatus.setText("Помилка завантаження даних");
                    logger.error("Помилка завантаження статистики: ", getException());
                    showError("Помилка", "Не вдалося завантажити статистику: " + getException().getMessage());
                });
            }
        };

        new Thread(loadDataTask).start();
    }

    private void applyFilters() {
        if (allVehicles == null) return;

        String statusFilter = cmbStatusFilter.getValue();
        String fuelTypeFilter = cmbFuelTypeFilter.getValue();
        boolean onlyWithTrips = chkShowOnlyWithTrips.isSelected();

        List<Vehicle> filtered = allVehicles.stream()
                .filter(vehicle -> {

                    if (!"Всі статуси".equals(statusFilter)) {
                        switch (statusFilter) {
                            case "Активні":
                                if (vehicle.getStatus() != VehicleStatus.ACTIVE) return false;
                                break;
                            case "На ТО":
                                if (vehicle.getStatus() != VehicleStatus.MAINTENANCE) return false;
                                break;
                            case "Неактивні":
                                if (vehicle.getStatus() == VehicleStatus.ACTIVE) return false;
                                break;
                        }
                    }

                    if (!"Всі типи палива".equals(fuelTypeFilter)) {
                        String fuelType = vehicle.getFuelTypeName();
                        if (fuelType == null) {
                            return false;
                        }
                        String filterLower = fuelTypeFilter.toLowerCase();
                        String fuelTypeLower = fuelType.toLowerCase();
                        boolean matches = false;
                        if (filterLower.contains("бензин") && fuelTypeLower.contains("бензин")) {
                            matches = true;
                        } else if (filterLower.contains("дизель") && fuelTypeLower.contains("дизель")) {
                            matches = true;
                        } else if (filterLower.contains("газ") && fuelTypeLower.contains("газ")) {
                            matches = true;
                        }
                        if (!matches) {
                            return false;
                        }
                    }

                    if (onlyWithTrips) {
                        List<Trip> trips = vehicleTripsMap != null ? vehicleTripsMap.get(vehicle.getId()) : null;
                        if (trips == null || trips.isEmpty()) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        filteredVehicles.setAll(filtered);
        lblStatus.setText("Показано автомобілів: " + filtered.size() + " з " + allVehicles.size());
    }

    private void updateGlobalStatistics() {
        if (globalStatistics == null) return;

        lblTotalTrips.setText(String.valueOf(globalStatistics.getTotalTrips()));
        lblTotalFuel.setText(String.format("%.1f л", globalStatistics.getEffectiveFuelConsumption().doubleValue()));
        lblTotalDistance.setText(String.format("%.1f км", globalStatistics.getEffectiveDistance().doubleValue()));

        BigDecimal averageConsumption = BigDecimal.ZERO;
        if (globalStatistics.getEffectiveDistance().compareTo(BigDecimal.ZERO) > 0) {
            averageConsumption = globalStatistics.getEffectiveFuelConsumption()
                    .divide(globalStatistics.getEffectiveDistance(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        lblAverageConsumption.setText(String.format("%.2f л/100км", averageConsumption.doubleValue()));

        long activeCount = allVehicles != null ? allVehicles.stream()
                .filter(v -> v.getStatus() == VehicleStatus.ACTIVE)
                .count() : 0;
        lblActiveVehicles.setText(String.valueOf(activeCount));
    }

    private void showVehicleDetails(Vehicle vehicle) {
        selectedVehicle = vehicle;

        vehicleInfoPanel.setVisible(true);
        vehicleInfoPanel.setManaged(true);
        tripStatsPanel.setVisible(true);
        tripStatsPanel.setManaged(true);
        tripHistoryPanel.setVisible(true);
        tripHistoryPanel.setManaged(true);

        lblSelectedVehicle.setText(" " + vehicle.getDisplayName());

        lblVehicleNumber.setText(vehicle.getLicensePlate());
        lblVehicleModel.setText(vehicle.getModel() != null ? vehicle.getModel() : "—");
        lblVehicleFuelType.setText(vehicle.getFuelTypeName() != null ? vehicle.getFuelTypeName() : "—");
        lblVehicleStatus.setText(vehicle.getStatus().toString());
        lblVehicleFuelBalance.setText(String.format("%.1f л (%s)",
                vehicle.getFuelBalance().doubleValue(),
                vehicle.getFuelBalanceStatus()));
        lblVehicleTankCapacity.setText(String.format("%.1f л", vehicle.getTankCapacity().doubleValue()));

        List<Trip> trips = vehicleTripsMap != null ? vehicleTripsMap.get(vehicle.getId()) : null;

        if (trips == null || trips.isEmpty()) {

            lblVehicleTotalTrips.setText("0");
            lblVehicleCompletedTrips.setText("0");
            lblVehicleTotalFuel.setText("0.0 л");
            lblVehicleTotalDistance.setText("0.0 км");
            vehicleTrips.clear();
        } else {

            long completedCount = trips.stream()
                    .filter(trip -> trip.getStatus() == TripStatus.COMPLETED)
                    .count();

            BigDecimal totalFuel = trips.stream()
                    .map(Trip::getTotalFuelConsumption)
                    .filter(fuel -> fuel != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDistance = trips.stream()
                    .map(Trip::getTotalDistance)
                    .filter(distance -> distance != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            lblVehicleTotalTrips.setText(String.valueOf(trips.size()));
            lblVehicleCompletedTrips.setText(String.valueOf(completedCount));
            lblVehicleTotalFuel.setText(String.format("%.1f л", totalFuel.doubleValue()));
            lblVehicleTotalDistance.setText(String.format("%.1f км", totalDistance.doubleValue()));

            List<Trip> sortedTrips = trips.stream()
                    .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                    .collect(Collectors.toList());

            vehicleTrips.setAll(sortedTrips);
        }

        applyTripFilters();

        hideTripDetails();
    }

    private void hideVehicleDetails() {
        selectedVehicle = null;
        lblSelectedVehicle.setText(" Оберіть автомобіль для перегляду інформації");

        vehicleInfoPanel.setVisible(false);
        vehicleInfoPanel.setManaged(false);
        tripStatsPanel.setVisible(false);
        tripStatsPanel.setManaged(false);
        tripHistoryPanel.setVisible(false);
        tripHistoryPanel.setManaged(false);
        selectedTripPanel.setVisible(false);
        selectedTripPanel.setManaged(false);

        txtFuelAmount.clear();
        lblRefuelStatus.setText("");
        lblRefuelStatus.setStyle("");

        vehicleTrips.clear();
    }

    private void applyTripFilters() {
        if (selectedVehicle == null) return;

        String statusFilter = cmbTripStatusFilter.getValue();
        List<Trip> allTrips = vehicleTripsMap != null ? vehicleTripsMap.get(selectedVehicle.getId()) : null;

        if (allTrips == null || allTrips.isEmpty()) {
            vehicleTrips.clear();
            return;
        }

        List<Trip> filtered = allTrips.stream()
                .filter(trip -> {
                    if (!"Всі статуси".equals(statusFilter)) {
                        String tripStatus = trip.getStatus().toString();
                        if (!tripStatus.equals(statusFilter)) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .collect(Collectors.toList());

        vehicleTrips.setAll(filtered);
    }

    private void showTripDetails(Trip trip) {
        selectedTripPanel.setVisible(true);
        selectedTripPanel.setManaged(true);

        String statusText = trip.getStatus().toString();
        lblTripStatusBadge.setText(statusText);
        String statusColor = switch (trip.getStatus()) {
            case CREATED -> "#E3F2FD; -fx-text-fill: #1976D2";
            case ASSIGNED -> "#FFF3E0; -fx-text-fill: #F57C00";
            case STARTED -> "#E8F5E9; -fx-text-fill: #388E3C";
            case PAUSED -> "#FFF9C4; -fx-text-fill: #F57F17";
            case COMPLETED -> "#E8F5E8; -fx-text-fill: #2E7D32";
            case CANCELLED -> "#FFEBEE; -fx-text-fill: #C62828";
            case DELETED -> "#F3E5F5; -fx-text-fill: #7B1FA2";
        };
        lblTripStatusBadge.setStyle("-fx-background-color: " + statusColor + "; -fx-background-radius: 12; -fx-padding: 4 12; -fx-font-weight: bold; -fx-font-size: 11px;");

        lblDetailTripNumber.setText(trip.getTripNumber());
        lblDetailTripType.setText(trip.getTripType().toString());
        lblDetailCreatedAt.setText(trip.getCreatedAt().format(DATE_FORMATTER));
        lblDetailUpdatedAt.setText(trip.getUpdatedAt() != null ? trip.getUpdatedAt().format(DATE_FORMATTER) : "—");

        lblDetailRequesterName.setText(trip.getRequesterName() != null ? trip.getRequesterName() : "—");
        lblDetailRequesterEmail.setText(trip.getRequesterEmail() != null ? trip.getRequesterEmail() : "—");
        lblDetailRequesterPhone.setText(trip.getRequesterPhone() != null ? trip.getRequesterPhone() : "—");

        lblDetailStartAddress.setText(trip.getStartAddress() != null ? trip.getStartAddress() : "—");
        if (trip.hasWaypoints()) {
            waypointsBox.setVisible(true);
            waypointsBox.setManaged(true);
            waypointsBox.getChildren().clear();
            for (int i = 0; i < trip.getWaypoints().size(); i++) {
                var waypoint = trip.getWaypoints().get(i);
                HBox waypointRow = new HBox(8.0);
                waypointRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                waypointRow.setStyle("-fx-padding: 5 0 5 25;");
                Label iconLabel = new Label("");
                iconLabel.setStyle("-fx-font-size: 14px;");
                StringBuilder waypointText = new StringBuilder();
                waypointText.append(i + 1).append(". ").append(waypoint.getAddress());
                if (waypoint.getDescription() != null && !waypoint.getDescription().trim().isEmpty()) {
                    waypointText.append(" (").append(waypoint.getDescription()).append(")");
                }
                if (waypoint.getEstimatedStopTime() > 0) {
                    waypointText.append(" • ").append(waypoint.getFormattedStopTime());
                }
                Label textLabel = new Label(waypointText.toString());
                textLabel.setWrapText(true);
                textLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
                waypointRow.getChildren().addAll(iconLabel, textLabel);
                waypointsBox.getChildren().add(waypointRow);
            }
            if (trip.getTotalStopTime() > 0) {
                Label totalStopLabel = new Label("⏱ Загальний час зупинок: " + trip.getTotalStopTime() + " хв");
                totalStopLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #238A90; -fx-font-weight: bold; -fx-padding: 5 0 0 25;");
                waypointsBox.getChildren().add(totalStopLabel);
            }
        } else {
            waypointsBox.setVisible(false);
            waypointsBox.setManaged(false);
        }
        lblDetailEndAddress.setText(trip.getEndAddress() != null ? trip.getEndAddress() : "—");

        boolean hasPlannedMetrics = trip.getPlannedStartTime() != null || trip.getPlannedEndTime() != null ||
                trip.getPlannedDistance() != null || trip.getPlannedFuelConsumption() != null;
        plannedMetricsPanel.setVisible(hasPlannedMetrics);
        plannedMetricsPanel.setManaged(hasPlannedMetrics);
        if (hasPlannedMetrics) {
            lblDetailPlannedStart.setText(trip.getPlannedStartTime() != null ? 
                    trip.getPlannedStartTime().format(DATE_FORMATTER) : "—");
            lblDetailPlannedEnd.setText(trip.getPlannedEndTime() != null ? 
                    trip.getPlannedEndTime().format(DATE_FORMATTER) : "—");
            lblDetailPlannedDistance.setText(trip.getPlannedDistance() != null ? 
                    String.format("%.1f км", trip.getPlannedDistance().doubleValue()) : "—");
            lblDetailPlannedFuel.setText(trip.getPlannedFuelConsumption() != null ? 
                    String.format("%.2f л", trip.getPlannedFuelConsumption().doubleValue()) : "—");
        }

        boolean hasActualMetrics = trip.getActualStartTime() != null || trip.getActualEndTime() != null ||
                trip.getActualDistance() != null || trip.getActualFuelConsumption() != null;
        actualMetricsPanel.setVisible(hasActualMetrics);
        actualMetricsPanel.setManaged(hasActualMetrics);
        if (hasActualMetrics) {
            lblDetailActualStart.setText(trip.getActualStartTime() != null ? 
                    trip.getActualStartTime().format(DATE_FORMATTER) : "—");
            lblDetailActualEnd.setText(trip.getActualEndTime() != null ? 
                    trip.getActualEndTime().format(DATE_FORMATTER) : "—");
            lblDetailActualDistance.setText(trip.getActualDistance() != null ? 
                    String.format("%.1f км", trip.getActualDistance().doubleValue()) : "—");
            lblDetailActualFuel.setText(trip.getActualFuelConsumption() != null ? 
                    String.format("%.2f л", trip.getActualFuelConsumption().doubleValue()) : "—");
        }

        boolean hasPurpose = trip.getPurpose() != null && !trip.getPurpose().trim().isEmpty();
        purposeBox.setVisible(hasPurpose);
        purposeBox.setManaged(hasPurpose);
        if (hasPurpose) {
            lblDetailPurpose.setText(trip.getPurpose());
        }

        boolean hasAttorney = trip.getPowerOfAttorney() != null && !trip.getPowerOfAttorney().trim().isEmpty();
        attorneyBox.setVisible(hasAttorney);
        attorneyBox.setManaged(hasAttorney);
        if (hasAttorney) {
            lblDetailPowerOfAttorney.setText(trip.getPowerOfAttorney());
        }

        deliveryBox.setVisible(trip.isCanDriverDeliver());
        deliveryBox.setManaged(trip.isCanDriverDeliver());

        boolean hasNotes = trip.getNotes() != null && !trip.getNotes().trim().isEmpty();
        notesBox.setVisible(hasNotes);
        notesBox.setManaged(hasNotes);
        if (hasNotes) {
            txtDetailNotes.setText(trip.getNotes());
        }

        boolean hasAdditionalInfo = hasPurpose || hasAttorney || trip.isCanDriverDeliver() || hasNotes;
        additionalInfoPanel.setVisible(hasAdditionalInfo);
        additionalInfoPanel.setManaged(hasAdditionalInfo);
    }

    private void hideTripDetails() {
        selectedTripPanel.setVisible(false);
        selectedTripPanel.setManaged(false);
        lblTripStatusBadge.setText("");
        lblDetailTripNumber.setText("—");
        lblDetailTripType.setText("—");
        lblDetailCreatedAt.setText("—");
        lblDetailUpdatedAt.setText("—");
        lblDetailRequesterName.setText("—");
        lblDetailRequesterEmail.setText("—");
        lblDetailRequesterPhone.setText("—");
        lblDetailStartAddress.setText("—");
        if (waypointsBox != null) {
            waypointsBox.getChildren().clear();
            waypointsBox.setVisible(false);
            waypointsBox.setManaged(false);
        }
        lblDetailEndAddress.setText("—");
        lblDetailPlannedStart.setText("—");
        lblDetailPlannedEnd.setText("—");
        lblDetailPlannedDistance.setText("—");
        lblDetailPlannedFuel.setText("—");
        lblDetailActualStart.setText("—");
        lblDetailActualEnd.setText("—");
        lblDetailActualDistance.setText("—");
        lblDetailActualFuel.setText("—");
        lblDetailPurpose.setText("—");
        lblDetailPowerOfAttorney.setText("—");
        txtDetailNotes.clear();
    }

    @FXML
    private void handleBackToMenu() {
        try {
            logger.info("Повернення до головного меню");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/menu.fxml"));
            Scene scene = new Scene(loader.load());

            Stage currentStage = (Stage) btnBackToMenu.getScene().getWindow();
            currentStage.setScene(scene);
            currentStage.setTitle("Головне меню - Система обліку палива");
            currentStage.setWidth(1200);
            currentStage.setHeight(800);
            currentStage.setMinWidth(1000);
            currentStage.setMinHeight(700);

        } catch (Exception e) {
            logger.error("Помилка повернення до меню: ", e);
            showError("Помилка", "Не вдалося повернутися до головного меню:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        logger.info("Оновлення статистики");
        loadStatisticsData();
    }

    @FXML
    private void handleEditVehicle() {
        try {
            logger.info("Відкриття діалогу редагування автомобіля");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/vehicle-edit-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Редагування автомобіля");
            dialogStage.setScene(scene);
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);

            dialogStage.setOnHidden(_ -> {
                logger.info("Діалог редагування закрито, оновлення даних");
                loadStatisticsData();
            });

            dialogStage.showAndWait();

        } catch (Exception e) {
            logger.error("Помилка відкриття діалогу редагування: ", e);
            showError("Помилка", "Не вдалося відкрити діалог редагування:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleRefuel() {
        if (selectedVehicle == null) {
            showError("Помилка", "Оберіть автомобіль для дозаправки");
            return;
        }

        try {
            String amountText = txtFuelAmount.getText().trim();
            if (amountText.isEmpty()) {
                showError("Помилка", "Введіть кількість літрів для дозаправки");
                return;
            }

            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Помилка", "Кількість літрів повинна бути більше 0");
                return;
            }

            performRefuel(amount);

        } catch (NumberFormatException e) {
            showError("Помилка", "Невірний формат числа. Введіть число (наприклад: 25.5)");
        }
    }

    @FXML
    private void handleQuickRefuel10() {
        if (selectedVehicle != null) {
            performRefuel(BigDecimal.valueOf(10));
        }
    }

    @FXML
    private void handleQuickRefuel20() {
        if (selectedVehicle != null) {
            performRefuel(BigDecimal.valueOf(20));
        }
    }

    @FXML
    private void handleQuickRefuel50() {
        if (selectedVehicle != null) {
            performRefuel(BigDecimal.valueOf(50));
        }
    }

    @FXML
    private void handleFillTank() {
        if (selectedVehicle == null) {
            showError("Помилка", "Оберіть автомобіль для заправки");
            return;
        }

        if (selectedVehicle.getTankCapacity() == null) {
            showError("Помилка", "Не вказана ємність бака для цього автомобіля");
            return;
        }

        BigDecimal currentFuel = selectedVehicle.getFuelBalance() != null ? 
                selectedVehicle.getFuelBalance() : BigDecimal.ZERO;
        BigDecimal amountToFill = selectedVehicle.getTankCapacity().subtract(currentFuel);

        if (amountToFill.compareTo(BigDecimal.ZERO) <= 0) {
            showInfo("Інформація", "Бак вже повний!");
            return;
        }

        performRefuel(amountToFill);
    }

    private void performRefuel(BigDecimal amount) {
        if (selectedVehicle == null) return;

        Task<Boolean> refuelTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                BigDecimal currentFuel = selectedVehicle.getFuelBalance() != null ? 
                        selectedVehicle.getFuelBalance() : BigDecimal.ZERO;
                BigDecimal newBalance = currentFuel.add(amount);

                if (selectedVehicle.getTankCapacity() != null && 
                    newBalance.compareTo(selectedVehicle.getTankCapacity()) > 0) {
                    Platform.runLater(() -> {
                        String message = String.format(
                            "УВАГА: Після дозаправки (%.1f л) перевищить ємність бака (%.1f л)\n\n" +
                            "Максимально можна дозаправити: %.1f л",
                            newBalance.doubleValue(),
                            selectedVehicle.getTankCapacity().doubleValue(),
                            selectedVehicle.getTankCapacity().subtract(currentFuel).doubleValue()
                        );
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Перевищення ємності бака");
                        alert.setHeaderText("Підтвердження дозаправки");
                        alert.setContentText(message);
                        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                            continueRefuel(newBalance);
                        }
                    });
                    return false;
                }

                Platform.runLater(() -> continueRefuel(newBalance));
                return true;
            }
        };

        new Thread(refuelTask).start();
    }

    private void continueRefuel(BigDecimal newBalance) {
        Task<Boolean> updateTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                boolean success = vehicleDAO.updateFuelBalance(selectedVehicle.getId(), newBalance);
                Platform.runLater(() -> {
                    if (success) {
                        selectedVehicle.setFuelBalance(newBalance);
                        updateVehicleCollections(selectedVehicle);
                        updateVehicleDisplay();
                        txtFuelAmount.clear();
                        lblRefuelStatus.setText(" Дозаправка успішна! Новий баланс: " + 
                                String.format("%.1f л", newBalance.doubleValue()));
                        lblRefuelStatus.setStyle("-fx-text-fill: #34C759;");
                        logger.info("Дозаправка автомобіля {}: новий баланс {} л", 
                                selectedVehicle.getDisplayName(), newBalance);
                        showInfo("Дозаправка", " Автомобіль успішно дозаправлено!\n\n" +
                                "Новий баланс: " + String.format("%.1f л", newBalance.doubleValue()));
                    } else {
                        lblRefuelStatus.setText(" Помилка дозаправки");
                        lblRefuelStatus.setStyle("-fx-text-fill: #FF3B30;");
                        showError("Помилка", "Не вдалося оновити баланс палива в базі даних");
                    }
                });
                return success;
            }
        };

        new Thread(updateTask).start();
    }

    private void updateVehicleCollections(Vehicle updatedVehicle) {
        if (updatedVehicle == null) {
            return;
        }

        if (allVehicles != null) {
            for (int i = 0; i < allVehicles.size(); i++) {
                if (allVehicles.get(i).getId() == updatedVehicle.getId()) {
                    allVehicles.set(i, updatedVehicle);
                    break;
                }
            }
        }

        if (filteredVehicles != null) {
            for (int i = 0; i < filteredVehicles.size(); i++) {
                if (filteredVehicles.get(i).getId() == updatedVehicle.getId()) {
                    filteredVehicles.set(i, updatedVehicle);
                    break;
                }
            }
        }
    }

    private void updateVehicleDisplay() {
        if (selectedVehicle != null) {
            lblVehicleFuelBalance.setText(String.format("%.1f л (%s)",
                    selectedVehicle.getFuelBalance().doubleValue(),
                    selectedVehicle.getFuelBalanceStatus()));
            listVehicles.refresh();
        }
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