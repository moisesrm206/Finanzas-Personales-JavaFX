package org.example.gestionfinanciera.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.example.gestionfinanciera.dao.AccountDAOImpl;
import org.example.gestionfinanciera.dao.BudgetDAOImpl;
import org.example.gestionfinanciera.dao.CategoryDAOImpl;
import org.example.gestionfinanciera.dao.MovementDAOImpl;
import org.example.gestionfinanciera.models.Account;
import org.example.gestionfinanciera.models.Category;
import org.example.gestionfinanciera.models.Movement;
import org.example.gestionfinanciera.models.Movement.PaymentMethod;
import org.example.gestionfinanciera.services.AccountService;
import org.example.gestionfinanciera.services.AccountServiceImpl;
import org.example.gestionfinanciera.services.ActualSession;
import org.example.gestionfinanciera.services.CategoryService;
import org.example.gestionfinanciera.services.CategoryServiceImpl;
import org.example.gestionfinanciera.services.MovementFilter;
import org.example.gestionfinanciera.services.MovementService;
import org.example.gestionfinanciera.services.MovementServiceImpl;
import org.example.gestionfinanciera.services.MovementStates;
import org.example.gestionfinanciera.services.MovementStats;
import org.example.gestionfinanciera.services.MovementTypes;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Panel de movimientos: lista con búsqueda y filtros, menú de tres puntos por movimiento
 * (modificar o eliminar) y un formulario que se abre sobre la misma ventana, desenfocando
 * la vista principal. El mismo formulario sirve para crear y para modificar.
 *
 * Todo se consulta y se guarda a través de los servicios, dentro de un Task.
 */
public class FinancesController {

    private static final Locale MX = Locale.of("es", "MX");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String ALL = "Todos";
    /** Opción "sin filtro" del filtro de categorías (no existe en la base de datos). */
    private static final Category ALL_CATEGORIES = new Category(null, "Todas las categorías", null);

    private static final String MSG_OK = "mov-msg-ok";
    private static final String MSG_ERROR = "mov-msg-error";

    // Vista principal
    @FXML private StackPane mainLayer;
    @FXML private TextField txtSearch;
    @FXML private ToggleButton btnFilter;
    @FXML private HBox boxFilters;
    @FXML private ComboBox<Category> cmbFilterCategory;
    @FXML private ComboBox<String> cmbFilterType;
    @FXML private ComboBox<String> cmbFilterState;
    @FXML private Label lblMessage;
    @FXML private Label lblCount;
    @FXML private ListView<Movement> listMovements;
    @FXML private ProgressIndicator piLoading;

    // Formulario
    @FXML private StackPane overlay;
    @FXML private Label lblFormTitle;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cmbType;
    @FXML private TextField txtAmount;
    @FXML private ComboBox<Category> cmbCategory;
    @FXML private Label lblAccount;
    @FXML private ComboBox<Account> cmbAccount;
    @FXML private VBox boxDestination;
    @FXML private ComboBox<Account> cmbDestination;
    @FXML private ComboBox<String> cmbState;
    @FXML private ComboBox<PaymentMethod> cmbPayment;
    @FXML private TextField txtConcept;
    @FXML private TextField txtTags;
    @FXML private Label lblFormError;
    @FXML private Button btnSaveForm;

    private final CategoryDAOImpl categoryDAO = new CategoryDAOImpl();
    private final MovementDAOImpl movementDAO = new MovementDAOImpl();
    private final AccountDAOImpl accountDAO = new AccountDAOImpl();
    private final BudgetDAOImpl budgetDAO = new BudgetDAOImpl();

    private final MovementService movementService = new MovementServiceImpl(movementDAO, categoryDAO);
    private final CategoryService categoryService = new CategoryServiceImpl(categoryDAO, movementDAO, budgetDAO);
    private final AccountService accountService = new AccountServiceImpl(accountDAO, movementDAO);

    private final NumberFormat money = NumberFormat.getCurrencyInstance(MX);
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private int runningTasks = 0;

