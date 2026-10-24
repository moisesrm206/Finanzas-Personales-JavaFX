package org.example.gestionfinanciera.controllers;

import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import org.example.gestionfinanciera.charts.dashboard;

import java.time.YearMonth;

public class HomeController {

    // Componentes gráficos inyectados desde home-view.fxml
    @FXML private ComboBox<YearMonth> cbMonth;
    @FXML private ComboBox<Integer> cbYear;
    @FXML private PieChart pieExpenses;
    @FXML private Label lblPieSummary;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private LineChart<String, Number> lineTrend;
    @FXML private Label lblTrendSummary;
    @FXML private Label lblError;
    @FXML private ProgressIndicator piLoading;

    private dashboard dashboardComponent;

    @FXML
    public void initialize() {
        // Inicializamos el componente dashboard pasándole las referencias de la vista
        dashboardComponent = new dashboard(
                cbMonth, cbYear, pieExpenses, lblPieSummary,
                xAxis, yAxis, lineTrend, lblTrendSummary, lblError, piLoading
        );
    }

    // Si en un botón de "Actualizar" de tu Home quieres refrescar los gráficos:
    @FXML
    private void handleRefresh() {
        if (dashboardComponent != null) {
            dashboardComponent.refresh();
        }
    }
}