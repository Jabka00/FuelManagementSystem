package org.example.fuelmanagement.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.Optional;
import org.example.fuelmanagement.dao.TripDAO;
import org.example.fuelmanagement.dao.VehicleDAO;
import org.example.fuelmanagement.dao.DriverDAO;
import org.example.fuelmanagement.model.RouteInfo;
import org.example.fuelmanagement.model.Trip;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.Driver;
import org.example.fuelmanagement.model.Waypoint;
import org.example.fuelmanagement.model.enums.Season;
import org.example.fuelmanagement.model.enums.TripStatus;
import org.example.fuelmanagement.model.enums.TripType;
import org.example.fuelmanagement.service.GoogleMapsService;
import org.example.fuelmanagement.util.FuelCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class TripDetailController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(TripDetailController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private Button btnBack;
    @FXML private Label lblTripNumber;
    @FXML private Label lblStatus;
    @FXML private Label lblRouteType;
    @FXML private Button btnRestore;
    @FXML private Button btnEdit;
    @FXML private Button btnChangeVehicle;
    @FXML private Button btnGoogleMaps;
    @FXML private Button btnDelete;
    @FXML private Label lblCustomerName;
    @FXML private Label lblCustomerEmail;
    @FXML private Label lblCustomerPhone;
    @FXML private Label lblVehicleName;
    @FXML private Label lblFuelType;
    @FXML private Label lblDriverName;
    @FXML private Label lblDriverPhone;
    @FXML private VBox vboxRoute;
    @FXML private VBox vboxPlanned;
    @FXML private VBox cardActual;
    @FXML private VBox vboxActual;
    @FXML private Label lblPlannedStart;
    @FXML private Label lblPlannedEnd;
    @FXML private Label lblActualStart;
    @FXML private Label lblActualEnd;
    @FXML private GridPane gridAdditional;

    private Trip trip;
    private Vehicle vehicle;
    private Driver driver;
    private TripDAO tripDAO;
    private VehicleDAO vehicleDAO;
    private DriverDAO driverDAO;
    private GoogleMapsService googleMapsService;
    private Runnable onChangeCallback;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            tripDAO = new TripDAO();
            vehicleDAO = new VehicleDAO();
            driverDAO = new DriverDAO();
            googleMapsService = new GoogleMapsService();
            logger.info("TripDetailController успішно ініціалізовано");
        } catch (Exception e) {
            logger.error("Помилка ініціалізації TripDetailController: ", e);
            showError("Помилка ініціалізації", "Не вдалося ініціалізувати контролер: " + e.getMessage());
        }
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
        if (trip.getVehicleId() > 0) {
            try {
                this.vehicle = vehicleDAO.findById(trip.getVehicleId());
            } catch (Exception e) {
                logger.error("Помилка завантаження автомобіля: ", e);
            }
        }
        if (trip.getDriverId() != null) {
            try {
                this.driver = driverDAO.findById(trip.getDriverId());
            } catch (Exception e) {
                logger.error("Помилка завантаження водія: ", e);
            }
        }
        populateData();
    }

    public void setOnChangeCallback(Runnable callback) {
        this.onChangeCallback = callback;
    }

    private void populateData() {
        if (trip == null) {
            return;
        }

        lblTripNumber.setText("Поїздка " + trip.getTripNumber());
        lblStatus.setText(getStatusLabel(trip.getStatus()));
        lblStatus.setStyle(getStatusStyle(trip.getStatus()));
        lblRouteType.setText(trip.getTripType().toString());

        boolean isDeleted = trip.getStatus() == TripStatus.DELETED;
        boolean isInProgress = trip.getStatus() == TripStatus.STARTED || trip.getStatus() == TripStatus.PAUSED;
        btnRestore.setVisible(isDeleted);
        btnRestore.setManaged(isDeleted);
        btnEdit.setVisible(!isDeleted);
        btnEdit.setManaged(!isDeleted);
        btnChangeVehicle.setVisible(!isDeleted && !isInProgress);
        btnChangeVehicle.setManaged(!isDeleted && !isInProgress);
        btnGoogleMaps.setVisible(!isDeleted);
        btnGoogleMaps.setManaged(!isDeleted);
        btnDelete.setVisible(!isDeleted && !isInProgress);
        btnDelete.setManaged(!isDeleted && !isInProgress);

        lblCustomerName.setText(trip.getRequesterName() != null ? trip.getRequesterName() : "—");
        lblCustomerEmail.setText(trip.getRequesterEmail() != null ? trip.getRequesterEmail() : "—");
        lblCustomerPhone.setText(trip.getRequesterPhone() != null ? trip.getRequesterPhone() : "—");

        if (vehicle != null) {
            lblVehicleName.setText(vehicle.getDisplayName());
            lblFuelType.setText(vehicle.getFuelTypeName() != null ? vehicle.getFuelTypeName() : "—");
        } else {
            lblVehicleName.setText("ID: " + trip.getVehicleId());
            lblFuelType.setText("—");
        }

        if (driver != null) {
            lblDriverName.setText(driver.getFullName());
            lblDriverPhone.setText(driver.getPhone() != null ? driver.getPhone() : "—");
        } else {
            lblDriverName.setText(trip.getDriverId() != null ? "ID: " + trip.getDriverId() : "Не призначено");
            lblDriverPhone.setText("—");
        }

        populateRouteCard();

        populatePlannedCard();

        if (trip.getActualDistance() != null || trip.getActualFuelConsumption() != null) {
            cardActual.setVisible(true);
            cardActual.setManaged(true);
            populateActualCard();
        } else {
            cardActual.setVisible(false);
            cardActual.setManaged(false);
        }

        lblPlannedStart.setText(trip.getPlannedStartTime() != null ? trip.getPlannedStartTime().format(DATE_FORMATTER) : "—");
        lblPlannedEnd.setText(trip.getPlannedEndTime() != null ? trip.getPlannedEndTime().format(DATE_FORMATTER) : "—");
        lblActualStart.setText(trip.getActualStartTime() != null ? trip.getActualStartTime().format(DATE_FORMATTER) : "—");
        lblActualEnd.setText(trip.getActualEndTime() != null ? trip.getActualEndTime().format(DATE_FORMATTER) : "—");

        populateAdditionalCard();
    }

    private void populateRouteCard() {
        vboxRoute.getChildren().clear();

        addRouteItem("Початок:", trip.getStartAddress(), true);

        if (trip.hasWaypoints()) {
            Label waypointsLabel = new Label("Проміжні точки:");
            waypointsLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 13px;");
            vboxRoute.getChildren().add(waypointsLabel);

            VBox waypointsBox = new VBox(5);
            waypointsBox.setPadding(new Insets(0, 0, 0, 15));
            for (int i = 0; i < trip.getWaypoints().size(); i++) {
                Waypoint wp = trip.getWaypoints().get(i);
                StringBuilder wpText = new StringBuilder("• " + wp.getAddress());
                if (wp.getDescription() != null && !wp.getDescription().trim().isEmpty()) {
                    wpText.append(" (").append(wp.getDescription()).append(")");
                }
                if (wp.getEstimatedStopTime() > 0) {
                    wpText.append(" — ").append(wp.getFormattedStopTime());
                }
                Label wpLabel = new Label(wpText.toString());
                wpLabel.setStyle("-fx-text-fill: #212121; -fx-font-size: 12px;");
                wpLabel.setWrapText(true);
                waypointsBox.getChildren().add(wpLabel);
            }
            vboxRoute.getChildren().add(waypointsBox);
        }

        addRouteItem("Кінець:", trip.getEndAddress(), true);

        addRouteItem("Тип:", trip.getTripType().toString(), false);

        if (trip.hasWaypoints()) {
            addRouteItem("Загальний час зупинок:", trip.getTotalStopTime() + " хв", false);
        }
    }

    private void populatePlannedCard() {
        vboxPlanned.getChildren().clear();

        if (trip.getPlannedDistance() != null) {
            addMetricItem(vboxPlanned, "Дистанція (загальна):", String.format("%.1f км", trip.getPlannedDistance().doubleValue()));
        }

        if (trip.getPlannedCityKm() != null) {
            addMetricItem(vboxPlanned, "Дистанція (місто):", String.format("%.1f км", trip.getPlannedCityKm().doubleValue()));
        }

        if (trip.getPlannedHighwayKm() != null) {
            addMetricItem(vboxPlanned, "Дистанція (траса):", String.format("%.1f км", trip.getPlannedHighwayKm().doubleValue()));
        }

        if (trip.getPlannedFuelConsumption() != null) {
            addMetricItem(vboxPlanned, "Витрата пального:", String.format("%.2f л", trip.getPlannedFuelConsumption().doubleValue()));
        }

        if (vehicle != null && vehicle.hasRefrigerator()) {
            BigDecimal refrigeratorPercent = trip.getRefrigeratorUsagePercent();
            if (refrigeratorPercent != null && refrigeratorPercent.compareTo(BigDecimal.ZERO) > 0) {
                addMetricItem(vboxPlanned, "Холодильник:", String.format("%.0f%%", refrigeratorPercent.doubleValue()));
                BigDecimal additionalFuelPercent = new BigDecimal("15")
                    .multiply(refrigeratorPercent)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                addMetricItem(vboxPlanned, "Дод. витрата (холод.):", String.format("+%.1f%%", additionalFuelPercent.doubleValue()));
            }
        }
    }

    private void populateActualCard() {
        vboxActual.getChildren().clear();

        if (trip.getActualDistance() != null) {
            addMetricItem(vboxActual, "Дистанція (загальна):", String.format("%.1f км", trip.getActualDistance().doubleValue()));
        }

        if (trip.getActualCityKm() != null) {
            addMetricItem(vboxActual, "Дистанція (місто):", String.format("%.1f км", trip.getActualCityKm().doubleValue()));
        }

        if (trip.getActualHighwayKm() != null) {
            addMetricItem(vboxActual, "Дистанція (траса):", String.format("%.1f км", trip.getActualHighwayKm().doubleValue()));
        }

        if (trip.getRouteDeviationPercent() != null) {
            String deviationText = String.format("%+.1f%%", trip.getRouteDeviationPercent().doubleValue());
            String deviationColor = trip.getRouteDeviationPercent().compareTo(new BigDecimal("10")) > 0 ? "#FF5252" : "#4CAF50";
            HBox deviationBox = new HBox(10);
            deviationBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label deviationLabel = new Label("Відхилення:");
            deviationLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 13px;");
            Label deviationValue = new Label(deviationText);
            deviationValue.setStyle("-fx-text-fill: " + deviationColor + "; -fx-font-weight: bold; -fx-font-size: 13px;");
            deviationBox.getChildren().addAll(deviationLabel, deviationValue);
            vboxActual.getChildren().add(deviationBox);
        }

        if (trip.getActualFuelConsumption() != null) {
            addMetricItem(vboxActual, "Витрата пального:", String.format("%.2f л", trip.getActualFuelConsumption().doubleValue()));
        }
    }

    private void populateAdditionalCard() {
        gridAdditional.getChildren().clear();
        int col = 0;
        int row = 0;

        if (trip.getPurpose() != null && !trip.getPurpose().trim().isEmpty()) {
            VBox purposeBox = createInfoBlock("Ціль:", trip.getPurpose());
            gridAdditional.add(purposeBox, col++, row);
        }

        if (trip.getPowerOfAttorney() != null && !trip.getPowerOfAttorney().trim().isEmpty()) {
            VBox poaBox = createInfoBlock("Довіреність:", trip.getPowerOfAttorney());
            gridAdditional.add(poaBox, col++, row);
        }

        if (col >= 3) {
            col = 0;
            row++;
        }
        VBox deliveryBox = createInfoBlock("Водій може сам завезти вантаж:", trip.isCanDriverDeliver() ? "Так" : "Ні");
        gridAdditional.add(deliveryBox, col++, row);

        if (col >= 3) {
            col = 0;
            row++;
        }
        VBox seasonBox = createInfoBlock("Сезон:", trip.getSeason().toString());
        gridAdditional.add(seasonBox, col++, row);

        if (col >= 3) {
            col = 0;
            row++;
        }
        VBox createdBox = createInfoBlock("Дата створення:", trip.getCreatedAt().format(DATE_FORMATTER));
        gridAdditional.add(createdBox, col++, row);

        if (trip.getUpdatedAt() != null) {
            if (col >= 3) {
                col = 0;
                row++;
            }
            VBox updatedBox = createInfoBlock("Дата оновлення:", trip.getUpdatedAt().format(DATE_FORMATTER));
            gridAdditional.add(updatedBox, col++, row);
        }

        if (col >= 3) {
            col = 0;
            row++;
        }
        VBox createdByBox = createInfoBlock("Хто створив:", trip.getCreatedBy() != null ? trip.getCreatedBy() : "system");
        gridAdditional.add(createdByBox, col++, row);

        if (trip.getNotes() != null && !trip.getNotes().trim().isEmpty()) {
            row++;
            VBox notesBox = createInfoBlock("Примітки:", trip.getNotes());
            GridPane.setColumnSpan(notesBox, 3);
            gridAdditional.add(notesBox, 0, row);
        }
    }

    private void addRouteItem(String label, String value, boolean wrapText) {
        HBox hbox = new HBox(10);
        hbox.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #757575; -fx-font-size: 13px;");
        labelNode.setMinWidth(80);

        Label valueNode = new Label(value != null ? value : "—");
        valueNode.setStyle("-fx-text-fill: #212121; -fx-font-size: 13px;");
        valueNode.setWrapText(wrapText);
        HBox.setHgrow(valueNode, Priority.ALWAYS);

        hbox.getChildren().addAll(labelNode, valueNode);
        vboxRoute.getChildren().add(hbox);
    }

    private void addMetricItem(VBox container, String label, String value) {
        HBox hbox = new HBox(10);
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #757575; -fx-font-size: 13px;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #212121; -fx-font-weight: bold; -fx-font-size: 13px;");

        hbox.getChildren().addAll(labelNode, valueNode);
        container.getChildren().add(hbox);
    }

    private VBox createInfoBlock(String label, String value) {
        VBox vbox = new VBox(5);

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #757575; -fx-font-size: 12px;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #212121; -fx-font-size: 13px;");
        valueNode.setWrapText(true);

        vbox.getChildren().addAll(labelNode, valueNode);
        return vbox;
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

    private String getStatusStyle(TripStatus status) {
        String baseStyle = "-fx-padding: 5 12; -fx-background-radius: 15; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: ";
        return switch (status) {
            case CREATED -> baseStyle + "#9E9E9E;";
            case ASSIGNED -> baseStyle + "#2196F3;";
            case STARTED -> baseStyle + "#4CAF50;";
            case PAUSED -> baseStyle + "#FF9800;";
            case COMPLETED -> baseStyle + "#4CAF50;";
            case CANCELLED -> baseStyle + "#757575;";
            case DELETED -> baseStyle + "#F44336;";
            default -> baseStyle + "#9E9E9E;";
        };
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) btnBack.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleEdit() {
        try {
            logger.info("Відкриття діалогу редагування маршруту для поїздки {}", trip.getTripNumber());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/trip-route-edit-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            TripRouteEditDialogController controller = loader.getController();
            String vehicleInfo = vehicle != null ? vehicle.getDisplayName() : "ID: " + trip.getVehicleId();
            controller.setTrip(trip, vehicleInfo);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Редагування маршруту поїздки");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(btnEdit.getScene().getWindow());
            dialogStage.setScene(scene);
            dialogStage.setMinWidth(900);
            dialogStage.setMinHeight(600);

            dialogStage.showAndWait();

            if (controller.isChangesSaved()) {
                Trip updatedTrip = tripDAO.findById(trip.getId());
                if (updatedTrip != null) {
                    setTrip(updatedTrip);
                    if (onChangeCallback != null) {
                        onChangeCallback.run();
                    }
                    logger.info("Маршрут поїздки оновлено");
                }
            }

        } catch (Exception e) {
            logger.error("Помилка відкриття діалогу редагування маршруту: ", e);
            showError("Помилка", "Не вдалося відкрити діалог редагування маршруту:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleExportToMaps() {
        try {
            logger.info("Експорт маршруту в Google Maps для поїздки {}", trip.getTripNumber());

            String startAddress = trip.getStartAddress();
            String endAddress = trip.getEndAddress();

            if (startAddress == null || startAddress.trim().isEmpty() ||
                endAddress == null || endAddress.trim().isEmpty()) {
                showError("Помилка", "Не вдалося експортувати маршрут: відсутні адреси початку або кінця поїздки");
                return;
            }

            List<String> waypoints = new ArrayList<>();
            if (trip.hasWaypoints()) {
                for (Waypoint waypoint : trip.getWaypoints()) {
                    if (waypoint.getAddress() != null && !waypoint.getAddress().trim().isEmpty()) {
                        waypoints.add(waypoint.getAddress());
                    }
                }
            }

            boolean isRoundTrip = trip.getTripType() == TripType.ROUND_TRIP;
            String googleMapsUrl = googleMapsService.generateGoogleMapsUrl(startAddress, endAddress, waypoints, isRoundTrip);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(googleMapsUrl));
                logger.info("Маршрут відкрито в Google Maps");
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(googleMapsUrl);
                clipboard.setContent(content);
                showInfo("Успішно!", "Маршрут відкрито в Google Maps!\n\nПосилання також скопійовано в буфер обміну.");
            } else {
                showError("Помилка", "Не вдалося відкрити браузер. Спробуйте вручну скопіювати посилання.");
            }

        } catch (Exception e) {
            logger.error(" Помилка експорту маршруту: ", e);
            showError("Помилка", "Не вдалося експортувати маршрут в Google Maps:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleChangeVehicle() {
        try {
            logger.info(" Відкриття діалогу зміни автомобіля для поїздки {}", trip.getTripNumber());

            List<Vehicle> availableVehicles = vehicleDAO.findAllActive();
            if (availableVehicles.isEmpty()) {
                showError("Помилка", "Немає доступних автомобілів для вибору.");
                return;
            }

            availableVehicles.removeIf(v -> v.getId() == trip.getVehicleId());
            if (availableVehicles.isEmpty()) {
                showError("Помилка", "Немає інших доступних автомобілів для вибору.");
                return;
            }

            Dialog<Vehicle> dialog = new Dialog<>();
            dialog.setTitle("Змінити автомобіль");
            dialog.setHeaderText("Виберіть новий автомобіль для поїздки " + trip.getTripNumber());

            ButtonType confirmButtonType = new ButtonType("Змінити", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setMinWidth(500);

            Label lblCurrentVehicle = new Label("Поточний автомобіль:");
            lblCurrentVehicle.setStyle("-fx-font-weight: bold; -fx-text-fill: #757575;");
            String currentVehicleInfo = vehicle != null 
                ? vehicle.getDisplayName() + " (" + vehicle.getFuelTypeName() + ")"
                : "ID: " + trip.getVehicleId();
            Label lblCurrentVehicleValue = new Label(currentVehicleInfo);
            lblCurrentVehicleValue.setStyle("-fx-font-size: 14px;");

            Label lblNewVehicle = new Label("Новий автомобіль:");
            lblNewVehicle.setStyle("-fx-font-weight: bold;");
            ComboBox<Vehicle> cmbVehicle = new ComboBox<>();
            cmbVehicle.getItems().addAll(availableVehicles);
            cmbVehicle.setMinWidth(400);
            cmbVehicle.setPromptText("Виберіть автомобіль...");
            cmbVehicle.setCellFactory(listView -> new ListCell<Vehicle>() {
                @Override
                protected void updateItem(Vehicle item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getDisplayName() + " | " + item.getFuelTypeName() + 
                                " | Баланс: " + item.getFuelBalance() + "л");
                    }
                }
            });
            cmbVehicle.setButtonCell(new ListCell<Vehicle>() {
                @Override
                protected void updateItem(Vehicle item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Виберіть автомобіль...");
                    } else {
                        setText(item.getDisplayName() + " | " + item.getFuelTypeName());
                    }
                }
            });

            Label lblFuelInfo = new Label("");
            lblFuelInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: #4CAF50;");
            lblFuelInfo.setWrapText(true);

            cmbVehicle.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    BigDecimal newFuel = calculateFuelForVehicle(newVal);
                    BigDecimal oldFuel = trip.getPlannedFuelConsumption();
                    String fuelChange = "";
                    if (oldFuel != null && newFuel != null) {
                        BigDecimal diff = newFuel.subtract(oldFuel);
                        String sign = diff.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                        fuelChange = " (" + sign + String.format("%.2f", diff) + " л)";
                    }
                    lblFuelInfo.setText("Нові планові витрати палива: " + 
                        (newFuel != null ? String.format("%.2f", newFuel) + " л" + fuelChange : "не вдалося розрахувати"));
                    boolean isSummer = trip.getSeason() == Season.SUMMER;
                    BigDecimal cityRate = isSummer ? newVal.getCityRateSummer() : newVal.getCityRateWinter();
                    BigDecimal highwayRate = isSummer ? newVal.getHighwayRateSummer() : newVal.getHighwayRateWinter();
                    if (cityRate != null && highwayRate != null) {
                        lblFuelInfo.setText(lblFuelInfo.getText() + 
                            "\n Норми (" + (isSummer ? "літо" : "зима") + "): місто " + 
                            cityRate.multiply(BigDecimal.valueOf(100)).setScale(1, java.math.RoundingMode.HALF_UP) + " л/100км, траса " + 
                            highwayRate.multiply(BigDecimal.valueOf(100)).setScale(1, java.math.RoundingMode.HALF_UP) + " л/100км");
                    }
                } else {
                    lblFuelInfo.setText("");
                }
            });

            Label lblWarning = new Label(" Увага: при зміні автомобіля буде автоматично перераховано планові витрати палива " +
                "згідно з нормами нового автомобіля.");
            lblWarning.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12px;");
            lblWarning.setWrapText(true);

            content.getChildren().addAll(
                lblCurrentVehicle, lblCurrentVehicleValue, 
                new Separator(),
                lblNewVehicle, cmbVehicle, 
                lblFuelInfo,
                new Separator(),
                lblWarning
            );

            dialog.getDialogPane().setContent(content);

            dialog.getDialogPane().lookupButton(confirmButtonType).setDisable(true);
            cmbVehicle.valueProperty().addListener((obs, oldVal, newVal) -> {
                dialog.getDialogPane().lookupButton(confirmButtonType).setDisable(newVal == null);
            });

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == confirmButtonType) {
                    return cmbVehicle.getValue();
                }
                return null;
            });

            Optional<Vehicle> result = dialog.showAndWait();

            if (result.isPresent()) {
                Vehicle newVehicle = result.get();
                performVehicleChange(newVehicle);
            }

        } catch (Exception e) {
            logger.error("Помилка відкриття діалогу зміни автомобіля: ", e);
            showError("Помилка", "Не вдалося відкрити діалог зміни автомобіля:\n" + e.getMessage());
        }
    }

    private BigDecimal calculateFuelForVehicle(Vehicle newVehicle) {
        if (newVehicle == null || trip == null) {
            logger.warn("calculateFuelForVehicle: newVehicle або trip є null");
            return null;
        }

        try {
            boolean isSummer = trip.getSeason() == Season.SUMMER;
            logger.info(" calculateFuelForVehicle для поїздки {} (тип: {})", 
                trip.getTripNumber(), trip.getTripType());
            logger.info(" Дані поїздки: plannedDistance={}, plannedCityKm={}, plannedHighwayKm={}", 
                trip.getPlannedDistance(), trip.getPlannedCityKm(), trip.getPlannedHighwayKm());
            logger.info(" Новий автомобіль: {}, cityRate={}, highwayRate={} ({})", 
                newVehicle.getDisplayName(),
                isSummer ? newVehicle.getCityRateSummer() : newVehicle.getCityRateWinter(),
                isSummer ? newVehicle.getHighwayRateSummer() : newVehicle.getHighwayRateWinter(),
                isSummer ? "літо" : "зима");
            if (trip.getTripType().isSpecialType()) {
                BigDecimal baseFuel = FuelCalculator.calculateFuelConsumptionUniversal(newVehicle, trip, isSummer);
                BigDecimal refrigeratorPercent = trip.getRefrigeratorUsagePercent();
                if (refrigeratorPercent != null && refrigeratorPercent.compareTo(BigDecimal.ZERO) > 0 && newVehicle.hasRefrigerator()) {
                    baseFuel = FuelCalculator.applyRefrigeratorMultiplier(baseFuel, refrigeratorPercent, true, newVehicle);
                }
                return baseFuel.setScale(2, java.math.RoundingMode.HALF_UP);
            }

            BigDecimal cityKm = trip.getPlannedCityKm() != null ? trip.getPlannedCityKm() : BigDecimal.ZERO;
            BigDecimal highwayKm = trip.getPlannedHighwayKm() != null ? trip.getPlannedHighwayKm() : BigDecimal.ZERO;
            BigDecimal totalKm = trip.getPlannedDistance() != null ? trip.getPlannedDistance() : BigDecimal.ZERO;
            if (cityKm.compareTo(BigDecimal.ZERO) == 0 && highwayKm.compareTo(BigDecimal.ZERO) == 0 
                && totalKm.compareTo(BigDecimal.ZERO) > 0) {
                highwayKm = totalKm;
                logger.info("city/highway відстані не задані, використовуємо totalKm {} як трасу", totalKm);
            }
            RouteInfo routeInfo = new RouteInfo();
            routeInfo.setCityDistance(cityKm);
            routeInfo.setHighwayDistance(highwayKm);
            routeInfo.setTotalDistance(totalKm);
            logger.info("RouteInfo для розрахунку: total={}, city={}, highway={}", 
                routeInfo.getTotalDistance(), routeInfo.getCityDistance(), routeInfo.getHighwayDistance());

            BigDecimal baseFuel = FuelCalculator.calculatePlannedFuelConsumption(newVehicle, routeInfo, isSummer);
            logger.info(" Розраховано базове паливо: {} л", baseFuel);

            BigDecimal refrigeratorPercent = trip.getRefrigeratorUsagePercent();
            if (refrigeratorPercent != null && refrigeratorPercent.compareTo(BigDecimal.ZERO) > 0 && newVehicle.hasRefrigerator()) {
                baseFuel = FuelCalculator.applyRefrigeratorMultiplier(baseFuel, refrigeratorPercent, true, newVehicle);
            }

            return baseFuel.setScale(2, java.math.RoundingMode.HALF_UP);

        } catch (Exception e) {
            logger.error("Помилка розрахунку палива для нового автомобіля: ", e);
            return null;
        }
    }

    private void performVehicleChange(Vehicle newVehicle) {
        try {
            logger.info(" Зміна автомобіля для поїздки {} з {} на {}", 
                trip.getTripNumber(), 
                vehicle != null ? vehicle.getDisplayName() : trip.getVehicleId(),
                newVehicle.getDisplayName());

            BigDecimal newFuelConsumption = calculateFuelForVehicle(newVehicle);

            if (newFuelConsumption == null) {
                showError("Помилка", "Не вдалося розрахувати витрати палива для нового автомобіля.");
                return;
            }

            BigDecimal oldFuelConsumption = trip.getPlannedFuelConsumption();
            String oldVehicleName = vehicle != null ? vehicle.getDisplayName() : "ID: " + trip.getVehicleId();

            boolean success = tripDAO.updateVehicle(trip.getId(), newVehicle.getId(), newFuelConsumption);

            if (success) {
                trip.setVehicleId(newVehicle.getId());
                trip.setPlannedFuelConsumption(newFuelConsumption);
                this.vehicle = newVehicle;

                populateData();

                if (onChangeCallback != null) {
                    onChangeCallback.run();
                }

                String fuelDiff = "";
                if (oldFuelConsumption != null) {
                    BigDecimal diff = newFuelConsumption.subtract(oldFuelConsumption);
                    String sign = diff.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                    fuelDiff = " (" + sign + String.format("%.2f", diff) + " л)";
                }

                showInfo("Успішно!", 
                    "Автомобіль поїздки успішно змінено!\n\n" +
                    "Було: " + oldVehicleName + "\n" +
                    "Стало: " + newVehicle.getDisplayName() + "\n\n" +
                    "Нові планові витрати палива: " + String.format("%.2f", newFuelConsumption) + " л" + fuelDiff);

                logger.info("Автомобіль успішно змінено. Нові витрати: {} л", newFuelConsumption);
            } else {
                showError("Помилка", "Не вдалося оновити автомобіль в базі даних.");
            }

        } catch (Exception e) {
            logger.error("Помилка зміни автомобіля: ", e);
            showError("Помилка", "Сталася помилка при зміні автомобіля:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
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
                performDelete();
            }
        });
    }

    @FXML
    private void handleRestore() {
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
                performRestore();
            }
        });
    }

    private void performDelete() {
        try {
            logger.info(" Видалення поїздки {} (ID: {})", trip.getTripNumber(), trip.getId());

            boolean success = tripDAO.softDelete(trip.getId());

            if (success) {
                showInfo("Успішно!", "Поїздка " + trip.getTripNumber() + " успішно видалена.");
                if (onChangeCallback != null) {
                    onChangeCallback.run();
                }
                handleBack();
                logger.info(" Поїздка {} успішно видалена", trip.getTripNumber());
            } else {
                showError("Помилка", "Не вдалося видалити поїздку. Спробуйте ще раз.");
                logger.error(" Не вдалося видалити поїздку {}", trip.getTripNumber());
            }

        } catch (Exception e) {
            logger.error("Помилка видалення поїздки {}: ", trip.getTripNumber(), e);
            showError("Помилка", "Сталася помилка при видаленні поїздки:\n" + e.getMessage());
        }
    }

    private void performRestore() {
        try {
            logger.info("Відновлення поїздки {} (ID: {})", trip.getTripNumber(), trip.getId());

            boolean success = tripDAO.restore(trip.getId());

            if (success) {
                showInfo("Успішно!", "Поїздка " + trip.getTripNumber() + " успішно відновлена.");
                if (onChangeCallback != null) {
                    onChangeCallback.run();
                }
                handleBack();
                logger.info(" Поїздка {} успішно відновлена", trip.getTripNumber());
            } else {
                showError("Помилка", "Не вдалося відновити поїздку. Спробуйте ще раз.");
                logger.error(" Не вдалося відновити поїздку {}", trip.getTripNumber());
            }

        } catch (Exception e) {
            logger.error(" Помилка відновлення поїздки {}: ", trip.getTripNumber(), e);
            showError("Помилка", "Сталася помилка при відновленні поїздки:\n" + e.getMessage());
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
