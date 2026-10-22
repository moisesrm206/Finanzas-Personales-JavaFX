package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.models.User;
import org.example.gestionfinanciera.dao.userDAO;

import java.util.List;

public interface userService {
    private final userDAO dao;

    public userService(userDAO dao) {
        this.dao = dao;
    }
    public Task<Void> addUserTask(User user){
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Lógica para agregar el usuario a la base de datos
                return null;
            }
        };
    }
}
