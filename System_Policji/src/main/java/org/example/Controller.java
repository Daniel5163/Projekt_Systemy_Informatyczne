package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Controller {

    @FXML private VBox loginPane;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private VBox buttonsBox;
    @FXML private Button logoutButton;
    @FXML private VBox reportBox;
    @FXML private TextArea reportTextArea;
    @FXML private VBox contentBox;
    @FXML private VBox messagingBox;
    @FXML private ComboBox<String> recipientComboBox;
    @FXML private TextArea messageTextArea;
    @FXML private ListView<String> messagesListView;
    @FXML private TextField licensePlateField;
    @FXML private TextField checkUserField;
    @FXML private VBox problemReportBox;
    @FXML private TextArea problemReportTextArea;


    @FXML private VBox managementBox;
    @FXML private TextField newUserField;
    @FXML private PasswordField newPasswordField;
    @FXML private ComboBox<String> newRoleField;


    @FXML private TextField fineAmountField;
    @FXML private TextField reasonField;
    @FXML private VBox policemanActionsBox;
    @FXML private TextField driverIdField;
    @FXML private TextField penaltyPointsField;
    @FXML private TextField checkDriverIdField;

    private String currentUser;
    private String currentRole;

    @FXML
    protected void onLoginClicked() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String response = sendCommand("LOGIN " + username + " " + password);

        if (response.startsWith("SUCCESS:")) {
            currentRole = response.split(":")[1];
            currentUser = username;
            statusLabel.setText("Zalogowany jako: " + currentRole);

            loginPane.setVisible(false);
            contentBox.setVisible(true);
            messagingBox.setVisible(true);
            loadRecipients();


            setupUIForRole(currentRole);
        } else {
            statusLabel.setText(response);
        }
    }


    @FXML
    protected void onLogoutClicked() {
        String response = sendCommand("LOGOUT " + currentUser);
        statusLabel.setText(response);


        loginPane.setVisible(true);
        contentBox.setVisible(false);
        usernameField.clear();
        passwordField.clear();


        clearManagementFields();
        clearPolicemanFields();
        reportTextArea.clear();
        messageTextArea.clear();
        messagesListView.getItems().clear();
    }
    @FXML
    protected void onSaveReportClicked() {
        String reportContent = reportTextArea.getText();
        if (reportContent.isEmpty()) {
            statusLabel.setText("Raport nie może być pusty!");
            return;
        }

        String response = sendCommand("SAVE_REPORT " + currentUser + " " + currentRole + " " +
                reportContent.replace(" ", "_"));
        statusLabel.setText(response);
        reportTextArea.clear();
    }

    private String sendCommand(String command) {
        try (Socket socket = new Socket("localhost", 5556);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(command);
            return in.readLine();

        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR: Nie udało się połączyć z serwerem.";
        }
    }

    private void setupUIForRole(String role) {
        buttonsBox.getChildren().clear();

        problemReportBox.setVisible(true);

        if ("chief".equals(role)) {
            managementBox.setVisible(true);
            policemanActionsBox.setVisible(true);
            reportBox.setVisible(true);
        } else if ("man".equals(role)) {
            managementBox.setVisible(false);
            policemanActionsBox.setVisible(true);
            reportBox.setVisible(true);
        }

        logoutButton.setVisible(true);
    }

    @FXML
    protected void addUser() {
        if (newRoleField.getItems().isEmpty()) {
            newRoleField.getItems().addAll("man", "chief");
        }

        String newUsername = newUserField.getText();
        String newPassword = newPasswordField.getText();
        String newRole = newRoleField.getValue();

        if (newUsername.isEmpty() || newPassword.isEmpty() || newRole.isEmpty()) {
            statusLabel.setText("Uzupełnij wszystkie pola!");
            return;
        }

        String response = sendCommand("ADD_USER " + currentUser + " " + newUsername + " " + newPassword + " " + newRole);
        statusLabel.setText(response);
        clearManagementFields();
    }

    @FXML
    protected void removeUser() {
        String usernameToRemove = newUserField.getText();
        if (usernameToRemove.isEmpty()) {
            statusLabel.setText("Wpisz nazwę użytkownika do usunięcia!");
            return;
        }

        String response = sendCommand("REMOVE_USER " + currentUser + " " + usernameToRemove);
        statusLabel.setText(response);
        clearManagementFields();
    }

    private void clearManagementFields() {
        newUserField.clear();
        newPasswordField.clear();
        newRoleField.getSelectionModel().clearSelection();
    }

    @FXML
    void issuePenaltyTicket() {
        String driverId = driverIdField.getText();
        String points = penaltyPointsField.getText();
        String fineAmount = fineAmountField.getText();
        String reason = reasonField.getText();

        if (driverId.isEmpty() || points.isEmpty() || fineAmount.isEmpty() || reason.isEmpty()) {
            statusLabel.setText("Uzupełnij wszystkie pola!");
            return;
        }

        String response = sendCommand("ISSUE_TICKET " + currentUser + " " + driverId + " " + points +
                " " + fineAmount + " " + reason.replace(" ", "_"));
        statusLabel.setText(response);
        clearPolicemanFields();
    }

    @FXML
    protected void onCheckDriverClicked() {
        String driverId = checkDriverIdField.getText();
        if (driverId.isEmpty()) {
            statusLabel.setText("Wprowadź ID kierowcy!");
            return;
        }

        String response = sendCommand("CHECK_DRIVER " + currentUser + " " + driverId);
        statusLabel.setText(response);
        checkDriverIdField.clear();
    }

    private void clearPolicemanFields() {
        driverIdField.clear();
        penaltyPointsField.clear();
        fineAmountField.clear();
        reasonField.clear();
    }

    private void loadRecipients() {
        String response = sendCommand("GET_USERS " + currentUser);
        if (response.startsWith("SUCCESS:")) {
            String[] users = response.substring(8).split(",");
            recipientComboBox.getItems().clear();
            recipientComboBox.getItems().addAll(users);
        }
    }

    @FXML
    protected void onSendMessageClicked() {
        String recipient = recipientComboBox.getValue();
        String message = messageTextArea.getText();

        if (recipient == null || recipient.isEmpty()) {
            statusLabel.setText("Wybierz odbiorcę!");
            return;
        }

        if (message.isEmpty()) {
            statusLabel.setText("Wiadomość nie może być pusta!");
            return;
        }

        String response = sendCommand("SEND_MSG " + currentUser + " " + recipient + " " + message.replace(" ", "_"));
        statusLabel.setText(response);
        messageTextArea.clear();

        loadMessages();
    }

    private void loadMessages() {
        String response = sendCommand("GET_MSGS " + currentUser);
        if (response.startsWith("SUCCESS:")) {
            String[] messages = response.substring(8).split(";;");
            messagesListView.getItems().clear();
            messagesListView.getItems().addAll(messages);
        }
    }
    @FXML
    protected void onCheckLicensePlateClicked() {
        String licensePlate = licensePlateField.getText();
        if (licensePlate.isEmpty()) {
            statusLabel.setText("Wprowadź numer rejestracyjny!");
            return;
        }

        String response = sendCommand("CHECK_PLATE " + currentUser + " " + licensePlate);
        statusLabel.setText(response);
        licensePlateField.clear();
    }

    @FXML
    protected void onCheckUserClicked() {
        String username = checkUserField.getText();
        if (username.isEmpty()) {
            statusLabel.setText("Wprowadź nazwę użytkownika!");
            return;
        }

        String response = sendCommand("CHECK_USER " + currentUser + " " + username);
        statusLabel.setText(response);
        checkUserField.clear();
    }

    @FXML
    protected void onRefreshMessagesClicked() {
        loadMessages();
        statusLabel.setText("Wiadomości odświeżone");
    }

    @FXML
    protected void onReportProblemClicked() {
        String problem = problemReportTextArea.getText();
        if (problem.isEmpty()) {
            statusLabel.setText("Opis problemu nie może być pusty!");
            return;
        }

        String response = sendCommand("REPORT_PROBLEM " + currentUser + " " + problem.replace(" ", "_"));
        statusLabel.setText(response);
        problemReportTextArea.clear();
    }


}