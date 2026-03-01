package org.example.fuelmanagement.controller.handler;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.example.fuelmanagement.controller.WaypointDialogController;
import org.example.fuelmanagement.controller.helper.AlertHelper;
import org.example.fuelmanagement.controller.helper.FormValidationHelper;
import org.example.fuelmanagement.model.Waypoint;
import org.example.fuelmanagement.service.GoogleMapsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WaypointHandler {
    private static final Logger logger = LoggerFactory.getLogger(WaypointHandler.class);

    private final GoogleMapsService googleMapsService;
    private final ObservableList<Waypoint> waypointsData;
    private final TableView<Waypoint> tblWaypoints;

    public WaypointHandler(
            GoogleMapsService googleMapsService,
            ObservableList<Waypoint> waypointsData,
            TableView<Waypoint> tblWaypoints
    ) {
        this.googleMapsService = googleMapsService;
        this.waypointsData = waypointsData;
        this.tblWaypoints = tblWaypoints;
    }

    public boolean addWaypoint(Window ownerWindow) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/waypoint-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            WaypointDialogController controller = loader.getController();
            controller.setGoogleMapsService(googleMapsService);
            controller.setExistingWaypoints(waypointsData);

            Waypoint newWaypoint = new Waypoint();
            controller.setWaypoint(newWaypoint);

            Stage dialogStage = createDialogStage("Додати проміжну точку", ownerWindow, scene);
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isSaved()) {
                Waypoint savedWaypoint = controller.getWaypoint();
                if (savedWaypoint != null) {
                    if (isDuplicateWaypoint(savedWaypoint)) {
                        AlertHelper.showWarning("Дублювання адреси",
                                "Проміжна точка з адресою '" + savedWaypoint.getAddress() + 
                                "' вже існує в маршруті.\n\nБудь ласка, введіть іншу адресу або видаліть існуючу точку.");
                        logger.info("Спроба додати дубльовану адресу: {}", savedWaypoint.getAddress());
                        return false;
                    }

                    savedWaypoint.setSequenceOrder(waypointsData.size() + 1);
                    waypointsData.add(savedWaypoint);
                    updateWaypointSequence();
                    logger.info("Додано проміжну точку: {}", savedWaypoint.getFullDescription());
                    return true;
                }
            }

        } catch (IOException e) {
            logger.error("Помилка відкриття діалогу проміжної точки: ", e);
            AlertHelper.showError("Помилка", "Не вдалося відкрити діалог додавання проміжної точки");
        }
        return false;
    }

    public boolean editWaypoint(Waypoint waypoint, Window ownerWindow) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/waypoint-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            WaypointDialogController controller = loader.getController();
            controller.setGoogleMapsService(googleMapsService);
            controller.setExistingWaypoints(waypointsData);
            controller.setWaypoint(waypoint);

            Stage dialogStage = createDialogStage("Редагувати проміжну точку", ownerWindow, scene);
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isSaved()) {
                Waypoint updatedWaypoint = controller.getWaypoint();
                if (updatedWaypoint != null) {
                    int index = waypointsData.indexOf(waypoint);
                    if (index >= 0) {
                        String oldAddress = FormValidationHelper.normalizeAddress(waypoint.getAddress());
                        String newAddress = FormValidationHelper.normalizeAddress(updatedWaypoint.getAddress());

                        if (!oldAddress.equals(newAddress) && isDuplicateWaypoint(updatedWaypoint)) {
                            AlertHelper.showWarning("Дублювання адреси",
                                    "Проміжна точка з адресою '" + updatedWaypoint.getAddress() + 
                                    "' вже існує в маршруті.\n\nБудь ласка, введіть іншу адресу.");
                            logger.info("Спроба змінити на дубльовану адресу: {}", updatedWaypoint.getAddress());
                            return false;
                        }

                        waypointsData.set(index, updatedWaypoint);
                        tblWaypoints.refresh();
                        logger.info("Оновлено проміжну точку: {}", updatedWaypoint.getFullDescription());
                        return true;
                    }
                }
            }

        } catch (IOException e) {
            logger.error("Помилка відкриття діалогу редагування проміжної точки: ", e);
            AlertHelper.showError("Помилка", "Не вдалося відкрити діалог редагування проміжної точки");
        }
        return false;
    }

    public boolean deleteWaypoint(Waypoint waypoint) {
        boolean confirmed = AlertHelper.showConfirmation(
                "Підтвердження видалення",
                "Видалити проміжну точку?",
                "Ви впевнені, що хочете видалити проміжну точку:\n" + waypoint.getFullDescription()
        );

        if (confirmed) {
            waypointsData.remove(waypoint);
            updateWaypointSequence();
            logger.info("Видалено проміжну точку: {}", waypoint.getFullDescription());
            return true;
        }
        return false;
    }

    public void clearWaypoints() {
        waypointsData.clear();
        logger.info("Очищено всі проміжні точки");
    }

    public void updateWaypointSequence() {
        for (int i = 0; i < waypointsData.size(); i++) {
            waypointsData.get(i).setSequenceOrder(i + 1);
        }
        tblWaypoints.refresh();
    }

    public boolean isDuplicateWaypoint(Waypoint newWaypoint) {
        if (newWaypoint == null || newWaypoint.getAddress() == null) {
            return false;
        }

        String newAddress = FormValidationHelper.normalizeAddress(newWaypoint.getAddress());

        for (Waypoint existing : waypointsData) {
            if (existing.getAddress() != null) {
                String existingAddress = FormValidationHelper.normalizeAddress(existing.getAddress());
                if (existingAddress.equals(newAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Stage createDialogStage(String title, Window owner, Scene scene) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setScene(scene);
        dialogStage.setMinWidth(750.0);
        dialogStage.setMinHeight(650.0);
        dialogStage.setResizable(true);
        dialogStage.sizeToScene();
        return dialogStage;
    }

    public ObservableList<Waypoint> getWaypointsData() {
        return waypointsData;
    }
}