    // Datos cargados
    private final ObservableList<Movement> movements = FXCollections.observableArrayList();
    private final FilteredList<Movement> filtered = new FilteredList<>(movements, m -> true);
    private final ObservableList<Account> accountItems = FXCollections.observableArrayList();
    private final List<Category> categories = new ArrayList<>();
    private final Map<String, String> categoryNames = new HashMap<>();
    private final Map<String, String> accountNames = new HashMap<>();

    /** Movimiento que se está modificando; null cuando se está creando uno nuevo. */
    private Movement editing;
    private ParallelTransition animation;

    @FXML
    private void initialize() {
        piLoading.visibleProperty().bind(busy);
        btnSaveForm.disableProperty().bind(busy);

        // Los mensajes solo ocupan espacio cuando tienen texto
        lblMessage.visibleProperty().bind(lblMessage.textProperty().isNotEmpty());
        lblMessage.managedProperty().bind(lblMessage.visibleProperty());
        lblFormError.visibleProperty().bind(lblFormError.textProperty().isNotEmpty());
        lblFormError.managedProperty().bind(lblFormError.visibleProperty());

        setupList();
        setupFilters();
        setupForm();

        loadAll();
    }

    // =====================================================================
    //  Lista
    // =====================================================================

    private void setupList() {
        listMovements.setItems(filtered);
        listMovements.setCellFactory(list -> new MovementCell());
        listMovements.setPlaceholder(new Label("No hay movimientos para mostrar."));
        lblCount.textProperty().bind(Bindings.createStringBinding(
                () -> filtered.size() == 1 ? "1 movimiento" : filtered.size() + " movimientos", filtered));
    }

    /** Una fila de la lista: concepto, detalle, fecha, estado, monto y menú de tres puntos. */
    private final class MovementCell extends ListCell<Movement> {

        private final Label lblConcept = new Label();
        private final Label lblDetail = new Label();
        private final Label lblDate = new Label();
        private final Label lblState = new Label();
        private final Label lblAmount = new Label();
        private final Label lblType = new Label();
        private final HBox row;

        MovementCell() {
            lblConcept.getStyleClass().add("mov-concept");
            lblDetail.getStyleClass().add("mov-detail");
            lblDate.getStyleClass().add("mov-date");
            lblState.getStyleClass().add("mov-state");
            lblAmount.getStyleClass().add("mov-amount");
            lblType.getStyleClass().add("mov-type");

            VBox left = new VBox(4, lblConcept, lblDetail);
            left.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(left, Priority.ALWAYS);

            VBox dateBox = new VBox(6, lblDate, lblState);
            dateBox.setAlignment(Pos.CENTER_LEFT);
            dateBox.setMinWidth(120);
            dateBox.setPrefWidth(120);

            VBox amountBox = new VBox(4, lblAmount, lblType);
            amountBox.setAlignment(Pos.CENTER_RIGHT);
            amountBox.setMinWidth(150);
            amountBox.setPrefWidth(150);

            MenuItem editItem = new MenuItem("Modificar");
            editItem.setGraphic(new FontIcon("fas-pen"));
            editItem.setOnAction(event -> {
                if (getItem() != null) {
                    openForm(getItem());
                }
            });

            MenuItem deleteItem = new MenuItem("Eliminar");
            deleteItem.setGraphic(new FontIcon("fas-trash"));
            deleteItem.getStyleClass().add("mov-menu-delete");
            deleteItem.setOnAction(event -> {
                if (getItem() != null) {
                    confirmDelete(getItem());
                }
            });

            FontIcon dots = new FontIcon("fas-ellipsis-v");
            dots.setIconSize(16);
            MenuButton menu = new MenuButton();
            menu.setGraphic(dots);
            menu.getItems().addAll(editItem, deleteItem);
            menu.getStyleClass().add("mov-menu");

            row = new HBox(16, left, dateBox, amountBox, menu);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("mov-row");
            row.prefWidthProperty().bind(widthProperty().subtract(8));

            setPrefWidth(0);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Movement movement, boolean empty) {
            super.updateItem(movement, empty);
            if (empty || movement == null) {
                setGraphic(null);
                return;
            }

            boolean income = MovementTypes.INCOME.equalsIgnoreCase(movement.getType());
            boolean expense = MovementTypes.EXPENSE.equalsIgnoreCase(movement.getType());

            lblConcept.setText(movement.getConcept());
            lblDetail.setText(detailText(movement));

            LocalDate day = MovementStats.dayOf(movement);
            lblDate.setText(day == null ? "" : DATE_FORMAT.format(day));

            lblState.setText(capitalize(movement.getState()));
            lblState.getStyleClass().removeAll("mov-state-aplicado", "mov-state-pendiente", "mov-state-conciliado");
            if (movement.getState() != null) {
                lblState.getStyleClass().add("mov-state-" + movement.getState().toLowerCase(Locale.ROOT));
            }

            lblAmount.setText((income ? "+" : expense ? "-" : "") + money.format(movement.getAmount()));
            lblAmount.getStyleClass().removeAll("mov-amount-income", "mov-amount-expense", "mov-amount-transfer");
            lblAmount.getStyleClass().add(income ? "mov-amount-income"
                    : expense ? "mov-amount-expense" : "mov-amount-transfer");

            lblType.setText(movement.getType());
            setGraphic(row);
        }
    }

