package org.example.gestionfinanciera.controllers;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.example.gestionfinanciera.dao.AccountDAOImpl;
import org.example.gestionfinanciera.dao.BudgetDAOImpl;
import org.example.gestionfinanciera.dao.CategoryDAOImpl;
import org.example.gestionfinanciera.dao.MovementDAOImpl;
import org.example.gestionfinanciera.export.ExportFactory;
import org.example.gestionfinanciera.models.Account;
import org.example.gestionfinanciera.models.Category;
import org.example.gestionfinanciera.models.Movement;
import org.example.gestionfinanciera.models.User;
import org.example.gestionfinanciera.services.AccountService;
import org.example.gestionfinanciera.services.AccountServiceImpl;
import org.example.gestionfinanciera.services.CategoryService;
import org.example.gestionfinanciera.services.CategoryServiceImpl;
import org.example.gestionfinanciera.services.ExportService;
import org.example.gestionfinanciera.services.MovementFilter;
import org.example.gestionfinanciera.services.MovementService;
import org.example.gestionfinanciera.services.MovementServiceImpl;
import org.example.gestionfinanciera.services.MovementStates;
import org.example.gestionfinanciera.services.MovementTypes;
import org.example.gestionfinanciera.services.ActualSession;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exporta a PDF o Excel los movimientos del usuario que coinciden con los filtros elegidos.
 * Los movimientos se consultan en Firebase (MovementService) dentro de un Task.
 */
public class ExportController {

    private static final String ALL = "Todos";
    /** Opción "sin filtro" del ComboBox de categorías (no existe en la base de datos). */
    private static final Category ALL_CATEGORIES = new Category(null, "Todas las categorías", null);

    private static final String STATUS_OK = "export-status-ok";
    private static final String STATUS_ERROR = "export-status-error";
    private static final String STATUS_INFO = "export-status-info";

    @FXML private ComboBox<Category> cmbCategory;
    @FXML private ComboBox<String> cmbType;
    @FXML private ComboBox<String> cmbState;
    @FXML private TextField txtTag;
    @FXML private Button btnExcel;
    @FXML private Button btnPdf;
    @FXML private Label lblStatus;
    @FXML private ProgressIndicator piLoading;

    private final MovementService movementService =
            new MovementServiceImpl(new MovementDAOImpl(), new CategoryDAOImpl());
    private final CategoryService categoryService =
            new CategoryServiceImpl(new CategoryDAOImpl(), new MovementDAOImpl(), new BudgetDAOImpl());
    private final AccountService accountService =
            new AccountServiceImpl(new AccountDAOImpl(), new MovementDAOImpl());
    private final ExportService exportService = new ExportService();

    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    // id -> nombre, para que el archivo muestre nombres y no los ids de Firestore
    private final Map<String, String> categoryNames = new HashMap<>();
    private final Map<String, String> accountNames = new HashMap<>();

    @FXML
    private void initialize() {
        // El mensaje de estado solo ocupa espacio cuando tiene texto
        lblStatus.visibleProperty().bind(lblStatus.textProperty().isNotEmpty());
        lblStatus.managedProperty().bind(lblStatus.visibleProperty());

        btnExcel.disableProperty().bind(busy);
        btnPdf.disableProperty().bind(busy);
        piLoading.visibleProperty().bind(busy);

        cmbCategory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.getCategoryName();
            }

