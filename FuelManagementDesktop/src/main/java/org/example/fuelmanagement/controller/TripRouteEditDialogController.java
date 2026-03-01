package org.example.fuelmanagement.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.fuelmanagement.dao.TripDAO;
import org.example.fuelmanagement.dao.VehicleDAO;
import org.example.fuelmanagement.dao.WaypointDAO;
import org.example.fuelmanagement.model.RouteInfo;
import org.example.fuelmanagement.model.Trip;
import org.example.fuelmanagement.model.Vehicle;
import org.example.fuelmanagement.model.Waypoint;
import org.example.fuelmanagement.model.enums.Season;
import org.example.fuelmanagement.model.enums.TripType;
import org.example.fuelmanagement.service.GoogleMapsService;
import org.example.fuelmanagement.util.FuelCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

public class TripRouteEditDialogController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(TripRouteEditDialogController.class);

    @FXML private Label lblTripInfo;
    @FXML private Label lblVehicleInfo;
    @FXML private TextField txtStartAddress;
    @FXML private TextField txtEndAddress;
    @FXML private TableView<Waypoint> tblWaypoints;
    @FXML private TableColumn<Waypoint, Integer> colOrder;
    @FXML private TableColumn<Waypoint, String> colAddress;
    @FXML private TableColumn<Waypoint, String> colDescription;
    @FXML private TableColumn<Waypoint, Integer> colStopTime;
    @FXML private TableColumn<Waypoint, String> colNotes;
    @FXML private TableColumn<Waypoint, Void> colActions;
    @FXML private Button btnAddWaypoint;
    @FXML private Button btnReverseRoute;
    @FXML private Button btnClearAllWaypoints;
    @FXML private Button btnMoveSelectedUp;
    @FXML private Button btnMoveSelectedDown;
    @FXML private Button btnDeleteSelected;
    @FXML private Button btnDuplicateSelected;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private HBox progressBox;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblProgress;
    @FXML private Label lblWaypointCount;

    private Trip trip;
    private Vehicle vehicle;
    private ObservableList<Waypoint> waypoints;
    private TripDAO tripDAO;
    private WaypointDAO waypointDAO;
    private VehicleDAO vehicleDAO;
    private GoogleMapsService googleMapsService;
    private boolean changesSaved = false;
    private static final DataFormat WAYPOINT_FORMAT = new DataFormat("application/x-waypoint");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tripDAO = new TripDAO();
        waypointDAO = new WaypointDAO();
        vehicleDAO = new VehicleDAO();
        googleMapsService = new GoogleMapsService();
        waypoints = FXCollections.observableArrayList();
        setupTableColumns();
        setupDragAndDrop();
        setupContextMenu();
        setupSelectionListener();
        setupKeyboardShortcuts();
        tblWaypoints.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    public void setTrip(Trip trip, String vehicleInfo) {
        this.trip = trip;
        this.vehicle = vehicleDAO.findById(trip.getVehicleId());
        lblTripInfo.setText("Поїздка: " + trip.getTripNumber() + " (Статус: " + trip.getStatus() + ")");
        lblVehicleInfo.setText("Автомобіль: " + vehicleInfo);
        txtStartAddress.setText(trip.getStartAddress());
        txtEndAddress.setText(trip.getEndAddress());
        if (trip.hasWaypoints()) {
            waypoints.setAll(new ArrayList<>(trip.getWaypoints()));
        } else {
            waypoints.clear();
        }
        updateWaypointSequence();
        tblWaypoints.setItems(waypoints);
        updateWaypointCount();
        logger.info("Завантажено маршрут поїздки {} для редагування", trip.getTripNumber());
    }

    private void setupTableColumns() {
        colOrder.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getSequenceOrder()).asObject());
        colAddress.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAddress()));
        colAddress.setCellFactory(TextFieldTableCell.forTableColumn());
        colAddress.setOnEditCommit(event -> {
            Waypoint wp = event.getRowValue();
            wp.setAddress(event.getNewValue());
            logger.info("Змінено адресу точки: {}", event.getNewValue());
        });
        colDescription.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDescription()));
        colDescription.setCellFactory(TextFieldTableCell.forTableColumn());
        colDescription.setOnEditCommit(event -> {
            Waypoint wp = event.getRowValue();
            wp.setDescription(event.getNewValue());
            logger.info("Змінено опис точки: {}", event.getNewValue());
        });
        colStopTime.setCellValueFactory(cellData -> 
            new SimpleIntegerProperty(cellData.getValue().getEstimatedStopTime()).asObject());
        colStopTime.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Integer>() {
            @Override
            public String toString(Integer value) {
                return value != null ? value.toString() : "0";
            }
            @Override
            public Integer fromString(String string) {
                try {
                    return Integer.parseInt(string.trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }));
        colStopTime.setOnEditCommit(event -> {
            Waypoint wp = event.getRowValue();
            wp.setEstimatedStopTime(event.getNewValue());
            logger.info("Змінено час зупинки: {} хв", event.getNewValue());
        });
        if (colNotes != null) {
            colNotes.setCellValueFactory(cellData -> {
                String notes = cellData.getValue().getNotes();
                if (notes != null && notes.length() > 30) {
                    notes = notes.substring(0, 30) + "...";
                }
                return new SimpleStringProperty(notes);
            });
        }
        colActions.setCellFactory(param -> new TableCell<Waypoint, Void>() {
            private final Button btnEdit = new Button("");
            private final Button btnDelete = new Button("");
            private final Button btnUp = new Button("⬆");
            private final Button btnDown = new Button("⬇");
            private final Button btnDuplicate = new Button("");
            private final Button btnInsertBefore = new Button("⬅+");
            private final Button btnInsertAfter = new Button("+➡");
            private final HBox pane = new HBox(3);

            {
                String smallBtnStyle = "-fx-font-size: 9px; -fx-padding: 2 4; -fx-min-width: 24px; -fx-background-radius: 3;";
                btnEdit.setStyle("-fx-background-color: #238A90; -fx-text-fill: white;" + smallBtnStyle);
                btnDelete.setStyle("-fx-background-color: #FF6B6B; -fx-text-fill: white;" + smallBtnStyle);
                btnUp.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;" + smallBtnStyle);
                btnDown.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;" + smallBtnStyle);
                btnDuplicate.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white;" + smallBtnStyle);
                btnInsertBefore.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;" + smallBtnStyle);
                btnInsertAfter.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;" + smallBtnStyle);
                btnEdit.setTooltip(new Tooltip("Редагувати"));
                btnDelete.setTooltip(new Tooltip("Видалити"));
                btnUp.setTooltip(new Tooltip("Вгору"));
                btnDown.setTooltip(new Tooltip("Вниз"));
                btnDuplicate.setTooltip(new Tooltip("Дублювати"));
                btnInsertBefore.setTooltip(new Tooltip("Вставити перед"));
                btnInsertAfter.setTooltip(new Tooltip("Вставити після"));
                pane.setAlignment(Pos.CENTER);
                pane.getChildren().addAll(btnUp, btnDown, btnEdit, btnDuplicate, btnInsertBefore, btnInsertAfter, btnDelete);

                btnEdit.setOnAction(event -> {
                    Waypoint waypoint = getTableView().getItems().get(getIndex());
                    handleEditWaypoint(waypoint);
                });

                btnDelete.setOnAction(event -> {
                    Waypoint waypoint = getTableView().getItems().get(getIndex());
                    handleDeleteWaypoint(waypoint);
                });

                btnUp.setOnAction(event -> {
                    int index = getIndex();
                    if (index > 0) {
                        moveWaypoint(index, index - 1);
                    }
                });

                btnDown.setOnAction(event -> {
                    int index = getIndex();
                    if (index < getTableView().getItems().size() - 1) {
                        moveWaypoint(index, index + 1);
                    }
                });
                btnDuplicate.setOnAction(event -> {
                    Waypoint waypoint = getTableView().getItems().get(getIndex());
                    handleDuplicateWaypoint(waypoint, getIndex() + 1);
                });
                btnInsertBefore.setOnAction(event -> {
                    handleInsertWaypointAt(getIndex());
                });
                btnInsertAfter.setOnAction(event -> {
                    handleInsertWaypointAt(getIndex() + 1);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    int index = getIndex();
                    btnUp.setDisable(index == 0);
                    btnDown.setDisable(index >= waypoints.size() - 1);
                    setGraphic(pane);
                }
            }
        });

        tblWaypoints.setItems(waypoints);
        tblWaypoints.setEditable(true);
    }
    private void setupDragAndDrop() {
        tblWaypoints.setRowFactory(tv -> {
            TableRow<Waypoint> row = new TableRow<>();
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Integer index = row.getIndex();
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(WAYPOINT_FORMAT, index);
                    db.setContent(cc);
                    event.consume();
                }
            });
            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(WAYPOINT_FORMAT)) {
                    if (row.getIndex() != (Integer) db.getContent(WAYPOINT_FORMAT)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        event.consume();
                    }
                }
            });
            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(WAYPOINT_FORMAT)) {
                    int draggedIndex = (Integer) db.getContent(WAYPOINT_FORMAT);
                    int dropIndex = row.isEmpty() ? waypoints.size() : row.getIndex();
                    Waypoint waypoint = waypoints.remove(draggedIndex);
                    waypoints.add(dropIndex > draggedIndex ? dropIndex - 1 : dropIndex, waypoint);
                    updateWaypointSequence();
                    tblWaypoints.refresh();
                    tblWaypoints.getSelectionModel().select(dropIndex > draggedIndex ? dropIndex - 1 : dropIndex);
                    event.setDropCompleted(true);
                    event.consume();
                    logger.info("Переміщено точку drag-and-drop: {} -> {}", draggedIndex, dropIndex);
                }
            });
            row.setOnDragDone(event -> {
                event.consume();
            });
            return row;
        });
    }
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem miEdit = new MenuItem(" Редагувати");
        miEdit.setOnAction(e -> {
            Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleEditWaypoint(selected);
            }
        });
        MenuItem miDuplicate = new MenuItem(" Дублювати");
        miDuplicate.setOnAction(e -> {
            Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
            if (selected != null) {
                int index = waypoints.indexOf(selected);
                handleDuplicateWaypoint(selected, index + 1);
            }
        });
        MenuItem miInsertBefore = new MenuItem("⬅+ Вставити перед");
        miInsertBefore.setOnAction(e -> {
            int index = tblWaypoints.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                handleInsertWaypointAt(index);
            }
        });
        MenuItem miInsertAfter = new MenuItem("+➡ Вставити після");
        miInsertAfter.setOnAction(e -> {
            int index = tblWaypoints.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                handleInsertWaypointAt(index + 1);
            }
        });
        MenuItem miMoveToTop = new MenuItem("⬆⬆ На початок");
        miMoveToTop.setOnAction(e -> handleMoveToTop());
        MenuItem miMoveToBottom = new MenuItem("⬇⬇ В кінець");
        miMoveToBottom.setOnAction(e -> handleMoveToBottom());
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        MenuItem miCopyAddress = new MenuItem(" Копіювати адресу");
        miCopyAddress.setOnAction(e -> {
            Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getAddress() != null) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(selected.getAddress());
                clipboard.setContent(content);
                logger.info("Скопійовано адресу: {}", selected.getAddress());
            }
        });
        MenuItem miSetAsStart = new MenuItem(" Встановити як початок");
        miSetAsStart.setOnAction(e -> {
            Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getAddress() != null) {
                txtStartAddress.setText(selected.getAddress());
                logger.info("Встановлено початкову адресу: {}", selected.getAddress());
            }
        });
        MenuItem miSetAsEnd = new MenuItem(" Встановити як кінець");
        miSetAsEnd.setOnAction(e -> {
            Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getAddress() != null) {
                txtEndAddress.setText(selected.getAddress());
                logger.info("Встановлено кінцеву адресу: {}", selected.getAddress());
            }
        });
        SeparatorMenuItem separator2 = new SeparatorMenuItem();
        MenuItem miDeleteSelected = new MenuItem(" Видалити обрані");
        miDeleteSelected.setOnAction(e -> handleDeleteSelectedWaypoints());
        contextMenu.getItems().addAll(
            miEdit, miDuplicate, 
            miInsertBefore, miInsertAfter,
            miMoveToTop, miMoveToBottom,
            separator1,
            miCopyAddress, miSetAsStart, miSetAsEnd,
            separator2,
            miDeleteSelected
        );
        tblWaypoints.setContextMenu(contextMenu);
    }
    private void setupSelectionListener() {
        tblWaypoints.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            if (btnMoveSelectedUp != null) btnMoveSelectedUp.setDisable(!hasSelection);
            if (btnMoveSelectedDown != null) btnMoveSelectedDown.setDisable(!hasSelection);
            if (btnDeleteSelected != null) btnDeleteSelected.setDisable(!hasSelection);
            if (btnDuplicateSelected != null) btnDuplicateSelected.setDisable(!hasSelection);
        });
    }
    private void setupKeyboardShortcuts() {
        tblWaypoints.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                handleDeleteSelectedWaypoints();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.D) {
                Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    int index = waypoints.indexOf(selected);
                    handleDuplicateWaypoint(selected, index + 1);
                }
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.UP) {
                handleMoveSelectedUp();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == KeyCode.DOWN) {
                handleMoveSelectedDown();
                event.consume();
            }
        });
    }
    private void moveWaypoint(int fromIndex, int toIndex) {
        Waypoint waypoint = waypoints.remove(fromIndex);
        waypoints.add(toIndex, waypoint);
        updateWaypointSequence();
        tblWaypoints.refresh();
        tblWaypoints.getSelectionModel().select(toIndex);
    }

    @FXML
    private void handleAddWaypoint() {
        handleInsertWaypointAt(waypoints.size());
    }
    private void handleInsertWaypointAt(int index) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/waypoint-dialog.fxml"));
            Scene scene = new Scene(loader.load());
            WaypointDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Додати проміжну точку");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(btnAddWaypoint.getScene().getWindow());
            dialogStage.setScene(scene);
            dialogStage.setMinWidth(750.0);
            dialogStage.setMinHeight(650.0);
            dialogStage.setResizable(true);
            dialogStage.sizeToScene();
            Waypoint newWaypoint = new Waypoint();
            newWaypoint.setTripId(trip.getId());
            newWaypoint.setSequenceOrder(index + 1);
            controller.setWaypoint(newWaypoint);
            controller.setDialogStage(dialogStage);
            controller.setExistingWaypoints(waypoints);
            controller.setRouteAddresses(txtStartAddress.getText(), txtEndAddress.getText());
            controller.setGoogleMapsService(googleMapsService);
            dialogStage.showAndWait();
            if (controller.isSaved()) {
                waypoints.add(index, newWaypoint);
                updateWaypointSequence();
                tblWaypoints.refresh();
                updateWaypointCount();
                logger.info("Додано нову проміжну точку: {}", newWaypoint.getAddress());
            }
        } catch (Exception e) {
            logger.error("Помилка відкриття діалогу додавання точки: ", e);
            showError("Помилка", "Не вдалося відкрити діалог додавання точки:\n" + e.getMessage());
        }
    }

    private void handleEditWaypoint(Waypoint waypoint) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/waypoint-dialog.fxml"));
            Scene scene = new Scene(loader.load());
            WaypointDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Редагувати проміжну точку");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(btnAddWaypoint.getScene().getWindow());
            dialogStage.setScene(scene);
            dialogStage.setMinWidth(750.0);
            dialogStage.setMinHeight(650.0);
            dialogStage.setResizable(true);
            dialogStage.sizeToScene();
            controller.setWaypoint(waypoint);
            controller.setDialogStage(dialogStage);
            controller.setExistingWaypoints(waypoints);
            controller.setRouteAddresses(txtStartAddress.getText(), txtEndAddress.getText());
            controller.setGoogleMapsService(googleMapsService);
            dialogStage.showAndWait();
            if (controller.isSaved()) {
                tblWaypoints.refresh();
                logger.info("Відредаговано проміжну точку: {}", waypoint.getAddress());
            }
        } catch (Exception e) {
            logger.error("Помилка відкриття діалогу редагування точки: ", e);
            showError("Помилка", "Не вдалося відкрити діалог редагування точки:\n" + e.getMessage());
        }
    }

    private void handleDeleteWaypoint(Waypoint waypoint) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Підтвердження видалення");
        confirmAlert.setHeaderText("Видалити проміжну точку?");
        confirmAlert.setContentText("Ви впевнені, що хочете видалити точку:\n" + waypoint.getAddress());
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            waypoints.remove(waypoint);
            updateWaypointSequence();
            tblWaypoints.refresh();
            updateWaypointCount();
            logger.info("Видалено проміжну точку: {}", waypoint.getAddress());
        }
    }
    private void handleDuplicateWaypoint(Waypoint original, int insertIndex) {
        Waypoint duplicate = new Waypoint();
        duplicate.setTripId(original.getTripId());
        duplicate.setAddress(original.getAddress() + " ");
        duplicate.setDescription(original.getDescription());
        duplicate.setEstimatedStopTime(original.getEstimatedStopTime());
        duplicate.setNotes(original.getNotes());
        duplicate.setLatitude(original.getLatitude());
        duplicate.setLongitude(original.getLongitude());
        waypoints.add(insertIndex, duplicate);
        updateWaypointSequence();
        tblWaypoints.refresh();
        updateWaypointCount();
        tblWaypoints.getSelectionModel().select(insertIndex);
        logger.info("Дубльовано проміжну точку: {}", original.getAddress());
    }
    @FXML
    private void handleDeleteSelectedWaypoints() {
        List<Waypoint> selected = new ArrayList<>(tblWaypoints.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }
        String message = selected.size() == 1 
            ? "Ви впевнені, що хочете видалити точку:\n" + selected.get(0).getAddress()
            : "Ви впевнені, що хочете видалити " + selected.size() + " точок?";
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Підтвердження видалення");
        confirmAlert.setHeaderText("Видалити обрані точки?");
        confirmAlert.setContentText(message);
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            waypoints.removeAll(selected);
            updateWaypointSequence();
            tblWaypoints.refresh();
            updateWaypointCount();
            logger.info("Видалено {} проміжних точок", selected.size());
        }
    }
    @FXML
    private void handleMoveSelectedUp() {
        Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int index = waypoints.indexOf(selected);
            if (index > 0) {
                moveWaypoint(index, index - 1);
            }
        }
    }
    @FXML
    private void handleMoveSelectedDown() {
        Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int index = waypoints.indexOf(selected);
            if (index < waypoints.size() - 1) {
                moveWaypoint(index, index + 1);
            }
        }
    }
    private void handleMoveToTop() {
        Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int index = waypoints.indexOf(selected);
            if (index > 0) {
                waypoints.remove(index);
                waypoints.add(0, selected);
                updateWaypointSequence();
                tblWaypoints.refresh();
                tblWaypoints.getSelectionModel().select(0);
                logger.info("Переміщено точку на початок: {}", selected.getAddress());
            }
        }
    }
    private void handleMoveToBottom() {
        Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int index = waypoints.indexOf(selected);
            if (index < waypoints.size() - 1) {
                waypoints.remove(index);
                waypoints.add(selected);
                updateWaypointSequence();
                tblWaypoints.refresh();
                tblWaypoints.getSelectionModel().select(waypoints.size() - 1);
                logger.info("Переміщено точку в кінець: {}", selected.getAddress());
            }
        }
    }
    @FXML
    private void handleDuplicateSelected() {
        Waypoint selected = tblWaypoints.getSelectionModel().getSelectedItem();
        if (selected != null) {
            int index = waypoints.indexOf(selected);
            handleDuplicateWaypoint(selected, index + 1);
        }
    }
    @FXML
    private void handleReverseRoute() {
        if (waypoints.isEmpty()) {
            showInfo("Інформація", "Немає проміжних точок для реверсу.");
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Реверс маршруту");
        confirmAlert.setHeaderText("Розвернути порядок точок?");
        confirmAlert.setContentText("Порядок всіх проміжних точок буде змінено на зворотній.\n\n" +
            "Також можна поміняти місцями початкову та кінцеву адреси.");
        ButtonType btnReverseAll = new ButtonType(" Розвернути все");
        ButtonType btnReverseWaypoints = new ButtonType(" Тільки точки");
        ButtonType btnCancel = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(btnReverseAll, btnReverseWaypoints, btnCancel);
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnReverseAll) {
                FXCollections.reverse(waypoints);
                String tempStart = txtStartAddress.getText();
                txtStartAddress.setText(txtEndAddress.getText());
                txtEndAddress.setText(tempStart);
                updateWaypointSequence();
                tblWaypoints.refresh();
                logger.info("Розвернуто весь маршрут (точки + початок/кінець)");
            } else if (result.get() == btnReverseWaypoints) {
                FXCollections.reverse(waypoints);
                updateWaypointSequence();
                tblWaypoints.refresh();
                logger.info("Розвернуто порядок проміжних точок");
            }
        }
    }
    @FXML
    private void handleClearAllWaypoints() {
        if (waypoints.isEmpty()) {
            return;
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Очищення точок");
        confirmAlert.setHeaderText("Видалити всі проміжні точки?");
        confirmAlert.setContentText("Ви впевнені, що хочете видалити всі " + waypoints.size() + " проміжних точок?");
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            waypoints.clear();
            tblWaypoints.refresh();
            updateWaypointCount();
            logger.info("Очищено всі проміжні точки");
        }
    }
    @FXML
    private void handleSwapStartEnd() {
        String tempStart = txtStartAddress.getText();
        txtStartAddress.setText(txtEndAddress.getText());
        txtEndAddress.setText(tempStart);
        logger.info("Поміняно місцями початкову та кінцеву адреси");
    }

    private void updateWaypointSequence() {
        for (int i = 0; i < waypoints.size(); i++) {
            waypoints.get(i).setSequenceOrder(i + 1);
        }
    }
    private void updateWaypointCount() {
        if (lblWaypointCount != null) {
            lblWaypointCount.setText("Точок: " + waypoints.size());
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            String newStartAddress = txtStartAddress.getText().trim();
            String newEndAddress = txtEndAddress.getText().trim();

            trip.setStartAddress(newStartAddress);
            trip.setEndAddress(newEndAddress);

            List<Integer> existingWaypointIds = new ArrayList<>();
            if (trip.getWaypoints() != null) {
                for (Waypoint w : trip.getWaypoints()) {
                    if (w.getId() > 0) {
                        existingWaypointIds.add(w.getId());
                    }
                }
            }

            List<Integer> currentWaypointIds = new ArrayList<>();
            for (Waypoint waypoint : waypoints) {
                if (waypoint.getId() > 0) {
                    currentWaypointIds.add(waypoint.getId());
                    waypointDAO.update(waypoint);
                    logger.info("Оновлено проміжну точку: {}", waypoint.getAddress());
                } else {
                    waypoint.setTripId(trip.getId());
                    Waypoint created = waypointDAO.create(waypoint);
                    if (created != null) {
                        waypoint.setId(created.getId());
                        logger.info("Створено нову проміжну точку: {}", waypoint.getAddress());
                    }
                }
            }

            for (Integer oldId : existingWaypointIds) {
                if (!currentWaypointIds.contains(oldId)) {
                    waypointDAO.delete(oldId);
                    logger.info("Видалено проміжну точку ID: {}", oldId);
                }
            }

            trip.setWaypoints(new ArrayList<>(waypoints));

            showProgress("Перерахунок маршруту...");
            btnSave.setDisable(true);
            btnCancel.setDisable(true);

            logger.info(" Перерахунок відстані та витрат палива...");
            boolean recalculationSuccess = recalculateRouteDistanceAndFuel();
            hideProgress();
            btnSave.setDisable(false);
            btnCancel.setDisable(false);
            if (!recalculationSuccess) {
                Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                warningAlert.setTitle("Попередження");
                warningAlert.setHeaderText("Маршрут збережено, але є проблеми");
                warningAlert.setContentText("Не вдалося перерахувати відстань через Google Maps API.\n" +
                        "Маршрут збережено, але відстань і витрати палива не оновлено.\n" +
                        "Ви можете оновити їх пізніше вручну.");
                warningAlert.showAndWait();
            }

            showProgress("Збереження маршруту...");
            boolean success = tripDAO.updateRoute(trip);
            hideProgress();

            if (!success) {
                showError("Помилка", "Не вдалося оновити маршрут поїздки");
                return;
            }

            changesSaved = true;

            logger.info(" Маршрут поїздки {} успішно оновлено", trip.getTripNumber());
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Успіх");
            successAlert.setHeaderText(null);
            successAlert.setContentText("Маршрут поїздки успішно оновлено!\n\n" +
                    "Точок маршруту: " + waypoints.size() + "\n" +
                    "Відстань: " + (trip.getPlannedDistance() != null ? trip.getPlannedDistance() + " км" : "не вдалося розрахувати") + "\n" +
                    "Планові витрати палива: " + (trip.getPlannedFuelConsumption() != null ? trip.getPlannedFuelConsumption() + " л" : "не вдалося розрахувати"));
            successAlert.showAndWait();

            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            logger.error("Помилка збереження маршруту: ", e);
            showError("Помилка", "Не вдалося зберегти зміни:\n" + e.getMessage());
        }
    }

    private boolean recalculateRouteDistanceAndFuel() {
        try {
            if (!googleMapsService.isConfigured()) {
                logger.warn("Google Maps API не налаштовано, пропускаємо перерахунок відстані");
                return false;
            }

            if (vehicle == null) {
                logger.warn("Дані про автомобіль не завантажено, пропускаємо перерахунок палива");
                return false;
            }

            RouteInfo routeInfo;

            if (waypoints.isEmpty()) {
                logger.info(" Розрахунок маршруту без проміжних точок: {} → {}", 
                    trip.getStartAddress(), trip.getEndAddress());
                routeInfo = googleMapsService.calculateRoute(trip.getStartAddress(), trip.getEndAddress());
            } else {
                List<String> waypointAddresses = new ArrayList<>();
                for (Waypoint wp : waypoints) {
                    waypointAddresses.add(wp.getAddress());
                }
                logger.info(" Розрахунок маршруту з {} проміжними точками", waypoints.size());
                routeInfo = googleMapsService.calculateRouteWithWaypoints(
                    trip.getStartAddress(), 
                    trip.getEndAddress(), 
                    waypointAddresses
                );
            }

            if (routeInfo == null || routeInfo.isEmpty()) {
                logger.warn(" Google Maps API повернув порожній результат");
                return false;
            }

            BigDecimal totalDistance = routeInfo.getTotalDistance();
            BigDecimal cityDistance = routeInfo.getCityDistance();
            BigDecimal highwayDistance = routeInfo.getHighwayDistance();

            boolean isSummer = trip.getSeason() == Season.SUMMER;
            BigDecimal fuelConsumption = FuelCalculator.calculateFuelConsumptionForTrip(
                vehicle, routeInfo, trip.getTripType(), isSummer
            );

            if (trip.getTripType() == TripType.ROUND_TRIP) {
                totalDistance = totalDistance.multiply(BigDecimal.valueOf(2));
                cityDistance = cityDistance.multiply(BigDecimal.valueOf(2));
                highwayDistance = highwayDistance.multiply(BigDecimal.valueOf(2));
                logger.info(" Round trip: подвоюємо відстані для збереження повного маршруту");
            }

            trip.setPlannedDistance(totalDistance);
            trip.setPlannedCityKm(cityDistance);
            trip.setPlannedHighwayKm(highwayDistance);
            trip.setPlannedFuelConsumption(fuelConsumption);

            logger.info(" Відстань: {} км (місто: {} км, траса: {} км), витрати: {} л", 
                totalDistance, cityDistance, highwayDistance, fuelConsumption);

            return true;

        } catch (Exception e) {
            logger.error("Помилка перерахунку відстані та палива: ", e);
            return false;
        }
    }

    @FXML
    private void handleCancel() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Підтвердження");
        confirmAlert.setHeaderText("Скасувати редагування?");
        confirmAlert.setContentText("Всі незбережені зміни буде втрачено. Продовжити?");
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            stage.close();
        }
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (txtStartAddress.getText() == null || txtStartAddress.getText().trim().isEmpty()) {
            errors.append("• Початкова адреса не може бути порожньою\n");
        }

        if (txtEndAddress.getText() == null || txtEndAddress.getText().trim().isEmpty()) {
            errors.append("• Кінцева адреса не може бути порожньою\n");
        }

        for (Waypoint waypoint : waypoints) {
            if (waypoint.getAddress() == null || waypoint.getAddress().trim().isEmpty()) {
                errors.append("• Всі проміжні точки повинні мати адресу\n");
                break;
            }
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Помилка валідації");
            alert.setHeaderText("Будь ласка, виправте наступні помилки:");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return false;
        }

        return true;
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

    public boolean isChangesSaved() {
        return changesSaved;
    }

    public Trip getTrip() {
        return trip;
    }

    private void showProgress(String message) {
        if (progressBox != null && lblProgress != null) {
            lblProgress.setText(message);
            progressBox.setVisible(true);
            progressBox.setManaged(true);
        }
    }

    private void hideProgress() {
        if (progressBox != null) {
            progressBox.setVisible(false);
            progressBox.setManaged(false);
        }
    }
}
