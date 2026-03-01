package org.example.fuelmanagement.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.fuelmanagement.dao.DriverDAO;
import org.example.fuelmanagement.model.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class DriverCreateDialogController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(DriverCreateDialogController.class);
    @FXML private Label lblTitle;
    @FXML private TextField txtFullName;
    @FXML private TextField txtPhone;
    @FXML private Button btnSave;
    @FXML private Button btnClear;
    @FXML private Button btnClose;
    @FXML private Label lblStatus;
    @FXML private TableView<Driver> tableDrivers;
    @FXML private TableColumn<Driver, String> colName;
    @FXML private TableColumn<Driver, String> colPhone;
    private final DriverDAO driverDAO = new DriverDAO();
    private final ObservableList<Driver> driversList = FXCollections.observableArrayList();
    private Driver currentDriver; 
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Ініціалізація діалогу створення/редагування водія");
        setupTable();
        loadDrivers();
        setupEventHandlers();
        updateStatus("Готово до роботи");
    }
    private void setupTable() {
        colName.setCellValueFactory(cellData -> {
            Driver driver = cellData.getValue();
            String name = driver != null && driver.getFullName() != null ? driver.getFullName() : "";
            return new javafx.beans.property.SimpleStringProperty(name);
        });
        colPhone.setCellValueFactory(cellData -> {
            Driver driver = cellData.getValue();
            String phone = driver != null && driver.getPhone() != null ? driver.getPhone() : "";
            return new javafx.beans.property.SimpleStringProperty(phone);
        });
        colName.setCellFactory(column -> new TableCell<Driver, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (getTableRow() != null && getTableRow().isSelected()) {
                        setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: CENTER-LEFT;");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal; -fx-alignment: CENTER-LEFT;");
                    }
                }
            }
        });
        colPhone.setCellFactory(column -> new TableCell<Driver, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (getTableRow() != null && getTableRow().isSelected()) {
                        setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal; -fx-alignment: CENTER;");
                    }
                }
            }
        });
        tableDrivers.setItems(driversList);
        tableDrivers.getSelectionModel().selectedItemProperty().addListener((_, _, newSelection) -> {
            if (newSelection != null) {
                loadDriverForEdit(newSelection);
            }
        });
        tableDrivers.setRowFactory(_ -> new TableRow<Driver>() {
            @Override
            protected void updateItem(Driver item, boolean empty) {
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
                    setStyle("-fx-background-color: #238A90;");
                } else {
                    if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: rgba(174, 233, 237, 0.15);");
                    } else {
                        setStyle("-fx-background-color: white;");
                    }
                }
            }
        });
    }
    private void setupEventHandlers() {
        btnSave.setOnAction(_ -> handleSave());
        btnClear.setOnAction(_ -> handleClear());
        btnClose.setOnAction(_ -> handleClose());
    }
    private void loadDrivers() {
        try {
            logger.info("Завантаження списку водіїв");
            var drivers = driverDAO.findAll();
            logger.info("З БД отримано {} водіїв", drivers.size());
            driversList.clear();
            driversList.addAll(drivers);
            logger.info("Додано до списку {} водіїв", driversList.size());
            if (!drivers.isEmpty()) {
                logger.info("Перший водій: {} - {}", 
                    drivers.get(0).getFullName(), 
                    drivers.get(0).getPhone());
            }
            updateStatus(String.format("Завантажено %d водіїв", drivers.size()));
            tableDrivers.refresh();
        } catch (Exception e) {
            logger.error("Помилка завантаження водіїв: ", e);
            updateStatus("Помилка завантаження водіїв");
            showError("Помилка", "Не вдалося завантажити список водіїв:\n" + e.getMessage());
        }
    }
    private void loadDriverForEdit(Driver driver) {
        try {
            logger.info("Завантаження водія для редагування: {}", driver.getDisplayName());
            currentDriver = driver;
            txtFullName.setText(driver.getFullName() != null ? driver.getFullName() : "");
            txtPhone.setText(driver.getPhone() != null ? driver.getPhone() : "");
            lblTitle.setText(" Редагування водія");
            btnSave.setText("💾 Оновити");
            updateStatus("Редагується: " + driver.getDisplayName());
        } catch (Exception e) {
            logger.error("Помилка завантаження водія для редагування: ", e);
            showError("Помилка", "Не вдалося завантажити дані водія:\n" + e.getMessage());
        }
    }
    @FXML
    private void handleSave() {
        try {
            String fullName = txtFullName.getText().trim();
            String phone = txtPhone.getText().trim();
            if (fullName.isEmpty() || phone.isEmpty()) {
                updateStatus(" Заповніть всі обов'язкові поля");
                showError("Помилка", "Будь ласка, заповніть всі обов'язкові поля:\n- ПІБ\n- Телефон");
                return;
            }
            if (currentDriver != null) {
                currentDriver.setFullName(fullName);
                currentDriver.setPhone(phone);
                if (driverDAO.update(currentDriver)) {
                    logger.info("Водій успішно оновлено: {}", currentDriver.getDisplayName());
                    updateStatus(" Водія успішно оновлено");
                    showSuccess("Успіх", "Водія '" + currentDriver.getDisplayName() + "' успішно оновлено!");
                    loadDrivers();
                    handleClear();
                } else {
                    updateStatus(" Помилка оновлення водія");
                    showError("Помилка", "Не вдалося оновити водія. Спробуйте ще раз.");
                }
            } else {
                Driver newDriver = new Driver(fullName, phone);
                if (saveDriver(newDriver)) {
                    logger.info("Водій успішно створений: {}", newDriver.getDisplayName());
                    updateStatus(" Водія успішно створено");
                    showSuccess("Успіх", "Водія '" + newDriver.getDisplayName() + "' успішно додано!");
                    loadDrivers();
                    handleClear();
                } else {
                    updateStatus(" Помилка створення водія");
                    showError("Помилка", "Не вдалося створити водія. Спробуйте ще раз.");
                }
            }
        } catch (Exception e) {
            logger.error("Помилка збереження водія: ", e);
            updateStatus(" Помилка: " + e.getMessage());
            showError("Помилка", "Помилка збереження водія:\n" + e.getMessage());
        }
    }
    @FXML
    private void handleClear() {
        currentDriver = null;
        txtFullName.clear();
        txtPhone.clear();
        lblTitle.setText("➕ Додати водія");
        btnSave.setText("💾 Зберегти");
        tableDrivers.getSelectionModel().clearSelection();
        updateStatus("Готово до роботи");
    }
    @FXML
    private void handleClose() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
    private boolean saveDriver(Driver driver) {
        try {
            String sql = "INSERT INTO drivers (full_name, phone, status, created_at) " +
                        "VALUES (?, ?, ?, ?)";
            try (var conn = org.example.fuelmanagement.config.DatabaseConfig.getConnection();
                 var stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, driver.getFullName());
                stmt.setString(2, driver.getPhone());
                stmt.setString(3, driver.getStatus().getValue());
                stmt.setTimestamp(4, java.sql.Timestamp.valueOf(driver.getCreatedAt()));
                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    try (var generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            driver.setId(generatedKeys.getInt(1));
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Помилка збереження водія в БД: ", e);
        }
        return false;
    }
    private void updateStatus(String message) {
        if (lblStatus != null) {
            lblStatus.setText(message);
        }
    }
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public Driver createDriver() {
        return null;
    }
}
