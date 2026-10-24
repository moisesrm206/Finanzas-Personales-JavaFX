package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.models.Budget;

import java.time.YearMonth;
import java.util.List;

public interface BudgetService {

    /** Presupuestos de un mes (1 a 12). */
    Task<List<Budget>> loadBudgetsTask(String userId, int month);

    /** Crea el presupuesto de la categoría en ese mes o, si ya existe, actualiza su límite. */
    Task<Budget> saveBudgetTask(String userId, String categoryId, double limit, int month);

    Task<Void> deleteBudgetTask(String userId, String budgetId);

    /**
     * Cuánto se lleva gastado de cada presupuesto del mes y su nivel de alerta
     * (OK, WARNING desde el 80 %, EXCEEDED desde el 100 %). Ordenado del más al menos usado.
     */
    Task<List<BudgetStatus>> loadBudgetStatusTask(String userId, YearMonth month);
}
