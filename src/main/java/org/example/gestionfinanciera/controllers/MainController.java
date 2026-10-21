package org.example.gestionfinanciera.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

public class MainController {
    @FXML private StackPane content;

    @FXML
    public void initialize() {

            //goMovements(); // vista inicial
    }

//    @FXML private void goMovements() { show("finances-view.fxml"); }
//    @FXML private void goBudget() { show("budget-view.fxml"); }
//    @FXML private void goExport()    { show("export-view.fxml"); }
//
//    private void show(String file) {
//        URL location = MainController.class.getResource(file);
//        System.out.println("Loading FXML file: " + location);
//        try {
//            Node view = FXMLLoader.load(location);
//            content.getChildren().setAll(view);
//        } catch (IOException e) {
//            throw new RuntimeException("No se pudo cargar " + file, e);
//        }
//    }
}