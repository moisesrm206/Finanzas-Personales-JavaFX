package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.dao.BudgetDAO;
import org.example.gestionfinanciera.dao.CategoryDAO;
import org.example.gestionfinanciera.dao.MovementDAO;
import org.example.gestionfinanciera.models.Budget;
import org.example.gestionfinanciera.models.Category;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.example.gestionfinanciera.services.TaskUtil.task;

public class CategoryServiceImpl implements CategoryService {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Comida", "Transporte", "Servicios", "Entretenimiento", "Salud",
            "Educación", "Ropa", "Otros", "Sueldo");

    private final CategoryDAO categoryDAO;
    private final MovementDAO movementDAO;
    private final BudgetDAO budgetDAO;

    public CategoryServiceImpl(CategoryDAO categoryDAO, MovementDAO movementDAO, BudgetDAO budgetDAO) {
        this.categoryDAO = categoryDAO;
        this.movementDAO = movementDAO;
        this.budgetDAO = budgetDAO;
    }

    @Override
    public Task<List<Category>> loadCategoriesTask(String userId) {
        return task(() -> sortedByName(categoryDAO.findByUser(requireUser(userId))));
    }

    @Override
    public Task<List<Category>> ensureDefaultCategoriesTask(String userId) {
        return task(() -> {
            List<Category> existing = categoryDAO.findByUser(requireUser(userId));
            if (!existing.isEmpty()) {
                return sortedByName(existing);
            }
            List<Category> defaults = new ArrayList<>();
            for (String name : DEFAULT_CATEGORIES) {
                defaults.add(new Category(null, name, userId));
            }
            categoryDAO.createAll(defaults);
            return sortedByName(defaults);
        });
    }

    @Override
    public Task<Category> addCategoryTask(String userId, String name) {
        return task(() -> {
            String cleanName = validName(name);
            requireUser(userId);
            ensureNotDuplicated(userId, cleanName, null);
            return categoryDAO.create(new Category(null, cleanName, userId));
        });
    }

    @Override
    public Task<Category> renameCategoryTask(String userId, String categoryId, String newName) {
        return task(() -> {
            String cleanName = validName(newName);
            Category category = ownedCategory(userId, categoryId);
            ensureNotDuplicated(userId, cleanName, categoryId);
            category.setCategoryName(cleanName);
            categoryDAO.update(category);
            return category;
        });
    }

    @Override
    public Task<Void> deleteCategoryTask(String userId, String categoryId) {
        return task(() -> {
            ownedCategory(userId, categoryId);
            if (movementDAO.existsByCategory(userId, categoryId)) {
                throw new IllegalArgumentException(
                        "No se puede eliminar: la categoría tiene movimientos registrados.");
            }
            List<String> budgetIds = budgetDAO.findByUser(userId).stream()
                    .filter(budget -> categoryId.equals(budget.getCategoryId()))
                    .map(Budget::getBudgetId)
                    .toList();
            budgetDAO.deleteAll(budgetIds);
            categoryDAO.delete(categoryId);
            return null;
        });
    }

    private Category ownedCategory(String userId, String categoryId) {
        Category category = categoryDAO.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe."));
        if (userId == null || !userId.equals(category.getUserId())) {
            throw new IllegalArgumentException("La categoría no pertenece al usuario.");
        }
        return category;
    }

    private void ensureNotDuplicated(String userId, String name, String ignoredCategoryId) {
        boolean duplicated = categoryDAO.findByUser(userId).stream()
                .filter(category -> !category.getCategoryId().equals(ignoredCategoryId))
                .anyMatch(category -> name.equalsIgnoreCase(category.getCategoryName()));
        if (duplicated) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
        }
    }

    private String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio.");
        }
        String clean = name.trim();
        if (clean.length() > 40) {
            throw new IllegalArgumentException("El nombre de la categoría es demasiado largo (máximo 40).");
        }
        return clean;
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("No hay un usuario con sesión iniciada.");
        }
        return userId;
    }

    private List<Category> sortedByName(List<Category> categories) {
        List<Category> sorted = new ArrayList<>(categories);
        sorted.sort(Comparator.comparing(Category::getCategoryName, String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }
}
