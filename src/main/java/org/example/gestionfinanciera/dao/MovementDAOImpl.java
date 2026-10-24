package org.example.gestionfinanciera.dao;

import com.google.cloud.firestore.DocumentSnapshot;
import org.example.gestionfinanciera.models.Movement;
import org.example.gestionfinanciera.models.Movement.PaymentMethod;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovementDAOImpl extends FirestoreBaseDAO<Movement> implements MovementDAO {

    private static final String COLLECTION = "movements";

    public MovementDAOImpl() {
        super(COLLECTION);
    }

    @Override
    public List<Movement> findByUser(String userId) {
        List<Movement> movements = query(collection.whereEqualTo("userId", userId));
        // El orden se hace en Java: ordenar en Firestore junto con el filtro por usuario
        // exigiría crear un índice compuesto en la consola.
        movements.sort(Comparator.comparing(Movement::getDate,
                Comparator.nullsLast(Comparator.<Date>reverseOrder())));
        return movements;
    }

    @Override
    public boolean existsByCategory(String userId, String categoryId) {
        return !query(collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("categoryId", categoryId)
                .limit(1)).isEmpty();
    }

    @Override
    public boolean existsByAccount(String userId, String accountId) {
        boolean asOrigin = !query(collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("originAccountId", accountId)
                .limit(1)).isEmpty();
        if (asOrigin) {
            return true;
        }
        return !query(collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("destinationAccountId", accountId)
                .limit(1)).isEmpty();
    }

    @Override
    protected Map<String, Object> toDocument(Movement movement) {
        Map<String, Object> data = new HashMap<>();
        data.put("movementId", movement.getMovementId());
        data.put("userId", movement.getUserId());
        data.put("categoryId", movement.getCategoryId());
        data.put("originAccountId", movement.getOriginAccountId());
        data.put("destinationAccountId", movement.getDestinationAccountId());
        data.put("amount", movement.getAmount());
        data.put("type", movement.getType());
        data.put("concept", movement.getConcept());
        data.put("date", movement.getDate());
        data.put("paymentMethod",
                movement.getPaymentMethod() == null ? null : movement.getPaymentMethod().name());
        data.put("state", movement.getState());
        // Firestore no admite arreglos (String[]), solo listas
        data.put("tags", movement.getTags() == null ? List.of() : Arrays.asList(movement.getTags()));
        return data;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Movement fromSnapshot(DocumentSnapshot snapshot) {
        List<String> tagList = (List<String>) snapshot.get("tags");
        String[] tags = tagList == null ? new String[0] : tagList.toArray(new String[0]);
        Double amount = snapshot.getDouble("amount");

        return new Movement(
                snapshot.getId(),
                snapshot.getString("categoryId"),
                snapshot.getString("originAccountId"),
                snapshot.getString("destinationAccountId"),
                snapshot.getString("userId"),
                amount == null ? 0 : amount,
                snapshot.getString("type"),
                snapshot.getString("concept"),
                parsePaymentMethod(snapshot.getString("paymentMethod")),
                snapshot.getDate("date"),
                snapshot.getString("state"),
                tags);
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null) {
            return null;
        }
        try {
            return PaymentMethod.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected String getId(Movement movement) {
        return movement.getMovementId();
    }

    @Override
    protected void setId(Movement movement, String id) {
        movement.setMovementId(id);
    }
}
