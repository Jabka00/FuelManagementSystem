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
import javafx.stage.Stage;
import org.example.fuelmanagement.dao.TripDAO;
import org.example.fuelmanagement.dao.VehicleDAO;
import org.example.fuelmanagement.dao.DriverDAO;
import org.example.fuelmanagement.model.Trip;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.Driver;
import org.example.fuelmanagement.model.enums.Season;
import org.example.fuelmanagement.model.enums.TripStatus;
import org.example.fuelmanagement.model.enums.TripType;
import org.example.fuelmanagement.service.GoogleMapsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class HistoryController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final int PAGE_SIZE = 40;

    @FXML private Button btnBackToMenu;
    @FXML private Button btnRefresh;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private DatePicker dpFromDate;
    @FXML private CheckBox chkShowOnlyAssigned;
    @FXML private TextField txtSearch;
    @FXML private Label lblResultsCount;

    @FXML private Button btnFirstPage;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Button btnLastPage;
    @FXML private Label lblPageInfo;
    @FXML private HBox paginationBox;

    @FXML private TableView<Trip> tblTrips;
    @FXML private TableColumn<Trip, String> colTripNumber;
    @FXML private TableColumn<Trip, String> colStatus;
    @FXML private TableColumn<Trip, String> colRequester;
    @FXML private TableColumn<Trip, String> colRoute;
    @FXML private TableColumn<Trip, String> colVehicle;
    @FXML private TableColumn<Trip, String> colDriver;
    @FXML private TableColumn<Trip, String> colDistance;
    @FXML private TableColumn<Trip, String> colActualDistance;
    @FXML private TableColumn<Trip, String> colDeviation;
    @FXML private TableColumn<Trip, String> colActualStart;
    @FXML private TableColumn<Trip, String> colActualEnd;
    @FXML private TableColumn<Trip, String> colFuel;
    @FXML private TableColumn<Trip, String> colRefrigerator;
    @FXML private TableColumn<Trip, String> colCreated;
    @FXML private TableColumn<Trip, Void> colActions;

    @FXML private ProgressIndicator progressLoading;

    private TripDAO tripDAO;
    private VehicleDAO vehicleDAO;
    private DriverDAO driverDAO;
    private GoogleMapsService googleMapsService;
    private ObservableList<Trip> displayedTrips;
    private Map<Integer, Vehicle> vehiclesCache;
    private Map<Integer, Driver> driversCache;

    private int currentPage = 1;
    private int totalPages = 1;
    private int totalCount = 0;
    private Timer searchDebounceTimer;
    private static final long SEARCH_DEBOUNCE_DELAY = 400; 

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Ініціалізація контролера історії поїздок");

        try {

            tripDAO = new TripDAO();
            vehicleDAO = new VehicleDAO();
            driverDAO = new DriverDAO();
            googleMapsService = new GoogleMapsService();

            vehiclesCache = new HashMap<>();
            driversCache = new HashMap<>();

            setupUI();
            loadCaches();
            loadTripsHistory();

            logger.info("Контролер історії поїздок успішно ініціалізовано");

        } catch (Exception e) {
            logger.error("Помилка ініціалізації контролера історії: ", e);
            showError("Помилка ініціалізації", "Не вдалося ініціалізувати контролер: " + e.getMessage());
        }
    }

    private void setupUI() {

        cmbStatusFilter.setItems(FXCollections.observableArrayList(
                "Всі статуси", "Створено", "Призначено", "Розпочато", "Завершено", "Скасовано", "Видалено"
        ));
        cmbStatusFilter.setValue("Всі статуси");

        setupTableColumns();

        setupEventHandlers();

        progressLoading.setVisible(false);

        displayedTrips = FXCollections.observableArrayList();
        tblTrips.setItems(displayedTrips);
        setupPaginationControls();
    }
    private void setupPaginationControls() {
        if (btnFirstPage != null) {
            btnFirstPage.setOnAction(e -> {
                if (currentPage > 1) {
                    currentPage = 1;
                    loadTripsPage();
                }
            });
        }
        if (btnPrevPage != null) {
            btnPrevPage.setOnAction(e -> {
                if (currentPage > 1) {
                    currentPage--;
                    loadTripsPage();
                }
            });
        }
        if (btnNextPage != null) {
            btnNextPage.setOnAction(e -> {
                if (currentPage < totalPages) {
                    currentPage++;
                    loadTripsPage();
                }
            });
        }
        if (btnLastPage != null) {
            btnLastPage.setOnAction(e -> {
                if (currentPage < totalPages) {
                    currentPage = totalPages;
                    loadTripsPage();
                }
            });
        }
        updatePaginationControls();
    }
    private void updatePaginationControls() {
        Platform.runLater(() -> {
            if (lblPageInfo != null) {
                lblPageInfo.setText(String.format("Сторінка %d з %d", currentPage, totalPages));
            }
            if (btnFirstPage != null) {
                btnFirstPage.setDisable(currentPage <= 1);
            }
            if (btnPrevPage != null) {
                btnPrevPage.setDisable(currentPage <= 1);
            }
            if (btnNextPage != null) {
                btnNextPage.setDisable(currentPage >= totalPages);
            }
            if (btnLastPage != null) {
                btnLastPage.setDisable(currentPage >= totalPages);
            }
            if (paginationBox != null) {
                paginationBox.setVisible(totalPages > 1);
                paginationBox.setManaged(totalPages > 1);
            }
        });
    }

    private void setupTableColumns() {

        colTripNumber.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTripNumber()));

        colStatus.setCellFactory(column -> new javafx.scene.control.TableCell<Trip, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    Trip trip = getTableRow().getItem();
                    TripStatus status = trip.getStatus();
                    Label badge = new Label(getStatusLabel(status));
                    badge.setStyle(getStatusBadgeStyle(status));
                    badge.setPadding(new javafx.geometry.Insets(4, 10, 4, 10));
                    setGraphic(badge);
                    setText(null);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus().toString()));

        colRequester.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRequesterName()));

        colRoute.setCellValueFactory(cellData -> {
            Trip trip = cellData.getValue();
            return new SimpleStringProperty(trip.getRouteDescription());
        });

        colVehicle.setCellValueFactory(cellData -> {
            Vehicle vehicle = vehiclesCache.get(cellData.getValue().getVehicleId());
            String vehicleInfo = vehicle != null ? vehicle.getDisplayName() : "ID: " + cellData.getValue().getVehicleId();
            return new SimpleStringProperty(vehicleInfo);
        });

        colDriver.setCellValueFactory(cellData -> {
            Integer driverId = cellData.getValue().getDriverId();
            if (driverId == null) {
                return new SimpleStringProperty("Не призначено");
            }
            Driver driver = driversCache.get(driverId);
            String driverInfo = driver != null ? driver.getFullName() : "ID: " + driverId;
            return new SimpleStringProperty(driverInfo);
        });

        colDistance.setCellValueFactory(cellData -> {
            BigDecimal distance = cellData.getValue().getPlannedDistance();
            String distanceStr = distance != null ? String.format("%.1f км", distance.doubleValue()) : "—";
            return new SimpleStringProperty(distanceStr);
        });

        colActualDistance.setCellValueFactory(cellData -> {
            BigDecimal distance = cellData.getValue().getActualDistance();
            String distanceStr = distance != null ? String.format("%.1f км", distance.doubleValue()) : "—";
            return new SimpleStringProperty(distanceStr);
        });

        colDeviation.setCellValueFactory(cellData -> {
            BigDecimal deviation = cellData.getValue().getRouteDeviationPercent();
            String deviationStr = deviation != null ? String.format("%+.1f%%", deviation.doubleValue()) : "—";
            return new SimpleStringProperty(deviationStr);
        });

        colActualStart.setCellValueFactory(cellData -> {
            LocalDateTime t = cellData.getValue().getActualStartTime();
            String val = t != null ? t.format(DATE_FORMATTER) : "—";
            return new SimpleStringProperty(val);
        });

        colActualEnd.setCellValueFactory(cellData -> {
            LocalDateTime t = cellData.getValue().getActualEndTime();
            String val = t != null ? t.format(DATE_FORMATTER) : "—";
            return new SimpleStringProperty(val);
        });

        colFuel.setCellValueFactory(cellData -> {
            BigDecimal fuel = cellData.getValue().getTotalFuelConsumption();
            String fuelStr = fuel != null ? String.format("%.2f л", fuel.doubleValue()) : "—";
            return new SimpleStringProperty(fuelStr);
        });

        colRefrigerator.setCellValueFactory(cellData -> {
            Trip trip = cellData.getValue();
            Vehicle vehicle = vehiclesCache.get(trip.getVehicleId());
            if (vehicle == null || !vehicle.hasRefrigerator()) {
                return new SimpleStringProperty("—");
            }
            BigDecimal refrigeratorPercent = trip.getRefrigeratorUsagePercent();
            if (refrigeratorPercent == null || refrigeratorPercent.compareTo(BigDecimal.ZERO) == 0) {
                return new SimpleStringProperty("0%");
            }
            return new SimpleStringProperty(String.format("%.0f%%", refrigeratorPercent.doubleValue()));
        });

        colCreated.setCellValueFactory(cellData -> {
            LocalDateTime created = cellData.getValue().getCreatedAt();
            String createdStr = created != null ? created.format(DATE_FORMATTER) : "—";
            return new SimpleStringProperty(createdStr);
        });

        colTripNumber.setPrefWidth(100);
        colStatus.setPrefWidth(110);
        colRequester.setPrefWidth(140);
        colRoute.setPrefWidth(250);
        colVehicle.setPrefWidth(140);
        colDriver.setPrefWidth(140);
        colDistance.setPrefWidth(80);
        colActualDistance.setPrefWidth(80);
        colDeviation.setPrefWidth(80);
        colRefrigerator.setPrefWidth(80);
        colActualStart.setPrefWidth(130);
        colActualEnd.setPrefWidth(130);
        colFuel.setPrefWidth(70);
        colCreated.setPrefWidth(120);
        colActions.setPrefWidth(120);
        setupActionsColumn();
    }
    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new javafx.scene.control.TableCell<Trip, Void>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnMap = new Button("🗺️");
            private final Button btnDelete = new Button("🗑️");
            private final Button btnRestore = new Button("♻️");
            private final HBox container = new HBox(5);
            {
                String baseStyle = "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 14px;";
                btnEdit.setStyle(baseStyle + "-fx-text-fill: #238A90;");
                btnMap.setStyle(baseStyle + "-fx-text-fill: #4285F4;");
                btnDelete.setStyle(baseStyle + "-fx-text-fill: #FF5252;");
                btnRestore.setStyle(baseStyle + "-fx-text-fill: #4CAF50;");
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(baseStyle + "-fx-text-fill: #238A90; -fx-background-color: rgba(35, 138, 144, 0.1);"));
                btnEdit.setOnMouseExited(e -> btnEdit.setStyle(baseStyle + "-fx-text-fill: #238A90;"));
                btnMap.setOnMouseEntered(e -> btnMap.setStyle(baseStyle + "-fx-text-fill: #4285F4; -fx-background-color: rgba(66, 133, 244, 0.1);"));
                btnMap.setOnMouseExited(e -> btnMap.setStyle(baseStyle + "-fx-text-fill: #4285F4;"));
                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(baseStyle + "-fx-text-fill: #FF5252; -fx-background-color: rgba(255, 82, 82, 0.1);"));
                btnDelete.setOnMouseExited(e -> btnDelete.setStyle(baseStyle + "-fx-text-fill: #FF5252;"));
                btnRestore.setOnMouseEntered(e -> btnRestore.setStyle(baseStyle + "-fx-text-fill: #4CAF50; -fx-background-color: rgba(76, 175, 80, 0.1);"));
                btnRestore.setOnMouseExited(e -> btnRestore.setStyle(baseStyle + "-fx-text-fill: #4CAF50;"));
                btnEdit.setTooltip(new Tooltip("Редагувати маршрут"));
                btnMap.setTooltip(new Tooltip("Відкрити в Google Maps"));
                btnDelete.setTooltip(new Tooltip("Видалити поїздку"));
                btnRestore.setTooltip(new Tooltip("Відновити поїздку"));
                container.setAlignment(javafx.geometry.Pos.CENTER);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Trip trip = getTableView().getItems().get(getIndex());
                    btnEdit.setOnAction(e -> {
                        e.consume();
                        openTripDetail(trip);
                    });
                    btnMap.setOnAction(e -> {
                        e.consume();
                        exportTripToMaps(trip);
                    });
                    btnDelete.setOnAction(e -> {
                        e.consume();
                        deleteTrip(trip);
                    });
                    btnRestore.setOnAction(e -> {
                        e.consume();
                        restoreTrip(trip);
                    });
                    boolean isDeleted = trip.getStatus() == TripStatus.DELETED;
                    boolean isInProgress = trip.getStatus() == TripStatus.STARTED || trip.getStatus() == TripStatus.PAUSED;
                    container.getChildren().clear();
                    container.getChildren().add(btnEdit);
                    container.getChildren().add(btnMap);
                    if (isDeleted) {
                        container.getChildren().add(btnRestore);
                    } else if (!isInProgress) {
                        container.getChildren().add(btnDelete);
                    }
                    setGraphic(container);
                }
            }
        });
    }

    private void setupEventHandlers() {
        tblTrips.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Trip selectedTrip = tblTrips.getSelectionModel().getSelectedItem();
                if (selectedTrip != null) {
                    openTripDetail(selectedTrip);
                }
            }
        });

        cmbStatusFilter.setOnAction(e -> applyFilters());
        dpFromDate.setOnAction(e -> applyFilters());
        chkShowOnlyAssigned.setOnAction(e -> applyFilters());

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            if (searchDebounceTimer != null) {
                searchDebounceTimer.cancel();
            }
            searchDebounceTimer = new Timer();
            searchDebounceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> applyFilters());
                }
            }, SEARCH_DEBOUNCE_DELAY);
        });
    }

    private void loadCaches() {
        Task<Void> loadCachesTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                List<Vehicle> vehicles = vehicleDAO.findAllActive();
                for (Vehicle vehicle : vehicles) {
                    vehiclesCache.put(vehicle.getId(), vehicle);
                }

                List<Driver> drivers = driverDAO.findAllActive();
                for (Driver driver : drivers) {
                    driversCache.put(driver.getId(), driver);
                }

                return null;
            }

            @Override
            protected void succeeded() {
                logger.info("Кеши завантажено: {} автомобілів, {} водіїв",
                        vehiclesCache.size(), driversCache.size());
            }

            @Override
            protected void failed() {
                logger.error("Помилка завантаження кешів: ", getException());
            }
        };

        new Thread(loadCachesTask).start();
    }

    private void loadTripsHistory() {
        currentPage = 1;
        loadTripsPage();
    }
    private void loadTripsPage() {
        progressLoading.setVisible(true);
        btnRefresh.setDisable(true);

        String statusFilter = cmbStatusFilter.getValue();
        LocalDateTime fromDate = dpFromDate.getValue() != null ?
                dpFromDate.getValue().atStartOfDay() : null;
        boolean onlyAssigned = chkShowOnlyAssigned.isSelected();
        String searchText = txtSearch.getText().trim();
        boolean showingDeleted = "Видалено".equals(statusFilter);

        Task<TripDAO.PagedResult> loadTripsTask = new Task<TripDAO.PagedResult>() {
            @Override
            protected TripDAO.PagedResult call() throws Exception {
                TripStatus status = null;
                if (showingDeleted) {
                    status = TripStatus.DELETED;
                } else if (!"Всі статуси".equals(statusFilter) && statusFilter != null) {
                    status = mapStatusFromUkrainian(statusFilter);
                }
                return tripDAO.findPaged(currentPage, PAGE_SIZE, status, fromDate, 
                        searchText, onlyAssigned, showingDeleted);
            }

            @Override
            protected void succeeded() {
                TripDAO.PagedResult result = getValue();
                Platform.runLater(() -> {
                    displayedTrips.setAll(result.getTrips());
                    totalCount = result.getTotalCount();
                    totalPages = result.getTotalPages();
                    currentPage = result.getPage();
                    updateResultsCount();
                    updatePaginationControls();
                    progressLoading.setVisible(false);
                    btnRefresh.setDisable(false);

                    logger.info("Завантажено сторінку {}/{}: {} поїздок, всього {}", 
                            currentPage, totalPages, result.getTrips().size(), totalCount);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressLoading.setVisible(false);
                    btnRefresh.setDisable(false);
                    logger.error("Помилка завантаження історії поїздок: ", getException());
                    showError("Помилка", "Не вдалося завантажити історію поїздок: " + getException().getMessage());
                });
            }
        };

        new Thread(loadTripsTask).start();
    }
    private TripStatus mapStatusFromUkrainian(String ukrainianStatus) {
        return switch (ukrainianStatus) {
            case "Створено" -> TripStatus.CREATED;
            case "Призначено" -> TripStatus.ASSIGNED;
            case "Розпочато" -> TripStatus.STARTED;
            case "Завершено" -> TripStatus.COMPLETED;
            case "Скасовано" -> TripStatus.CANCELLED;
            case "Видалено" -> TripStatus.DELETED;
            default -> null;
        };
    }

    private void applyFilters() {
        currentPage = 1;
        loadTripsPage();
    }

    private void updateResultsCount() {
        int startRecord = (currentPage - 1) * PAGE_SIZE + 1;
        int endRecord = Math.min(currentPage * PAGE_SIZE, totalCount);
        if (totalCount == 0) {
            lblResultsCount.setText("Поїздок не знайдено");
        } else if (totalPages == 1) {
            lblResultsCount.setText("Знайдено поїздок: " + totalCount);
        } else {
            lblResultsCount.setText(String.format("Показано %d-%d з %d поїздок", 
                    startRecord, endRecord, totalCount));
        }
    }

    private void openTripDetail(Trip trip) {
        try {
            logger.info("Відкриття детального перегляду поїздки {}", trip.getTripNumber());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/trip-detail.fxml"));
            Scene scene = new Scene(loader.load());

            TripDetailController controller = loader.getController();
            controller.setTrip(trip);
            controller.setOnChangeCallback(() -> {
                loadTripsHistory();
            });

            Stage detailStage = new Stage();
            detailStage.setTitle("Деталі поїздки - " + trip.getTripNumber());
            detailStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            detailStage.initOwner(btnBackToMenu.getScene().getWindow());
            detailStage.setScene(scene);
            detailStage.setWidth(1200);
            detailStage.setHeight(800);
            detailStage.setMinWidth(900);
            detailStage.setMinHeight(600);

            detailStage.showAndWait();

            logger.info("Детальний перегляд закрито");

        } catch (Exception e) {
            logger.error("Помилка відкриття детального перегляду: ", e);
            showError("Помилка", "Не вдалося відкрити детальний перегляд поїздки:\n" + e.getMessage());
        }
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
        loadTripsHistory();
    }

    @FXML
    private void handleClearFilters() {
        cmbStatusFilter.setValue("Всі статуси");
        dpFromDate.setValue(null);
        chkShowOnlyAssigned.setSelected(false);
        txtSearch.clear();
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
    private void exportTripToMaps(Trip trip) {
        try {
            logger.info("🗺️ Експорт маршруту в Google Maps для поїздки {}", trip.getTripNumber());

            String startAddress = trip.getStartAddress();
            String endAddress = trip.getEndAddress();

            if (startAddress == null || startAddress.trim().isEmpty() ||
                endAddress == null || endAddress.trim().isEmpty()) {
                showError("Помилка", "Не вдалося експортувати маршрут: відсутні адреси початку або кінця поїздки");
                return;
            }

            List<String> waypoints = new ArrayList<>();
            if (trip.hasWaypoints()) {
                for (var waypoint : trip.getWaypoints()) {
                    if (waypoint.getAddress() != null && !waypoint.getAddress().trim().isEmpty()) {
                        waypoints.add(waypoint.getAddress());
                    }
                }
            }

            boolean isRoundTrip = trip.getTripType() == TripType.ROUND_TRIP;
            String googleMapsUrl = googleMapsService.generateGoogleMapsUrl(startAddress, endAddress, waypoints, isRoundTrip);

            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(googleMapsUrl));
                logger.info("✅ Маршрут відкрито в Google Maps");
                showInfo("Успішно!", "Маршрут відкрито в Google Maps!");
            } else {
                showError("Помилка", "Не вдалося відкрити браузер");
            }

        } catch (Exception e) {
            logger.error("Помилка експорту маршруту: ", e);
            showError("Помилка", "Не вдалося експортувати маршрут в Google Maps:\n" + e.getMessage());
        }
    }
    private void deleteTrip(Trip trip) {
        if (trip.getStatus() == TripStatus.STARTED || trip.getStatus() == TripStatus.PAUSED) {
            showError("Помилка", "Не можна видалити поїздку, яка зараз виконується або призупинена");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Підтвердження видалення");
        confirmDialog.setHeaderText("Видалення поїздки");
        confirmDialog.setContentText(
            "Ви впевнені, що хочете видалити поїздку?\n\n" +
            "Поїздка: " + trip.getTripNumber() + "\n" +
            "Маршрут: " + trip.getRouteDescription() + "\n\n" +
            "Поїздку можна буде відновити пізніше."
        );

        ButtonType deleteButton = new ButtonType("Видалити", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(deleteButton, cancelButton);

        confirmDialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == deleteButton) {
                try {
                    logger.info("🗑️ Видалення поїздки {} (ID: {})", trip.getTripNumber(), trip.getId());

                    boolean success = tripDAO.softDelete(trip.getId());

                    if (success) {
                        loadTripsPage();
                        showInfo("Успішно!", "Поїздка " + trip.getTripNumber() + " успішно видалена.");
                        logger.info("✅ Поїздка {} успішно видалена", trip.getTripNumber());
                    } else {
                        showError("Помилка", "Не вдалося видалити поїздку. Спробуйте ще раз.");
                        logger.error("Не вдалося видалити поїздку {}", trip.getTripNumber());
                    }

                } catch (Exception e) {
                    logger.error("Помилка видалення поїздки {}: ", trip.getTripNumber(), e);
                    showError("Помилка", "Сталася помилка при видаленні поїздки:\n" + e.getMessage());
                }
            }
        });
    }
    private String getStatusLabel(TripStatus status) {
        return switch (status) {
            case CREATED -> "Створено";
            case ASSIGNED -> "Призначено";
            case STARTED -> "Розпочато";
            case PAUSED -> "Призупинено";
            case COMPLETED -> "Завершено";
            case CANCELLED -> "Скасовано";
            case DELETED -> "Видалено";
            default -> status.toString();
        };
    }
    private String getStatusBadgeStyle(TripStatus status) {
        String baseStyle = "-fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 600; ";
        return switch (status) {
            case CREATED -> baseStyle + "-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0;"; 
            case ASSIGNED -> baseStyle + "-fx-background-color: #FFF3E0; -fx-text-fill: #EF6C00;"; 
            case STARTED -> baseStyle + "-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32;"; 
            case PAUSED -> baseStyle + "-fx-background-color: #FFF9C4; -fx-text-fill: #F57F17;"; 
            case COMPLETED -> baseStyle + "-fx-background-color: #B2DFDB; -fx-text-fill: #00695C;"; 
            case CANCELLED -> baseStyle + "-fx-background-color: #F5F5F5; -fx-text-fill: #616161;"; 
            case DELETED -> baseStyle + "-fx-background-color: #FFCDD2; -fx-text-fill: #C62828;"; 
            default -> baseStyle + "-fx-background-color: #F5F5F5; -fx-text-fill: #616161;";
        };
    }
    private void restoreTrip(Trip trip) {
        if (trip.getStatus() != TripStatus.DELETED) {
            showError("Помилка", "Можна відновити тільки видалені поїздки");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Підтвердження відновлення");
        confirmDialog.setHeaderText("Відновлення поїздки");
        confirmDialog.setContentText(
            "Ви впевнені, що хочете відновити поїздку?\n\n" +
            "Поїздка: " + trip.getTripNumber() + "\n" +
            "Маршрут: " + trip.getRouteDescription() + "\n\n" +
            "Поїздка буде відновлена зі статусом 'Створено'."
        );

        ButtonType restoreButton = new ButtonType("Відновити", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(restoreButton, cancelButton);

        confirmDialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == restoreButton) {
                try {
                    logger.info("♻️ Відновлення поїздки {} (ID: {})", trip.getTripNumber(), trip.getId());

                    boolean success = tripDAO.restore(trip.getId());

                    if (success) {
                        if ("Видалено".equals(cmbStatusFilter.getValue())) {
                            loadTripsPage();
                        } else {
                            cmbStatusFilter.setValue("Всі статуси");
                            loadTripsHistory();
                        }
                        showInfo("Успішно!", "Поїздка " + trip.getTripNumber() + " успішно відновлена.");
                        logger.info("✅ Поїздка {} успішно відновлена", trip.getTripNumber());
                    } else {
                        showError("Помилка", "Не вдалося відновити поїздку. Спробуйте ще раз.");
                        logger.error("Не вдалося відновити поїздку {}", trip.getTripNumber());
                    }

                } catch (Exception e) {
                    logger.error("Помилка відновлення поїздки {}: ", trip.getTripNumber(), e);
                    showError("Помилка", "Сталася помилка при відновленні поїздки:\n" + e.getMessage());
                }
            }
        });
    }
}