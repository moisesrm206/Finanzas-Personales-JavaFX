package org.example.gestionfinanciera.viewModel;

import com.google.firebase.messaging.LightSettings;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.example.gestionfinanciera.models.User;
import org.example.gestionfinanciera.services.userService;

import java.util.List;

public class userViewModel {
    private final userService service;
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private BooleanProperty processingUser;
    public userViewModel(userService userService, ObservableList<User> users) { this.service = userService; }

    public ObservableList<User> getUsers() {
        return users;
    }
    public BooleanProperty processingUserProperty() { return processingUser; }

    private void loadUsers() {
        Task<List<User>> task = new service.createUser() {
            @Override
            protected List<User> call() throws Exception {
                return userService.getUsers();
            }
        };
        task.setOnSucceeded(e -> users.setAll(task.getValue()));
        new Thread(task).start();
    }


}
