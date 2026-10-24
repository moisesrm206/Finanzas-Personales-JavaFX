package org.example.gestionfinanciera.services;

import org.example.gestionfinanciera.models.Budget;

/** Cuánto se ha gastado de un presupuesto y qué nivel de alerta le corresponde. */
public record BudgetStatus(Budget budget, String categoryName, double spent,
                           double percentage, Level level) {

    public enum Level {
        OK,        // menos del 80 %
        WARNING,   // 80 % o más: cerca de rebasar el límite
        EXCEEDED   // 100 % o más
    }
}
