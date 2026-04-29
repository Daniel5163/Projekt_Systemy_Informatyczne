package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.Socket;

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

        String ticketId = ticketIdFieldPolice.getText();
        String driverId = driverIdField.getText();
        String points = penaltyPointsField.getText();
        String fine = fineAmountField.getText();
        String reason = reasonField.getText();

        if (ticketId.isBlank() || driverId.isBlank() || points.isBlank() || fine.isBlank() || reason.isBlank()) {
            showPopup("Błąd", "Uzupełnij wszystkie pola");
            return;
        }

        String cmd = "ISSUE_TICKET "
                + currentUser + " "
                + ticketId + " "
                + driverId + " "
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

            String display =
                    "ID: " + p[0] +
                            " | Punkty: " + p[2] +
                            " | " + (p[4].equals("true") ? "Opłacony" : "NIEOPŁACONY");

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

        onRefreshPendingUsers(); // odśwież listę
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

            if (p[0].equals(id)) {

                String details =
                        "ID: " + p[0] + "\n" +
                                "Punkty: " + p[2] + "\n" +
                                "Powód: " + p[3] + "\n" +
                                "Status: " + (p[4].equals("true") ? "Opłacony" : "Nieopłacony");

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Mandat");
                alert.setHeaderText("Szczegóły mandatu");
                alert.setContentText(details);

                ButtonType payBtn = new ButtonType("Opłać");
                ButtonType closeBtn = new ButtonType("Zamknij", ButtonBar.ButtonData.CANCEL_CLOSE);

                if (p[4].equals("false")) {
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
}