package org.example.gestionfinanciera.charts;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;
import org.example.gestionfinanciera.dao.CategoryDAOImpl;
import org.example.gestionfinanciera.dao.MovementDAOImpl;
import org.example.gestionfinanciera.services.*;

import java.text.NumberFormat;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

public class dashboard {

    private static final Locale MX = Locale.of("es", "MX");
    private static final String[] MONTH_LABELS =
            {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    private final ComboBox<YearMonth> cbMonth;
    private final ComboBox<Integer> cbYear;
    private final PieChart pieExpenses;
    private final Label lblPieSummary;
    private final CategoryAxis xAxis;
    private final NumberAxis yAxis;
    private final LineChart<String, Number> lineTrend;
    private final Label lblTrendSummary;
    private final Label lblError;
    private final ProgressIndicator piLoading;

    private final MovementService movementService =
            new MovementServiceImpl(new MovementDAOImpl(), new CategoryDAOImpl());
    private final NumberFormat money = NumberFormat.getCurrencyInstance(MX);

    private final BooleanProperty loadingPie = new SimpleBooleanProperty(false);
    private final BooleanProperty loadingLine = new SimpleBooleanProperty(false);

    private int pieRequest = 0;
    private int lineRequest = 0;

    // Constructor que recibe los componentes inyectados desde el HomeController
    public dashboard(ComboBox<YearMonth> cbMonth, ComboBox<Integer> cbYear,
                     PieChart pieExpenses, Label lblPieSummary,
                     CategoryAxis xAxis, NumberAxis yAxis,
                     LineChart<String, Number> lineTrend, Label lblTrendSummary,
                     Label lblError, ProgressIndicator piLoading) {
        this.cbMonth = cbMonth;
        this.cbYear = cbYear;
        this.pieExpenses = pieExpenses;
        this.lblPieSummary = lblPieSummary;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.lineTrend = lineTrend;
        this.lblTrendSummary = lblTrendSummary;
        this.lblError = lblError;
        this.piLoading = piLoading;

        initDashboard();
    }

    private void initDashboard() {
        if (piLoading != null) {
            piLoading.visibleProperty().bind(loadingPie.or(loadingLine));
        }

        // Selector de mes: los últimos 12 meses
        YearMonth current = YearMonth.now();
        ObservableList<YearMonth> months = FXCollections.observableArrayList();
        for (int i = 0; i < 12; i++) {
            months.add(current.minusMonths(i));
        }
        if (cbMonth != null) {
            cbMonth.setItems(months);
            cbMonth.setConverter(new StringConverter<>() {
                @Override
                public String toString(YearMonth month) {
                    return month == null ? "" : monthLabel(month);
                }
                @Override
                public YearMonth fromString(String text) {
                    return null;
                }
            });
            cbMonth.setValue(current);
            cbMonth.valueProperty().addListener((obs, oldValue, newValue) -> loadPie());
        }

        // Selector de año: el actual y los dos anteriores
        int year = Year.now().getValue();
        if (cbYear != null) {
            cbYear.setItems(FXCollections.observableArrayList(year, year - 1, year - 2));
            cbYear.setValue(year);
            cbYear.valueProperty().addListener((obs, oldValue, newValue) -> loadLine());
        }

        if (xAxis != null) xAxis.setCategories(FXCollections.observableArrayList(MONTH_LABELS));
        if (yAxis != null) yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, "$", ""));
        if (pieExpenses != null) pieExpenses.setAnimated(false);
        if (lineTrend != null) lineTrend.setAnimated(false);

