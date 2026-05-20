package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.Socket;

import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.control.*;

import java.util.List;
import java.util.ArrayList;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Map;
import java.util.HashMap;

import java.util.LinkedHashMap;

import java.util.function.Consumer;

import javafx.scene.layout.HBox;
import javafx.scene.control.Button;

import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;


import javafx.scene.layout.AnchorPane;

import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import javafx.stage.Window;

public class Controller {

    @FXML private VBox loginPane;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML private VBox contentBox;
    @FXML private Button logoutButton;
    @FXML private Button patrolButton;
    @FXML private Button changepasswordButton;

    @FXML private VBox managementBox;
    @FXML private VBox policemanActionsBox;
    @FXML private VBox reportBox;
    @FXML private VBox messagingBox;
    @FXML private VBox problemReportBox;
    @FXML private VBox citizenBox;

    @FXML private TextField newUserField;
    @FXML private PasswordField newPasswordField;
    @FXML private ComboBox<String> newRoleField;
    @FXML private TextField checkUserField;

    @FXML private TextField checkDriverIdField;
    @FXML private TextField driverIdField;
    @FXML private TextField penaltyPointsField;
    @FXML private TextField fineAmountField;
    @FXML private TextField reasonField;
    @FXML private TextField licensePlateField;

    @FXML private TextArea reportTextArea;

    @FXML private ComboBox<String> recipientComboBox;
    @FXML private TextArea messageTextArea;
    @FXML private ListView<String> messagesListView;

    @FXML private TextField citizenIdField;
    @FXML private TextField ticketIdField;

    @FXML private TextArea problemReportTextArea;

    @FXML private TextField ticketIdFieldPolice;

    @FXML private VBox pendingUsersBox;
    @FXML private ListView<String> pendingUsersListView;
    @FXML private TextField approveUserField;

    @FXML private ListView<String> allUsersListView;
    @FXML private TextField deleteUserField;

    @FXML private ListView<String> ticketsListView;

    @FXML private TextField peselFieldPolice;
    @FXML private TextField nameFieldPolice;
    @FXML private TextField surnameFieldPolice;


    @FXML private Button dayStatsButton;
    @FXML private Button weekStatsButton;
    @FXML private Button monthStatsButton;

    @FXML
    private AnchorPane mapContainer;

    @FXML private Button viewReportsButton;

    @FXML
    private WebView webView;

    private WebEngine webEngine;

    private String currentUser;
    private String currentRole;

    private boolean mapReady = false;

