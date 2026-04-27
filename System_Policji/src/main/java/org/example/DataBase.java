package org.example;

import java.sql.Connection;
import java.sql.DriverManager;

public class DataBase {

    private static final String URL =
            "jdbc:mysql://localhost:3306/police_system";

    private static final String USER = "root";
    private static final String PASSWORD = "admin";

    public static Connection connect() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}