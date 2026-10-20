package org.example.gestionfinanciera.models;

public class Category {
    int categoryId;
    String categoryName;
    int userID;

    public Category(int categoryId, String categoryName, int userID) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.userID = userID;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }


}