    private void showPopup(String title, String message) {

        if (message == null) message = "Brak odpowiedzi";

        if (message.startsWith("SUCCESS:")) message = message.substring(8);
        if (message.startsWith("ERROR:")) message = message.substring(6);
        if (message.startsWith("WARNING:")) message = message.substring(8);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    @FXML
    protected void onLoginClicked() {

        String user = usernameField.getText();
        String pass = passwordField.getText();

        String res = send("LOGIN|" + user + "|" + pass);

        if (res.startsWith("SUCCESS:")) {
            String role = res.split(":")[1];

            currentUser = user;
            currentRole = role;

            loginPane.setVisible(false);
            contentBox.setVisible(true);

            setupUIForRole(role);

            if (webEngine != null) {
                webEngine.executeScript("logoutPolice();"); // wyczyść stary stan
                webEngine.executeScript("clearAllPins();"); // wyczyść pinezki
            }

            if (webEngine != null) {
                setupMapBridge(currentUser);
            }

            if ("man".equals(role) || "chief".equals(role)) {
                webEngine.executeScript("loginPolice('" + currentUser + "');");
            }


        } else {
            showPopup("Błąd logowania", res);
        }
    }

    @FXML
    protected void onLogoutClicked() {
        send("LOGOUT|" + currentUser);

        if (webEngine != null) {
            webEngine.executeScript("logoutPolice();");
            webEngine.executeScript("clearAllPins();");
        }

        currentUser = null;
        currentRole = null;

        loginPane.setVisible(true);
        contentBox.setVisible(false);

        if (logoutButton != null) logoutButton.setVisible(false);
        if (patrolButton != null) patrolButton.setVisible(false);
        if (changepasswordButton != null) changepasswordButton.setVisible(false);

        if (webEngine != null) {
            webEngine.executeScript("logoutPolice();");
        }

        showPopup("Wylogowano", "Pomyślnie wylogowano");
    }

    private void setupUIForRole(String role) {

        managementBox.setVisible(false);
        policemanActionsBox.setVisible(false);
        reportBox.setVisible(false);
        citizenBox.setVisible(false);
        messagingBox.setVisible(false);
        pendingUsersBox.setVisible(false);
        problemReportBox.setVisible(false);

        if (logoutButton != null) {
            logoutButton.setVisible(true);
        }

        if (changepasswordButton != null) {
            changepasswordButton.setVisible(true);
        }

        if (patrolButton != null) {
            patrolButton.setVisible("man".equals(role) || "chief".equals(role));
        }

        if ("chief".equals(role)) {
            managementBox.setVisible(true);
            policemanActionsBox.setVisible(true);
            reportBox.setVisible(true);
            messagingBox.setVisible(true);
            problemReportBox.setVisible(true);
            pendingUsersBox.setVisible(true);

            if (viewReportsButton != null) viewReportsButton.setVisible(true);
        }

        if ("man".equals(role)) {
            policemanActionsBox.setVisible(true);
            reportBox.setVisible(true);
            messagingBox.setVisible(true);
            problemReportBox.setVisible(true);

            if (viewReportsButton != null) viewReportsButton.setVisible(false);
        }

        if ("citizen".equals(role)) {
            citizenBox.setVisible(true);
            problemReportBox.setVisible(true);
        }
    }

    private void setupMapBridge(String username) {
        try {
            JSObject window = (JSObject) webEngine.executeScript("window");

            MapBridge bridge = new MapBridge(username, webEngine);
            window.setMember("javaBridge", bridge);

            System.out.println(" MapBridge ustawiony dla użytkownika: " + username);

            PauseTransition pause = new PauseTransition(Duration.millis(800));
            pause.setOnFinished(e -> {
                if (webEngine != null) {
                    webEngine.executeScript("console.log('JavaBridge powinien być gotowy');");
                }
            });
            pause.play();

        } catch (Exception e) {
            System.err.println("Błąd setupMapBridge: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void addUser() {
        String res = send("ADD_USER|" + currentUser + "|"
                + newUserField.getText() + "|"
                + newPasswordField.getText() + "|"
                + newRoleField.getValue());

        showPopup("Dodawanie użytkownika", res);
    }

    @FXML
    protected void removeUser() {
        String res = send("REMOVE_USER|" + currentUser + "|" + newUserField.getText());
        showPopup("Usuwanie użytkownika", res);
    }

    @FXML
    protected void onCheckUserClicked() {
        showPopup("Sprawdzenie użytkownika",
                send("CHECK_USER|" + currentUser + "|" + checkUserField.getText()));
    }

    @FXML
    protected void onCheckDriverClicked() {
        showPopup("Kierowca",
                send("CHECK_DRIVER|" + currentUser + "|" + checkDriverIdField.getText()));
    }

    @FXML
    protected void onCheckLicensePlateClicked() {

        String res = send("CHECK_PLATE|" + currentUser + "|" + licensePlateField.getText());

        if (res == null || res.isBlank()) {
            showPopup("Pojazd", "Brak danych");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacje o pojeździe");
        alert.setHeaderText("Szczegóły pojazdu + właściciel");

        TextArea area = new TextArea(res
                .replace(";;", "\n")
                .replace("|", " | ")
        );

        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefHeight(350);
        area.setPrefWidth(600);

        ScrollPane scroll = new ScrollPane(area);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(400);

        VBox box = new VBox(scroll);
        box.setPadding(new Insets(10));

        alert.getDialogPane().setContent(box);
        alert.getDialogPane().setPrefSize(650, 500);

        alert.showAndWait();
    }

    @FXML
    protected void issuePenaltyTicket() {

        String pesel = peselFieldPolice.getText();
        String name = nameFieldPolice.getText();
        String surname = surnameFieldPolice.getText();
        String points = penaltyPointsField.getText();
        String fine = fineAmountField.getText();
        String reason = reasonField.getText();

        if (pesel.isBlank() || name.isBlank() || surname.isBlank()
                || points.isBlank() || fine.isBlank() || reason.isBlank()) {

            showPopup("Błąd", "Uzupełnij wszystkie pola");
            return;
        }

        if (pesel.length() != 11) {
            showPopup("Błąd", "PESEL musi mieć 11 cyfr");
            return;
        }

        String cmd = "ISSUE_TICKET|"
                + currentUser + "|"
                + pesel + "|"
                + name + "|"
                + surname + "|"
                + points + "|"
                + fine + "|"
                + reason.replace(" ", "_");

        showPopup("Mandat", send(cmd));
    }

    @FXML
    protected void onSaveReportClicked() {
        showPopup("Raport",
                send("SAVE_REPORT|" + currentUser + "|" + currentRole + "|" +
                        reportTextArea.getText().replace("|", "_")));
    }

    @FXML
    protected void onSendMessageClicked() {
        if (currentUser == null) {
            showPopup("Błąd", "Musisz być zalogowany");
            return;
        }

        String res = send("GET_USERS|" + currentUser);

        if (!res.startsWith("SUCCESS:")) {
            showPopup("Błąd", "Nie udało się pobrać listy użytkowników: " + res);
            return;
        }

        String data = res.substring(8).trim();
        if (data.isBlank()) {
            showPopup("Błąd", "Brak użytkowników w systemie");
            return;
        }

        List<String> policeDisplay = new ArrayList<>();
        List<String> policeLogins = new ArrayList<>();

        for (String entry : data.split(";;")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty() || trimmed.equals(currentUser)) continue;

            String[] parts = trimmed.split("\\|");
            String username = parts[0].trim();
            String role = parts.length > 1 ? parts[1].trim() : "";

            if ("man".equals(role) || "chief".equals(role)) {
                String roleName = "chief".equals(role) ? "Komendant" : "Policjant";
                String display = username + " (" + roleName + ")";

                policeDisplay.add(display);
                policeLogins.add(username);
            }
        }

        if (policeDisplay.isEmpty()) {
            showPopup("Brak odbiorców", "Nie znaleziono policjantów ani komendanta w systemie.");
            return;
        }

        ListView<CheckBox> listView = new ListView<>();
        for (int i = 0; i < policeDisplay.size(); i++) {
            CheckBox cb = new CheckBox(policeDisplay.get(i));
            cb.setUserData(policeLogins.get(i));
            listView.getItems().add(cb);
        }

        Button nextBtn = new Button("Dalej →");
        VBox vbox = new VBox(10,
                new Label("Wybierz odbiorców (tylko służba):"),
                listView,
                nextBtn);
        vbox.setPadding(new Insets(15));

        Stage stage1 = new Stage();
        stage1.setTitle("Wybór odbiorców");
        stage1.setScene(new Scene(vbox, 420, 550));
        stage1.show();

        nextBtn.setOnAction(e -> {
            List<String> selected = new ArrayList<>();
            for (CheckBox cb : listView.getItems()) {
                if (cb.isSelected()) {
                    selected.add((String) cb.getUserData());
                }
            }

            if (selected.isEmpty()) {
                showPopup("Błąd", "Wybierz co najmniej jednego odbiorcę");
                return;
            }

            stage1.close();
            showMessageInputDialog(selected);
        });
    }

    @FXML
    protected void onRefreshMessagesClicked() {
        String res = send("GET_MSGS|" + currentUser);

        if (res.startsWith("SUCCESS:")) {
            messagesListView.getItems().setAll(res.substring(8).split(";;"));
        } else {
            showPopup("Błąd", res);
        }
    }

    @FXML
    protected void onGetPointsClicked() {
        showPopup("Punkty",
                send("GET_POINTS|" + citizenIdField.getText()));
    }

    @FXML
    protected void onGetTicketsClicked() {

        String pesel = citizenIdField.getText();

        if (pesel.isBlank()) {
            showPopup("Błąd", "Podaj PESEL");
            return;
        }

        String res = send("GET_TICKETS|" + pesel);

        if (!res.startsWith("SUCCESS:")) {
            showPopup("Błąd", res);
            return;
        }

        String data = res.substring(8);

        ticketsListView.getItems().clear();

        if (data.equals("EMPTY") || data.isBlank()) {
            ticketsListView.getItems().add("Brak mandatów 🎉");
            return;
        }

        for (String t : data.split(";;")) {

            String[] p = t.split(",");

            if (p.length < 4) continue;

            String display =
                    "ID: " + p[0] +
                            " | Punkty: " + p[1] +
                            " | " + (p[3].equals("true") ? "Opłacony" : "NIEOPŁACONY");

            ticketsListView.getItems().add(display);
        }
    }

    @FXML
    protected void onCreateAccountClicked() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Tworzenie konta");
        dialog.setHeaderText("Wprowadź dane nowego użytkownika");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField loginField = new TextField();
        loginField.setPromptText("Login");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Hasło");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("policjant", "komendant", "obywatel");
        roleBox.getSelectionModel().select("obywatel");

        TextField policeIdField = new TextField();
        policeIdField.setPromptText("Identyfikator policjanta");

        TextField peselField = new TextField();
        peselField.setPromptText("PESEL");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);

        VBox vbox = new VBox(10,
                new Label("Login:"), loginField,
                new Label("Hasło:"), passwordField,
                new Label("Rola:"), roleBox,
                policeIdField,
                peselField,
                errorLabel
        );

        dialog.getDialogPane().setContent(vbox);

        Runnable updateUI = () -> {

            policeIdField.setVisible(false);
            peselField.setVisible(false);
            errorLabel.setVisible(false);

            String role = roleBox.getValue();

            if ("policjant".equals(role)) {
                policeIdField.setVisible(true);
            }

            if ("obywatel".equals(role)) {
                peselField.setVisible(true);
            }
        };

        roleBox.setOnAction(e -> updateUI.run());
        updateUI.run();

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {

            errorLabel.setVisible(false);

            if (loginField.getText().isBlank() || passwordField.getText().isBlank()) {
                errorLabel.setText("Uzupełnij login i hasło");
                errorLabel.setVisible(true);
                event.consume();
                return;
            }

            if ("policjant".equals(roleBox.getValue()) && policeIdField.getText().isBlank()) {
                errorLabel.setText("Podaj identyfikator policjanta");
                errorLabel.setVisible(true);
                event.consume();
                return;
            }

            if ("obywatel".equals(roleBox.getValue())) {

                if (peselField.getText().isBlank()) {
                    errorLabel.setText("Podaj PESEL");
                    errorLabel.setVisible(true);
                    event.consume();
                    return;
                }

                if (peselField.getText().length() != 11) {
                    errorLabel.setText("PESEL musi mieć 11 cyfr");
                    errorLabel.setVisible(true);
                    event.consume();
                }
            }
        });

        dialog.showAndWait().ifPresent(result -> {

            if (result == ButtonType.OK) {

                String role = switch (roleBox.getValue()) {
                    case "komendant" -> "chief";
                    case "policjant" -> "man";
                    default -> "citizen";
                };

                String cmd = "ADD_USER|" + currentUser + "|"
                        + loginField.getText() + "|"
                        + passwordField.getText() + "|"
                        + role;

                if ("man".equals(role)) {
                    cmd += "|" + policeIdField.getText();
                }

                if ("citizen".equals(role)) {
                    cmd += "|" + peselField.getText();
                }

                showPopup("Tworzenie konta", send(cmd));
            }
        });
    }