    private String detailText(Movement movement) {
        StringBuilder text = new StringBuilder(
                categoryNames.getOrDefault(movement.getCategoryId(), "Sin categoría"));

        String origin = accountNames.get(movement.getOriginAccountId());
        if (origin != null) {
            text.append(" · ").append(origin);
            String destination = accountNames.get(movement.getDestinationAccountId());
            if (destination != null) {
                text.append(" → ").append(destination);
            }
        }
        text.append(" · ").append(paymentLabel(movement.getPaymentMethod()));

        if (movement.getTags() != null && movement.getTags().length > 0) {
            text.append("   ").append(String.join(" ", movement.getTags()));
        }
        return text.toString();
    }

    // =====================================================================
    //  Búsqueda y filtros
    // =====================================================================

    private void setupFilters() {
        boxFilters.visibleProperty().bind(btnFilter.selectedProperty());
        boxFilters.managedProperty().bind(btnFilter.selectedProperty());

        cmbFilterCategory.setConverter(converter(Category::getCategoryName));
        cmbFilterCategory.setItems(FXCollections.observableArrayList(ALL_CATEGORIES));
        cmbFilterCategory.setValue(ALL_CATEGORIES);

        cmbFilterType.setConverter(converter(this::capitalize));
        cmbFilterType.setItems(withAllOption(MovementTypes.ALL));
        cmbFilterType.setValue(ALL);

        cmbFilterState.setConverter(converter(this::capitalize));
        cmbFilterState.setItems(withAllOption(MovementStates.ALL));
        cmbFilterState.setValue(ALL);

        // El filtro se aplica al instante sobre los movimientos ya cargados
        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        cmbFilterCategory.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        cmbFilterType.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        cmbFilterState.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        Category category = cmbFilterCategory.getValue();
        String categoryId = (category == null || category == ALL_CATEGORIES) ? null : category.getCategoryId();
        String text = txtSearch.getText() == null || txtSearch.getText().isBlank() ? null : txtSearch.getText();

        MovementFilter filter = new MovementFilter(text, categoryId,
                optionOrNull(cmbFilterType.getValue()), optionOrNull(cmbFilterState.getValue()),
                null, null, null, null);
        filtered.setPredicate(movement -> filter.matches(movement, categoryNames));
    }

    @FXML
    private void clearFilters() {
        txtSearch.clear();
        cmbFilterCategory.setValue(ALL_CATEGORIES);
        cmbFilterType.setValue(ALL);
        cmbFilterState.setValue(ALL);
    }

    // =====================================================================
    //  Carga de datos
    // =====================================================================

    /** Carga categorías, cuentas y movimientos (en ese orden, porque la lista usa los nombres). */
    private void loadAll() {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }

