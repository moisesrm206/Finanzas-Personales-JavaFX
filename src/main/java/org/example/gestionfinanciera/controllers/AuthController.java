package org.example.gestionfinanciera.controllers;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class AuthController {

    private static final String SUBTITULO_LOGIN =
            "Inicia sesión para revisar tus ahorros, gastos y objetivos financieros en un solo lugar.";
    private static final String SUBTITULO_REGISTRO =
            "Crea tu cuenta para empezar a organizar tus finanzas de manera inteligente y segura.";

    // Encabezado
    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;

    // Contenedores y botones principales
    @FXML private VBox boxLogin;
    @FXML private VBox boxRegistro;
    @FXML private Button btnLogin;
    @FXML private Button btnRegistrar;

    // Login
    @FXML private TextField txtUsuarioLogin;
    @FXML private PasswordField pfLogin;
    @FXML private TextField tfLogin;              // muestra la contraseña en texto visible
    @FXML private ToggleButton tglLogin;
    @FXML private Label lblErrorLogin;

    // Registro
    @FXML private TextField txtUsuarioRegistro;
    @FXML private PasswordField pfRegistro;
    @FXML private TextField tfRegistro;
    @FXML private PasswordField pfConfirmar;
    @FXML private TextField tfConfirmar;
    @FXML private ToggleButton tglRegistro;
    @FXML private Label lblErrorRegistro;

    @FXML private FontIcon icoLogin, icoRegistro;

    @FXML
    public void initialize() {
        // Mostrar / ocultar contraseña
        vincularPassword(pfLogin, tfLogin, tglLogin);
        vincularPassword(pfRegistro, tfRegistro, tglRegistro);
        vincularPassword(pfConfirmar, tfConfirmar, tglRegistro); // mismo botón para ambos campos
        vincularIcono(icoLogin, tglLogin);
        vincularIcono(icoRegistro, tglRegistro);

        // El mensaje de error desaparece cuando el usuario vuelve a escribir
        limpiarErrorAlEscribir(lblErrorLogin, txtUsuarioLogin, pfLogin, tfLogin);
        limpiarErrorAlEscribir(lblErrorRegistro, txtUsuarioRegistro, pfRegistro, tfRegistro, pfConfirmar, tfConfirmar);
    }

    // Alternar formularios

    @FXML
    private void mostrarRegistro() {
        cambiarModo(false);
    }

    @FXML
    private void mostrarLogin() {
        cambiarModo(true);
    }

    private void cambiarModo(boolean modoLogin) {
        boxLogin.setVisible(modoLogin);
        boxLogin.setManaged(modoLogin);
        boxRegistro.setVisible(!modoLogin);
        boxRegistro.setManaged(!modoLogin);

        // Solo el botón del formulario visible debe responder a la tecla Enter, para evitar que se envíen ambos formularios al mismo tiempo
        btnLogin.setDefaultButton(modoLogin);
        btnRegistrar.setDefaultButton(!modoLogin);

        lblTitulo.setText(modoLogin ? "Gestiona tu futuro" : "Comienza tu camino");
        lblSubtitulo.setText(modoLogin ? SUBTITULO_LOGIN : SUBTITULO_REGISTRO);

        limpiarCampos();
    }

    private void limpiarCampos() {
        txtUsuarioLogin.clear();
        pfLogin.clear();
        txtUsuarioRegistro.clear();
        pfRegistro.clear();
        pfConfirmar.clear();
        tglLogin.setSelected(false);
        tglRegistro.setSelected(false);
        lblErrorLogin.setText("");
        lblErrorRegistro.setText("");
    }

    // Acciones de los botones

    @FXML
    private void iniciarSesion() {
        String usuario = txtUsuarioLogin.getText().trim();
        String password = pfLogin.getText();

        if (usuario.isEmpty() || password.isEmpty()) {
            lblErrorLogin.setText("Escribe tu usuario y tu contraseña.");
            return;
        }

        // TODO: llamar al servicio de autenticación dentro de un Task
        // (si falla: lblErrorLogin.setText("Usuario o contraseña incorrectos."))
    }

    @FXML
    private void registrar() {
        String usuario = txtUsuarioRegistro.getText().trim();
        String password = pfRegistro.getText();
        String confirmar = pfConfirmar.getText();

        if (usuario.isEmpty() || password.isEmpty() || confirmar.isEmpty()) {
            lblErrorRegistro.setText("Completa todos los campos.");
            return;
        }
        if (password.length() < 6) {
            lblErrorRegistro.setText("La contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!password.equals(confirmar)) {
            lblErrorRegistro.setText("Las contraseñas no coinciden.");
            return;
        }

        // TODO: verificar que el usuario no exista y guardarlo con hash, dentro de un Task
        // (si ya existe: lblErrorRegistro.setText("Ese nombre de usuario ya está en uso."))
    }

    // Utilidades

    /** Alterna entre el PasswordField y un TextField con el mismo texto. */
    private void vincularPassword(PasswordField pf, TextField tf, ToggleButton toggle) {
        tf.textProperty().bindBidirectional(pf.textProperty());

        tf.visibleProperty().bind(toggle.selectedProperty());
        tf.managedProperty().bind(toggle.selectedProperty());

        pf.visibleProperty().bind(toggle.selectedProperty().not());
        pf.managedProperty().bind(toggle.selectedProperty().not());
    }

    private void vincularIcono(FontIcon icono, ToggleButton toggle) {
        toggle.selectedProperty().addListener((obs, anterior, seleccionado) ->
                icono.setIconLiteral(seleccionado ? "fas-eye-slash" : "fas-eye"));
    }

    private void limpiarErrorAlEscribir(Label error, TextField... campos) {
        for (TextField campo : campos) {
            campo.textProperty().addListener((obs, anterior, nuevo) -> error.setText(""));
        }
    }
}