            @Override
            public Category fromString(String text) {
                return null;
            }
        });
        cmbCategory.setItems(FXCollections.observableArrayList(ALL_CATEGORIES));
        cmbCategory.setValue(ALL_CATEGORIES);

        cmbType.setConverter(capitalized());
        cmbType.setItems(withAllOption(MovementTypes.ALL));
        cmbType.setValue(ALL);

        cmbState.setConverter(capitalized());
        cmbState.setItems(withAllOption(MovementStates.ALL));
        cmbState.setValue(ALL);

        loadCategories();
        loadAccounts();
    }

    // ---------- Carga de categorías y cuentas del usuario ----------

    private void loadCategories() {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        Task<List<Category>> task = categoryService.loadCategoriesTask(userId);
        task.setOnSucceeded(event -> {
            List<Category> categories = task.getValue();
            categoryNames.clear();
            for (Category category : categories) {
                categoryNames.put(category.getCategoryId(), category.getCategoryName());
            }
            ObservableList<Category> items = FXCollections.observableArrayList();
            items.add(ALL_CATEGORIES);
            items.addAll(categories);
            cmbCategory.setItems(items);
            cmbCategory.setValue(ALL_CATEGORIES);
        });
        task.setOnFailed(event -> showError(task.getException()));
        run(task);
    }

    private void loadAccounts() {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        Task<List<Account>> task = accountService.loadAccountsTask(userId);
        task.setOnSucceeded(event -> {
            accountNames.clear();
            for (Account account : task.getValue()) {
                accountNames.put(account.getAccountId(), account.getAccountName());
            }
        });
        // Si falla no es grave: el archivo mostrará los ids de las cuentas
        task.setOnFailed(event -> task.getException().printStackTrace());
        run(task);
    }

    // ---------- Exportación ----------

    @FXML
    private void exportToExcel() {
        startExport(ExportFactory.Format.XLSX, "Libro Excel (*.xlsx)", ".xlsx");
    }

    @FXML
    private void exportToPdf() {
        startExport(ExportFactory.Format.PDF, "Documento PDF (*.pdf)", ".pdf");
    }

    /** Paso 1: consultar en Firebase los movimientos que cumplen el filtro. */
    private void startExport(ExportFactory.Format format, String description, String extension) {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }

        Task<List<Movement>> search = movementService.searchMovementsTask(userId, buildFilter());
        busy.set(true);
        setStatus("Consultando movimientos...", STATUS_INFO);

        search.setOnSucceeded(event -> {
            busy.set(false);
            List<Movement> movements = search.getValue();
            if (movements.isEmpty()) {
                setStatus("No hay movimientos que coincidan con los filtros elegidos.", STATUS_ERROR);
                return;
            }
            File target = chooseFile(description, extension);
            if (target == null) {
                setStatus("Exportación cancelada.", STATUS_INFO);
                return;
            }
            writeFile(format, movements, target, extension);
        });
        search.setOnFailed(event -> {
            busy.set(false);
            showError(search.getException());
        });
        run(search);
    }

    /** Paso 2: el usuario elige dónde guardar el archivo. */
    private File chooseFile(String description, String extension) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar movimientos");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, "*" + extension));
        chooser.setInitialFileName("movimientos_" + LocalDate.now() + extension);
        return chooser.showSaveDialog(lblStatus.getScene().getWindow());
    }

    /** Paso 3: generar el archivo en otro hilo y abrirlo al terminar. */
    private void writeFile(ExportFactory.Format format, List<Movement> movements,
                           File target, String extension) {
        String path = target.getAbsolutePath();
        if (!path.toLowerCase().endsWith(extension)) {
            path += extension; // si el usuario no escribió la extensión, se la agregamos
        }
        final String finalPath = path;
        final List<Movement> rows = withReadableNames(movements);
        final int count = movements.size();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                exportService.export(rows, finalPath, format);
                openFile(finalPath);
                return null;
            }
        };

        busy.set(true);
        setStatus("Generando archivo con " + count + " movimientos...", STATUS_INFO);
        task.setOnSucceeded(event -> {
            busy.set(false);
            setStatus("Se exportaron " + count + " movimientos a " + new File(finalPath).getName() + ".",
                    STATUS_OK);
        });
        task.setOnFailed(event -> {
            busy.set(false);
            showError(task.getException());
        });
        run(task);
    }

    /**
     * Los exportadores escriben lo que hay en los campos de categoría, cuentas y usuario.
     * Para que el archivo muestre nombres en lugar de ids de Firestore se exportan COPIAS
     * de los movimientos con el nombre en esos campos; los datos de la base no se modifican.
     */
    private List<Movement> withReadableNames(List<Movement> movements) {
        User user = ActualSession.getInstance().getUser();
        String userName = user == null || user.getName() == null ? "" : user.getName();

        List<Movement> copies = new ArrayList<>();
        for (Movement movement : movements) {
            copies.add(new Movement(
                    movement.getMovementId(),
                    categoryNames.getOrDefault(movement.getCategoryId(), nullToEmpty(movement.getCategoryId())),
                    accountNames.getOrDefault(movement.getOriginAccountId(), nullToEmpty(movement.getOriginAccountId())),
                    accountNames.getOrDefault(movement.getDestinationAccountId(), nullToEmpty(movement.getDestinationAccountId())),
                    userName,
                    movement.getAmount(),
                    movement.getType(),
                    movement.getConcept(),
                    movement.getPaymentMethod(),
                    movement.getDate(),
                    movement.getState(),
                    movement.getTags()));
        }
        return copies;
    }

    private MovementFilter buildFilter() {
        Category category = cmbCategory.getValue();
        String categoryId = (category == null || category == ALL_CATEGORIES) ? null : category.getCategoryId();
        String tag = txtTag.getText() == null || txtTag.getText().isBlank() ? null : txtTag.getText().trim();

        return new MovementFilter(null, categoryId, optionOrNull(cmbType.getValue()),
                optionOrNull(cmbState.getValue()), tag, null, null, null);
    }

    // ---------- Utilidades ----------

    private ObservableList<String> withAllOption(List<String> values) {
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add(ALL);
        items.addAll(values);
        return items;
    }

    private String optionOrNull(String value) {
        return value == null || ALL.equals(value) ? null : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Muestra el valor con la primera letra en mayúscula (el valor guardado no cambia). */
    private StringConverter<String> capitalized() {
        return new StringConverter<>() {
            @Override
            public String toString(String value) {
                if (value == null || value.isEmpty()) {
                    return "";
                }
                return Character.toUpperCase(value.charAt(0)) + value.substring(1);
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        };
    }

    private void setStatus(String text, String styleClass) {
        lblStatus.getStyleClass().removeAll(STATUS_OK, STATUS_ERROR, STATUS_INFO);
        if (text != null && !text.isEmpty()) {
            lblStatus.getStyleClass().add(styleClass);
        }
        lblStatus.setText(text == null ? "" : text);
    }

    private void showError(Throwable error) {
        if (error instanceof IllegalArgumentException) {
            setStatus(error.getMessage(), STATUS_ERROR);
            return;
        }
        error.printStackTrace();
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        setStatus("No se pudo completar la operación: " + cause.getMessage(), STATUS_ERROR);
    }

    private String currentUserId() {
        try {
            return ActualSession.getInstance().getUserId();
        } catch (IllegalStateException e) {
            setStatus("No hay una sesión iniciada.", STATUS_ERROR);
            return null;
        }
    }

    private void openFile(String filename) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(new File(filename));
            } catch (IOException ex) {
                // no hay una aplicación registrada para este tipo de archivo
            }
        }
    }

    private void run(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}