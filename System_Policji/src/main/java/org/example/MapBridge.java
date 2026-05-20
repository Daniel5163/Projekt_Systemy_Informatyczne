package org.example;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import java.util.Locale;

public class MapBridge {

    private final String username;
    private WebEngine webEngine;

    public MapBridge(String username) {
        this.username = username;
    }

    public void setWebEngine(WebEngine webEngine) {
        this.webEngine = webEngine;
    }

    public MapBridge(String username, WebEngine webEngine) {
        this.username = username;
        this.webEngine = webEngine;
    }

    public void sendLocation(double lat, double lng) {
        System.out.println(" [POLICJANT] " + username + "  " + lat + ", " + lng);

        try (Socket socket = new Socket("localhost", 5556);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("ADD_PATROL_LOCATION|" + username + "|" + lat + "|" + lng);
            String response = in.readLine();

            System.out.println("Serwer: " + response);

            if (response != null && response.startsWith("SUCCESS")) {
                Platform.runLater(() -> {
                    if (webEngine != null) {

                        String script = String.format(Locale.US,
                                "updatePatrolPin(%.8f, %.8f, '%s');",
                                lng, lat, username.replace("'", "\\'")
                        );
                        System.out.println("Wykonuję JS: " + script);
                        webEngine.executeScript(script);
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshPatrolPins() {
        if (webEngine == null) return;

        Platform.runLater(() -> {
            try (Socket socket = new Socket("localhost", 5556);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("GET_PATROL_LOCATIONS|" + username);
                String response = in.readLine();

                if (response != null && response.startsWith("SUCCESS:")) {
                    String data = response.substring(8);

                    webEngine.executeScript("clearAllPins();");

                    if (data != null && !data.isBlank()) {
                        for (String line : data.split(";;")) {
                            if (line.trim().isEmpty()) continue;
                            String[] p = line.split("\\|");
                            if (p.length >= 4) {
                                try {
                                    double lat = Double.parseDouble(p[2]);
                                    double lon = Double.parseDouble(p[3]);
                                    String user = p[1];

                                    webEngine.executeScript(
                                            "updatePatrolPin(" + lon + ", " + lat + ", '" + user.replace("'", "\\'") + "');"
                                    );
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Błąd refreshPatrolPins: " + e.getMessage());
            }
        });
    }
}