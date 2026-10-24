package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.Category;

import java.util.List;

public interface CategoryDAO extends BaseDAO<Category> {

    List<Category> findByUser(String userId);
}
