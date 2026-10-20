package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.Category;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class categoryDAOImpl implements categoryDAO{
    @Override
    public void save(Category category) {
        // Implementation for adding a category
    }

    @Override
    public Optional<Category> findById(int id) throws SQLException {
        return Optional.empty();
    }

    @Override
    public List<Category> findAll() throws SQLException {
        return List.of();
    }

    @Override
    public void update(Category category) {
        // Implementation for updating a category
    }

    @Override
    public void delete(int id) {
        // Implementation for deleting a category
    }


}
