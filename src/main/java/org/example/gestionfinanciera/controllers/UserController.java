package org.example.gestionfinanciera.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.io.IOException;
import org.example.gestionfinanciera.dao.userDAOImpl;
import org.example.gestionfinanciera.models.User;
import org.example.gestionfinanciera.services.ActualSession;
import org.example.gestionfinanciera.services.userService;
import org.example.gestionfinanciera.services.userServiceImpl;

public class UserController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblAvailability;
    @FXML private Label lblMessage;
    @FXML private Button btnSave;
    @FXML private Button btnDelete;

    private final userService userService = new userServiceImpl(new userDAOImpl());

    @FXML
    private void initialize() {
        User user = ActualSession.getInstance().getUser();
        if (user != null) {
            txtUsername.setText(user.getName());
        }
        txtUsername.textProperty().addListener((obs, oldValue, newValue) -> checkAvailability());
    }

    @FXML
    private void checkAvailability() {
        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        if (username.isBlank()) {
            lblAvailability.setText("");
            return;
        }
        Task<Boolean> task = userService.usernameAvailableTask(username, ActualSession.getInstance().getUserId());
        task.setOnSucceeded(event -> lblAvailability.setText(
                task.getValue() ? "Nombre disponible." : "Ese nombre ya existe."));
        task.setOnFailed(event -> lblAvailability.setText("No se pudo comprobar el nombre."));
        run(task);
    }

    @FXML
    private void saveProfile() {
        String current = txtCurrentPassword.getText();
        String next = txtNewPassword.getText();
        if (!next.equals(txtConfirmPassword.getText())) {
            showMessage("Las nuevas contraseñas no coinciden.", true);
            return;
        }
        setBusy(true);
        Task<User> task = userService.updateProfileTask(
                ActualSession.getInstance().getUserId(), txtUsername.getText(), current, next);
        task.setOnSucceeded(event -> {
            setBusy(false);
            ActualSession.getInstance().iniciar(task.getValue());
            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
            showMessage("Datos de usuario actualizados.", false);
        });
        task.setOnFailed(event -> {
            setBusy(false);
            showMessage(message(task.getException()), true);
        });
        run(task);
    }

    @FXML
    private void deleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Se eliminarán tu usuario, movimientos, cuentas, categorías y presupuestos. Esta acción no se puede deshacer.",
                ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Eliminar cuenta");
        alert.setHeaderText("¿Eliminar permanentemente tu cuenta?");
        alert.showAndWait().filter(button -> button == ButtonType.OK).ifPresent(button -> {
            setBusy(true);
            Task<Void> task = userService.deleteAccountDataTask(ActualSession.getInstance().getUserId());
            task.setOnSucceeded(event -> {
                setBusy(false);
                ActualSession.getInstance().cerrar();
                try {
                    Parent view = FXMLLoader.load(getClass().getResource(
                            "/org/example/gestionfinanciera/auth-view.fxml"));
                    btnDelete.getScene().setRoot(view);
                } catch (IOException e) {
                    showMessage("La cuenta fue eliminada, pero no se pudo abrir el inicio de sesión.", true);
                }
            });
            task.setOnFailed(event -> {
                setBusy(false);
                showMessage(message(task.getException()), true);
            });
            run(task);
        });
    }

    private void setBusy(boolean busy) {
        btnSave.setDisable(busy);
        btnDelete.setDisable(busy);
    }

    private void showMessage(String message, boolean error) {
        lblMessage.setText(message);
        lblMessage.getStyleClass().removeAll("profile-ok", "profile-error");
        lblMessage.getStyleClass().add(error ? "profile-error" : "profile-ok");
    }

    private String message(Throwable error) {
        Throwable cause = error;
        while (cause != null && cause.getCause() != null) cause = cause.getCause();
        return cause == null || cause.getMessage() == null ? "No se pudo completar la operación." : cause.getMessage();
    }

    private void run(Task<?> task) {
        Thread thread = new Thread(task, "user-operation");
        thread.setDaemon(true);
        thread.start();
    }
}
