package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.Category;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface categoryDAO {
    void save(Category category) throws SQLException;

    Optional <Category> findById(int id) throws SQLException;

    List<Category> findAll() throws SQLException;

    void update(Category category) throws SQLException;

    void delete(int id) throws SQLException;
}
