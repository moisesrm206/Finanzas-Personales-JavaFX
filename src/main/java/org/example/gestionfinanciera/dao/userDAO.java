package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.User;

public interface userDAO {
    User getUserById(String id);
    void saveUser(User user);
    void updateUser(User user);
    void deleteUser(User user);
}
