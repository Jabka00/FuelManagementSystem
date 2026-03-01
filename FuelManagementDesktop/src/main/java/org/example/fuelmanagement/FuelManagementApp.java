package org.example.fuelmanagement;

import org.example.fuelmanagement.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FuelManagementApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(FuelManagementApp.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Запуск додатку...");

            DatabaseConfig.initialize();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/menu.fxml"));
            if (loader.getLocation() == null) {
                throw new IllegalStateException("Не знайдено файл menu.fxml");
            }

            Scene scene = new Scene(loader.load());

            primaryStage.setScene(scene);
            primaryStage.setTitle("Головне меню - Система обліку палива");
            primaryStage.setWidth(1200);
            primaryStage.setHeight(800);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.setMaximized(false);
            primaryStage.centerOnScreen();

            primaryStage.setOnCloseRequest(event -> {
                logger.info("Закриття додатку...");
                DatabaseConfig.close();
                System.exit(0);
            });

            primaryStage.show();
            logger.info("Додаток успішно запущено з головним меню");

        } catch (Exception e) {
            logger.error("Помилка запуску додатку: ", e);
            showErrorAndExit("Критична помилка", "Не вдалося запустити додаток:\n" + e.getMessage());
        }
    }

    private void showErrorAndExit(String title, String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println(title + ": " + message);
            e.printStackTrace();
        }
        System.exit(1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}