package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Aplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(
                Aplication.class.getResource("/hello-view.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("System Policji");

        stage.setWidth(1400);
        stage.setHeight(700);

        stage.setScene(scene);

        stage.show();
    }

    public static void main(String[] args) {

        System.setProperty("prism.order", "sw");

        launch(args);
    }
}