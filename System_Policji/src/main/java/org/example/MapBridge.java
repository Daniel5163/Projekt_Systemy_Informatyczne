package org.example;

import java.io.*;
import java.net.Socket;

public class MapBridge {

    private String username;

    public MapBridge(String username) {
        this.username = username;
    }

    public void sendLocation(double lat, double lng) {

        System.out.println("Kliknięto mapę: " + lat + ", " + lng);

        try (
                Socket socket = new Socket("localhost", 5556);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {

            out.println("UPDATE_LOCATION|" + username + "|" + lat + "|" + lng);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}