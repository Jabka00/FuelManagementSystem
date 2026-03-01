package org.example.fuelmanagement.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class MenuController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

    @FXML private Button btnCreateTrip;
    @FXML private Button btnViewHistory;
    @FXML private Button btnViewStatistics;
    @FXML private Button btnViewVehicles;
    @FXML private Button btnAddDriver;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Ініціалізація головного меню");
    }

    @FXML
    private void handleCreateTrip() {
        try {
            logger.info("Перехід до створення поїздки");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load());

            Stage currentStage = (Stage) btnCreateTrip.getScene().getWindow();
            currentStage.setScene(scene);
            currentStage.setTitle("Створення поїздки - Система обліку палива");
            currentStage.setWidth(1200);
            currentStage.setHeight(800);
            currentStage.setMinWidth(1000);
            currentStage.setMinHeight(700);

        } catch (Exception e) {
            logger.error("Помилка переходу до створення поїздки: ", e);
            showError("Помилка", "Не вдалося відкрити форму створення поїздки:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleViewHistory() {
        try {
            logger.info("Перехід до історії поїздок");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history.fxml"));
            Scene scene = new Scene(loader.load());

            Stage currentStage = (Stage) btnViewHistory.getScene().getWindow();
            currentStage.setScene(scene);
            currentStage.setTitle("Історія поїздок - Система обліку палива");
            currentStage.setWidth(1200);
            currentStage.setHeight(800);
            currentStage.setMinWidth(1000);
            currentStage.setMinHeight(700);

        } catch (Exception e) {
            logger.error("Помилка переходу до історії поїздок: ", e);
            showError("Помилка", "Не вдалося відкрити історію поїздок:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleViewStatistics() {
        try {
            logger.info("Перехід до статистики");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics.fxml"));
            Scene scene = new Scene(loader.load());

            Stage currentStage = (Stage) btnViewStatistics.getScene().getWindow();
            currentStage.setScene(scene);
            currentStage.setTitle("Статистика автопарку - Система обліку палива");
            currentStage.setWidth(1200);
            currentStage.setHeight(800);
            currentStage.setMinWidth(1000);
            currentStage.setMinHeight(700);

        } catch (Exception e) {
            logger.error("Помилка переходу до статистики: ", e);
            showError("Помилка", "Не вдалося відкрити статистику:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleViewVehicles() {
        try {
            logger.info("Перехід до автопарку");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/vehicles.fxml"));
            Scene scene = new Scene(loader.load());

            Stage currentStage = (Stage) btnViewVehicles.getScene().getWindow();
            currentStage.setScene(scene);
            currentStage.setTitle("Автопарк - Система обліку палива");
            currentStage.setWidth(1200);
            currentStage.setHeight(800);
            currentStage.setMinWidth(1000);
            currentStage.setMinHeight(700);

        } catch (Exception e) {
            logger.error("Помилка переходу до автопарку: ", e);
            showError("Помилка", "Не вдалося відкрити автопарк:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleAddDriver() {
        try {
            logger.info("Перехід до управління водіями");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/driver-create-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            Stage currentStage = (Stage) btnAddDriver.getScene().getWindow();
            currentStage.setScene(scene);
            currentStage.setTitle("Управління водіями - Система обліку палива");
            currentStage.setWidth(1200);
            currentStage.setHeight(800);
            currentStage.setMinWidth(1000);
            currentStage.setMinHeight(700);

        } catch (Exception e) {
            logger.error("Помилка переходу до управління водіями: ", e);
            showError("Помилка", "Не вдалося відкрити управління водіями:\n" + e.getMessage());
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
}