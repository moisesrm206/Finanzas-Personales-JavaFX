package org.example.gestionfinanciera.models;

public class Category {
    private String categoryId;
    private String categoryName;
    private String userId; // antes "userID": ahora se llama igual en todos los modelos

    public Category() {
    }

    public Category(String categoryId, String categoryName, String userId) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.userId = userId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
