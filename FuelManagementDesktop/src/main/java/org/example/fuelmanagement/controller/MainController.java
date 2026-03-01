package org.example.fuelmanagement.controller;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.fuelmanagement.config.DatabaseConfig;
import org.example.fuelmanagement.controller.handler.EmailHandler;
import org.example.fuelmanagement.controller.handler.WaypointHandler;
import org.example.fuelmanagement.controller.helper.AlertHelper;
import org.example.fuelmanagement.controller.helper.FormValidationHelper;
import org.example.fuelmanagement.controller.helper.RouteCalculationHelper;
import org.example.fuelmanagement.controller.helper.TripFormBuilder;
import org.example.fuelmanagement.dao.DriverDAO;
import org.example.fuelmanagement.dao.TripDAO;
import org.example.fuelmanagement.dao.VehicleDAO;
import org.example.fuelmanagement.model.Driver;
import org.example.fuelmanagement.model.RouteInfo;
import org.example.fuelmanagement.model.Trip;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.Waypoint;
import org.example.fuelmanagement.model.enums.TripType;
import org.example.fuelmanagement.service.EmailService;
import org.example.fuelmanagement.service.GoogleMapsService;
import org.example.fuelmanagement.util.EmailParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private Button btnCheckEmail;
    @FXML private TextArea txtEmailStatus;
    @FXML private ProgressIndicator progressEmail;

    @FXML private TextField txtRequesterName;
    @FXML private TextField txtRequesterEmail;
    @FXML private TextField txtRequesterPhone;

    @FXML private TextField txtStartAddress;
    @FXML private TextField txtEndAddress;
    @FXML private TextArea txtPurpose;
    @FXML private ComboBox<String> cmbTripType;
    @FXML private DatePicker dpStartDate;
    @FXML private ComboBox<Integer> cmbStartHour;
    @FXML private ComboBox<Integer> cmbStartMinute;
    @FXML private DatePicker dpEndDate;
    @FXML private ComboBox<Integer> cmbEndHour;
    @FXML private ComboBox<Integer> cmbEndMinute;
    @FXML private ComboBox<Driver> cmbDriver;
    @FXML private ComboBox<Vehicle> cmbVehicle;
    @FXML private CheckBox chkCanDriverDeliver;
    @FXML private TextField txtPowerOfAttorney;

    @FXML private VBox refrigeratorPanel;
    @FXML private CheckBox chkRefrigeratorEnabled;
    @FXML private Label lblRefrigeratorPercent;
    @FXML private HBox hboxRefrigeratorPercent;
    @FXML private TextField txtRefrigeratorPercent;
    @FXML private Label lblRefrigeratorHint;

    @FXML private Button btnCreateTrip;
    @FXML private Button btnSpecialTrip;
    @FXML private Button btnClearForm;
    @FXML private Button btnTestDB;
    @FXML private Button btnCalculateRoute;
    @FXML private Button btnBackToMenu;

    @FXML private TableView<EmailParser.ParsedEmailData> tblParsedEmails;
    @FXML private TableColumn<EmailParser.ParsedEmailData, String> colEmailStatus;
    @FXML private TableColumn<EmailParser.ParsedEmailData, String> colRequesterName;
    @FXML private TableColumn<EmailParser.ParsedEmailData, String> colRequesterPhone;
    @FXML private TableColumn<EmailParser.ParsedEmailData, String> colTripType;
    @FXML private TableColumn<EmailParser.ParsedEmailData, String> colStartAddress;
    @FXML private TableColumn<EmailParser.ParsedEmailData, String> colEndAddress;
    @FXML private TableColumn<EmailParser.ParsedEmailData, String> colStartTime;
    @FXML private TableColumn<EmailParser.ParsedEmailData, String> colPurpose;

    @FXML private TextArea txtRouteInfo;
    @FXML private TextArea logArea;

    @FXML private Button btnAddWaypoint;
    @FXML private TableView<Waypoint> tblWaypoints;
    @FXML private TableColumn<Waypoint, Integer> colWaypointOrder;
    @FXML private TableColumn<Waypoint, String> colWaypointAddress;
    @FXML private TableColumn<Waypoint, String> colWaypointDescription;
    @FXML private TableColumn<Waypoint, String> colWaypointStopTime;
    @FXML private TableColumn<Waypoint, String> colWaypointActions;

    private DriverDAO driverDAO;
    private VehicleDAO vehicleDAO;
    private TripDAO tripDAO;

    private EmailService emailService;
    private GoogleMapsService googleMapsService;

    private EmailHandler emailHandler;
    private WaypointHandler waypointHandler;
    private RouteCalculationHelper routeHelper;

    private static ObservableList<EmailParser.ParsedEmailData> parsedEmailsData;
    private ObservableList<Waypoint> waypointsData;
    private RouteInfo currentRoute;
    private EmailParser.ParsedEmailData currentSelectedEmail;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Ініціалізація головного контролера");

        try {
            initializeDAOs();
            initializeServices();
            initializeHelpers();
            setupUI();
            loadData();

            logInitializationStatus();
            logger.info("Головний контролер успішно ініціалізовано");

        } catch (Exception e) {
            logger.error("Помилка ініціалізації контролера: ", e);
            logArea.appendText("Помилка ініціалізації: " + e.getMessage() + "\n");
            AlertHelper.showError("Помилка ініціалізації", 
                    "Не вдалося ініціалізувати контролер: " + e.getMessage());
        }
    }

    private void initializeDAOs() {
        driverDAO = new DriverDAO();
        vehicleDAO = new VehicleDAO();
        tripDAO = new TripDAO();
        vehicleDAO.createTestVehicleIfNotExists();
    }

    private void initializeServices() {
        setupEmailService();
        setupGoogleMapsService();
    }

    private void initializeHelpers() {
        emailHandler = new EmailHandler(emailService, tripDAO);
        routeHelper = new RouteCalculationHelper(googleMapsService);
    }

    private void loadData() {
        loadDrivers();
        loadVehicles();
    }

    private void logInitializationStatus() {
        logArea.appendText("Система готова до роботи\n");
        logArea.appendText("Email: готовий | Maps: " +
                (googleMapsService.isConfigured() ? "готовий" : "не налаштований") + "\n");
        logArea.appendText("Водії та автомобілі завантажені\n");

        if (parsedEmailsData != null && !parsedEmailsData.isEmpty()) {
            logArea.appendText("Відновлено " + parsedEmailsData.size() + " заявок з попереднього сеансу\n");
            emailHandler.checkUsedEmails(parsedEmailsData);
            tblParsedEmails.refresh();
        }
        logArea.appendText("\nНатисніть 'Перевірити пошту' для пошуку нових заявок\n");
    }

    private void setupGoogleMapsService() {
        try {
            googleMapsService = new GoogleMapsService();

            if (!googleMapsService.isConfigured()) {
                logArea.appendText("Google Maps API не налаштований. Додайте API ключ в application.properties\n");
                btnCalculateRoute.setDisable(true);
            } else {
                logArea.appendText("Google Maps API працює\n");
            }
        } catch (Exception e) {
            logger.error("Помилка ініціалізації Google Maps Service: ", e);
            logArea.appendText("Помилка Google Maps: " + e.getMessage() + "\n");
            btnCalculateRoute.setDisable(true);
        }
    }

    private void setupEmailService() {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            
            Properties config = new Properties();
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties")) {
                if (inputStream != null) {
                    config.load(inputStream);
                } else {
                    logger.warn("Файл application.properties не знайдено");
                }
            }

            String host = getEnvOrProperty(dotenv, config, "EMAIL_SERVER", "email.server", "imap.gmail.com");
            int port = Integer.parseInt(getEnvOrProperty(dotenv, config, "EMAIL_PORT", "email.port", "993"));
            String username = getEnvOrProperty(dotenv, config, "EMAIL_USERNAME", "email.username", "");
            String password = getEnvOrProperty(dotenv, config, "EMAIL_PASSWORD", "email.password", "");

            if (!username.isEmpty() && !password.isEmpty()) {
                emailService = new EmailService(host, port, username, password);

                if (emailService.testConnection()) {
                    txtEmailStatus.setText("Email сервіс налаштовано і готовий до роботи.\n" +
                            "Пошук листів з темою: 'Запит в Автотранспортний відділ'");
                    logArea.appendText("Email налаштовано: " + username + "\n");
                    logArea.appendText("Цільова тема: 'Запит в Автотранспортний відділ'\n");
                    logArea.appendText("Тест підключення успішний\n");
                } else {
                    txtEmailStatus.setText("Email налаштовано, але підключення невдале. Перевірте налаштування.");
                    logArea.appendText("Email: проблема з підключенням\n");
                    btnCheckEmail.setDisable(true);
                }
            } else {
                txtEmailStatus.setText("Email сервіс не налаштовано.\n" +
                        "Додайте EMAIL_USERNAME та EMAIL_PASSWORD в .env файл");
                btnCheckEmail.setDisable(true);
                logArea.appendText("Email не налаштовано (відсутні логін/пароль)\n");
            }
        } catch (Exception e) {
            logger.error("Помилка налаштування email сервісу: ", e);
            txtEmailStatus.setText("Помилка налаштування email: " + e.getMessage());
            btnCheckEmail.setDisable(true);
            logArea.appendText("Email помилка: " + e.getMessage() + "\n");
        }
    }
    
    private String getEnvOrProperty(Dotenv dotenv, Properties config, String envKey, String propertyKey, String defaultValue) {
        String envValue = dotenv.get(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return config.getProperty(propertyKey, defaultValue);
    }

    private void setupUI() {
        setupTripTypeComboBox();
        setupTimeComboBoxes();
        setupDataTables();
        setupListeners();
        setupValidationListeners();

        progressEmail.setVisible(false);

        if (txtRouteInfo != null) {
            txtRouteInfo.setText("Оберіть автомобіль та введіть адреси для розрахунку витрат палива");
            txtRouteInfo.setEditable(false);
        }
    }

    private void setupTripTypeComboBox() {
        cmbTripType.setItems(FXCollections.observableArrayList("В один бік", "В обидві сторони"));
        cmbTripType.setValue("В обидві сторони");
    }

    private void setupTimeComboBoxes() {
        ObservableList<Integer> hours = FXCollections.observableArrayList();
        ObservableList<Integer> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) hours.add(i);
        for (int i = 0; i < 60; i++) minutes.add(i);

        cmbStartHour.setItems(hours);
        cmbStartMinute.setItems(minutes);
        cmbEndHour.setItems(hours);
        cmbEndMinute.setItems(minutes);

        setupTimeCellFactories(cmbStartHour);
        setupTimeCellFactories(cmbStartMinute);
        setupTimeCellFactories(cmbEndHour);
        setupTimeCellFactories(cmbEndMinute);
    }

    private void setupTimeCellFactories(ComboBox<Integer> comboBox) {
        comboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%02d", item));
            }
        });
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%02d", item));
            }
        });
    }

    private void setupDataTables() {
        if (parsedEmailsData == null) {
            parsedEmailsData = FXCollections.observableArrayList();
        }
        waypointsData = FXCollections.observableArrayList();

        tblParsedEmails.setItems(parsedEmailsData);
        tblWaypoints.setItems(waypointsData);

        waypointHandler = new WaypointHandler(googleMapsService, waypointsData, tblWaypoints);

        setupEmailTableColumns();
        setupWaypointTableColumns();
        setupEmailTableRowFactory();
    }

    private void setupEmailTableColumns() {
        colEmailStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    EmailParser.ParsedEmailData data = getTableRow().getItem();
                    Label badge = createStatusBadge(data);
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        colEmailStatus.setCellValueFactory(cellData -> new SimpleStringProperty(""));

        colRequesterName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().requesterName));
        colRequesterPhone.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().requesterPhone));
        colTripType.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().tripType != null
                                ? (cellData.getValue().tripType == TripType.ROUND_TRIP ? "В обидві сторони" : "В один бік")
                                : "Не визначено"));
        colStartAddress.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().startAddress));
        colEndAddress.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().endAddress));
        colStartTime.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().plannedStartTime != null
                                ? cellData.getValue().plannedStartTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                                : "Не вказано"));
        colPurpose.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().purpose != null && cellData.getValue().purpose.length() > 80
                                ? cellData.getValue().purpose.substring(0, 80) + "..."
                                : cellData.getValue().purpose));
    }

    private Label createStatusBadge(EmailParser.ParsedEmailData data) {
        Label badge = new Label();
        badge.setPrefWidth(75);
        badge.setAlignment(Pos.CENTER);

        if (data.isUsed) {
            badge.setText("Виконано");
            badge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; " +
                    "-fx-background-radius: 12; -fx-border-color: #A5D6A7; " +
                    "-fx-border-radius: 12; -fx-border-width: 1;");
        } else if (!data.isValid) {
            badge.setText("Помилка");
            badge.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; " +
                    "-fx-background-radius: 12; -fx-border-color: #EF9A9A; " +
                    "-fx-border-radius: 12; -fx-border-width: 1;");
        } else {
            badge.setText("Нова");
            badge.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 8; " +
                    "-fx-background-radius: 12; -fx-border-color: #90CAF9; " +
                    "-fx-border-radius: 12; -fx-border-width: 1;");
        }
        return badge;
    }

    private void setupWaypointTableColumns() {
        colWaypointOrder.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getSequenceOrder()).asObject());
        colWaypointAddress.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAddress()));
        colWaypointDescription.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription()));
        colWaypointStopTime.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFormattedStopTime()));

        colWaypointActions.setCellFactory(__ -> new TableCell<>() {
            private final Button editButton = new Button("Редагувати");
            private final Button deleteButton = new Button("Видалити");

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                        "-fx-background-radius: 4; -fx-padding: 4 8;");
                deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; " +
                        "-fx-background-radius: 4; -fx-padding: 4 8;");

                editButton.setOnAction(__ -> {
                    Waypoint waypoint = getTableView().getItems().get(getIndex());
                    if (waypointHandler.editWaypoint(waypoint, btnAddWaypoint.getScene().getWindow())) {
                        logArea.appendText("Оновлено проміжну точку: " + waypoint.getFullDescription() + "\n");
                        triggerRouteRecalculation();
                    }
                });

                deleteButton.setOnAction(__ -> {
                    Waypoint waypoint = getTableView().getItems().get(getIndex());
                    if (waypointHandler.deleteWaypoint(waypoint)) {
                        logArea.appendText("Видалено проміжну точку\n");
                        triggerRouteRecalculation();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, editButton, deleteButton));
            }
        });
    }

    private void setupEmailTableRowFactory() {
        tblParsedEmails.setRowFactory(tableView -> {
            TableRow<EmailParser.ParsedEmailData> row = new TableRow<>() {
                @Override
                protected void updateItem(EmailParser.ParsedEmailData item, boolean empty) {
                    super.updateItem(item, empty);
                    setStyle(empty || item == null ? "" : (item.isUsed ? "-fx-background-color: #FAFAFA;" : ""));
                }
            };
            row.setPrefHeight(60.0);
            row.setMinHeight(50.0);
            row.setMaxHeight(80.0);
            return row;
        });
    }

    private void setupListeners() {
        tblParsedEmails.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        fillFormFromParsedEmail(newSelection);
                    }
                });

        txtStartAddress.textProperty().addListener((obs, oldVal, newVal) -> triggerRouteRecalculation());
        txtEndAddress.textProperty().addListener((obs, oldVal, newVal) -> triggerRouteRecalculation());
        cmbVehicle.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRefrigeratorPanelVisibility(newVal);
            triggerRouteRecalculation();
        });

        cmbTripType.valueProperty().addListener((_, _, newValue) -> {
            if (currentRoute != null && cmbVehicle.getValue() != null) {
                logger.info("Зміна типу поїздки на: {}", newValue);
                displayRouteInfo(currentRoute, false);
                logArea.appendText("Тип поїздки змінено на: " + newValue + "\n");
            }
        });

        chkRefrigeratorEnabled.selectedProperty().addListener((_, _, isSelected) -> {
            lblRefrigeratorPercent.setVisible(isSelected);
            lblRefrigeratorPercent.setManaged(isSelected);
            hboxRefrigeratorPercent.setVisible(isSelected);
            hboxRefrigeratorPercent.setManaged(isSelected);
            if (isSelected && txtRefrigeratorPercent.getText().isEmpty()) {
                txtRefrigeratorPercent.setText("100");
            }
            triggerRouteRecalculation();
        });

        txtRefrigeratorPercent.textProperty().addListener((_, _, _) -> {
            if (chkRefrigeratorEnabled.isSelected()) {
                triggerRouteRecalculation();
            }
        });
    }

    private void setupValidationListeners() {
        txtRequesterName.textProperty().addListener((_, _, _) -> 
                FormValidationHelper.clearFieldError(txtRequesterName));
        txtRequesterEmail.textProperty().addListener((_, _, newVal) -> {
            if (!newVal.trim().isEmpty() && FormValidationHelper.isValidEmail(newVal.trim())) {
                FormValidationHelper.clearFieldError(txtRequesterEmail);
            } else if (!newVal.trim().isEmpty()) {
                FormValidationHelper.markFieldAsError(txtRequesterEmail, "Некоректний формат email");
            } else {
                FormValidationHelper.clearFieldError(txtRequesterEmail);
            }
        });
        txtRequesterPhone.textProperty().addListener((_, _, _) -> 
                FormValidationHelper.clearFieldError(txtRequesterPhone));
        txtStartAddress.textProperty().addListener((_, _, _) -> 
                FormValidationHelper.clearFieldError(txtStartAddress));
        txtEndAddress.textProperty().addListener((_, _, _) -> 
                FormValidationHelper.clearFieldError(txtEndAddress));
        txtPurpose.textProperty().addListener((_, _, _) -> 
                FormValidationHelper.clearTextAreaError(txtPurpose));
        cmbVehicle.valueProperty().addListener((_, _, _) -> 
                FormValidationHelper.clearComboBoxError(cmbVehicle));
        cmbDriver.valueProperty().addListener((_, _, _) -> 
                FormValidationHelper.clearComboBoxError(cmbDriver));
        dpStartDate.valueProperty().addListener((_, _, _) -> 
                FormValidationHelper.clearDatePickerError(dpStartDate));
    }

    private void loadDrivers() {
        Task<List<Driver>> loadDriversTask = new Task<>() {
            @Override
            protected List<Driver> call() throws Exception {
                return driverDAO.findAllActive();
            }

            @Override
            protected void succeeded() {
                List<Driver> drivers = getValue();
                Platform.runLater(() -> {
                    cmbDriver.setItems(FXCollections.observableArrayList(drivers));
                    logger.info("Завантажено {} водіїв", drivers.size());
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    logger.error("Помилка завантаження водіїв: ", getException());
                    logArea.appendText("Помилка завантаження водіїв: " + getException().getMessage() + "\n");
                    AlertHelper.showError("Помилка", "Не вдалося завантажити список водіїв");
                });
            }
        };
        new Thread(loadDriversTask).start();
    }

    private void loadVehicles() {
        Task<List<Vehicle>> loadVehiclesTask = new Task<>() {
            @Override
            protected List<Vehicle> call() throws Exception {
                return vehicleDAO.findAllActive();
            }

            @Override
            protected void succeeded() {
                List<Vehicle> vehicles = getValue();
                Platform.runLater(() -> {
                    cmbVehicle.setItems(FXCollections.observableArrayList(vehicles));
                    logger.info("Завантажено {} автомобілів", vehicles.size());
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    logger.error("Помилка завантаження автомобілів: ", getException());
                    logArea.appendText("Помилка завантаження автомобілів: " + getException().getMessage() + "\n");
                    AlertHelper.showError("Помилка", "Не вдалося завантажити список автомобілів");
                });
            }
        };
        new Thread(loadVehiclesTask).start();
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
            AlertHelper.showError("Помилка", "Не вдалося повернутися до головного меню:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleCheckEmail() {
        emailHandler.checkEmails(
                parsedEmailsData,
                addedCount -> {
                    tblParsedEmails.refresh();
                    if (addedCount == 0 && parsedEmailsData.isEmpty()) {
                        txtEmailStatus.setText("Не знайдено нових листів з темою 'Запит в Автотранспортний відділ'.\n" +
                                "Перевірте чи є непрочитані листи з відповідною темою.");
                        logArea.appendText("Заявок не знайдено\n");
                    } else if (addedCount == 0) {
                        txtEmailStatus.setText("Всі знайдені заявки вже є в таблиці.");
                        logArea.appendText("Нових заявок немає (всі вже в таблиці)\n");
                    } else {
                        txtEmailStatus.setText(String.format("Додано %d нових заявок! (всього: %d)\n" +
                                "Клікніть на заявку в таблиці для автозаповнення форми.", addedCount, parsedEmailsData.size()));
                        logArea.appendText(String.format("Додано %d нових заявок (всього в таблиці: %d)\n",
                                addedCount, parsedEmailsData.size()));
                        AlertHelper.showInfo("Нові заявки",
                                String.format("Додано %d нових заявок!\n\nВсього в таблиці: %d заявок.\n" +
                                        "Оберіть потрібну заявку для заповнення форми.", addedCount, parsedEmailsData.size()));
                    }
                },
                error -> {
                    String errorMsg = error.getMessage();
                    txtEmailStatus.setText("Помилка перевірки пошти: " + errorMsg);
                    logArea.appendText("Помилка email: " + errorMsg + "\n");
                    String hint = emailHandler.getErrorHint(errorMsg);
                    if (hint != null) {
                        logArea.appendText(hint + "\n");
                    }
                },
                () -> {
                    btnCheckEmail.setDisable(true);
                    progressEmail.setVisible(true);
                    txtEmailStatus.setText("Пошук листів з темою 'Запит в Автотранспортний відділ'...");
                    logArea.appendText("\nПошук заявок...\n");
                },
                () -> {
                    progressEmail.setVisible(false);
                    btnCheckEmail.setDisable(false);
                }
        );
    }

    @FXML
    private void handleCalculateRoute() {
        calculateRoute(true);
    }

    @FXML
    private void handleAddWaypoint() {
        if (waypointHandler.addWaypoint(btnAddWaypoint.getScene().getWindow())) {
            logArea.appendText("Додано проміжну точку\n");
            triggerRouteRecalculation();
        }
    }

    @FXML
    private void handleCreateTrip() {
        if (!validateTripForm()) {
            return;
        }

        logArea.appendText("\nСтворення поїздки...\n");
        btnCreateTrip.setDisable(true);

        Task<Trip> createTripTask = new Task<>() {
            @Override
            protected Trip call() throws Exception {
                return tripDAO.create(buildTripFromForm());
            }

            @Override
            protected void succeeded() {
                Trip createdTrip = getValue();
                Platform.runLater(() -> {
                    btnCreateTrip.setDisable(false);

                    if (createdTrip != null) {
                        logArea.appendText("Поїздка створена (#" + createdTrip.getTripNumber() + ")\n");
                        if (currentSelectedEmail != null) {
                            currentSelectedEmail.isUsed = true;
                            tblParsedEmails.refresh();
                            logArea.appendText("Заявку відмічено як використану\n");
                        }
                        showTripCreatedDialog(createdTrip);
                        handleClearForm();
                    } else {
                        logArea.appendText("Помилка створення поїздки\n");
                        AlertHelper.showError("Помилка", "Не вдалося створити поїздку. Перевірте логи для деталей.");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    btnCreateTrip.setDisable(false);
                    String errorMsg = getException().getMessage();
                    logArea.appendText("Помилка створення поїздки: " + errorMsg + "\n");
                    logger.error("Помилка створення поїздки: ", getException());
                    AlertHelper.showError("Помилка створення поїздки", "Не вдалося створити поїздку:\n" + errorMsg);
                });
            }
        };
        new Thread(createTripTask).start();
    }

    @FXML
    private void handleSpecialTrip() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/special-trip-dialog.fxml"));
            VBox dialogRoot = loader.load();

            SpecialTripDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Спеціальна робота");
            dialogStage.initOwner(btnSpecialTrip.getScene().getWindow());
            Scene scene = new Scene(dialogRoot, 650, 700);
            dialogStage.setScene(scene);
            dialogStage.setMinWidth(600);
            dialogStage.setMinHeight(550);
            dialogStage.setResizable(true);

            dialogStage.showAndWait();

            Trip createdTrip = controller.getCreatedTrip();
            if (createdTrip != null) {
                logArea.appendText("Створено спеціальну поїздку: " + createdTrip.getTripType() + "\n");
                logArea.appendText("   Витрата палива: " + createdTrip.getPlannedFuelConsumption() + " л\n");
                AlertHelper.showInfo("Успішно", "Спеціальну поїздку створено.\n" +
                        "Поїздка типу '" + createdTrip.getTripType() + "' успішно створена.\n" +
                        "Витрата палива: " + createdTrip.getPlannedFuelConsumption() + " л");
            }

        } catch (Exception e) {
            logger.error("Помилка відкриття діалогу спеціальної поїздки", e);
            AlertHelper.showError("Помилка", "Не вдалося відкрити діалог:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClearForm() {
        txtRequesterName.clear();
        txtRequesterEmail.clear();
        txtRequesterPhone.clear();
        txtStartAddress.clear();
        txtEndAddress.clear();
        txtPurpose.clear();
        txtPowerOfAttorney.clear();
        chkCanDriverDeliver.setSelected(false);
        cmbTripType.setValue("В обидві сторони");
        dpStartDate.setValue(null);
        cmbStartHour.setValue(null);
        cmbStartMinute.setValue(null);
        dpEndDate.setValue(null);
        cmbEndHour.setValue(null);
        cmbEndMinute.setValue(null);
        cmbDriver.setValue(null);
        cmbVehicle.setValue(null);

        tblParsedEmails.getSelectionModel().clearSelection();
        waypointHandler.clearWaypoints();
        currentRoute = null;
        currentSelectedEmail = null;

        if (txtRouteInfo != null) {
            txtRouteInfo.setText("Оберіть автомобіль та введіть адреси для розрахунку витрат палива");
        }

        logArea.appendText("\nФорма очищена\n");
    }

    @FXML
    @SuppressWarnings("unused")
    private void testDatabaseConnection() {
        try {
            logArea.appendText("\nТестування підключення до БД...\n");

            try (Connection ignored = DatabaseConfig.getConnection()) {
                logArea.appendText("Підключення до БД успішне!\n");
                logger.info("Тест підключення до БД пройшов успішно");
                AlertHelper.showInfo("Тест БД", "Підключення до бази даних успішне!");
            }

        } catch (Exception e) {
            logArea.appendText("Помилка підключення до БД: " + e.getMessage() + "\n");
            logger.error("Помилка тестування БД: ", e);
            AlertHelper.showError("Помилка БД", "Не вдалося підключитися до бази даних:\n" + e.getMessage());
        }
    }

    private void triggerRouteRecalculation() {
        if (shouldRecalculateRoute()) {
            Platform.runLater(() -> calculateRoute(false));
        }
    }

    private boolean shouldRecalculateRoute() {
        return !txtStartAddress.getText().trim().isEmpty() &&
                !txtEndAddress.getText().trim().isEmpty() &&
                cmbVehicle.getValue() != null;
    }

    private void calculateRoute(boolean showProgress) {
        String startAddr = txtStartAddress.getText().trim();
        String endAddr = txtEndAddress.getText().trim();
        Vehicle selectedVehicle = cmbVehicle.getValue();

        if (startAddr.isEmpty() || endAddr.isEmpty()) {
            if (showProgress) AlertHelper.showError("Помилка", "Введіть початкову та кінцеву адреси");
            return;
        }

        if (selectedVehicle == null) {
            if (showProgress) AlertHelper.showError("Помилка", "Оберіть автомобіль для розрахунку витрат палива");
            return;
        }

        if (!routeHelper.isConfigured()) {
            if (showProgress) AlertHelper.showError("Google Maps API", "API ключ не налаштований. Додайте ключ в application.properties");
            return;
        }

        if (showProgress) {
            btnCalculateRoute.setDisable(true);
            String waypointMsg = !waypointsData.isEmpty() 
                    ? "\nРозрахунок маршруту з " + waypointsData.size() + " проміжними точками...\n"
                    : "\nРозрахунок маршруту...\n";
            logArea.appendText(waypointMsg);
        }

        TripType tripType = getTripType();

        routeHelper.calculateRoute(
                startAddr, endAddr, waypointsData, tripType,
                routeInfo -> {
                    currentRoute = routeInfo;
                    displayRouteInfo(routeInfo, showProgress);
                    if (showProgress) btnCalculateRoute.setDisable(false);
                },
                throwable -> {
                    logger.error("Помилка розрахунку маршруту: ", throwable);
                    if (txtRouteInfo != null) {
                        txtRouteInfo.setText("Помилка розрахунку маршруту: " + throwable.getMessage());
                    }
                    if (showProgress) {
                        logArea.appendText("Помилка розрахунку маршруту: " + throwable.getMessage() + "\n");
                        btnCalculateRoute.setDisable(false);
                    }
                }
        );
    }

    private void displayRouteInfo(RouteInfo routeInfo, boolean showInLog) {
        if (txtRouteInfo == null) return;

        Vehicle vehicle = cmbVehicle.getValue();
        TripType tripType = getTripType();
        BigDecimal refrigeratorPercent = getRefrigeratorUsagePercent();

        String report = routeHelper.createFuelReport(
                routeInfo, vehicle, tripType, refrigeratorPercent,
                waypointsData, txtStartAddress.getText().trim(), txtEndAddress.getText().trim()
        );
        txtRouteInfo.setText(report);

        if (showInLog && !routeInfo.isEmpty()) {
            logArea.appendText(routeHelper.createLogMessage(routeInfo, vehicle, tripType, refrigeratorPercent, waypointsData));
        }
    }

    private void fillFormFromParsedEmail(EmailParser.ParsedEmailData emailData) {
        currentSelectedEmail = emailData;
        logArea.appendText("\nФорма заповнена з заявки\n");
        if (emailData.isUsed) {
            logArea.appendText("Ця заявка вже була використана раніше\n");
        }

        txtRequesterName.setText(emailData.requesterName);
        txtRequesterEmail.setText(emailData.requesterEmail);
        txtRequesterPhone.setText(emailData.requesterPhone);
        txtStartAddress.setText(emailData.startAddress);
        txtEndAddress.setText(emailData.endAddress);
        txtPurpose.setText(emailData.purpose);
        txtPowerOfAttorney.setText(emailData.powerOfAttorney);
        chkCanDriverDeliver.setSelected(emailData.canDriverDeliver);

        cmbTripType.setValue(emailData.tripType == TripType.ROUND_TRIP ? "В обидві сторони" : "В один бік");

        if (emailData.plannedStartTime != null) {
            dpStartDate.setValue(emailData.plannedStartTime.toLocalDate());
            cmbStartHour.setValue(emailData.plannedStartTime.getHour());
            cmbStartMinute.setValue(emailData.plannedStartTime.getMinute());
        }

        if (emailData.plannedEndTime != null) {
            dpEndDate.setValue(emailData.plannedEndTime.toLocalDate());
            cmbEndHour.setValue(emailData.plannedEndTime.getHour());
            cmbEndMinute.setValue(emailData.plannedEndTime.getMinute());
        }

        logArea.appendText("Оберіть автомобіль для розрахунку витрат\n");

        AlertHelper.showInfo("Автозаповнення", "Форма автоматично заповнена з email заявки!\n\n" +
                "Замовник: " + emailData.requesterName + "\n" +
                "Маршрут: " + emailData.startAddress + " → " + emailData.endAddress + "\n\n" +
                "Оберіть автомобіль та водія для завершення створення поїздки");
    }

    private boolean validateTripForm() {
        clearValidationErrors();
        FormValidationHelper.ValidationResult result = new FormValidationHelper.ValidationResult();

        if (txtRequesterName.getText().trim().isEmpty()) {
            FormValidationHelper.markFieldAsError(txtRequesterName, "Обов'язкове поле");
            result.addError("Введіть ПІБ замовника");
        }

        String email = txtRequesterEmail.getText().trim();
        if (!email.isEmpty() && !FormValidationHelper.isValidEmail(email)) {
            FormValidationHelper.markFieldAsError(txtRequesterEmail, "Некоректний email");
            result.addError("Введіть коректний email замовника");
        }

        if (txtRequesterPhone.getText().trim().isEmpty()) {
            FormValidationHelper.markFieldAsWarning(txtRequesterPhone, "Рекомендовано заповнити");
        }

        if (txtStartAddress.getText().trim().isEmpty()) {
            FormValidationHelper.markFieldAsError(txtStartAddress, "Обов'язкове поле");
            result.addError("Введіть початкову адресу");
        }

        if (txtEndAddress.getText().trim().isEmpty()) {
            FormValidationHelper.markFieldAsError(txtEndAddress, "Обов'язкове поле");
            result.addError("Введіть кінцеву адресу");
        }

        if (cmbVehicle.getValue() == null) {
            FormValidationHelper.markComboBoxAsError(cmbVehicle, "Оберіть автомобіль");
            result.addError("Оберіть автомобіль");
        }

        if (cmbDriver.getValue() == null) {
            FormValidationHelper.markComboBoxAsWarning(cmbDriver, "Рекомендовано вибрати водія");
        }

        if (dpStartDate.getValue() == null) {
            FormValidationHelper.markDatePickerAsWarning(dpStartDate);
        }

        if (txtPurpose.getText().trim().isEmpty()) {
            FormValidationHelper.markTextAreaAsWarning(txtPurpose, "Рекомендовано заповнити");
        }

        if (result.hasErrors()) {
            AlertHelper.showError("Помилки валідації",
                    "Виправте наступні помилки:\n\n" + result.getErrorMessage() +
                            "\nПоля з червоною рамкою обов'язкові для заповнення\n" +
                            "Поля з жовтою рамкою рекомендовано заповнити");
            logArea.appendText("Форма містить помилки валідації\n");
            return false;
        }

        return true;
    }

    private void clearValidationErrors() {
        FormValidationHelper.clearFieldError(txtRequesterName);
        FormValidationHelper.clearFieldError(txtRequesterEmail);
        FormValidationHelper.clearFieldError(txtRequesterPhone);
        FormValidationHelper.clearFieldError(txtStartAddress);
        FormValidationHelper.clearFieldError(txtEndAddress);
        FormValidationHelper.clearTextAreaError(txtPurpose);
        FormValidationHelper.clearComboBoxError(cmbVehicle);
        FormValidationHelper.clearComboBoxError(cmbDriver);
        FormValidationHelper.clearDatePickerError(dpStartDate);
    }

    private Trip buildTripFromForm() {
        return new TripFormBuilder()
                .requesterName(txtRequesterName.getText())
                .requesterEmail(txtRequesterEmail.getText())
                .requesterPhone(txtRequesterPhone.getText())
                .startAddress(txtStartAddress.getText())
                .endAddress(txtEndAddress.getText())
                .purpose(txtPurpose.getText())
                .powerOfAttorney(txtPowerOfAttorney.getText())
                .canDriverDeliver(chkCanDriverDeliver.isSelected())
                .tripTypeFromString(cmbTripType.getValue())
                .vehicle(cmbVehicle.getValue())
                .driver(cmbDriver.getValue())
                .startDateTime(dpStartDate.getValue(), cmbStartHour.getValue(), cmbStartMinute.getValue())
                .endDateTime(dpEndDate.getValue(), cmbEndHour.getValue(), cmbEndMinute.getValue())
                .refrigeratorPercent(getRefrigeratorUsagePercent())
                .routeInfo(currentRoute)
                .waypoints(waypointsData)
                .build();
    }

    private void showTripCreatedDialog(Trip trip) {
        String message = TripFormBuilder.createSuccessMessage(
                trip, cmbVehicle.getValue(), cmbDriver.getValue(), waypointsData, currentRoute);

        AlertHelper.showTripCreatedDialog(message).ifPresent(response -> {
            if (response.getButtonData() == ButtonBar.ButtonData.OTHER) {
                txtRequesterName.clear();
                txtRequesterEmail.clear();
                txtRequesterPhone.clear();
                txtStartAddress.clear();
                txtEndAddress.clear();
                txtPurpose.clear();
                txtPowerOfAttorney.clear();

                if (txtRouteInfo != null) {
                    txtRouteInfo.setText("Оберіть автомобіль та введіть адреси для розрахунку витрат палива");
                }
                currentRoute = null;
                logArea.appendText("\nГотово до створення нової поїздки...\n");
            }
        });
    }

    private void updateRefrigeratorPanelVisibility(Vehicle vehicle) {
        if (vehicle != null && vehicle.hasRefrigerator()) {
            refrigeratorPanel.setVisible(true);
            refrigeratorPanel.setManaged(true);
            logArea.appendText("Автомобіль має холодильник - доступні налаштування\n");
        } else {
            refrigeratorPanel.setVisible(false);
            refrigeratorPanel.setManaged(false);
            chkRefrigeratorEnabled.setSelected(false);
        }
    }

    private BigDecimal getRefrigeratorUsagePercent() {
        if (!chkRefrigeratorEnabled.isSelected()) {
            return BigDecimal.ZERO;
        }
        try {
            String text = txtRefrigeratorPercent.getText().trim();
            if (text.isEmpty()) return BigDecimal.ZERO;

            BigDecimal percent = new BigDecimal(text);
            if (percent.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
            if (percent.compareTo(BigDecimal.valueOf(100)) > 0) return BigDecimal.valueOf(100);

            return percent;
        } catch (NumberFormatException e) {
            logger.warn("Некоректний формат відсотка холодильника: {}", txtRefrigeratorPercent.getText());
            return BigDecimal.ZERO;
        }
    }

    private TripType getTripType() {
        return "В обидві сторони".equals(cmbTripType.getValue()) ? TripType.ROUND_TRIP : TripType.ONE_WAY;
    }
}