        // Si el usuario es nuevo y no tiene categorías ni cuentas, se crea un conjunto básico
        Task<List<Category>> categoriesTask = categoryService.ensureDefaultCategoriesTask(userId);
        begin();
        categoriesTask.setOnSucceeded(event -> {
            end();
            applyCategories(categoriesTask.getValue());

            Task<List<Account>> accountsTask = accountService.ensureDefaultAccountsTask(userId);
            begin();
            accountsTask.setOnSucceeded(accountsEvent -> {
                end();
                applyAccounts(accountsTask.getValue());
                loadMovements();
            });
            accountsTask.setOnFailed(accountsEvent -> {
                end();
                showError(accountsTask.getException(), false);
            });
            run(accountsTask);
        });
        categoriesTask.setOnFailed(event -> {
            end();
            showError(categoriesTask.getException(), false);
        });
        run(categoriesTask);
    }

    private void loadMovements() {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        Task<List<Movement>> task = movementService.loadMovementsTask(userId);
        begin();
        task.setOnSucceeded(event -> {
            end();
            movements.setAll(task.getValue());
            applyFilters();
            listMovements.refresh();
        });
        task.setOnFailed(event -> {
            end();
            showError(task.getException(), false);
        });
        run(task);
    }

    private void applyCategories(List<Category> list) {
        categories.clear();
        categories.addAll(list);
        categoryNames.clear();
        for (Category category : list) {
            categoryNames.put(category.getCategoryId(), category.getCategoryName());
        }

        cmbCategory.setItems(FXCollections.observableArrayList(list));

        ObservableList<Category> filterItems = FXCollections.observableArrayList();
        filterItems.add(ALL_CATEGORIES);
        filterItems.addAll(list);
        cmbFilterCategory.setItems(filterItems);
        cmbFilterCategory.setValue(ALL_CATEGORIES);
    }

    private void applyAccounts(List<Account> list) {
        accountNames.clear();
        for (Account account : list) {
            accountNames.put(account.getAccountId(), account.getAccountName());
        }
        accountItems.setAll(list);
    }

    // =====================================================================
    //  Formulario (crear / modificar)
    // =====================================================================

    private void setupForm() {
        dpDate.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DATE_FORMAT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                try {
                    return text == null || text.isBlank() ? null : LocalDate.parse(text.trim(), DATE_FORMAT);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });

        cmbType.setConverter(converter(this::capitalize));
        cmbType.setItems(FXCollections.observableArrayList(MovementTypes.ALL));

        cmbState.setConverter(converter(this::capitalize));
        cmbState.setItems(FXCollections.observableArrayList(MovementStates.ALL));

        cmbPayment.setConverter(converter(FinancesController::paymentLabel));
        cmbPayment.setItems(FXCollections.observableArrayList(PaymentMethod.values()));

        cmbCategory.setConverter(converter(Category::getCategoryName));
        cmbAccount.setConverter(converter(Account::getAccountName));
        cmbDestination.setConverter(converter(Account::getAccountName));
        cmbAccount.setItems(accountItems);
        cmbDestination.setItems(accountItems);

        // La cuenta destino solo aplica a las transferencias
        BooleanBinding transfer = Bindings.createBooleanBinding(
                () -> MovementTypes.TRANSFER.equals(cmbType.getValue()), cmbType.valueProperty());
        boxDestination.visibleProperty().bind(transfer);
        boxDestination.managedProperty().bind(transfer);
        lblAccount.textProperty().bind(Bindings.when(transfer).then("Cuenta origen").otherwise("Cuenta"));

        // Esc cierra el formulario
        overlay.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelForm();
                event.consume();
            }
        });
    }

    @FXML
    private void newMovement() {
        openForm(null);
    }

    /** Abre el formulario vacío (movement == null) o con los datos del movimiento a modificar. */
    private void openForm(Movement movement) {
        editing = movement;
        lblFormTitle.setText(movement == null ? "Nuevo movimiento" : "Modificar movimiento");
        lblFormError.setText("");

        if (movement == null) {
            dpDate.setValue(LocalDate.now());
            cmbType.setValue(MovementTypes.EXPENSE);
            txtAmount.clear();
            cmbCategory.setValue(null);
            cmbAccount.setValue(null);
            cmbDestination.setValue(null);
            cmbState.setValue(MovementStates.APPLIED);
            cmbPayment.setValue(PaymentMethod.CASH);
            txtConcept.clear();
            txtTags.clear();
        } else {
            dpDate.setValue(MovementStats.dayOf(movement));
            cmbType.setValue(movement.getType());
            txtAmount.setText(BigDecimal.valueOf(movement.getAmount()).stripTrailingZeros().toPlainString());
            cmbCategory.setValue(findCategory(movement.getCategoryId()));
            cmbAccount.setValue(findAccount(movement.getOriginAccountId()));
            cmbDestination.setValue(findAccount(movement.getDestinationAccountId()));
            cmbState.setValue(movement.getState());
            cmbPayment.setValue(movement.getPaymentMethod());
            txtConcept.setText(movement.getConcept());
            txtTags.setText(movement.getTags() == null ? "" : String.join(", ", movement.getTags()));
        }
        showOverlay();
    }

    @FXML
    private void cancelForm() {
        hideOverlay();
    }

    @FXML
    private void saveMovement() {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }

        LocalDate day = dpDate.getValue();
        if (day == null) {
            lblFormError.setText("Elige una fecha válida (dd/mm/aaaa).");
            return;
        }
        Double amount = parseAmount(txtAmount.getText());
        if (amount == null || amount <= 0) {
            lblFormError.setText("Escribe un monto válido mayor que cero, por ejemplo 350.50.");
            return;
        }

        // El resto de validaciones (categoría, cuenta, método de pago...) las hace el servicio
        Movement movement = new Movement();
        if (editing != null) {
            movement.setMovementId(editing.getMovementId());
        }
        movement.setUserId(userId);
        movement.setDate(toDate(day));
        movement.setType(cmbType.getValue());
        movement.setAmount(amount);
        movement.setCategoryId(cmbCategory.getValue() == null ? null : cmbCategory.getValue().getCategoryId());
        movement.setOriginAccountId(cmbAccount.getValue() == null ? null : cmbAccount.getValue().getAccountId());
        movement.setDestinationAccountId(
                cmbDestination.getValue() == null ? null : cmbDestination.getValue().getAccountId());
        movement.setState(cmbState.getValue());
        movement.setPaymentMethod(cmbPayment.getValue());
        movement.setConcept(txtConcept.getText());
        movement.setTags(parseTags(txtTags.getText()));

        boolean creating = (editing == null);
        Task<?> task = creating
                ? movementService.addMovementTask(movement)
                : movementService.updateMovementTask(movement);

        begin();
        lblFormError.setText("");
        task.setOnSucceeded(event -> {
            end();
            hideOverlay();
            setMessage(creating ? "Movimiento registrado." : "Movimiento actualizado.", false);
            loadMovements();
        });
        task.setOnFailed(event -> {
            end();
            showError(task.getException(), true);
        });
        run(task);
    }

    /** Al modificar sin cambiar el día se conserva la hora original; si no, se usa la hora actual. */
    private Date toDate(LocalDate day) {
        LocalTime time = LocalTime.now();
        if (editing != null && editing.getDate() != null) {
            if (day.equals(MovementStats.dayOf(editing))) {
                return editing.getDate();
            }
            time = editing.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        }
        return Date.from(day.atTime(time).atZone(ZoneId.systemDefault()).toInstant());
    }

    // =====================================================================
    //  Eliminar
    // =====================================================================

    private void confirmDelete(Movement movement) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el movimiento \"" + movement.getConcept() + "\" por "
                        + money.format(movement.getAmount()) + "? Esta acción no se puede deshacer.",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Eliminar movimiento");
        alert.setHeaderText(null);
        alert.initOwner(listMovements.getScene().getWindow());
        alert.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .ifPresent(button -> deleteMovement(movement));
    }

    private void deleteMovement(Movement movement) {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        Task<Void> task = movementService.deleteMovementTask(userId, movement.getMovementId());
        begin();
        task.setOnSucceeded(event -> {
            end();
            setMessage("Movimiento eliminado.", false);
            loadMovements();
        });
        task.setOnFailed(event -> {
            end();
            showError(task.getException(), false);
        });
        run(task);
    }

    // =====================================================================
    //  Formulario: desenfoque y animación
    // =====================================================================

    private void showOverlay() {
        stopAnimation();
        overlay.setVisible(true);
        overlay.setManaged(true);
        overlay.setOpacity(0);

        GaussianBlur blur = new GaussianBlur(0);
        mainLayer.setEffect(blur);

        FadeTransition fade = new FadeTransition(Duration.millis(160), overlay);
        fade.setFromValue(0);
        fade.setToValue(1);
        Timeline blurIn = new Timeline(
                new KeyFrame(Duration.millis(180), new KeyValue(blur.radiusProperty(), 12)));

        animation = new ParallelTransition(fade, blurIn);
        animation.play();

        btnSaveForm.setDefaultButton(true); // Enter guarda mientras el formulario está abierto
        Platform.runLater(() -> txtConcept.requestFocus());
    }

    private void hideOverlay() {
        stopAnimation();
        btnSaveForm.setDefaultButton(false);

        FadeTransition fade = new FadeTransition(Duration.millis(140), overlay);
        fade.setToValue(0);
        ParallelTransition out = new ParallelTransition(fade);
        if (mainLayer.getEffect() instanceof GaussianBlur blur) {
            out.getChildren().add(new Timeline(
                    new KeyFrame(Duration.millis(140), new KeyValue(blur.radiusProperty(), 0))));
        }
        out.setOnFinished(event -> {
            overlay.setVisible(false);
            overlay.setManaged(false);
            mainLayer.setEffect(null);
        });
        animation = out;
        out.play();
    }

    private void stopAnimation() {
        if (animation != null) {
            animation.stop();
        }
    }

    // =====================================================================
    //  Utilidades
    // =====================================================================

    private Category findCategory(String id) {
        for (Category category : categories) {
            if (category.getCategoryId().equals(id)) {
                return category;
            }
        }
        return null;
    }

    private Account findAccount(String id) {
        for (Account account : accountItems) {
            if (account.getAccountId().equals(id)) {
                return account;
            }
        }
        return null;
    }

    /** Acepta formatos como "350", "1,250.50" o "$350". Devuelve null si no es un número. */
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

    /** Separa las etiquetas por comas, espacios o punto y coma. El servicio agrega el "#" y las normaliza. */
    private String[] parseTags(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        List<String> tags = new ArrayList<>();
        for (String part : text.trim().split("[,;\\s]+")) {
            if (!part.isBlank()) {
                tags.add(part);
            }
        }
        return tags.toArray(new String[0]);
    }

    private static String paymentLabel(PaymentMethod method) {
        if (method == null) {
            return "Sin método de pago";
        }
        return switch (method) {
            case CASH -> "Efectivo";
            case CREDIT_CARD -> "Tarjeta de crédito";
            case DEBIT_CARD -> "Tarjeta de débito";
            case BANK_TRANSFER -> "Transferencia";
            case CHECK -> "Cheque";
            case SPEI -> "SPEI";
        };
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private <T> StringConverter<T> converter(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : toText.apply(value);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        };
    }

    private ObservableList<String> withAllOption(List<String> values) {
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add(ALL);
        items.addAll(values);
        return items;
    }

    private String optionOrNull(String value) {
        return value == null || ALL.equals(value) ? null : value;
    }

    private void setMessage(String text, boolean error) {
        lblMessage.getStyleClass().removeAll(MSG_OK, MSG_ERROR);
        if (text != null && !text.isEmpty()) {
            lblMessage.getStyleClass().add(error ? MSG_ERROR : MSG_OK);
        }
        lblMessage.setText(text == null ? "" : text);
    }

    private void showError(Throwable error, boolean inForm) {
        String message;
        if (error instanceof IllegalArgumentException) {
            message = error.getMessage();
        } else {
            error.printStackTrace();
            Throwable cause = error;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            message = "No se pudo completar la operación: " + cause.getMessage();
        }
        if (inForm) {
            lblFormError.setText(message);
        } else {
            setMessage(message, true);
        }
    }

    private String currentUserId() {
        try {
            return ActualSession.getInstance().getUserId();
        } catch (IllegalStateException e) {
            setMessage("No hay una sesión iniciada.", true);
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
