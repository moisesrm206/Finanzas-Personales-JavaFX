package org.example.gestionfinanciera.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.example.gestionfinanciera.services.ActualSession;

import java.io.IOException;

public class MainController {
    @FXML private StackPane content;

    @FXML
    public void initialize() {
        // Cargar la vista inicial (home-view.fxml) al iniciar la aplicación

    }
    @FXML private void goHome() { show("home-view.fxml"); }
    @FXML private void goMovements() { show("finances-view.fxml"); }
    @FXML private void goBudget() { show("budget-view.fxml"); }
    @FXML private void goExport() { show("export-view.fxml"); }
    @FXML private void openProfile() { show("user-view.fxml"); }

    @FXML
    private void logout() {
        ActualSession.getInstance().cerrar();
        try {
            Parent view = FXMLLoader.load(getClass().getResource(
                    "/org/example/gestionfinanciera/auth-view.fxml"));
            Stage stage = (Stage) content.getScene().getWindow();
            stage.getScene().setRoot(view);
            stage.setTitle("Gestión de Finanzas Personales");
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("No se pudo cerrar la sesión.", e);
        }
    }

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