        refresh();
    }

    public void refresh() {
        loadPie();
        loadLine();
    }

    private void loadPie() {
        if (cbMonth == null || pieExpenses == null) return;
        YearMonth month = cbMonth.getValue();
        String userId = currentUserId();
        if (month == null || userId == null) return;

        int request = ++pieRequest;
        Task<Map<String, Double>> task = movementService.loadExpenseDistributionTask(userId, month);
        loadingPie.unbind();
        loadingPie.bind(task.runningProperty());

        task.setOnSucceeded(event -> {
            if (request == pieRequest) {
                if (lblError != null) lblError.setText("");
                showPie(task.getValue(), month);
            }
        });
        task.setOnFailed(event -> {
            if (request == pieRequest) showError(task.getException());
        });
        run(task);
    }

    private void showPie(Map<String, Double> expenses, YearMonth month) {
        double total = expenses.values().stream().mapToDouble(Double::doubleValue).sum();

        ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : expenses.entrySet()) {
            double percent = total > 0 ? entry.getValue() / total * 100 : 0;
            String label = String.format(MX, "%s (%.1f %%)", entry.getKey(), percent);
            slices.add(new PieChart.Data(label, entry.getValue()));
        }

        pieExpenses.setData(slices);
        pieExpenses.setTitle("Distribución de gastos · " + monthLabel(month));

        for (PieChart.Data slice : slices) {
            installTooltip(slice.getNode(), slice.getName() + "\n" + money.format(slice.getPieValue()));
        }

        if (slices.isEmpty()) {
            if (lblPieSummary != null) lblPieSummary.setText("No hay gastos registrados en este mes.");
            return;
        }
        Map.Entry<String, Double> top = expenses.entrySet().iterator().next();
        if (lblPieSummary != null) {
            lblPieSummary.setText(String.format(MX, "Total gastado: %s · Mayor gasto: %s (%.1f %%)",
                    money.format(total), top.getKey(), top.getValue() / total * 100));
        }
    }

    private void loadLine() {
        if (cbYear == null || lineTrend == null) return;
        Integer year = cbYear.getValue();
        String userId = currentUserId();
        if (year == null || userId == null) return;

        int request = ++lineRequest;
        Task<MonthlyTotals> task = movementService.loadMonthlyTrendTask(userId, year);
        loadingLine.unbind();
        loadingLine.bind(task.runningProperty());

        task.setOnSucceeded(event -> {
            if (request == lineRequest) {
                if (lblError != null) lblError.setText("");
                showLine(task.getValue(), year);
            }
        });
        task.setOnFailed(event -> {
            if (request == lineRequest) showError(task.getException());
        });
        run(task);
    }

    private void showLine(MonthlyTotals totals, int year) {
        int lastMonth = (year == Year.now().getValue()) ? YearMonth.now().getMonthValue() : 12;

        XYChart.Series<String, Number> income = new XYChart.Series<>();
        income.setName("Ingresos");
        XYChart.Series<String, Number> expenses = new XYChart.Series<>();
        expenses.setName("Gastos");

        double totalIncome = 0;
        double totalExpenses = 0;
        int deficitMonths = 0;

        for (int i = 0; i < lastMonth; i++) {
            income.getData().add(new XYChart.Data<>(MONTH_LABELS[i], totals.income()[i]));
            expenses.getData().add(new XYChart.Data<>(MONTH_LABELS[i], totals.expenses()[i]));
            totalIncome += totals.income()[i];
            totalExpenses += totals.expenses()[i];
            if (totals.expenses()[i] > totals.income()[i]) deficitMonths++;
        }

        lineTrend.setTitle("Ingresos vs gastos · " + year);
        lineTrend.getData().clear();
        lineTrend.getData().add(income);
        lineTrend.getData().add(expenses);
        addPointTooltips(income);
        addPointTooltips(expenses);

        if (lblTrendSummary != null) {
            lblTrendSummary.setText(String.format(MX,
                    "Balance del periodo: %s · Los gastos superaron a los ingresos en %d de %d meses",
                    money.format(totalIncome - totalExpenses), deficitMonths, lastMonth));
        }
    }

    private void addPointTooltips(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> point : series.getData()) {
            installTooltip(point.getNode(), series.getName() + " " + point.getXValue()
                    + ": " + money.format(point.getYValue().doubleValue()));
        }
    }

    private void installTooltip(Node node, String text) {
        if (node != null) Tooltip.install(node, new Tooltip(text));
    }

    private String monthLabel(YearMonth month) {
        String name = month.getMonth().getDisplayName(TextStyle.FULL, MX);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " " + month.getYear();
    }

    private String currentUserId() {
        try {
            return ActualSession.getInstance().getUserId();
        } catch (IllegalStateException e) {
            if (lblError != null) lblError.setText("No hay una sesión iniciada.");
            return null;
        }
    }

    private void showError(Throwable error) {
        if (error instanceof IllegalArgumentException) {
            if (lblError != null) lblError.setText(error.getMessage());
            return;
        }
        error.printStackTrace();
        if (lblError != null) lblError.setText("No se pudieron cargar los gráficos. Revisa tu conexión.");
    }

    private void run(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}