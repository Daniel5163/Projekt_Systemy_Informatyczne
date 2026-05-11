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

public class Controller {

    @FXML private VBox loginPane;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML private VBox contentBox;
    @FXML private Button logoutButton;

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






    private String currentUser;
    private String currentRole;

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

        String res = send("LOGIN " + user + " " + pass);

        if (res.startsWith("SUCCESS:")) {
            currentRole = res.split(":")[1];
            currentUser = user;

            loginPane.setVisible(false);
            contentBox.setVisible(true);


            setupUIForRole(currentRole);

        } else {
            showPopup("Błąd logowania", res);
        }
    }

    @FXML
    protected void onLogoutClicked() {
        send("LOGOUT " + currentUser);

        currentUser = null;
        currentRole = null;

        loginPane.setVisible(true);
        contentBox.setVisible(false);

        showPopup("Wylogowano", "Pomyślnie wylogowano");
    }

    private void setupUIForRole(String role) {

        managementBox.setVisible(false);
        policemanActionsBox.setVisible(false);
        reportBox.setVisible(false);
        citizenBox.setVisible(false);

        messagingBox.setVisible(true);
        problemReportBox.setVisible(true);

        if ("chief".equals(role)) {
            managementBox.setVisible(true);
            policemanActionsBox.setVisible(true);
            reportBox.setVisible(true);
        }

        if ("man".equals(role)) {
            policemanActionsBox.setVisible(true);
            reportBox.setVisible(true);
        }

        if ("citizen".equals(role)) {
            citizenBox.setVisible(true);
        }

        if ("chief".equals(role)) {
            managementBox.setVisible(true);
            policemanActionsBox.setVisible(true);
            reportBox.setVisible(true);
            onRefreshPendingUsers();

            pendingUsersBox.setVisible(true);
        }
    }

    @FXML
    protected void addUser() {
        String res = send("ADD_USER " + currentUser + " "
                + newUserField.getText() + " "
                + newPasswordField.getText() + " "
                + newRoleField.getValue());

        showPopup("Dodawanie użytkownika", res);
    }

    @FXML
    protected void removeUser() {
        String res = send("REMOVE_USER " + currentUser + " " + newUserField.getText());
        showPopup("Usuwanie użytkownika", res);
    }

    @FXML
    protected void onCheckUserClicked() {
        showPopup("Sprawdzenie użytkownika",
                send("CHECK_USER " + currentUser + " " + checkUserField.getText()));
    }

    @FXML
    protected void onCheckDriverClicked() {
        showPopup("Kierowca",
                send("CHECK_DRIVER " + currentUser + " " + checkDriverIdField.getText()));
    }

    @FXML
    protected void onCheckLicensePlateClicked() {
        showPopup("Pojazd",
                send("CHECK_PLATE " + currentUser + " " + licensePlateField.getText()));
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

        String cmd = "ISSUE_TICKET "
                + currentUser + " "
                + pesel + " "
                + name + " "
                + surname + " "
                + points + " "
                + fine + " "
                + reason.replace(" ", "_");

        showPopup("Mandat", send(cmd));
    }

    @FXML
    protected void onSaveReportClicked() {
        showPopup("Raport",
                send("SAVE_REPORT " + currentUser + " " + currentRole + " " +
                        reportTextArea.getText().replace(" ", "_")));
    }

    @FXML
    protected void onSendMessageClicked() {
        String res = send("SEND_MSG " + currentUser + " "
                + recipientComboBox.getValue() + " "
                + messageTextArea.getText().replace(" ", "_"));

        showPopup("Wiadomość", res);
    }

    @FXML
    protected void onRefreshMessagesClicked() {
        String res = send("GET_MSGS " + currentUser);

        if (res.startsWith("SUCCESS:")) {
            messagesListView.getItems().setAll(res.substring(8).split(";;"));
        } else {
            showPopup("Błąd", res);
        }
    }

    @FXML
    protected void onGetPointsClicked() {
        showPopup("Punkty",
                send("GET_POINTS " + citizenIdField.getText()));
    }

    @FXML
    protected void onGetTicketsClicked() {

        String pesel = citizenIdField.getText();

        if (pesel.isBlank()) {
            showPopup("Błąd", "Podaj PESEL");
            return;
        }

        String res = send("GET_TICKETS " + pesel);

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

            if (p.length < 4) continue; // zabezpieczenie

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

                String cmd = "ADD_USER " + currentUser + " "
                        + loginField.getText() + " "
                        + passwordField.getText() + " "
                        + role;

                if ("man".equals(role)) {
                    cmd += " " + policeIdField.getText();
                }

                if ("citizen".equals(role)) {
                    cmd += " " + peselField.getText();
                }

                showPopup("Tworzenie konta", send(cmd));
            }
        });
    }

    @FXML
    protected void onPayTicketClicked() {
        showPopup("Płatność",
                send("PAY_TICKET " + ticketIdField.getText()));
    }

    @FXML
    protected void onReportProblemClicked() {
        showPopup("Zgłoszenie",
                send("ADD_INCIDENT " +
                        problemReportTextArea.getText().replace(" ", "_")));
    }

    @FXML
    protected void onRefreshPendingUsers() {

        String res = send("GET_PENDING_USERS " + currentUser);

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

        String res = send("APPROVE_USER " + currentUser + " " + user);

        showPopup("Zatwierdzanie", res);

        onRefreshPendingUsers();
    }

    @FXML
    protected void onLoadAllUsers() {

        String res = send("GET_ALL_USERS " + currentUser);

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

        String res = send("DELETE_USER " + currentUser + " " + user);

        showPopup("Usuwanie", res);

        onLoadAllUsers(); // refresh
    }

    @FXML
    protected void initialize() {

        pendingUsersListView.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                String selected = pendingUsersListView
                        .getSelectionModel()
                        .getSelectedItem();

                if (selected != null && !selected.equals("Brak oczekujących kont")) {
                    showPendingUserDialog(selected);
                }
            }
        });

        allUsersListView.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                String selected = allUsersListView
                        .getSelectionModel()
                        .getSelectedItem();

                if (selected != null && !selected.equals("Brak użytkowników")) {
                    showDeleteUserDialog(selected);
                }
            }
        });

        ticketsListView.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                String selected = ticketsListView
                        .getSelectionModel()
                        .getSelectedItem();

                if (selected != null && !selected.contains("Brak mandatów")) {
                    showTicketDialog(selected);
                }
            }
        });
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

                String cmd = "CHANGE_PASSWORD "
                        + currentUser + " "
                        + oldPassField.getText() + " "
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
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            out.println("GET_POLICEMEN");

            String response = in.readLine();

            if (response == null || !response.startsWith("SUCCESS:")) {
                showPopup("Błąd", "Nie udało się pobrać policjantów");
                return;
            }

            String data = response.substring(8);

            if (data.isBlank()) {
                showPopup("Info", "Brak policjantów w bazie");
                return;
            }

            ListView<CheckBox> listView = new ListView<>();

            for (String p : data.split(";;")) {

                String[] parts = p.split("\\|");

                String username = parts[0];
                String id = parts.length > 1 ? parts[1] : "-";

                CheckBox cb = new CheckBox(username + " (ID: " + id + ")");

                cb.setUserData(p);

                listView.getItems().add(cb);
            }

            Button createBtn = new Button("Stwórz patrol");

            VBox layout = new VBox(10, listView, createBtn);
            layout.setPadding(new Insets(10));

            Stage stage = new Stage();
            stage.setScene(new Scene(layout, 320, 450));
            stage.setTitle("Tworzenie patrolu");
            stage.show();

            createBtn.setOnAction(e -> {

                List<String> selected = new ArrayList<>();

                for (CheckBox cb : listView.getItems()) {
                    if (cb.isSelected()) {
                        selected.add((String) cb.getUserData());
                    }
                }

                if (selected.isEmpty()) {
                    showPopup("Błąd", "Wybierz co najmniej jednego policjanta");
                    return;
                }

                String members = String.join(",", selected);

                try {
                    Socket s2 = new Socket("localhost", 5556);
                    PrintWriter out2 = new PrintWriter(s2.getOutputStream(), true);
                    BufferedReader in2 = new BufferedReader(new InputStreamReader(s2.getInputStream()));

                    out2.println("CREATE_PATROL " + currentUser + " " + members);

                    String res = in2.readLine();

                    showPopup("Patrol", res);

                    stage.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showPopup("Błąd", "Nie udało się utworzyć patrolu");
                }

            });

        } catch (Exception e) {
            e.printStackTrace();
            showPopup("Błąd", "Brak połączenia z serwerem");
        }
    }

    @FXML
    protected void onLoadPatrolsClicked() {

        String response = send("GET_PATROLS " + currentUser);

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

            for (String patrol : data.split(";;")) {

                VBox patrolBox = new VBox(10);

                patrolBox.setStyle("""
                -fx-background-color: white;
                -fx-border-color: #cccccc;
                -fx-padding: 10;
                -fx-border-radius: 5;
                -fx-background-radius: 5;
            """);

                String[] split = patrol.split("->");

                String info = split[0].trim();
                String members = split.length > 1 ? split[1].trim() : "";

                final String patrolId = info
                        .split("\\|")[0]
                        .replace("Patrol #", "")
                        .trim();

                Label infoLabel = new Label("🚓 " + info);

                TextArea membersArea = new TextArea(members);
                membersArea.setEditable(false);
                membersArea.setWrapText(true);
                membersArea.setPrefHeight(80);

                TextField locationField = new TextField();
                locationField.setPromptText("Wpisz adres patrolu");

                Label statusLabel = new Label();

                Button sendBtn = new Button("📍 Wyślij patrol");
                Button checkBtn = new Button("📏 Sprawdź lokalizację");
                Button deleteBtn = new Button("🗑 Usuń patrol");

                sendBtn.setStyle("-fx-base: #2196F3;");
                checkBtn.setStyle("-fx-base: #FFC107;");
                deleteBtn.setStyle("-fx-base: #e53935;");

                sendBtn.setOnAction(e -> {

                    String address = locationField.getText();

                    if (address.isBlank()) {
                        showPopup("Błąd", "Podaj adres");
                        return;
                    }

                    String cmd = "SEND_PATROL " + currentUser + " " + patrolId + " " + address;

                    String res = send(cmd);

                    showPopup("Patrol", res);
                });

                checkBtn.setOnAction(e -> {

                    String address = locationField.getText();

                    if (address.isBlank()) {
                        showPopup("Błąd", "Podaj adres");
                        return;
                    }

                    double distance = Math.random() * 20;

                    String status;

                    if (distance < 3) {
                        status = "🟢 bardzo blisko";
                    } else if (distance < 10) {
                        status = "🟡 średnio daleko";
                    } else {
                        status = "🔴 daleko";
                    }

                    statusLabel.setText(
                            "Odległość: " + String.format("%.1f km", distance)
                                    + " | " + status
                    );
                });

                deleteBtn.setOnAction(e -> {

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Usuwanie patrolu");
                    confirm.setHeaderText("Czy usunąć patrol #" + patrolId + "?");

                    confirm.showAndWait().ifPresent(result -> {

                        if (result == ButtonType.OK) {

                            String res = send(
                                    "DELETE_PATROL "
                                            + currentUser + " "
                                            + patrolId
                            );

                            showPopup("Patrol", res);

                            stage.close();
                            onLoadPatrolsClicked(); // refresh
                        }
                    });
                });

                patrolBox.getChildren().addAll(
                        infoLabel,
                        membersArea,
                        locationField,
                        statusLabel,
                        sendBtn,
                        checkBtn,
                        deleteBtn
                );

                root.getChildren().add(patrolBox);
            }
        }

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);

        stage.setScene(new Scene(scroll, 500, 450));
        stage.show();
    }

    private void showTicketDialog(String ticketData) {

        String id = ticketData.split(" \\| ")[0]
                .replace("ID: ", "")
                .trim();

        String res = send("GET_TICKETS " + citizenIdField.getText());

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

                        String payRes = send("PAY_TICKET " + p[0]);
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

                String res = send("APPROVE_USER " + currentUser + " " + username);
                showPopup("Zatwierdzanie", res);

            } else if (result == deleteBtn) {

                String res = send("DELETE_PENDING " + currentUser + " " + username);
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

                String res = send("DELETE_USER " + currentUser + " " + username);
                showPopup("Usuwanie użytkownika", res);

                onLoadAllUsers(); // refresh listy
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
                        "SEND_PATROL "
                                + currentUser + " "
                                + patrolData.replace(" ", "_") + " "
                                + address.replace(" ", "_")
                );

                showPopup("Patrol", res);
            }

            else if (result.get() == checkBtn) {

                String address = locationField.getText();

                if (address.isBlank()) {
                    showPopup("Błąd", "Podaj adres");
                    continue;
                }

                /*
                 * - Google Maps API
                 * - OpenStreetMap
                 * - GPS patroli
                 */

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
                        "DELETE_PATROL "
                                + currentUser + " "
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

}