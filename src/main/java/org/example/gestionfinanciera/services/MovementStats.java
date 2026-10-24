package org.example.gestionfinanciera.services;

import org.example.gestionfinanciera.models.Movement;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cálculos sobre listas de movimientos, usados por los presupuestos y por los gráficos. */
public final class MovementStats {

    private MovementStats() {
    }

    public static LocalDate dayOf(Movement movement) {
        if (movement == null || movement.getDate() == null) {
            return null;
        }
        return movement.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static YearMonth monthOf(Movement movement) {
        LocalDate day = dayOf(movement);
        return day == null ? null : YearMonth.from(day);
    }

    /** categoryId -> total gastado en el mes (solo movimientos de tipo Egreso). */
    public static Map<String, Double> expensesByCategory(List<Movement> movements, YearMonth month) {
        Map<String, Double> totals = new HashMap<>();
        for (Movement movement : movements) {
            if (MovementTypes.EXPENSE.equalsIgnoreCase(movement.getType())
                    && month.equals(monthOf(movement))) {
                totals.merge(movement.getCategoryId(), movement.getAmount(), Double::sum);
            }
        }
        return totals;
    }

    /** Ingresos y gastos de cada mes del año. Las transferencias no cuentan como ingreso ni gasto. */
    public static MonthlyTotals monthlyTotals(List<Movement> movements, int year) {
        double[] income = new double[12];
        double[] expenses = new double[12];
        for (Movement movement : movements) {
            YearMonth month = monthOf(movement);
            if (month == null || month.getYear() != year) {
                continue;
            }
            int index = month.getMonthValue() - 1;
            if (MovementTypes.INCOME.equalsIgnoreCase(movement.getType())) {
                income[index] += movement.getAmount();
            } else if (MovementTypes.EXPENSE.equalsIgnoreCase(movement.getType())) {
                expenses[index] += movement.getAmount();
            }
        }
        return new MonthlyTotals(income, expenses);
    }
}