    @FXML
    protected void onPayTicketClicked() {
        showPopup("Płatność",
                send("PAY_TICKET|" + ticketIdField.getText()));
    }

    @FXML
    protected void onReportProblemClicked() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Zgłoś problem");

        TextField addressField = new TextField();
        addressField.setPromptText("Adres");

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Powód zgłoszenia");
        reasonArea.setWrapText(true);

        VBox box = new VBox(10,
                new Label("Adres:"), addressField,
                new Label("Powód:"), reasonArea
        );

        dialog.getDialogPane().setContent(box);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {

            if (result == ButtonType.OK) {

                if (addressField.getText().isBlank() || reasonArea.getText().isBlank()) {
                    showPopup("Błąd", "Uzupełnij wszystkie pola");
                    return;
                }

                String cmd =
                        "ADD_INCIDENT|" +
                                currentUser + "|" +
                                addressField.getText().replace("|", "_") + "|" +
                                reasonArea.getText().replace("|", "_");

                showPopup("Zgłoszenie", send(cmd));
            }
        });
    }

    @FXML
    protected void onRefreshPendingUsers() {

        String res = send("GET_PENDING_USERS|" + currentUser);

        if (!res.startsWith("SUCCESS:")) {
            showPopup("Błąd", res);
            return;
        }

        String data = res.substring(8);

        if (data.isBlank()) {
            pendingUsersListView.getItems().setAll("Brak oczekujących kont");
            return;
        }

        pendingUsersListView.getItems().setAll(data.split(";;"));
    }

    @FXML
    protected void onApproveUserClicked() {

        String user = approveUserField.getText();

        if (user.isBlank()) {
            showPopup("Błąd", "Podaj login");
            return;
        }

        String res = send("APPROVE_USER|" + currentUser + "|" + user);

        showPopup("Zatwierdzanie", res);

        onRefreshPendingUsers();
    }

    @FXML
    protected void onLoadAllUsers() {

        String res = send("GET_ALL_USERS|" + currentUser);

        if (!res.startsWith("SUCCESS:")) {
            showPopup("Błąd", res);
            return;
        }

        String data = res.substring(8);

        if (data.isBlank()) {
            allUsersListView.getItems().setAll("Brak użytkowników");
            return;
        }

        allUsersListView.getItems().setAll(data.split(";;"));
    }

    @FXML
    protected void onDeleteUser() {

        String user = deleteUserField.getText();

        if (user.isBlank()) {
            showPopup("Błąd", "Podaj login");
            return;
        }

        String res = send("DELETE_USER|" + currentUser + "|" + user);

        showPopup("Usuwanie", res);

        onLoadAllUsers();
    }

    @FXML
    protected void initialize() {

        if (managementBox != null) managementBox.managedProperty().bind(managementBox.visibleProperty());
        if (policemanActionsBox != null) policemanActionsBox.managedProperty().bind(policemanActionsBox.visibleProperty());
        if (reportBox != null) reportBox.managedProperty().bind(reportBox.visibleProperty());
        if (citizenBox != null) citizenBox.managedProperty().bind(citizenBox.visibleProperty());
        if (messagingBox != null) messagingBox.managedProperty().bind(messagingBox.visibleProperty());
        if (problemReportBox != null) problemReportBox.managedProperty().bind(problemReportBox.visibleProperty());
        if (pendingUsersBox != null) pendingUsersBox.managedProperty().bind(pendingUsersBox.visibleProperty());
        if (loginPane != null) loginPane.managedProperty().bind(loginPane.visibleProperty());
        if (contentBox != null) contentBox.managedProperty().bind(contentBox.visibleProperty());
        if (viewReportsButton !=null) viewReportsButton.managedProperty().bind(viewReportsButton.visibleProperty());

        webView.setContextMenuEnabled(false);
        webEngine = webView.getEngine();

        webEngine.load(getClass().getResource("/map.html").toExternalForm());

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {

            if (newState == Worker.State.SUCCEEDED) {

                Platform.runLater(() -> {

                    webView.requestLayout();
                    forceMapResize();

                    PauseTransition pause1 = new PauseTransition(Duration.millis(300));
                    pause1.setOnFinished(e -> forceMapResize());
                    pause1.play();

                    PauseTransition pause2 = new PauseTransition(Duration.millis(800));
                    pause2.setOnFinished(e -> forceMapResize());
                    pause2.play();

                    if (currentUser != null) {
                        setupMapBridge(currentUser);
                    } else {
                        setupMapBridge("guest");
                    }
                });
            }
        });

        webView.widthProperty().addListener((obs, oldVal, newVal) -> forceMapResize());
        webView.heightProperty().addListener((obs, oldVal, newVal) -> forceMapResize());
        mapContainer.widthProperty().addListener((obs, oldVal, newVal) -> forceMapResize());
        mapContainer.heightProperty().addListener((obs, oldVal, newVal) -> forceMapResize());

        webView.setMaxWidth(Double.MAX_VALUE);
        webView.setMaxHeight(Double.MAX_VALUE);

        AnchorPane.setTopAnchor(webView, 0.0);
        AnchorPane.setBottomAnchor(webView, 0.0);
        AnchorPane.setLeftAnchor(webView, 0.0);
        AnchorPane.setRightAnchor(webView, 0.0);

        pendingUsersListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = pendingUsersListView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.contains("Brak oczekujących")) {
                    showPendingUserDialog(selected);
                }
            }
        });

        allUsersListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = allUsersListView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.contains("Brak użytkowników")) {
                    showDeleteUserDialog(selected);
                }
            }
        });

        ticketsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = ticketsListView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.contains("Brak mandatów")) {
                    showTicketDialog(selected);
                }
            }
        });

        Platform.runLater(() -> {
            PauseTransition pt = new PauseTransition(Duration.millis(800));
            pt.setOnFinished(e -> forceMapResize());
            pt.play();
        });

        if (logoutButton != null) logoutButton.setVisible(false);
        if (patrolButton != null) patrolButton.setVisible(false);
        if (changepasswordButton != null) changepasswordButton.setVisible(false);
    }

    @FXML
    protected void onChangePasswordClicked() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Zmiana hasła");
        dialog.setHeaderText("Podaj stare i nowe hasło");

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField oldPassField = new PasswordField();
        oldPassField.setPromptText("Stare hasło");

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("Nowe hasło");

        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Powtórz nowe hasło");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setVisible(false);

        VBox vbox = new VBox(10,
                new Label("Stare hasło:"), oldPassField,
                new Label("Nowe hasło:"), newPassField,
                new Label("Powtórz hasło:"), confirmPassField,
                errorLabel
        );

        dialog.getDialogPane().setContent(vbox);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {

            errorLabel.setVisible(false);

            if (oldPassField.getText().isBlank()
                    || newPassField.getText().isBlank()
                    || confirmPassField.getText().isBlank()) {

                errorLabel.setText("Uzupełnij wszystkie pola");
                errorLabel.setVisible(true);
                event.consume();
                return;
            }

            if (!newPassField.getText().equals(confirmPassField.getText())) {
                errorLabel.setText("Hasła się nie zgadzają");
                errorLabel.setVisible(true);
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(result -> {

            if (result == ButtonType.OK) {

                String cmd = "CHANGE_PASSWORD|"
                        + currentUser + "|"
                        + oldPassField.getText() + "|"
                        + newPassField.getText();

                String res = send(cmd);

                showPopup("Zmiana hasła", res);
            }
        });
    }

    @FXML
    public void onCreatePatrolClicked() {

        try {

            Socket s = new Socket("localhost", 5556);

            PrintWriter out =
                    new PrintWriter(s.getOutputStream(), true);

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(s.getInputStream())
                    );

            out.println("GET_POLICEMEN");

            String response = in.readLine();

            if (response == null || !response.startsWith("SUCCESS:")) {

                showPopup(
                        "Błąd",
                        "Nie udało się pobrać policjantów"
                );

                return;
            }

            String data = response.substring(8);

            if (data.isBlank()) {

                showPopup(
                        "Info",
                        "Brak policjantów w bazie"
                );

                return;
            }

            ListView<CheckBox> listView = new ListView<>();

            for (String p : data.split(";;")) {

                String[] parts = p.split("\\|");

                String username =
                        parts.length > 0
                                ? parts[0]
                                : "unknown";

                String role =
                        parts.length > 1
                                ? parts[1]
                                : "-";

                String id =
                        parts.length > 2
                                ? parts[2]
                                : "-";

                CheckBox cb = new CheckBox(
                        username +
                                " | " +
                                role +
                                " | ID: " +
                                id
                );

                cb.setUserData(username);

                listView.getItems().add(cb);
            }

            Button createBtn =
                    new Button("Stwórz patrol");

            VBox layout =
                    new VBox(10, listView, createBtn);

            layout.setPadding(new Insets(10));

            Stage stage = new Stage();

            stage.setScene(
                    new Scene(layout, 350, 500)
            );

            stage.setTitle("Tworzenie patrolu");

            stage.show();

            createBtn.setOnAction(e -> {

                List<String> selected =
                        new ArrayList<>();

                for (CheckBox cb : listView.getItems()) {

                    if (cb.isSelected()) {

                        selected.add(
                                (String) cb.getUserData()
                        );
                    }
                }

                if (selected.isEmpty()) {

                    showPopup(
                            "Błąd",
                            "Wybierz co najmniej jednego policjanta"
                    );

                    return;
                }

                String members =
                        String.join(",", selected);

                try {

                    Socket s2 =
                            new Socket("localhost", 5556);

                    PrintWriter out2 =
                            new PrintWriter(
                                    s2.getOutputStream(),
                                    true
                            );

                    BufferedReader in2 =
                            new BufferedReader(
                                    new InputStreamReader(
                                            s2.getInputStream()
                                    )
                            );

                    String cmd =
                            "CREATE_PATROL|"
                                    + currentUser
                                    + "|"
                                    + members;

                    System.out.println(
                            "SEND: " + cmd
                    );

                    out2.println(cmd);

                    String res = in2.readLine();

                    if (res == null) {
                        res = "ERROR: brak odpowiedzi serwera";
                    }

                    showPopup("Patrol", res);

                    stage.close();

                } catch (Exception ex) {

                    ex.printStackTrace();

                    showPopup(
                            "Błąd",
                            "Nie udało się utworzyć patrolu"
                    );
                }
            });

        } catch (Exception e) {

            e.printStackTrace();

            showPopup(
                    "Błąd",
                    "Brak połączenia z serwerem"
            );
        }
    }

    @FXML
    protected void onLoadPatrolsClicked() {

        String response = send("GET_PATROLS_DISPATCHES|" + currentUser);

        if (!response.startsWith("SUCCESS:")) {
            showPopup("Błąd", response);
            return;
        }

        String data = response.substring(8);

        Stage stage = new Stage();
        stage.setTitle("Patrole");

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        if (data.isBlank() || data.equals("Brak patroli")) {
            root.getChildren().add(new Label("Brak patroli 🚓"));
        } else {

            Map<String, PatrolUIData> patrolMap = new LinkedHashMap<>();

            for (String row : data.split(";;")) {

                String[] p = row.split("\\|");

                if (p.length < 3) continue;

                String patrolId = p[0].trim();
                String createdBy = p[1].trim();
                String createdAt = p[2].trim();

                String dispatches = "";

                if (p.length > 3) {

                    StringBuilder sb = new StringBuilder();

                    for (int i = 3; i < p.length; i++) {

                        sb.append(p[i]);

                        if (i < p.length - 1) {
                            sb.append("|");
                        }
                    }

                    dispatches = sb.toString();
                }

                PatrolUIData patrol =
                        patrolMap.computeIfAbsent(
                                patrolId,
                                k -> new PatrolUIData(createdBy, createdAt)
                        );

                if (!dispatches.isBlank()) {

                    for (String d : dispatches.split("##")) {

                        String[] dp = d.split("\\|");

                        String incidentId =
                                dp.length > 0 ? dp[0] : "-";

                        String address =
                                dp.length > 1 ? dp[1] : "brak adresu";

                        String time =
                                dp.length > 2 ? dp[2] : "-";

                        patrol.dispatches.add(
                                "🚨 Incident #" + incidentId +
                                        " | 📍 " + address +
                                        " | 🕒 " + time
                        );
                    }
                }
            }

            for (String patrolId : patrolMap.keySet()) {

                PatrolUIData dataObj = patrolMap.get(patrolId);

                VBox patrolBox = new VBox(8);
                patrolBox.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #cccccc;
                -fx-padding: 10;
                -fx-border-radius: 5;
                -fx-background-radius: 5;
            """);

                Label title = new Label(
                        "🚓 Patrol #" + patrolId +
                                " | " + dataObj.createdBy +
                                " | " + dataObj.createdAt
                );

                VBox actionsBox = new VBox(5);

                if (dataObj.dispatches.isEmpty()) {

                    Label empty = new Label("📭 Brak akcji");
                    empty.setStyle("-fx-text-fill: gray;");
                    actionsBox.getChildren().add(empty);

                } else {

                    for (String d : dataObj.dispatches) {
                        Label lbl = new Label(d);
                        lbl.setStyle("-fx-text-fill: red;");
                        actionsBox.getChildren().add(lbl);
                    }
                }

                TextField locationField = new TextField();
                locationField.setPromptText("Adres akcji patrolu");

                Button sendBtn = new Button("📍 Wyślij patrol");
                Button deleteBtn = new Button("🗑 Usuń");

                Label distanceLabel = new Label();

                sendBtn.setOnAction(e -> {
                    String address = locationField.getText();
                    if (address.isBlank()) {
                        showPopup("Błąd", "Podaj adres");
                        return;
                    }

                    String res = send("SEND_PATROL|" + currentUser + "|" + patrolId + "|" + address);
                    showPopup("Patrol", res);
                });


                deleteBtn.setOnAction(e -> {
                    String res = send("DELETE_PATROL|" + currentUser + "|" + patrolId);
                    showPopup("Usuwanie", res);
                    onLoadPatrolsClicked();
                });

                patrolBox.getChildren().addAll(
                        title,
                        actionsBox,
                        locationField,
                        sendBtn,
                        deleteBtn,
                        distanceLabel
                );

                root.getChildren().add(patrolBox);
            }
        }

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);

        stage.setScene(new Scene(scroll, 500, 450));
        stage.show();
    }

    class PatrolUIData {
        String createdBy;
        String createdAt;
        List<String> dispatches = new ArrayList<>();

        PatrolUIData(String createdBy, String createdAt) {
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }
    }

    @FXML
    public void onAddLokalizationClicked() {

        try {

            Stage stage = new Stage();

            WebView webView = new WebView();

            webView.setZoom(1.0);
            webView.setContextMenuEnabled(false);

            WebEngine engine = webView.getEngine();

            AnchorPane pane = new AnchorPane(webView);

            AnchorPane.setTopAnchor(webView, 0.0);
            AnchorPane.setBottomAnchor(webView, 0.0);
            AnchorPane.setLeftAnchor(webView, 0.0);
            AnchorPane.setRightAnchor(webView, 0.0);

            webView.prefWidthProperty().bind(pane.widthProperty());
            webView.prefHeightProperty().bind(pane.heightProperty());

            Rectangle2D screen =
                    Screen.getPrimary().getVisualBounds();

            Scene scene = new Scene(
                    pane,
                    screen.getWidth(),
                    screen.getHeight()
            );

            stage.setScene(scene);

            stage.setTitle("Mapa patrolu");

            engine.load(
                    getClass()
                            .getResource("/map.html")
                            .toExternalForm()
            );

            engine.getLoadWorker().stateProperty().addListener(
                    (obs, oldState, newState) -> {

                        if (newState == Worker.State.SUCCEEDED) {

                            try {

                                JSObject window =
                                        (JSObject) engine.executeScript("window");

                                window.setMember(
                                        "javaBridge",
                                        new MapBridge(
                                                currentUser != null
                                                        ? currentUser
                                                        : "guest"
                                        )
                                );

                                forceMapResize(engine);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );

            pane.widthProperty().addListener(
                    (obs, oldVal, newVal) ->
                            forceMapResize(engine)
            );

            pane.heightProperty().addListener(
                    (obs, oldVal, newVal) ->
                            forceMapResize(engine)
            );

            stage.show();

            Platform.runLater(() -> forceMapResize(engine));

        } catch (Exception e) {

            e.printStackTrace();

            showPopup(
                    "Błąd",
                    "Nie udało się otworzyć mapy"
            );
        }
    }

    @FXML
    protected void onCheckPatrolLocationsClicked() {

        if (!"chief".equals(currentRole)) {
            showPopup("Błąd", "Tylko komendant może przeglądać lokalizacje patroli");
            return;
        }

        if (webEngine == null) {
            showPopup("Błąd", "Mapa nie jest załadowana");
            return;
        }

        loadAllPatrolLocationsOnMainMap();
    }

    private void loadAllPatrolLocationsOnMainMap() {

        System.out.println("📡 Komendant pobiera lokalizacje patroli...");

        try (Socket socket = new Socket("localhost", 5556);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_PATROL_LOCATIONS|" + currentUser);
            String response = in.readLine();

            if (response != null && response.startsWith("SUCCESS:")) {
                String data = response.substring(8);

                webEngine.executeScript("clearAllPins();");

                if (data != null && !data.trim().isEmpty()) {
                    int count = 0;
                    for (String line : data.split(";;")) {
                        if (line.trim().isEmpty()) continue;

                        String[] p = line.split("\\|");
                        if (p.length >= 4) {
                            try {
                                String username = p[1];
                                double lat = Double.parseDouble(p[2]);
                                double lon = Double.parseDouble(p[3]);

                                webEngine.executeScript(
                                        "updatePatrolPin(" + lon + ", " + lat + ", '" +
                                                username.replace("'", "\\'") + "');"
                                );
                                count++;
                            } catch (Exception ex) {
                                System.out.println("Błąd linii: " + line);
                            }
                        }
                    }
                    System.out.println(" Dodano " + count + " lokalizacji patroli na mapie");

                    if (count > 0) {
                        showPopup("Lokalizacje patroli", "Pomyślnie wczytano " + count + " aktywnych patroli.");
                    } else {
                        showPopup("Lokalizacje patroli", "Brak aktywnych lokalizacji patroli.");
                    }
                } else {
                    showPopup("Lokalizacje patroli", "Brak aktywnych lokalizacji patroli.");
                }

            } else {
                showPopup("Błąd", "Nieprawidłowa odpowiedź serwera");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showPopup("Błąd", "Brak połączenia z serwerem");
        }
    }

    @FXML
    protected void onLoadIncidentsClicked() {

        String response = send("GET_INCIDENTS|" + currentUser);

        if (!response.startsWith("SUCCESS:")) {
            showPopup("Błąd", response);
            return;
        }

        String data = response.substring(8);

        if (data.isBlank() || data.equals("Brak zgłoszeń")) {
            showPopup("Info", "Brak zgłoszeń");
            return;
        }

        ListView<String> listView = new ListView<>();

        for (String incident : data.split(";;")) {
            listView.getItems().add(incident);
        }

        Button openBtn = new Button("Otwórz zdarzenie");

        VBox layout = new VBox(10, listView, openBtn);
        layout.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.setTitle("Zgłoszenia");

        stage.setScene(new Scene(layout, 600, 400));
        stage.show();

        openBtn.setOnAction(e -> {

            String selected = listView.getSelectionModel().getSelectedItem();

            if (selected == null) {
                showPopup("Błąd", "Wybierz zgłoszenie");
                return;
            }

            showIncidentDetails(selected, stage);
        });
    }

    @FXML
    protected void onShowStatisticsClicked() {

        ListView<String> listView = new ListView<>();

        Button dayBtn = new Button("Dzień");
        Button weekBtn = new Button("Tydzień");
        Button monthBtn = new Button("Miesiąc");

        HBox buttons = new HBox(10, dayBtn, weekBtn, monthBtn);

        VBox root = new VBox(10, listView, buttons);
        root.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.setTitle("Statystyki mandatów");
        stage.setScene(new Scene(root, 450, 350));
        stage.show();

        Consumer<String> loadStats = (mode) -> {

            String res = send("GET_STATS|" + currentUser + "|" + mode);

            listView.getItems().clear();

            if (!res.startsWith("SUCCESS:")) {
                listView.getItems().add("Błąd: " + res);
                return;
            }

            String data = res.substring(8);

            if (data.isBlank() || data.equals("Brak danych")) {
                listView.getItems().add("Brak danych");
                return;
            }

            for (String stat : data.split(",")) {

                String[] s = stat.split("=");

                if (s.length < 2) continue;

                listView.getItems().add("🚨 " + s[0] + " → " + s[1]);
            }
        };

        dayBtn.setOnAction(e -> loadStats.accept("DAY"));
        weekBtn.setOnAction(e -> loadStats.accept("WEEK"));
        monthBtn.setOnAction(e -> loadStats.accept("MONTH"));

        loadStats.accept("WEEK");
    }

    @FXML
    protected void onLoadReportsClicked() {

        String res = send("GET_PENDING_REPORTS|" + currentUser);

        if (res == null || !res.startsWith("SUCCESS:")) {
            showPopup("Błąd", "Nieprawidłowa odpowiedź serwera: " + res);
            return;
        }

        String data = res.substring(8);

        ListView<String> listView = new ListView<>();

        if (data == null || data.isBlank() || data.equals("EMPTY")) {
            listView.getItems().add("Brak raportów");
        } else {
            for (String r : data.split(";;")) {
                if (!r.isBlank()) {
                    listView.getItems().add(r);
                }
            }
        }

        Button openBtn = new Button("Otwórz raport");

        VBox box = new VBox(10, listView, openBtn);
        box.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.setTitle("Raporty");
        stage.setScene(new Scene(box, 500, 400));
        stage.show();

        openBtn.setOnAction(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();

            if (selected == null || selected.equals("Brak raportów")) {
                showPopup("Błąd", "Wybierz raport");
                return;
            }

            showReportDetailsChief(selected, stage);
        });
    }

    @FXML
    protected void onMyReportsClicked() {

        String res = send("GET_MY_REPORTS|" + currentUser);

        if (!res.startsWith("SUCCESS:")) {
            showPopup("Błąd", res);
            return;
        }

        String data = res.substring(8);

        ListView<String> listView = new ListView<>();

        if (data.isBlank()) {
            listView.getItems().add("Brak raportów");
        } else {
            for (String r : data.split(";;")) {
                if (!r.isBlank()) {
                    listView.getItems().add(r);
                }
            }
        }

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selected = listView.getSelectionModel().getSelectedItem();

                if (selected != null && !selected.equals("Brak raportów")) {
                    Stage stage = new Stage();
                    showReportDetails(selected, stage);
                }
            }
        });

        Stage stage = new Stage();
        VBox root = new VBox(10, listView);
        root.setPadding(new Insets(10));

        stage.setTitle("Moje raporty");
        stage.setScene(new Scene(root, 800, 400));
        stage.show();
    }

    @FXML
    protected void onPatrolInfoClicked() {

        String res =
                send("GET_MY_PATROL|" + currentUser);

        if (!res.startsWith("SUCCESS:")) {
            showPopup("Patrol", res);
            return;
        }

        String data = res.substring(8);

        if (data.equals("BRAK_PATROLU")) {

            showPopup(
                    "Patrol",
                    "Nie jesteś w patrolu"
            );

            return;
        }

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Mój patrol");

        TextArea area = new TextArea(
                data.replace(";;", "\n")
        );

        area.setEditable(false);

        alert.getDialogPane().setContent(area);

        alert.showAndWait();
    }

    private void loadAllPatrolLocations(WebEngine engine) {
        try (Socket socket = new Socket("localhost", 5556);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_PATROL_LOCATIONS|" + currentUser);
            String response = in.readLine();

            if (response != null && response.startsWith("SUCCESS:")) {
                String data = response.substring(8);

                // Czyścimy mapę i dodajemy wszystkie pinezki
                engine.executeScript("clearAllPins();");

                if (!data.isBlank()) {
                    for (String line : data.split(";;")) {
                        if (line.trim().isEmpty()) continue;

                        String[] parts = line.split("\\|");
                        if (parts.length >= 4) {
                            try {
                                String username = parts[1];
                                double lat = Double.parseDouble(parts[2]);
                                double lon = Double.parseDouble(parts[3]);

                                engine.executeScript(
                                        "updatePatrolPin(" + lon + ", " + lat + ", '" + username + "');"
                                );
                            } catch (Exception ignored) {}
                        }
                    }
                } else {
                    engine.executeScript("alert('Brak aktywnych lokalizacji patroli');");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            engine.executeScript("alert('Błąd połączenia z serwerem');");
        }
    }

    @FXML
    protected void onRefreshUsersClicked() {
        if (currentUser == null) return;

        String res = send("GET_USERS|" + currentUser);

        recipientComboBox.getItems().clear();

        if (res.startsWith("SUCCESS:")) {
            String data = res.substring(8).trim();

            if (data.isBlank()) {
                recipientComboBox.getItems().add("Brak innych użytkowników");
            } else {
                recipientComboBox.getItems().addAll(data.split(";;"));

                // Automatycznie wybierz pierwszego użytkownika
                if (!recipientComboBox.getItems().isEmpty()) {
                    recipientComboBox.getSelectionModel().select(0);
                }
            }
        } else {
            recipientComboBox.getItems().add("Błąd pobierania listy");
            showPopup("Błąd", res);
        }
    }

    private void showReportDetails(String report, Stage parentStage){

        Label reportLabel = new Label(report);
        reportLabel.setWrapText(true);

        Button fixBtn = new Button("Popraw raport");
        Button closeBtn = new Button("Zamknij");

        VBox box = new VBox(10, reportLabel, fixBtn, closeBtn);
        box.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.setTitle("Szczegóły raportu");
        stage.setScene(new Scene(box, 500, 300));
        stage.show();

        boolean canFix = currentRole.equals("man")
                && report.toUpperCase().contains("REVISION");

        fixBtn.setDisable(!canFix);

        fixBtn.setOnAction(e -> {

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Popraw raport");
            dialog.setHeaderText("Wpisz poprawki");

            dialog.showAndWait().ifPresent(text -> {

                if (text.isBlank()) return;

                String id = extractReportId(report);

                String res = send(
                        "FIX_REPORT|" +
                                currentUser + "|" +
                                id + "|" +
                                text.replace("|", "_")
                );

                showPopup("Raport", res);

                stage.close();
            });
        });

        closeBtn.setOnAction(e -> stage.close());
    }

    private void showReportDetailsChief(String report, Stage parent) {

        Label label = new Label(report);
        label.setWrapText(true);

        Button approve = new Button("Zatwierdź");
        Button reject = new Button("Do poprawy");
        Button close = new Button("Zamknij");

        VBox box = new VBox(10, label, approve, reject, close);
        box.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.setTitle("Raport");
        stage.setScene(new Scene(box, 500, 300));
        stage.show();

        approve.setOnAction(e -> {
            String id = extractReportId(report);
            showPopup("Raport", send("APPROVE_REPORT|" + currentUser + "|" + id));
            stage.close();
        });

        reject.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Odrzuć raport");
            dialog.setHeaderText("Podaj powód");

            dialog.showAndWait().ifPresent(reason -> {
                if (reason.isBlank()) return;

                String id = extractReportId(report);

                showPopup(
                        "Raport",
                        send("REJECT_REPORT|" + currentUser + "|" + id + "|" + reason)
                );

                stage.close();
            });
        });

        close.setOnAction(e -> stage.close());
    }

    private String extractReportId(String report) {
        try {
            String[] parts = report.split("\\|");
            return parts[0].trim(); // OK
        } catch (Exception e) {
            return "";
        }
    }

    private void forceMapResize(WebEngine engine) {

        Platform.runLater(() -> {

            try {

                engine.executeScript("""
                
                if(window.map){

                    setTimeout(() => {

                        map.invalidateSize(true);

                    }, 300);
                }
                
            """);

            } catch (Exception ignored) {
            }
        });
    }

    private void showIncidentDetails(String incident, Stage parentStage) {

        Label incidentLabel = new Label(incident);
        incidentLabel.setWrapText(true);

        Button assignBtn = new Button("🚓 Przypisz patrol");
        Button closeBtn = new Button("Zamknij");

        VBox box = new VBox(10, incidentLabel, assignBtn, closeBtn);
        box.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.setTitle("Szczegóły zdarzenia");

        stage.setScene(new Scene(box, 500, 300));
        stage.show();

        assignBtn.setOnAction(e -> {

            stage.close();

            showPatrolSelectionWindow(incident);
        });

        closeBtn.setOnAction(e -> stage.close());
    }

    private void showPatrolSelectionWindow(String incidentData) {

        String response = send("GET_PATROLS|" + currentUser);

        if (!response.startsWith("SUCCESS:")) {
            showPopup("Błąd", response);
            return;
        }

        String data = response.substring(8);

        Stage stage = new Stage();
        stage.setTitle("Wybierz patrol");

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        for (String patrol : data.split(";;")) {

            Button patrolBtn = new Button(patrol);

            patrolBtn.setMaxWidth(Double.MAX_VALUE);

            patrolBtn.setOnAction(e -> {

                try {

                    String patrolId = patrol
                            .split("\\|")[0]
                            .replace("Patrol #", "")
                            .trim();

                    String incidentId = incidentData
                            .split("\\|")[0]
                            .replace("ID:", "")
                            .trim();

                    String res = send(
                            "SEND_PATROL_INCIDENT|"
                                    + currentUser + "|"
                                    + patrolId + "|"
                                    + incidentId
                    );

                    showPopup("Patrol", res);

                    stage.close();

                } catch (Exception ex) {

                    ex.printStackTrace();

                    showPopup(
                            "Błąd",
                            "Nie udało się wysłać patrolu"
                    );
                }
            });

            root.getChildren().add(patrolBtn);
        }

        ScrollPane scroll = new ScrollPane(root);

        scroll.setFitToWidth(true);

        stage.setScene(new Scene(scroll, 500, 400));

        stage.show();
    }

    private void showTicketDialog(String ticketData) {

        String id = ticketData.split(" \\| ")[0]
                .replace("ID: ", "")
                .trim();

        String res = send("GET_TICKETS|" + citizenIdField.getText());

        if (!res.startsWith("SUCCESS:")) {
            showPopup("Błąd", res);
            return;
        }

        String data = res.substring(8);

        for (String t : data.split(";;")) {

            String[] p = t.split(",");

            if (p.length < 4) continue;

            if (p[0].equals(id)) {

                String details =
                        "ID: " + p[0] + "\n" +
                                "Punkty: " + p[1] + "\n" +
                                "Powód: " + p[2] + "\n" +
                                "Status: " + (p[3].equals("true") ? "Opłacony" : "Nieopłacony");

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Mandat");
                alert.setHeaderText("Szczegóły mandatu");
                alert.setContentText(details);

                ButtonType payBtn = new ButtonType("Opłać");
                ButtonType closeBtn = new ButtonType("Zamknij", ButtonBar.ButtonData.CANCEL_CLOSE);

                if (p[3].equals("false")) {
                    alert.getButtonTypes().setAll(payBtn, closeBtn);
                } else {
                    alert.getButtonTypes().setAll(closeBtn);
                }

                alert.showAndWait().ifPresent(result -> {

                    if (result == payBtn) {

                        String payRes = send("PAY_TICKET|" + p[0]);
                        showPopup("Płatność", payRes);

                        onGetTicketsClicked();
                    }
                });

                return;
            }
        }
    }

    private void showPendingUserDialog(String userData) {

        String[] parts = userData.split(" \\| ");

        String username = parts[0].trim();
        String role = parts.length > 1 ? parts[1].trim() : "-";
        String policeId = parts.length > 2 ? parts[2].replace("ID:", "").trim() : "-";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Zarządzanie użytkownikiem");
        alert.setHeaderText("Dane użytkownika");

        alert.setContentText(
                "Login: " + username + "\n" +
                        "Rola: " + role + "\n" +
                        "ID: " + policeId
        );

        ButtonType approveBtn = new ButtonType("Zatwierdź");
        ButtonType deleteBtn = new ButtonType("Usuń");
        ButtonType cancelBtn = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(approveBtn, deleteBtn, cancelBtn);

        alert.showAndWait().ifPresent(result -> {

            if (result == approveBtn) {

                String res = send("APPROVE_USER|" + currentUser + "|" + username);
                showPopup("Zatwierdzanie", res);

            } else if (result == deleteBtn) {

                String res = send("DELETE_PENDING|" + currentUser + "|" + username);
                showPopup("Usuwanie", res);
            }

            onRefreshPendingUsers();
        });
    }

    private void showDeleteUserDialog(String userData) {

        String[] parts = userData.split(" \\| ");

        String username = parts[0].trim();
        String role = parts.length > 1 ? parts[1].trim() : "-";
        String policeId = parts.length > 2 ? parts[2].replace("ID:", "").trim() : "-";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Usuwanie użytkownika");
        alert.setHeaderText("Dane użytkownika");

        alert.setContentText(
                "Login: " + username + "\n" +
                        "Rola: " + role + "\n" +
                        "ID: " + policeId + "\n\n" +
                        "Czy na pewno chcesz usunąć użytkownika?"
        );

        ButtonType deleteBtn = new ButtonType("Usuń");
        ButtonType cancelBtn = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(deleteBtn, cancelBtn);

        alert.showAndWait().ifPresent(result -> {

            if (result == deleteBtn) {

                String res = send("DELETE_USER|" + currentUser + "|" + username);
                showPopup("Usuwanie użytkownika", res);

                onLoadAllUsers();
            }
        });
    }

    private String send(String cmd) {
        try (Socket socket = new Socket("localhost", 5556);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(cmd);

            String response = in.readLine();

            return response != null ? response : "ERROR: Podaj poprawne dane";

        } catch (Exception e) {
            return "ERROR: brak połączenia z serwerem";
        }
    }

    private void showPatrolDialog(String patrolData) {

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Szczegóły patrolu");
        alert.setHeaderText("Informacje o patrolu");

        TextArea patrolInfo = new TextArea();
        patrolInfo.setEditable(false);
        patrolInfo.setWrapText(true);
        patrolInfo.setPrefHeight(220);

        patrolInfo.setText(
                patrolData.replace(";;", "\n")
        );

        TextField locationField = new TextField();
        locationField.setPromptText("Podaj adres zgłoszenia");

        Label locationStatus = new Label();

        VBox layout = new VBox(10,
                new Label("Patrol:"),
                patrolInfo,

                new Separator(),

                new Label("Lokalizacja zgłoszenia:"),
                locationField,
                locationStatus
        );

        layout.setPadding(new Insets(10));

        alert.getDialogPane().setContent(layout);

        ButtonType sendBtn =
                new ButtonType("📍 Wyślij patrol");

        ButtonType checkBtn =
                new ButtonType("📏 Sprawdź lokalizację");

        ButtonType deleteBtn =
                new ButtonType("🗑 Usuń patrol");

        ButtonType closeBtn =
                new ButtonType("Zamknij",
                        ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(
                sendBtn,
                checkBtn,
                deleteBtn,
                closeBtn
        );

        while (true) {

            var result = alert.showAndWait();

            if (result.isEmpty()) {
                break;
            }

            if (result.get() == sendBtn) {

                String address = locationField.getText();

                if (address.isBlank()) {
                    showPopup("Błąd", "Podaj adres");
                    continue;
                }

                String res = send(
                        "SEND_PATROL|"
                                + currentUser + "|"
                                + patrolData.replace("|", "_") + "|"
                                + address.replace("|", "_")
                );

                showPopup("Patrol", res);
            }

            else if (result.get() == checkBtn) {

                String address = locationField.getText();

                if (address.isBlank()) {
                    showPopup("Błąd", "Podaj adres");
                    continue;
                }

                double distance = Math.random() * 20;

                String status;

                if (distance < 3) {
                    status = " Patrol bardzo blisko";
                }
                else if (distance < 10) {
                    status = " Patrol średnio daleko";
                }
                else {
                    status = " Patrol daleko";
                }

                locationStatus.setText(
                        "Odległość: "
                                + String.format("%.1f", distance)
                                + " km\n"
                                + status
                );
            }

            else if (result.get() == deleteBtn) {

                String patrolId =
                        patrolData.split(" ")[0];

                String res = send(
                        "DELETE_PATROL|"
                                + currentUser + "|"
                                + patrolId
                );

                showPopup("Usuwanie patrolu", res);

                onLoadPatrolsClicked();

                break;
            }

            else {
                break;
            }
        }
    }

    private void updatePatrolStatus(Label label, String patrolId) {

        String res = send("GET_PATROL_STATUS|" + patrolId);

        if (!res.startsWith("SUCCESS:")) {

            label.setText(" Brak aktywnej akcji");
            label.setStyle("-fx-text-fill: gray;");

            return;
        }

        String data = res.substring(8);

        if (data.isBlank() || data.equals("Brak akcji")) {

            label.setText(" Brak aktywnej akcji");
            label.setStyle("-fx-text-fill: gray;");

            return;
        }

        String[] parts = data.split("\\|");

        String status =
                parts.length > 0 ? parts[0] : "Akcja";

        String address =
                parts.length > 1 ? parts[1] : "Nieznany adres";

        label.setText(
                " " + status + "\n " + address
        );

        label.setStyle(
                "-fx-text-fill: green; -fx-font-weight: bold;"
        );
    }

    private String extractAddress(String incident) {

        String[] parts = incident.split(",");

        if (parts.length >= 3) {
            return parts[2];
        }

        return "Nieznany adres";
    }

    private void loadStats(String mode) {

        String res = send("GET_STATS|" + currentUser + "|" + mode);

        if (!res.startsWith("SUCCESS:")) {
            showPopup("Błąd", res);
            return;
        }

        String data = res.substring(8);

        ListView<String> list = new ListView<>();

        if (data.isBlank() || data.equals("Brak danych")) {
            list.getItems().add("Brak danych");
        } else {

            for (String stat : data.split(",")) {

                String[] s = stat.split("=");

                if (s.length < 2) continue;

                list.getItems().add(" " + s[0] + " → " + s[1]);
            }
        }

        Stage stage = new Stage();
        VBox root = new VBox(10, list);
        root.setPadding(new Insets(10));

        stage.setTitle("Statystyki mandatów");
        stage.setScene(new Scene(root, 400, 300));
        stage.show();
    }

    @FXML
    protected void onDayStatsClicked() {
        loadStats("DAY");
    }

    @FXML
    protected void onWeekStatsClicked() {
        loadStats("WEEK");
    }

    @FXML
    protected void onMonthStatsClicked() {
        loadStats("MONTH");
    }

    private void forceMapResizeAggressive(WebEngine engine) {
        Platform.runLater(() -> {
            try {
                engine.executeScript("""
                if (window.map) {
                    // Bardzo agresywne wymuszenie
                    window.map.invalidateSize(true);
                    window.map.invalidateSize(true);
                    
                    setTimeout(() => { 
                        if (window.map) window.map.invalidateSize(true); 
                    }, 50);
                    
                    setTimeout(() => { 
                        if (window.map) window.map.invalidateSize(true); 
                    }, 300);
                    
                    setTimeout(() => { 
                        if (window.map) window.map.invalidateSize(true); 
                    }, 800);
                }
            """);
            } catch (Exception ignored) {}
        });
    }

    private void forceMapResize() {
        if (webEngine == null) return;

        Platform.runLater(() -> {
            try {
                webEngine.executeScript("if (window.forceResize) window.forceResize();");
            } catch (Exception ignored) {}
        });
    }

    private void showMessageInputDialog(List<String> receivers) {
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Wpisz treść wiadomości...");
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(150);

        Label info = new Label("Wiadomość zostanie wysłana do " + receivers.size() + " odbiorców:");
        info.setStyle("-fx-font-weight: bold;");

        VBox vbox = new VBox(10, info, messageArea);
        vbox.setPadding(new Insets(15));

        Button sendBtn = new Button("✉️ Wyślij do wszystkich");

        Stage stage = new Stage();
        stage.setTitle("Napisz wiadomość");
        stage.setScene(new Scene(vbox, 450, 300));
        stage.show();

        sendBtn.setOnAction(e -> {
            String message = messageArea.getText().trim();
            if (message.isBlank()) {
                showPopup("Błąd", "Wiadomość nie może być pusta");
                return;
            }

            int successCount = 0;
            for (String receiver : receivers) {
                String res = send("SEND_MSG|" + currentUser + "|" +
                        receiver + "|" +
                        message.replace("|", "_").replace("\n", " "));

                if (res.startsWith("SUCCESS")) {
                    successCount++;
                }
            }

            stage.close();
            showPopup("Wiadomości wysłane",
                    "Pomyślnie wysłano do " + successCount + " z " + receivers.size() + " odbiorców.");

            onRefreshMessagesClicked();
        });

        VBox root = (VBox) vbox.getParent();
        if (root != null) root.getChildren().add(sendBtn);
        else vbox.getChildren().add(sendBtn);
    }

}

