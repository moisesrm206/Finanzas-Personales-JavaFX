package org.example.gestionfinanciera.dao;

import org.example.gestionfinanciera.models.Budget;

public interface budgetDAO {
    Budget getBudgetById(int id);
    void saveBudget(Budget budget);
    void updateBudget(Budget budget);
    void deleteBudget(Budget budget);
}
