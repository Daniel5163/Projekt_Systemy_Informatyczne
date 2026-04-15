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
    @FXML private TextArea citizenOutputArea;

    @FXML private TextArea problemReportTextArea;

    @FXML private TextField ticketIdFieldPolice;

    private String currentUser;
    private String currentRole;

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

            statusLabel.setText("Zalogowano jako: " + currentRole);

            setupUIForRole(currentRole);

        } else {
            statusLabel.setText(res);
        }
    }

    @FXML
    protected void onLogoutClicked() {
        send("LOGOUT " + currentUser);

        currentUser = null;
        currentRole = null;

        loginPane.setVisible(true);
        contentBox.setVisible(false);
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
    }

    @FXML
    protected void addUser() {
        String res = send("ADD_USER " + currentUser + " "
                + newUserField.getText() + " "
                + newPasswordField.getText() + " "
                + newRoleField.getValue());

        statusLabel.setText(res);
    }

    @FXML
    protected void removeUser() {
        String res = send("REMOVE_USER " + currentUser + " " + newUserField.getText());
        statusLabel.setText(res);
    }

    @FXML
    protected void onCheckUserClicked() {
        statusLabel.setText(send("CHECK_USER " + currentUser + " " + checkUserField.getText()));
    }

    @FXML
    protected void onCheckDriverClicked() {
        statusLabel.setText(send("CHECK_DRIVER " + currentUser + " " + checkDriverIdField.getText()));
    }

    @FXML
    protected void onCheckLicensePlateClicked() {
        statusLabel.setText(send("CHECK_PLATE " + currentUser + " " + licensePlateField.getText()));
    }

    @FXML
    protected void issuePenaltyTicket() {

        String ticketId = ticketIdFieldPolice.getText();
        String driverId = driverIdField.getText();
        String points = penaltyPointsField.getText();
        String fine = fineAmountField.getText();
        String reason = reasonField.getText();

        if (ticketId.isBlank() || driverId.isBlank() || points.isBlank() || fine.isBlank() || reason.isBlank()) {
            statusLabel.setText("ERROR: Uzupełnij wszystkie pola");
            return;
        }

        String cmd = "ISSUE_TICKET "
                + currentUser + " "
                + ticketId + " "
                + driverId + " "
                + points + " "
                + fine + " "
                + reason.replace(" ", "_");

        String res = send(cmd);
        statusLabel.setText(res);
    }

    @FXML
    protected void onSaveReportClicked() {
        statusLabel.setText(send(
                "SAVE_REPORT " + currentUser + " " + currentRole + " " +
                        reportTextArea.getText().replace(" ", "_")
        ));
    }

    @FXML
    protected void onReportProblemClicked() {
        statusLabel.setText(send(
                "ADD_INCIDENT " +
                        problemReportTextArea.getText().replace(" ", "_")
        ));
    }

    @FXML
    protected void onSendMessageClicked() {
        String res = send("SEND_MSG " + currentUser + " "
                + recipientComboBox.getValue() + " "
                + messageTextArea.getText().replace(" ", "_"));

        statusLabel.setText(res);
    }

    @FXML
    protected void onRefreshMessagesClicked() {
        String res = send("GET_MSGS " + currentUser);

        if (res.startsWith("SUCCESS:")) {
            messagesListView.getItems().setAll(res.substring(8).split(";;"));
        } else {
            statusLabel.setText(res);
        }
    }

    @FXML
    protected void onGetPointsClicked() {
        String res = send("GET_POINTS " + citizenIdField.getText());
        citizenOutputArea.setText(res);
    }

    @FXML
    protected void onGetTicketsClicked() {
        String res = send("GET_TICKETS " + citizenIdField.getText());

        if (!res.startsWith("SUCCESS:")) {
            citizenOutputArea.setText(res);
            return;
        }

        String data = res.substring(8);

        if (data.equals("EMPTY") || data.isBlank()) {
            citizenOutputArea.setText("Brak mandatów 🎉");
            return;
        }

        String[] tickets = data.split(";;");

        StringBuilder sb = new StringBuilder();

        for (String t : tickets) {
            String[] p = t.split(",");

            sb.append("🧾 Mandat ID: ").append(p[0]).append("\n")
                    .append("🚗 Kierowca: ").append(p[1]).append("\n")
                    .append("👮 Wystawił: ").append(p[2]).append("\n")
                    .append("⚠️ Punkty: ").append(p[3]).append("\n")
                    .append("💰 Grzywna: ").append(p[4]).append("\n")
                    .append("💳 Opłacony: ").append(p[5]).append("\n")
                    .append("----------------------\n");
        }

        citizenOutputArea.setText(sb.toString());
    }

    @FXML
    protected void onPayTicketClicked() {
        String res = send("PAY_TICKET " + ticketIdField.getText());
        citizenOutputArea.setText(res);
    }

    private String send(String cmd) {
        try (Socket socket = new Socket("localhost", 5556);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(cmd);

            String response = in.readLine();

            return response != null ? response : "ERROR: brak odpowiedzi";

        } catch (Exception e) {
            return "ERROR: brak połączenia z serwerem";
        }
    }
}