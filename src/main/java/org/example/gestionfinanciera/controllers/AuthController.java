package org.example.gestionfinanciera.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import org.example.gestionfinanciera.dao.userDAOImpl;
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
        String user = txtUserLogin.getText().trim();
        String password = pfLogin.getText();

        if (user.isEmpty() || password.isEmpty()) {
            lblErrorLogin.setText("Escribe tu user y tu contraseña.");
            return;
        }

        // TODO: llamar al servicio de autenticación dentro de un Task
        // (si falla: lblErrorLogin.setText("Usuario o contraseña incorrectos."))

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

        // TODO: verificar que el user no exista y guardarlo con hash, dentro de un Task
        // (si ya existe: lblErrorRegistro.setText("Ese nombre de user ya está en uso."))

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
}
