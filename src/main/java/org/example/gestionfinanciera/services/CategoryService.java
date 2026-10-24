package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.models.Category;

import java.util.List;

public interface CategoryService {

    Task<List<Category>> loadCategoriesTask(String userId);

    /** Si el usuario no tiene categorías, crea un conjunto básico (útil para usuarios nuevos). */
    Task<List<Category>> ensureDefaultCategoriesTask(String userId);

    Task<Category> addCategoryTask(String userId, String name);

    Task<Category> renameCategoryTask(String userId, String categoryId, String newName);

    /** No permite eliminar una categoría con movimientos; sus presupuestos se eliminan con ella. */
    Task<Void> deleteCategoryTask(String userId, String categoryId);
}
