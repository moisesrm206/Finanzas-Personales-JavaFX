package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.models.Movement;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public interface MovementService {

    /** Todos los movimientos del usuario, del más reciente al más antiguo. */
    Task<List<Movement>> loadMovementsTask(String userId);

    /** Movimientos que cumplen el filtro (texto, categoría, tipo, estado, etiqueta, fechas...). */
    Task<List<Movement>> searchMovementsTask(String userId, MovementFilter filter);

    Task<Movement> addMovementTask(Movement movement);

    Task<Void> updateMovementTask(Movement movement);

    Task<Void> deleteMovementTask(String userId, String movementId);

    /** Gráfico de pastel: nombre de categoría -> total gastado en el mes (de mayor a menor). */
    Task<Map<String, Double>> loadExpenseDistributionTask(String userId, YearMonth month);

    /** Gráfico de líneas: ingresos y gastos de cada mes del año. */
    Task<MonthlyTotals> loadMonthlyTrendTask(String userId, int year);
}
