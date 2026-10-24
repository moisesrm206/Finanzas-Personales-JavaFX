package org.example.gestionfinanciera.services;

import javafx.concurrent.Task;
import org.example.gestionfinanciera.dao.CategoryDAO;
import org.example.gestionfinanciera.dao.MovementDAO;
import org.example.gestionfinanciera.models.Category;
import org.example.gestionfinanciera.models.Movement;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.example.gestionfinanciera.services.TaskUtil.task;

public class MovementServiceImpl implements MovementService {

    private final MovementDAO movementDAO;
    private final CategoryDAO categoryDAO;

    public MovementServiceImpl(MovementDAO movementDAO, CategoryDAO categoryDAO) {
        this.movementDAO = movementDAO;
        this.categoryDAO = categoryDAO;
    }

    @Override
    public Task<List<Movement>> loadMovementsTask(String userId) {
        return task(() -> movementDAO.findByUser(requireUser(userId)));
    }

    @Override
    public Task<List<Movement>> searchMovementsTask(String userId, MovementFilter filter) {
        return task(() -> {
            MovementFilter criteria = filter == null ? MovementFilter.none() : filter;
            Map<String, String> categoryNames = categoryNames(requireUser(userId));
            return movementDAO.findByUser(userId).stream()
                    .filter(movement -> criteria.matches(movement, categoryNames))
                    .toList();
        });
    }

    @Override
    public Task<Movement> addMovementTask(Movement movement) {
        return task(() -> {
            validate(movement);
            normalize(movement);
            return movementDAO.create(movement);
        });
    }

    @Override
    public Task<Void> updateMovementTask(Movement movement) {
        return task(() -> {
            validate(movement);
            ownedMovement(movement.getUserId(), movement.getMovementId());
            normalize(movement);
            movementDAO.update(movement);
            return null;
        });
    }

    @Override
    public Task<Void> deleteMovementTask(String userId, String movementId) {
        return task(() -> {
            ownedMovement(userId, movementId);
            movementDAO.delete(movementId);
            return null;
        });
    }

    @Override
    public Task<Map<String, Double>> loadExpenseDistributionTask(String userId, YearMonth month) {
        return task(() -> {
            Map<String, String> names = categoryNames(requireUser(userId));
            Map<String, Double> byCategoryId =
                    MovementStats.expensesByCategory(movementDAO.findByUser(userId), month);

            Map<String, Double> byName = new LinkedHashMap<>();
            byCategoryId.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(entry -> byName.merge(
                            names.getOrDefault(entry.getKey(), "Sin categoría"),
                            entry.getValue(), Double::sum));
            return byName;
        });
    }

    @Override
    public Task<MonthlyTotals> loadMonthlyTrendTask(String userId, int year) {
        return task(() -> MovementStats.monthlyTotals(movementDAO.findByUser(requireUser(userId)), year));
    }

    // ---------- Validación ----------

    private void validate(Movement movement) {
        if (movement == null) {
            throw new IllegalArgumentException("El movimiento no puede ser null.");
        }
        requireUser(movement.getUserId());

        if (movement.getAmount() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        if (movement.getConcept() == null || movement.getConcept().isBlank()) {
            throw new IllegalArgumentException("El concepto es obligatorio.");
        }
        if (movement.getDate() == null) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }
        if (movement.getType() == null || !MovementTypes.ALL.contains(movement.getType())) {
            throw new IllegalArgumentException("Elige el tipo de movimiento.");
        }
        if (movement.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Elige el método de pago.");
        }
        if (movement.getState() != null && !movement.getState().isBlank()
                && !MovementStates.ALL.contains(movement.getState())) {
            throw new IllegalArgumentException("El estado no es válido.");
        }
        if (movement.getOriginAccountId() == null || movement.getOriginAccountId().isBlank()) {
            throw new IllegalArgumentException("Elige la cuenta.");
        }
        if (MovementTypes.TRANSFER.equals(movement.getType())) {
            String destination = movement.getDestinationAccountId();
            if (destination == null || destination.isBlank()) {
                throw new IllegalArgumentException("Elige la cuenta destino de la transferencia.");
            }
            if (destination.equals(movement.getOriginAccountId())) {
                throw new IllegalArgumentException("La cuenta origen y la cuenta destino deben ser distintas.");
            }
        }

        Category category = categoryDAO.findById(movement.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Elige una categoría válida."));
        if (!movement.getUserId().equals(category.getUserId())) {
            throw new IllegalArgumentException("La categoría no pertenece al usuario.");
        }
    }

    /** Deja el movimiento en forma consistente antes de guardarlo. */
    private void normalize(Movement movement) {
        if (movement.getState() == null || movement.getState().isBlank()) {
            movement.setState(MovementStates.APPLIED);
        }
        if (!MovementTypes.TRANSFER.equals(movement.getType())) {
            movement.setDestinationAccountId(null); // la cuenta destino solo aplica a transferencias
        }
        movement.setConcept(movement.getConcept().trim());
        movement.setTags(normalizeTags(movement.getTags()));
    }

    /** Quita espacios y vacíos, agrega "#" al inicio, pasa a minúsculas y elimina repetidas. */
    private String[] normalizeTags(String[] tags) {
        if (tags == null) {
            return new String[0];
        }
        List<String> clean = new ArrayList<>();
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            String value = tag.trim().toLowerCase(Locale.ROOT);
            if (!value.startsWith("#")) {
                value = "#" + value;
            }
            if (value.length() > 1 && !clean.contains(value)) {
                clean.add(value);
            }
        }
        return clean.toArray(new String[0]);
    }

    private Movement ownedMovement(String userId, String movementId) {
        Movement movement = movementDAO.findById(movementId)
                .orElseThrow(() -> new IllegalArgumentException("El movimiento no existe."));
        if (userId == null || !userId.equals(movement.getUserId())) {
            throw new IllegalArgumentException("El movimiento no pertenece al usuario.");
        }
        return movement;
    }

    private Map<String, String> categoryNames(String userId) {
        Map<String, String> names = new HashMap<>();
        for (Category category : categoryDAO.findByUser(userId)) {
            names.put(category.getCategoryId(), category.getCategoryName());
        }
        return names;
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("No hay un usuario con sesión iniciada.");
        }
        return userId;
    }
}
