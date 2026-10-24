package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.models.User;

import java.util.List;

public interface userService {
    Task<Void> addUserTask(User user);
    Task<List<User>> loadUsersTask();
    Task<User> authenticateTask(String username, String password);
    Task<User> registerTask(String username, String password);
}