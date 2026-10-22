package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.dao.userDAO;
import org.example.gestionfinanciera.models.User;

public class userServiceImpl implements userService{

    private final userDAO userDAO;

    public userServiceImpl(userDAO userDAO) {
        this.userDAO = userDAO;
    }


    @Override
    public Task<Void> createUser(String username, String password, id_user) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                User user = new User(username, password);
                user.setName(username);
                user.setPassword(password);
                userDAO.saveUser(user);
                return null;
            }
        };
    }
}