package org.example.gestionfinanciera.dao;

import com.google.cloud.firestore.DocumentSnapshot;
import org.example.gestionfinanciera.models.Budget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BudgetDAOImpl extends FirestoreBaseDAO<Budget> implements BudgetDAO {

    private static final String COLLECTION = "budgets";

    public BudgetDAOImpl() {
        super(COLLECTION);
    }

    @Override
    public List<Budget> findByUser(String userId) {
        return query(collection.whereEqualTo("userId", userId));
    }

    @Override
    public List<Budget> findByUserAndMonth(String userId, int month) {
        return query(collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("month", month));
    }

    @Override
    public Optional<Budget> findByUserCategoryAndMonth(String userId, String categoryId, int month) {
        return query(collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("categoryId", categoryId)
                .whereEqualTo("month", month)
                .limit(1))
                .stream().findFirst();
    }

    @Override
    protected Map<String, Object> toDocument(Budget budget) {
        Map<String, Object> data = new HashMap<>();
        data.put("budgetId", budget.getBudgetId());
        data.put("userId", budget.getUserId());
        data.put("categoryId", budget.getCategoryId());
        data.put("limit", budget.getLimit());
        data.put("month", budget.getMonth());
        return data;
    }

    @Override
    protected Budget fromSnapshot(DocumentSnapshot snapshot) {
        Double limit = snapshot.getDouble("limit");
        Long month = snapshot.getLong("month");
        return new Budget(
                snapshot.getId(),
                snapshot.getString("userId"),
                snapshot.getString("categoryId"),
                limit == null ? 0 : limit,
                month == null ? 0 : month.intValue());
    }

    @Override
    protected String getId(Budget budget) {
        return budget.getBudgetId();
    }

    @Override
    protected void setId(Budget budget, String id) {
        budget.setBudgetId(id);
    }
}
