package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.dao.BudgetDAO;
import org.example.gestionfinanciera.dao.CategoryDAO;
import org.example.gestionfinanciera.dao.MovementDAO;
import org.example.gestionfinanciera.models.Budget;
import org.example.gestionfinanciera.models.Category;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.example.gestionfinanciera.services.TaskUtil.task;

public class BudgetServiceImpl implements BudgetService {

    /** Porcentaje del límite a partir del cual se avisa que se está cerca de rebasarlo. */
    public static final double WARNING_PERCENT = 80.0;

    private final BudgetDAO budgetDAO;
    private final MovementDAO movementDAO;
    private final CategoryDAO categoryDAO;

    public BudgetServiceImpl(BudgetDAO budgetDAO, MovementDAO movementDAO, CategoryDAO categoryDAO) {
        this.budgetDAO = budgetDAO;
        this.movementDAO = movementDAO;
        this.categoryDAO = categoryDAO;
    }

    @Override
    public Task<List<Budget>> loadBudgetsTask(String userId, int month) {
        return task(() -> budgetDAO.findByUserAndMonth(requireUser(userId), validMonth(month)));
    }

    @Override
    public Task<Budget> saveBudgetTask(String userId, String categoryId, double limit, int month) {
        return task(() -> {
            requireUser(userId);
            validMonth(month);
            if (limit <= 0) {
                throw new IllegalArgumentException("El límite debe ser mayor que cero.");
            }
            Category category = categoryDAO.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("La categoría no existe."));
            if (!userId.equals(category.getUserId())) {
                throw new IllegalArgumentException("La categoría no pertenece al usuario.");
            }

            Optional<Budget> existing = budgetDAO.findByUserCategoryAndMonth(userId, categoryId, month);
            if (existing.isPresent()) {
                Budget budget = existing.get();
                budget.setLimit(limit);
                budgetDAO.update(budget);
                return budget;
            }
            return budgetDAO.create(new Budget(null, userId, categoryId, limit, month));
        });
    }

    @Override
    public Task<Void> deleteBudgetTask(String userId, String budgetId) {
        return task(() -> {
            Budget budget = budgetDAO.findById(budgetId)
                    .orElseThrow(() -> new IllegalArgumentException("El presupuesto no existe."));
            if (userId == null || !userId.equals(budget.getUserId())) {
                throw new IllegalArgumentException("El presupuesto no pertenece al usuario.");
            }
            budgetDAO.delete(budgetId);
            return null;
        });
    }

    @Override
    public Task<List<BudgetStatus>> loadBudgetStatusTask(String userId, YearMonth month) {
        return task(() -> {
            requireUser(userId);
            List<Budget> budgets = budgetDAO.findByUserAndMonth(userId, month.getMonthValue());
            Map<String, Double> spentByCategory =
                    MovementStats.expensesByCategory(movementDAO.findByUser(userId), month);

            Map<String, String> categoryNames = new HashMap<>();
            for (Category category : categoryDAO.findByUser(userId)) {
                categoryNames.put(category.getCategoryId(), category.getCategoryName());
            }

            List<BudgetStatus> result = new ArrayList<>();
            for (Budget budget : budgets) {
                double spent = spentByCategory.getOrDefault(budget.getCategoryId(), 0.0);
                double percentage = budget.getLimit() > 0 ? spent / budget.getLimit() * 100 : 0;
                BudgetStatus.Level level = percentage >= 100 ? BudgetStatus.Level.EXCEEDED
                        : percentage >= WARNING_PERCENT ? BudgetStatus.Level.WARNING
                        : BudgetStatus.Level.OK;
                result.add(new BudgetStatus(budget,
                        categoryNames.getOrDefault(budget.getCategoryId(), "Sin categoría"),
                        spent, percentage, level));
            }
            result.sort(Comparator.comparingDouble(BudgetStatus::percentage).reversed());
            return result;
        });
    }

    private int validMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12.");
        }
        return month;
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("No hay un usuario con sesión iniciada.");
        }
        return userId;
    }
}
