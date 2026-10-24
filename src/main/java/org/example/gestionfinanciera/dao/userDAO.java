package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.User;

import java.util.List;
import java.util.Optional;

public interface userDAO {
    User getUserById(String id);
    Optional<User> getUserByName(String name);
    List<User> getUsers();
    void saveUser(User user);
    void updateUser(User user);
    void deleteUser(User user);
}
