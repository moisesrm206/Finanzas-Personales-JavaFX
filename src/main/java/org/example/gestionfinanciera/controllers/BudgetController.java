package org.example.gestionfinanciera.controllers;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.example.gestionfinanciera.dao.BudgetDAOImpl;
import org.example.gestionfinanciera.dao.CategoryDAOImpl;
import org.example.gestionfinanciera.dao.MovementDAOImpl;
import org.example.gestionfinanciera.models.Budget;
import org.example.gestionfinanciera.models.Category;
import org.example.gestionfinanciera.services.ActualSession;
import org.example.gestionfinanciera.services.BudgetService;
import org.example.gestionfinanciera.services.BudgetServiceImpl;
import org.example.gestionfinanciera.services.BudgetStatus;
import org.example.gestionfinanciera.services.CategoryService;
import org.example.gestionfinanciera.services.CategoryServiceImpl;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla de presupuestos: fija un tope de gasto mensual por categoría y muestra cuánto
 * se lleva gastado, avisando cuando se está cerca de rebasarlo (desde el 80 %) o ya se rebasó.
 * Todo se consulta y se guarda a través de BudgetService, dentro de un Task.
 */
public class BudgetController {

    private static final Locale MX = Locale.of("es", "MX");

    private static final String ALERT_OK = "budget-alert-ok";
    private static final String ALERT_WARNING = "budget-alert-warning";
    private static final String ALERT_EXCEEDED = "budget-alert-exceeded";
    private static final String MSG_OK = "budget-msg-ok";
    private static final String MSG_ERROR = "budget-msg-error";

    @FXML private ComboBox<YearMonth> cbMonth;
    @FXML private ComboBox<Category> cbCategory;
    @FXML private TextField txtLimit;
    @FXML private Button btnSave;
    @FXML private Label lblFormMessage;
    @FXML private Label lblAlert;
    @FXML private Label lblEmpty;
    @FXML private VBox boxBudgets;
    @FXML private ProgressIndicator piLoading;

    private final BudgetDAOImpl budgetDAO = new BudgetDAOImpl();
    private final CategoryDAOImpl categoryDAO = new CategoryDAOImpl();
    private final MovementDAOImpl movementDAO = new MovementDAOImpl();

    private final BudgetService budgetService =
            new BudgetServiceImpl(budgetDAO, movementDAO, categoryDAO);
    private final CategoryService categoryService =
            new CategoryServiceImpl(categoryDAO, movementDAO, budgetDAO);

    private final NumberFormat money = NumberFormat.getCurrencyInstance(MX);
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private int runningTasks = 0;
    private int budgetsRequest = 0; // para ignorar respuestas atrasadas si se cambia de mes rápido

    @FXML
    private void initialize() {
        piLoading.visibleProperty().bind(busy);
        btnSave.disableProperty().bind(busy);

        // Los mensajes solo ocupan espacio cuando tienen texto
        lblAlert.visibleProperty().bind(lblAlert.textProperty().isNotEmpty());
        lblAlert.managedProperty().bind(lblAlert.visibleProperty());
        lblFormMessage.visibleProperty().bind(lblFormMessage.textProperty().isNotEmpty());
        lblFormMessage.managedProperty().bind(lblFormMessage.visibleProperty());

        // Los 12 meses del año actual (el presupuesto guarda el mes, sin año)
        int year = Year.now().getValue();
        ObservableList<YearMonth> months = FXCollections.observableArrayList();
        for (int month = 1; month <= 12; month++) {
            months.add(YearMonth.of(year, month));
        }
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
        cbMonth.setValue(YearMonth.now());
        cbMonth.valueProperty().addListener((obs, oldValue, newValue) -> {
            setFormMessage("", false);
            loadBudgets();
        });

        cbCategory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.getCategoryName();
            }

