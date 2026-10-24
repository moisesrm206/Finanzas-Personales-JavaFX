package org.example.gestionfinanciera.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.gestionfinanciera.dao.userDAOImpl;
import org.example.gestionfinanciera.models.User;
import org.example.gestionfinanciera.services.ActualSession;
import org.example.gestionfinanciera.services.userService;
import org.example.gestionfinanciera.services.userServiceImpl;
import org.kordamp.ikonli.javafx.FontIcon;

public class AuthController {

    private final userService userService = new userServiceImpl(new userDAOImpl());

    private static final String SUBTITULO_LOGIN =
            "Inicia sesión para revisar tus ahorros, gastos y objetivos financieros en un solo lugar.";
    private static final String SUBTITULO_REGISTRO =
            "Crea tu cuenta para empezar a organizar tus finanzas de manera inteligente y segura.";

    // Encabezado
    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;

    // Contenedores y botones principales
    @FXML private VBox boxLogin;
    @FXML private VBox boxRegister;
    @FXML private Button btnLogin;
    @FXML private Button btnRegister;

    // Login
    @FXML private TextField txtUserLogin;
    @FXML private PasswordField pfLogin;
    @FXML private TextField tfLogin;              // muestra la contraseña en texto visible
    @FXML private ToggleButton tglLogin;
    @FXML private Label lblErrorLogin;

    // Registro
    @FXML private TextField txtUserRegister;
    @FXML private PasswordField pfRegister;
    @FXML private TextField tfRegister;
    @FXML private PasswordField pfConfirm;
    @FXML private TextField tfConfirm;
    @FXML private ToggleButton tglRegister;
    @FXML private Label lblErrorRegister;

    @FXML private FontIcon icoLogin, icoRegistro;

    @FXML
    public void initialize() {
        // Mostrar / ocultar contraseña
        bindPassword(pfLogin, tfLogin, tglLogin);
        bindPassword(pfRegister, tfRegister, tglRegister);
        bindPassword(pfConfirm, tfConfirm, tglRegister); // mismo botón para ambos campos
        bindIcon(icoLogin, tglLogin);
        bindIcon(icoRegistro, tglRegister);

        // El mensaje de error desaparece cuando el usuario vuelve a escribir
        clearWritingErr(lblErrorLogin, txtUserLogin, pfLogin, tfLogin);
        clearWritingErr(lblErrorRegister, txtUserRegister, pfRegister, tfRegister, pfConfirm, tfConfirm);
    }

    // Alternar formularios

    @FXML
    private void showRegister() {
        switchMode(false);
    }

    @FXML
    private void showLogin() {
        switchMode(true);
    }

    private void switchMode(boolean loginMode) {
        boxLogin.setVisible(loginMode);
        boxLogin.setManaged(loginMode);
        boxRegister.setVisible(!loginMode);
        boxRegister.setManaged(!loginMode);

        // Solo el botón del formulario visible debe responder a la tecla Enter, para evitar que se envíen ambos formularios al mismo tiempo
        btnLogin.setDefaultButton(loginMode);
        btnRegister.setDefaultButton(!loginMode);

        lblTitulo.setText(loginMode ? "Gestiona tu futuro" : "Comienza tu camino");
        lblSubtitulo.setText(loginMode ? SUBTITULO_LOGIN : SUBTITULO_REGISTRO);

        cleanPlaces();
    }

    private void cleanPlaces() {
        txtUserLogin.clear();
        pfLogin.clear();
        txtUserRegister.clear();
        pfRegister.clear();
        pfConfirm.clear();
        tglLogin.setSelected(false);
        tglRegister.setSelected(false);
        lblErrorLogin.setText("");
        lblErrorRegister.setText("");
    }

    // Acciones de los botones

    @FXML
    private void signIn() {
        String username = txtUserLogin.getText().trim();
        String password = pfLogin.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblErrorLogin.setText("Escribe tu usuario y tu contraseña.");
            return;
        }

        setBusy(true);
        var task = userService.authenticateTask(username, password);
        task.setOnSucceeded(event -> {
            setBusy(false);
            User user = task.getValue();
            user.setPassword(null);
            ActualSession.getInstance().iniciar(user);
            openMainView(task.getValue());
        });
        task.setOnFailed(event -> {
            setBusy(false);
            lblErrorLogin.setText(messageFor(task.getException(), "Usuario o contraseña incorrectos."));
        });
        start(task);
    }

    @FXML
    private void logIn() {
        String user = txtUserRegister.getText().trim();
        String password = pfRegister.getText();
        String confirm = pfConfirm.getText();

        if (user.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            lblErrorRegister.setText("Completa todos los campos.");
            return;
        }
        if (password.length() < 6) {
            lblErrorRegister.setText("La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!password.equals(confirm)) {
            lblErrorRegister.setText("Las contraseñas no coinciden.");
            return;
        }

        setBusy(true);
        var task = userService.registerTask(user, password);
        task.setOnSucceeded(event -> {
            setBusy(false);
            switchMode(true);
            txtUserLogin.setText(user);
            lblErrorLogin.setText("Cuenta creada. Ya puedes iniciar sesión.");
        });
        task.setOnFailed(event -> {
            setBusy(false);
            lblErrorRegister.setText(messageFor(task.getException(), "No se pudo crear la cuenta."));
        });
        start(task);
    }

    // Utilidades

    /** Alterna entre el PasswordField y un TextField con el mismo texto. */
    private void bindPassword(PasswordField pf, TextField tf, ToggleButton toggle) {
        tf.textProperty().bindBidirectional(pf.textProperty());

        tf.visibleProperty().bind(toggle.selectedProperty());
        tf.managedProperty().bind(toggle.selectedProperty());

        pf.visibleProperty().bind(toggle.selectedProperty().not());
        pf.managedProperty().bind(toggle.selectedProperty().not());
    }

    private void bindIcon(FontIcon icon, ToggleButton toggle) {
        toggle.selectedProperty().addListener((obs, previous, selected) ->
                icon.setIconLiteral(selected ? "fas-eye-slash" : "fas-eye"));
    }

    private void clearWritingErr(Label error, TextField... fields) {
        for (TextField field : fields) {
            field.textProperty().addListener((obs, previous, recent) -> error.setText(""));
        }
    }

    private void start(javafx.concurrent.Task<?> task) {
        Thread thread = new Thread(task, "auth-operation");
        thread.setDaemon(true);
        thread.start();
    }

    private void setBusy(boolean busy) {
        btnLogin.setDisable(busy);
        btnRegister.setDisable(busy);
    }

    private String messageFor(Throwable error, String fallback) {
        return error == null || error.getMessage() == null ? fallback : error.getMessage();
    }

    private void openMainView(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/org/example/gestionfinanciera/templates/base-view.fxml"));
            Parent view = loader.load();
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.getScene().setRoot(view);
            stage.setTitle("Gestión de Finanzas Personales - " + user.getName());
        } catch (java.io.IOException e) {
            lblErrorLogin.setText("No se pudo abrir la aplicación.");
        }
    }
}
