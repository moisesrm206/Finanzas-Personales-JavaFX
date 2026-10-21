package org.example.gestionfinanciera;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FinancesApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException, ExecutionException, InterruptedException {
        FXMLLoader fxmlLoader = new FXMLLoader(FinancesApplication.class.getResource("auth-view.fxml"));
        FXMLLoader fxmlLoader2 = new FXMLLoader(FinancesApplication.class.getResource("finances-view.fxml"));
        FXMLLoader fxmlLoader3 = new FXMLLoader(FinancesApplication.class.getResource("templates/base-view.fxml"));

        Scene scene = new Scene(fxmlLoader3.load(), 320, 240);
        stage.setTitle("Aplicacion de finanzas personales");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}
