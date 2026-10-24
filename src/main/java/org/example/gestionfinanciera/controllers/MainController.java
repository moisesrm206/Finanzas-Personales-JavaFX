package org.example.gestionfinanciera.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {
    @FXML private StackPane content;

    @FXML
    public void initialize() {
        // Cargar la vista inicial (home-view.fxml) al iniciar la aplicación
        goHome();
    }
    @FXML private void goHome() { show("home-view.fxml"); }
    @FXML private void goMovements() { show("finances-view.fxml"); }
    @FXML private void goBudget() { show("budget-view.fxml"); }
    @FXML private void goExport() { show("export-view.fxml"); }

    private void show(String file) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(
                    "/org/example/gestionfinanciera/" + file));
            content.getChildren().setAll(view);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("No se pudo cargar " + file, e);
        }
    }
}