            @Override
            public Category fromString(String text) {
                return null;
            }
        });

        loadCategories();
        loadBudgets();
    }

    // ---------- Carga de datos ----------

    private void loadCategories() {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        // Si el usuario es nuevo y no tiene categorías, se crea un conjunto básico
        Task<List<Category>> task = categoryService.ensureDefaultCategoriesTask(userId);
        begin();
        task.setOnSucceeded(event -> {
            end();
            cbCategory.setItems(FXCollections.observableArrayList(task.getValue()));
        });
        task.setOnFailed(event -> {
            end();
            showError(task.getException());
        });
        run(task);
    }

    private void loadBudgets() {
        YearMonth month = cbMonth.getValue();
        String userId = currentUserId();
        if (month == null || userId == null) {
            return;
        }

        int request = ++budgetsRequest;
        Task<List<BudgetStatus>> task = budgetService.loadBudgetStatusTask(userId, month);
        begin();
        task.setOnSucceeded(event -> {
            end();
            if (request == budgetsRequest) {
                showBudgets(task.getValue());
            }
        });
        task.setOnFailed(event -> {
            end();
            if (request == budgetsRequest) {
                showError(task.getException());
            }
        });
        run(task);
    }

    // ---------- Guardar y eliminar ----------

    @FXML
    private void saveBudget() {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        Category category = cbCategory.getValue();
        YearMonth month = cbMonth.getValue();
        if (category == null) {
            setFormMessage("Elige una categoría.", true);
            return;
        }
        Double limit = parseAmount(txtLimit.getText());
        if (limit == null || limit <= 0) {
            setFormMessage("Escribe un límite válido mayor que cero, por ejemplo 3500.", true);
            return;
        }

        Task<Budget> task = budgetService.saveBudgetTask(
                userId, category.getCategoryId(), limit, month.getMonthValue());
        begin();
        task.setOnSucceeded(event -> {
            end();
            txtLimit.clear();
            setFormMessage("Presupuesto de " + category.getCategoryName()
                    + " guardado para " + monthLabel(month) + ".", false);
            loadBudgets();
        });
        task.setOnFailed(event -> {
            end();
            showError(task.getException());
        });
        run(task);
    }

    private void confirmDelete(BudgetStatus status) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el presupuesto de " + status.categoryName() + " para "
                        + monthLabel(cbMonth.getValue()) + "?",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Eliminar presupuesto");
        alert.setHeaderText(null);
        alert.initOwner(boxBudgets.getScene().getWindow());
        alert.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .ifPresent(button -> deleteBudget(status));
    }

    private void deleteBudget(BudgetStatus status) {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        Task<Void> task = budgetService.deleteBudgetTask(userId, status.budget().getBudgetId());
        begin();
        task.setOnSucceeded(event -> {
            end();
            setFormMessage("Presupuesto eliminado.", false);
            loadBudgets();
        });
        task.setOnFailed(event -> {
            end();
            showError(task.getException());
        });
        run(task);
    }

    /** Pasa los datos del presupuesto al formulario para cambiar su límite. */
    private void fillForm(BudgetStatus status) {
        for (Category category : cbCategory.getItems()) {
            if (category.getCategoryId().equals(status.budget().getCategoryId())) {
                cbCategory.setValue(category);
                break;
            }
        }
        txtLimit.setText(BigDecimal.valueOf(status.budget().getLimit())
                .stripTrailingZeros().toPlainString());
        txtLimit.requestFocus();
        txtLimit.selectAll();
        setFormMessage("Editando el límite de " + status.categoryName()
                + ". Presiona Guardar para aplicar el cambio.", false);
    }

    // ---------- Pintar la lista y el aviso ----------

    private void showBudgets(List<BudgetStatus> statuses) {
        boxBudgets.getChildren().clear();
        for (BudgetStatus status : statuses) {
            boxBudgets.getChildren().add(buildCard(status));
        }
        lblEmpty.setVisible(statuses.isEmpty());
        lblEmpty.setManaged(statuses.isEmpty());
        updateAlert(statuses);
    }

    private void updateAlert(List<BudgetStatus> statuses) {
        lblAlert.getStyleClass().removeAll(ALERT_OK, ALERT_WARNING, ALERT_EXCEEDED);
        if (statuses.isEmpty()) {
            lblAlert.setText("");
            lblAlert.setGraphic(null);
            return;
        }

        // El servicio ya entrega los presupuestos del más usado al menos usado
        List<String> parts = new ArrayList<>();
        boolean anyExceeded = false;
        for (BudgetStatus status : statuses) {
            String percent = String.format(MX, "%.0f %%", status.percentage());
            if (status.level() == BudgetStatus.Level.EXCEEDED) {
                anyExceeded = true;
                parts.add(status.categoryName() + " rebasó su límite (" + percent + ")");
            } else if (status.level() == BudgetStatus.Level.WARNING) {
                parts.add(status.categoryName() + " va en " + percent + " de su límite");
            }
        }

        if (parts.isEmpty()) {
            lblAlert.getStyleClass().add(ALERT_OK);
            lblAlert.setGraphic(new FontIcon("fas-check-circle"));
            lblAlert.setText("Todos tus presupuestos de este mes están dentro del límite.");
        } else {
            lblAlert.getStyleClass().add(anyExceeded ? ALERT_EXCEEDED : ALERT_WARNING);
            lblAlert.setGraphic(new FontIcon("fas-exclamation-triangle"));
            lblAlert.setText("Atención: " + String.join(" · ", parts));
        }
    }

    private Node buildCard(BudgetStatus status) {
        double limit = status.budget().getLimit();
        double remaining = limit - status.spent();

        Label name = label(status.categoryName(), "budget-card-title");
        Label badge = label(levelText(status.level()), "budget-badge", badgeClass(status.level()));
        Button edit = iconButton("fas-pen", "Editar límite", () -> fillForm(status));
        Button delete = iconButton("fas-trash", "Eliminar presupuesto",
                () -> confirmDelete(status), "budget-icon-delete");

        HBox top = new HBox(8, name, grow(), badge, edit, delete);
        top.setAlignment(Pos.CENTER_LEFT);

        Label amounts = label(money.format(status.spent()) + " de " + money.format(limit),
                "budget-card-amounts");
        Label percent = label(String.format(MX, "%.0f %%", status.percentage()), "budget-card-percent");
        HBox middle = new HBox(8, amounts, grow(), percent);
        middle.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar(Math.min(1.0, status.percentage() / 100.0));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add(barClass(status.level()));

        Label remainingLabel = label(remaining >= 0
                        ? "Te quedan " + money.format(remaining)
                        : "Te pasaste por " + money.format(-remaining),
                "budget-card-remaining");

        VBox card = new VBox(8, top, middle, bar, remainingLabel);
        card.getStyleClass().add("budget-card");
        return card;
    }

    private String levelText(BudgetStatus.Level level) {
        return switch (level) {
            case OK -> "Dentro del límite";
            case WARNING -> "Cerca del límite";
            case EXCEEDED -> "Límite rebasado";
        };
    }

    private String badgeClass(BudgetStatus.Level level) {
        return switch (level) {
            case OK -> "budget-badge-ok";
            case WARNING -> "budget-badge-warning";
            case EXCEEDED -> "budget-badge-exceeded";
        };
    }

    private String barClass(BudgetStatus.Level level) {
        return switch (level) {
            case OK -> "budget-bar-ok";
            case WARNING -> "budget-bar-warning";
            case EXCEEDED -> "budget-bar-exceeded";
        };
    }

    // ---------- Utilidades ----------

    private Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    private Region grow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private Button iconButton(String iconLiteral, String tooltip, Runnable action,
                              String... extraStyleClasses) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(14);

        Button button = new Button();
        button.setGraphic(icon);
        button.setTooltip(new Tooltip(tooltip));
        button.getStyleClass().add("budget-icon-btn");
        button.getStyleClass().addAll(extraStyleClasses);
        button.setOnAction(event -> action.run());
        return button;
    }

    /** Acepta formatos como "3500", "3,500.50" o "$3500". Devuelve null si no es un número. */
    private Double parseAmount(String text) {
        if (text == null) {
            return null;
        }
        String clean = text.replace("$", "").replace(",", "").trim();
        if (clean.isEmpty()) {
            return null;
        }
        try {
            double value = Double.parseDouble(clean);
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String monthLabel(YearMonth month) {
        if (month == null) {
            return "";
        }
        String name = month.getMonth().getDisplayName(TextStyle.FULL, MX);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " " + month.getYear();
    }

    private void setFormMessage(String text, boolean error) {
        lblFormMessage.getStyleClass().removeAll(MSG_OK, MSG_ERROR);
        if (text != null && !text.isEmpty()) {
            lblFormMessage.getStyleClass().add(error ? MSG_ERROR : MSG_OK);
        }
        lblFormMessage.setText(text == null ? "" : text);
    }

    private void showError(Throwable error) {
        if (error instanceof IllegalArgumentException) {
            setFormMessage(error.getMessage(), true);
            return;
        }
        error.printStackTrace();
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        setFormMessage("No se pudo completar la operación: " + cause.getMessage(), true);
    }

    private String currentUserId() {
        try {
            return ActualSession.getInstance().getUserId();
        } catch (IllegalStateException e) {
            setFormMessage("No hay una sesión iniciada.", true);
            return null;
        }
    }

    /** Lleva la cuenta de tareas en curso para mostrar el indicador de carga. */
    private void begin() {
        runningTasks++;
        busy.set(true);
    }

    private void end() {
        runningTasks = Math.max(0, runningTasks - 1);
        busy.set(runningTasks > 0);
    }

    private void run(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}