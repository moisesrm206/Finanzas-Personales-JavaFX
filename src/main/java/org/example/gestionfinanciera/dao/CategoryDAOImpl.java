package org.example.gestionfinanciera.dao;

import com.google.cloud.firestore.DocumentSnapshot;
import org.example.gestionfinanciera.models.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryDAOImpl extends FirestoreBaseDAO<Category> implements CategoryDAO {

    private static final String COLLECTION = "categories";

    public CategoryDAOImpl() {
        super(COLLECTION);
    }

    @Override
    public List<Category> findByUser(String userId) {
        return query(collection.whereEqualTo("userId", userId));
    }

    @Override
    protected Map<String, Object> toDocument(Category category) {
        Map<String, Object> data = new HashMap<>();
        data.put("categoryId", category.getCategoryId());
        data.put("userId", category.getUserId());
        data.put("categoryName", category.getCategoryName());
        return data;
    }

    @Override
    protected Category fromSnapshot(DocumentSnapshot snapshot) {
        return new Category(
                snapshot.getId(),
                snapshot.getString("categoryName"),
                snapshot.getString("userId"));
    }

    @Override
    protected String getId(Category category) {
        return category.getCategoryId();
    }

    @Override
    protected void setId(Category category, String id) {
        category.setCategoryId(id);
    }
}
