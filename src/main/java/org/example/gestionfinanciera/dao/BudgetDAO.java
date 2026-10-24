package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.Budget;

import java.util.List;
import java.util.Optional;

public interface BudgetDAO extends BaseDAO<Budget> {

    List<Budget> findByUser(String userId);

    /** Presupuestos de un mes (1 a 12). */
    List<Budget> findByUserAndMonth(String userId, int month);

    /** El presupuesto de una categoría en un mes, si existe (para no crear dos). */
    Optional<Budget> findByUserCategoryAndMonth(String userId, String categoryId, int month);
}
